package dev.andre.nutritionist.profile.outbox.cleanup

import dev.andre.nutritionist.profile.outbox.repository.OutboxJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant

/**
 * Результат выполнения стратегии очистки Outbox таблицы.
 */
sealed class CleanupResult {
    data class Success(
        val deletedCount: Int,
        val duration: Duration,
    ) : CleanupResult()

    data class Failure(
        val throwable: Throwable,
        val duration: Duration,
    ) : CleanupResult()
}

/**
 * Тип стратегии очистки Outbox.
 */
enum class CleanupStrategyType {
    BATCH,
    DIRECT,
    PARTITION,
}

/**
 * Интерфейс стратегии очистки опубликованных сообщений Transactional Outbox (AGENTS.md §4.3).
 */
interface OutboxCleanupStrategy {
    val type: CleanupStrategyType

    fun cleanup(cutoff: Instant): CleanupResult
}

/**
 * Стратегия пакетной очистки пачками по [batchSize] в отдельных коротких транзакциях.
 * Рекомендуется для High-Load: исключает длительные блокировки строк и перегрузку WAL.
 */
@Component
class BatchChunkCleanupStrategy(
    private val outboxRepository: OutboxJpaRepository,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${outbox.cleanup.batch-size:1000}") private val batchSize: Int = 1000,
) : OutboxCleanupStrategy {
    override val type: CleanupStrategyType = CleanupStrategyType.BATCH
    private val logger = LoggerFactory.getLogger(BatchChunkCleanupStrategy::class.java)

    override fun cleanup(cutoff: Instant): CleanupResult {
        val start = System.nanoTime()
        var totalDeleted = 0
        return try {
            while (true) {
                val deleted =
                    transactionTemplate.execute {
                        outboxRepository.deletePublishedBeforeBatch(cutoff, batchSize)
                    }
                totalDeleted += deleted
                logger.debug("Deleted outbox chunk of {} messages (total so far: {})", deleted, totalDeleted)
                if (deleted < batchSize) {
                    break
                }
            }
            CleanupResult.Success(totalDeleted, Duration.ofNanos(System.nanoTime() - start))
        } catch (e: Throwable) {
            logger.error("Error during batch chunk outbox cleanup after deleting {} records", totalDeleted, e)
            CleanupResult.Failure(e, Duration.ofNanos(System.nanoTime() - start))
        }
    }
}

/**
 * Стратегия прямого единовременного DELETE для небольших таблиц.
 */
@Component
class DirectDeleteCleanupStrategy(
    private val outboxRepository: OutboxJpaRepository,
) : OutboxCleanupStrategy {
    override val type: CleanupStrategyType = CleanupStrategyType.DIRECT
    private val logger = LoggerFactory.getLogger(DirectDeleteCleanupStrategy::class.java)

    override fun cleanup(cutoff: Instant): CleanupResult {
        val start = System.nanoTime()
        return try {
            val deleted = outboxRepository.deletePublishedBefore(cutoff)
            CleanupResult.Success(deleted, Duration.ofNanos(System.nanoTime() - start))
        } catch (e: Throwable) {
            logger.error("Error during direct outbox cleanup", e)
            CleanupResult.Failure(e, Duration.ofNanos(System.nanoTime() - start))
        }
    }
}

/**
 * Стратегия очистки для партиционированных таблиц (DROP PARTITION).
 */
@Component
class PartitionDropCleanupStrategy : OutboxCleanupStrategy {
    override val type: CleanupStrategyType = CleanupStrategyType.PARTITION
    private val logger = LoggerFactory.getLogger(PartitionDropCleanupStrategy::class.java)

    override fun cleanup(cutoff: Instant): CleanupResult {
        val start = System.nanoTime()
        logger.info("Partition drop cleanup strategy invoked for cutoff={}. Partitioning not active, skipping.", cutoff)
        return CleanupResult.Success(0, Duration.ofNanos(System.nanoTime() - start))
    }
}
