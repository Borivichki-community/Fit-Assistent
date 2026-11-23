package dev.andre.nutritionist.diary.grpc.idempotency

import dev.andre.nutritionist.diary.domain.model.IdempotencyKey
import dev.andre.nutritionist.diary.domain.model.UserId
import java.time.Duration

interface IdempotencyStore {
    fun tryInsert(
        scope: IdempotencyScope,
        requestHash: String,
        ttl: Duration,
    ): Boolean

    fun findForUpdate(scope: IdempotencyScope): StoredIdempotencyResponse?

    fun complete(
        scope: IdempotencyScope,
        responseBytes: ByteArray,
    )

    fun deleteExpired(limit: Int): Int

    data class IdempotencyScope(
        val userId: UserId,
        val rpcMethod: String,
        val key: IdempotencyKey,
    ) {
        init {
            require(userId.raw.isNotBlank() && userId.raw.length <= 64)
            require(rpcMethod.isNotBlank() && rpcMethod.length <= 255)
            require(key.raw.isNotBlank() && key.raw.length <= 64)
        }
    }

    class StoredIdempotencyResponse(
        val requestHash: String,
        val responseBytes: ByteArray?,
    )
}
