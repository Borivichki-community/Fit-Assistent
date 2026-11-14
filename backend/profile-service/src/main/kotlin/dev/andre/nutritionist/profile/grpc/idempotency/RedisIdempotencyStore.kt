package dev.andre.nutritionist.profile.grpc.idempotency

import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Боевая реализация хранилища ответов для обеспечения идемпотентности через Redis
 * (AGENTS.md §4.4, spring-grpc-service/SKILL.md §8).
 *
 * Сохраняет сериализованные байты ответов с настраиваемым TTL (по умолчанию 24 часа).
 */
@Component
@Primary
class RedisIdempotencyStore(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
) : IdempotencyStore {
    override fun get(key: String): ByteArray? = redisTemplate.opsForValue().get(formatKey(key))

    override fun put(
        key: String,
        responseBytes: ByteArray,
    ) {
        redisTemplate.opsForValue().set(formatKey(key), responseBytes, DEFAULT_TTL)
    }

    private fun formatKey(key: String): String = KEY_PREFIX + key

    companion object {
        const val KEY_PREFIX = "idempotency:"
        val DEFAULT_TTL: Duration = Duration.ofHours(24)
    }
}
