package dev.andre.nutritionist.profile.grpc.idempotency

/**
 * Хранилище ответов для обеспечения идемпотентности мутирующих gRPC-методов
 * (AGENTS.md §4.4, spring-grpc-service/SKILL.md §8).
 * Реализуется через Redis с TTL (24 часа) в [RedisIdempotencyStore].
 */
interface IdempotencyStore {
    fun get(key: String): ByteArray?

    fun put(
        key: String,
        responseBytes: ByteArray,
    )
}
