package dev.andre.nutritionist.profile.grpc.idempotency

/**
 * Результат проверки и атомарного захвата ключа идемпотентности.
 */
sealed interface IdempotencyResult {
    /** Ключ успешно захвачен со статусом PROCESSING, выполнение запроса разрешено */
    data object Acquired : IdempotencyResult

    /** Запрос с данным ключом уже выполняется в данный момент (защита от гонок) */
    data object InProgress : IdempotencyResult

    /** Запрос с данным ключом уже был успешно завершён ранее, возвращаем сохранённый ответ */
    class Cached(
        val responseBytes: ByteArray,
    ) : IdempotencyResult

    /** Ключ уже был использован ранее, но с другим телом запроса (хэш не совпадает) */
    data object PayloadMismatch : IdempotencyResult
}

/**
 * Хранилище состояний и ответов для обеспечения идемпотентности мутирующих gRPC-методов
 * (AGENTS.md §4.4, 02-backend-stack.md §7.1 и spring-grpc-service/SKILL.md §8).
 * Реализуется через Redis с lock TTL (60 секунд) и результатом TTL (24 часа) в [RedisIdempotencyStore].
 */
interface IdempotencyStore {
    /**
     * Попытка атомарного захвата ключа со статусом PROCESSING или извлечение закэшированного ответа.
     */
    fun tryAcquire(
        key: String,
        requestHash: ByteArray,
    ): IdempotencyResult

    /**
     * Фиксация успешного завершения запроса со статусом COMPLETED и сохранением байтов ответа.
     */
    fun complete(
        key: String,
        requestHash: ByteArray,
        responseBytes: ByteArray,
    )

    /**
     * Освобождение ключа (удаление) при сбое или отмене запроса для разрешения повтора.
     */
    fun release(key: String)

    /**
     * Получение сохранённого ответа (если статус COMPLETED) для совместимости и тестов.
     */
    fun get(key: String): ByteArray?

    /**
     * Упрощённое сохранение ответа для обратной совместимости.
     */
    fun put(
        key: String,
        responseBytes: ByteArray,
    )
}
