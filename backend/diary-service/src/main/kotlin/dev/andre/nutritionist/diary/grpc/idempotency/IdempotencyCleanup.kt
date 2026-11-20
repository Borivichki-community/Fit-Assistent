package dev.andre.nutritionist.diary.grpc.idempotency

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class IdempotencyCleanup(
    private val store: IdempotencyStore,
    @Value("\${diary.idempotency.cleanup-batch-size:1000}") private val batchSize: Int,
) {
    private val logger = LoggerFactory.getLogger(IdempotencyCleanup::class.java)

    init {
        require(batchSize > 0)
    }

    @Scheduled(
        initialDelayString = "\${diary.idempotency.cleanup-interval:PT1H}",
        fixedDelayString = "\${diary.idempotency.cleanup-interval:PT1H}",
    )
    fun cleanUp() {
        val deleted = store.deleteExpired(batchSize)
        logger.info("Expired idempotency responses removed: count={}", deleted)
    }
}
