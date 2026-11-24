package dev.andre.nutritionist.profile.outbox.processor

import dev.andre.nutritionist.profile.outbox.cleanup.CleanupResult
import dev.andre.nutritionist.profile.outbox.cleanup.OutboxCleanupStrategyFactory
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Фоновая задача периодической очистки таблицы outbox_messages (AGENTS.md §4.3).
 *
 * Делегирует выполнение выбранной стратегии ([OutboxCleanupStrategyFactory]), обеспечивает
 * безопасную обработку ошибок, экспоненциальный бэкофф при сбоях и сбор метрик Micrometer.
 */
@Component
@ConditionalOnProperty(name = ["outbox.cleanup.enabled"], havingValue = "true", matchIfMissing = true)
class OutboxCleanupJob(
    private val strategyFactory: OutboxCleanupStrategyFactory,
    meterRegistry: MeterRegistry,
    @Value("\${outbox.cleanup.retention-days:7}") private val retentionDays: Long = 7,
    @Value("\${outbox.cleanup.max-retries:3}") private val maxRetries: Int = 3,
    @Value("\${outbox.cleanup.retry-backoff-ms:1000}") private val retryBackoffMs: Long = 1000,
) {
    private val logger = LoggerFactory.getLogger(OutboxCleanupJob::class.java)

    private val deletedCounter: Counter =
        Counter
            .builder("outbox.cleanup.deleted.records")
            .description("Number of deleted outbox messages during cleanup")
            .register(meterRegistry)

    private val failureCounter: Counter =
        Counter
            .builder("outbox.cleanup.failures")
            .description("Number of failed outbox cleanup attempts")
            .register(meterRegistry)

    private val cleanupTimer: Timer =
        Timer
            .builder("outbox.cleanup.duration")
            .description("Duration of outbox cleanup operation")
            .register(meterRegistry)

    /**
     * Запуск по расписанию (по умолчанию раз в сутки в 03:00 ночи).
     */
    @Scheduled(cron = "\${outbox.cleanup.cron:0 0 3 * * ?}")
    fun runScheduledCleanup(): CleanupResult = cleanupOldMessagesWithRetry()

    /**
     * Очистка устаревших PUBLISHED сообщений с отсечкой по времени.
     * Возвращает количество удалённых записей для обратной совместимости.
     */
    fun cleanupOldMessages(): Int =
        when (val result = cleanupOldMessagesWithRetry()) {
            is CleanupResult.Success -> result.deletedCount
            is CleanupResult.Failure -> 0
        }

    /**
     * Выполнение очистки с экспоненциальным бэкоффом при сбоях.
     */
    fun cleanupOldMessagesWithRetry(): CleanupResult {
        val strategy = strategyFactory.getStrategy()
        val cutoff = Instant.now().minus(Duration.ofDays(retentionDays))
        logger.info(
            "Starting outbox cleanup using strategy '{}' for messages older than {} days (cutoff: {})",
            strategy.type,
            retentionDays,
            cutoff,
        )

        var attempt = 0
        var currentBackoff = retryBackoffMs

        while (attempt < maxRetries) {
            attempt++
            val sample = Timer.start()
            val result =
                try {
                    strategy.cleanup(cutoff)
                } catch (e: Exception) {
                    CleanupResult.Failure(e, Duration.ZERO)
                }
            sample.stop(cleanupTimer)

            when (result) {
                is CleanupResult.Success -> {
                    deletedCounter.increment(result.deletedCount.toDouble())
                    logger.info(
                        "Outbox cleanup completed successfully on attempt {}/{}: removed {} messages in {}ms",
                        attempt,
                        maxRetries,
                        result.deletedCount,
                        result.duration.toMillis(),
                    )
                    return result
                }

                is CleanupResult.Failure -> {
                    failureCounter.increment()
                    logger.warn(
                        "Outbox cleanup attempt {}/{} failed: {}",
                        attempt,
                        maxRetries,
                        result.throwable.message,
                        result.throwable,
                    )

                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(currentBackoff)
                            currentBackoff *= 2
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            return result
                        }
                    } else {
                        logger.error("Outbox cleanup exhausted all {} attempts. Operation aborted.", maxRetries)
                        return result
                    }
                }
            }
        }
        return CleanupResult.Failure(IllegalStateException("Cleanup retries exhausted"), Duration.ZERO)
    }
}
