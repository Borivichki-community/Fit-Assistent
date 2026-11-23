package dev.andre.nutritionist.profile.grpc.idempotency

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
 */
@Component
@Primary
class RedisIdempotencyStore(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
) : IdempotencyStore {
    override fun tryAcquire(
        key: String,
        requestHash: ByteArray,
    ): IdempotencyResult {
        require(requestHash.size == HASH_SIZE) { "requestHash must be $HASH_SIZE bytes (SHA-256)" }
        val formattedKey = formatKey(key)
        val processingValue = byteArrayOf(STATUS_PROCESSING) + requestHash

        val acquired = redisTemplate.opsForValue().setIfAbsent(formattedKey, processingValue, LOCK_TTL) == true
        return if (acquired) {
            IdempotencyResult.Acquired
        } else {
            resolveExistingKey(formattedKey, processingValue, requestHash)
        }
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

    override fun complete(
        key: String,
        requestHash: ByteArray,
        responseBytes: ByteArray,
    ) {
        require(requestHash.size == HASH_SIZE) { "requestHash must be $HASH_SIZE bytes (SHA-256)" }
        val formattedKey = formatKey(key)
        val completedValue = byteArrayOf(STATUS_COMPLETED) + requestHash + responseBytes
        redisTemplate.opsForValue().set(formattedKey, completedValue, DEFAULT_TTL)
    }

    override fun release(key: String) {
        redisTemplate.delete(formatKey(key))
    }

    override fun get(key: String): ByteArray? {
        val existingBytes = redisTemplate.opsForValue().get(formatKey(key)) ?: return null
        return extractCompletedResponse(existingBytes)
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
