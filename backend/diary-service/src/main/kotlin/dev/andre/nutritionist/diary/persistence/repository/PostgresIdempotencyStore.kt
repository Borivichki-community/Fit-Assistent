package dev.andre.nutritionist.diary.persistence.repository

import dev.andre.nutritionist.diary.grpc.idempotency.IdempotencyStore
import dev.andre.nutritionist.diary.grpc.idempotency.IdempotencyStore.IdempotencyScope
import dev.andre.nutritionist.diary.grpc.idempotency.IdempotencyStore.StoredIdempotencyResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration

@Repository
class PostgresIdempotencyStore(
    private val jdbc: JdbcTemplate,
) : IdempotencyStore {
    override fun tryInsert(
        scope: IdempotencyScope,
        requestHash: String,
        ttl: Duration,
    ): Boolean {
        check(TransactionSynchronizationManager.isActualTransactionActive())
        return jdbc.update(
            """
            INSERT INTO idempotency_keys (user_id, rpc_method, idempotency_key, request_hash, expires_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP + (? * INTERVAL '1 second'))
            ON CONFLICT (user_id, rpc_method, idempotency_key) DO NOTHING
            """.trimIndent(),
            scope.userId.raw,
            scope.rpcMethod,
            scope.key.raw,
            requestHash,
            ttl.seconds,
        ) == 1
    }

    override fun findForUpdate(scope: IdempotencyScope): StoredIdempotencyResponse? {
        check(TransactionSynchronizationManager.isActualTransactionActive())
        return jdbc
            .query(
                """
                SELECT request_hash, response_bytes FROM idempotency_keys
                WHERE user_id = ? AND rpc_method = ? AND idempotency_key = ?
                FOR UPDATE
                """.trimIndent(),
                { row, _ -> StoredIdempotencyResponse(row.getString("request_hash"), row.getBytes("response_bytes")) },
                scope.userId.raw,
                scope.rpcMethod,
                scope.key.raw,
            ).singleOrNull()
    }

    override fun complete(
        scope: IdempotencyScope,
        responseBytes: ByteArray,
    ) {
        check(TransactionSynchronizationManager.isActualTransactionActive())
        val updated =
            jdbc.update(
                """
                UPDATE idempotency_keys SET response_bytes = ?
                WHERE user_id = ? AND rpc_method = ? AND idempotency_key = ?
                """.trimIndent(),
                responseBytes,
                scope.userId.raw,
                scope.rpcMethod,
                scope.key.raw,
            )
        check(updated == 1) { "Idempotency record was not found" }
    }

    override fun deleteExpired(limit: Int): Int {
        require(limit > 0)
        return jdbc.update(
            """
            DELETE FROM idempotency_keys
            WHERE (user_id, rpc_method, idempotency_key) IN (
                SELECT user_id, rpc_method, idempotency_key FROM idempotency_keys
                WHERE expires_at <= CURRENT_TIMESTAMP
                ORDER BY expires_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            """.trimIndent(),
            limit,
        )
    }
}
