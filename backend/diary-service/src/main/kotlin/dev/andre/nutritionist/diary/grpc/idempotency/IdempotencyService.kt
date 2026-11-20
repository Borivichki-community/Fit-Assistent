package dev.andre.nutritionist.diary.grpc.idempotency

import dev.andre.nutritionist.diary.grpc.idempotency.IdempotencyStore.IdempotencyScope
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat

@Service
class IdempotencyService(
    private val store: IdempotencyStore,
    transactionManager: PlatformTransactionManager,
    @Value("\${diary.idempotency.ttl:PT24H}") private val ttl: Duration,
) {
    private val logger = LoggerFactory.getLogger(IdempotencyService::class.java)
    private val transaction =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
            isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
        }

    init {
        require(ttl.seconds > 0) { "Idempotency TTL must be at least one second" }
    }

    fun execute(
        scope: IdempotencyScope,
        requestBytes: ByteArray,
        operation: () -> ByteArray,
    ): ByteArray {
        val hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(requestBytes))
        return checkNotNull(
            transaction.execute {
                executeInTransaction(scope, hash, operation)
            },
        )
    }

    private fun executeInTransaction(
        scope: IdempotencyScope,
        requestHash: String,
        operation: () -> ByteArray,
    ): ByteArray {
        repeat(MAX_CLAIM_ATTEMPTS) {
            if (store.tryInsert(scope, requestHash, ttl)) {
                val response = operation()
                store.complete(scope, response)
                logger.info("Idempotency operation completed: method={}", scope.rpcMethod)
                return response
            }

            val stored = store.findForUpdate(scope)
            if (stored != null) {
                if (stored.requestHash != requestHash) {
                    logger.warn("Idempotency request conflict: method={}", scope.rpcMethod)
                    throw IdempotencyConflictException()
                }
                logger.info("Idempotency response replayed: method={}", scope.rpcMethod)
                return checkNotNull(stored.responseBytes) { "Idempotency response is incomplete" }
            }
        }
        error("Idempotency record expired concurrently; retry the request")
    }

    class IdempotencyConflictException : RuntimeException("Idempotency key was already used with a different request")

    companion object {
        private const val MAX_CLAIM_ATTEMPTS = 3
    }
}
