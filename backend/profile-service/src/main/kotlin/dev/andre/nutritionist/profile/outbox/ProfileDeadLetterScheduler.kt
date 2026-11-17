package dev.andre.nutritionist.profile.outbox

import dev.andre.nutritionist.outbox.OutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import java.sql.SQLException

class ProfileDeadLetterScheduler(
    private val processor: OutboxProcessor,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${profile.calories.dlt-outbox.poll-interval:PT1S}")
    fun publish() {
        try {
            processor.processBatch()
        } catch (exception: SQLException) {
            logger.warn("Profile DLT database unavailable: {}", exception.javaClass.simpleName)
        } catch (exception: IllegalStateException) {
            logger.warn("Profile DLT ownership unavailable: {}", exception.javaClass.simpleName)
        }
    }
}
