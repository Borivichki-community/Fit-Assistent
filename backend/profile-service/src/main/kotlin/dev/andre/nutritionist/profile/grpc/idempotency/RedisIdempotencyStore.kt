package dev.andre.nutritionist.profile.grpc.idempotency

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Боевая реализация хранилища идемпотентности через Redis со State Machine
 * (AGENTS.md §4.4, spring-grpc-service/SKILL.md §8).
 *
 * Формат бинарного значения в Redis:
 * - Байт 0: статус (1 = [STATUS_PROCESSING], 2 = [STATUS_COMPLETED])
 * - Байты 1..32: SHA-256 хэш запроса (32 байта)
 * - Байты 33..конец: сериализованный protobuf-ответ (при [STATUS_COMPLETED])
 *
 * Обеспечивает:
 * 1. Атомарное занятие ключа через SETNX с TTL 60 сек (защита от гонок и зависания).
 * 2. Обнаружение одновременных запросов (конкурентные запросы получают [IdempotencyResult.InProgress]).
 * 3. Валидацию хэша payload (при попытке повторить с другими данными — [IdempotencyResult.PayloadMismatch]).
 * 4. Сохранение успешного результата с TTL 24 часа.
 * 5. Удаление ключа при ошибке сервиса для возможности повтора.
 * 6. Отказоустойчивость: при отказе Redis срабатывает CircuitBreaker и сервис переходит
 *    в режим graceful degradation, допуская выполнение запроса с опорой на уникальные ограничения БД.
 */
@Component
@Primary
class RedisIdempotencyStore(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
    private val circuitBreakerRegistry: CircuitBreakerRegistry? = null,
) : IdempotencyStore {
    private val logger = LoggerFactory.getLogger(RedisIdempotencyStore::class.java)
    private val circuitBreaker = circuitBreakerRegistry?.circuitBreaker("redis-idempotency")

    @CircuitBreaker(name = "redis-idempotency", fallbackMethod = "fallbackTryAcquire")
    override fun tryAcquire(
        key: String,
        requestHash: ByteArray,
    ): IdempotencyResult {
        require(requestHash.size == HASH_SIZE) { "requestHash must be $HASH_SIZE bytes (SHA-256)" }
        return try {
            if (circuitBreaker != null) {
                circuitBreaker.executeSupplier { executeTryAcquire(key, requestHash) }
            } else {
                executeTryAcquire(key, requestHash)
            }
        } catch (ex: Throwable) {
            fallbackTryAcquire(key, requestHash, ex)
        }
    }

    private fun executeTryAcquire(
        key: String,
        requestHash: ByteArray,
    ): IdempotencyResult {
        val formattedKey = formatKey(key)
        val processingValue = byteArrayOf(STATUS_PROCESSING) + requestHash

        val acquired = redisTemplate.opsForValue().setIfAbsent(formattedKey, processingValue, LOCK_TTL) == true
        return if (acquired) {
            IdempotencyResult.Acquired
        } else {
            resolveExistingKey(formattedKey, processingValue, requestHash)
        }
    }

    @Suppress("UnusedPrivateMember")
    fun fallbackTryAcquire(
        key: String,
        requestHash: ByteArray,
        ex: Throwable,
    ): IdempotencyResult {
        logger.warn(
            "Redis idempotency store unavailable (circuit breaker open or connection failure). " +
                "Proceeding in degraded mode without Redis idempotency for key: {}. Error: {}",
            key,
            ex.message,
        )
        return IdempotencyResult.Acquired
    }

    private fun resolveExistingKey(
        formattedKey: String,
        processingValue: ByteArray,
        requestHash: ByteArray,
    ): IdempotencyResult {
        val existingBytes = redisTemplate.opsForValue().get(formattedKey)
        return if (existingBytes == null) {
            retryAcquire(formattedKey, processingValue)
        } else {
            parseExistingBytes(existingBytes, requestHash)
        }
    }

    private fun retryAcquire(
        formattedKey: String,
        processingValue: ByteArray,
    ): IdempotencyResult {
        val acquired = redisTemplate.opsForValue().setIfAbsent(formattedKey, processingValue, LOCK_TTL) == true
        return if (acquired) IdempotencyResult.Acquired else IdempotencyResult.InProgress
    }

    private fun parseExistingBytes(
        existingBytes: ByteArray,
        requestHash: ByteArray,
    ): IdempotencyResult {
        val validHeader =
            existingBytes.size >= HEADER_SIZE &&
                existingBytes.copyOfRange(1, HEADER_SIZE).contentEquals(requestHash)

        return if (!validHeader) {
            IdempotencyResult.PayloadMismatch
        } else {
            when (existingBytes[0]) {
                STATUS_PROCESSING -> IdempotencyResult.InProgress
                STATUS_COMPLETED -> IdempotencyResult.Cached(existingBytes.copyOfRange(HEADER_SIZE, existingBytes.size))
                else -> IdempotencyResult.PayloadMismatch
            }
        }
    }

    @CircuitBreaker(name = "redis-idempotency", fallbackMethod = "fallbackComplete")
    override fun complete(
        key: String,
        requestHash: ByteArray,
        responseBytes: ByteArray,
    ) {
        require(requestHash.size == HASH_SIZE) { "requestHash must be $HASH_SIZE bytes (SHA-256)" }
        try {
            if (circuitBreaker != null) {
                circuitBreaker.executeRunnable { executeComplete(key, requestHash, responseBytes) }
            } else {
                executeComplete(key, requestHash, responseBytes)
            }
        } catch (ex: Throwable) {
            fallbackComplete(key, requestHash, responseBytes, ex)
        }
    }

    private fun executeComplete(
        key: String,
        requestHash: ByteArray,
        responseBytes: ByteArray,
    ) {
        val formattedKey = formatKey(key)
        val completedValue = byteArrayOf(STATUS_COMPLETED) + requestHash + responseBytes
        redisTemplate.opsForValue().set(formattedKey, completedValue, DEFAULT_TTL)
    }

    @Suppress("UnusedPrivateMember")
    fun fallbackComplete(
        key: String,
        requestHash: ByteArray,
        responseBytes: ByteArray,
        ex: Throwable,
    ) {
        logger.warn("Failed to complete idempotency key [{}] in Redis (circuit open or error): {}", key, ex.message)
    }

    @CircuitBreaker(name = "redis-idempotency", fallbackMethod = "fallbackRelease")
    override fun release(key: String) {
        try {
            if (circuitBreaker != null) {
                circuitBreaker.executeRunnable { executeRelease(key) }
            } else {
                executeRelease(key)
            }
        } catch (ex: Throwable) {
            fallbackRelease(key, ex)
        }
    }

    private fun executeRelease(key: String) {
        redisTemplate.delete(formatKey(key))
    }

    @Suppress("UnusedPrivateMember")
    fun fallbackRelease(
        key: String,
        ex: Throwable,
    ) {
        logger.warn("Failed to release idempotency key [{}] in Redis (circuit open or error): {}", key, ex.message)
    }

    @CircuitBreaker(name = "redis-idempotency", fallbackMethod = "fallbackGet")
    override fun get(key: String): ByteArray? =
        try {
            if (circuitBreaker != null) {
                circuitBreaker.executeSupplier { executeGet(key) }
            } else {
                executeGet(key)
            }
        } catch (ex: Throwable) {
            fallbackGet(key, ex)
        }

    private fun executeGet(key: String): ByteArray? {
        val existingBytes = redisTemplate.opsForValue().get(formatKey(key)) ?: return null
        return extractCompletedResponse(existingBytes)
    }

    @Suppress("UnusedPrivateMember")
    fun fallbackGet(
        key: String,
        ex: Throwable,
    ): ByteArray? {
        logger.warn("Failed to get idempotency key [{}] from Redis (circuit open or error): {}", key, ex.message)
        return null
    }

    private fun extractCompletedResponse(existingBytes: ByteArray): ByteArray? =
        if (existingBytes.size >= HEADER_SIZE && existingBytes[0] == STATUS_COMPLETED) {
            existingBytes.copyOfRange(HEADER_SIZE, existingBytes.size)
        } else {
            null
        }

    override fun put(
        key: String,
        responseBytes: ByteArray,
    ) {
        complete(key, ByteArray(HASH_SIZE), responseBytes)
    }

    private fun formatKey(key: String): String = KEY_PREFIX + key

    companion object {
        const val KEY_PREFIX = "idempotency:"
        const val STATUS_PROCESSING: Byte = 1
        const val STATUS_COMPLETED: Byte = 2
        const val HASH_SIZE = 32
        const val HEADER_SIZE = 1 + HASH_SIZE
        val LOCK_TTL: Duration = Duration.ofMinutes(1)
        val DEFAULT_TTL: Duration = Duration.ofHours(24)
    }
}
