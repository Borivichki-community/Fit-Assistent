package dev.andre.nutritionist.profile.grpc.idempotency

/**
 * Хранилище ответов для обеспечения идемпотентности мутирующих gRPC-методов (AGENTS.md §4.4).
 * В продакшене реализуется через Redis с TTL, в тестах/локально — через InMemoryIdempotencyStore.
 */
interface IdempotencyStore {
    fun get(key: String): ByteArray?

    fun put(
        key: String,
        responseBytes: ByteArray,
    )
}
