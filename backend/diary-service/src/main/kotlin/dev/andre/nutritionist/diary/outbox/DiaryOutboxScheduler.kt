package dev.andre.nutritionist.diary.outbox

import dev.andre.nutritionist.outbox.OutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "diary.outbox", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class DiaryOutboxScheduler(
    private val processor: OutboxProcessor,
) {
    private val logger = LoggerFactory.getLogger(DiaryOutboxScheduler::class.java)

    @Scheduled(fixedDelayString = "\${diary.outbox.poll-interval:PT1S}", initialDelayString = "\${diary.outbox.poll-interval:PT1S}")
    fun publishPending() {
        try {
            processor.processBatch()
        } catch (exception: Exception) {
            logger.error("Outbox batch stopped; pending messages remain durable", exception)
        }
    }
}
