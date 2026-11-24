package dev.andre.nutritionist.profile.outbox

import dev.andre.nutritionist.profile.AbstractPostgresIntegrationTest
import dev.andre.nutritionist.profile.outbox.entity.OutboxMessageEntity
import dev.andre.nutritionist.profile.outbox.model.OutboxStatus
import dev.andre.nutritionist.profile.outbox.processor.OutboxCleanupJob
import dev.andre.nutritionist.profile.outbox.repository.OutboxJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant
import java.util.UUID

class OutboxCleanupJobTest : AbstractPostgresIntegrationTest() {
    @Autowired
    private lateinit var outboxRepository: OutboxJpaRepository

    @Autowired
    private lateinit var transactionTemplate: org.springframework.transaction.support.TransactionTemplate

    @Autowired
    private lateinit var outboxCleanupJob: OutboxCleanupJob

    @BeforeEach
    fun cleanDatabase() {
        outboxRepository.deleteAll()
    }

    @Test
    fun `cleanupOldMessages deletes only PUBLISHED messages older than retention days`() {
        val now = Instant.now()
        val eightDaysAgo = now.minus(Duration.ofDays(8))
        val tenDaysAgo = now.minus(Duration.ofDays(10))
        val oneDayAgo = now.minus(Duration.ofDays(1))

        // 1. Старое PUBLISHED сообщение (8 дней назад) -> должно быть удалено
        val oldPublished1 =
            OutboxMessageEntity(
                id = UUID.randomUUID(),
                aggregateType = "UserProfile",
                aggregateId = "user_old_1",
                topic = "user.registered",
                payload = byteArrayOf(1, 2, 3),
                status = OutboxStatus.PUBLISHED,
                createdAt = eightDaysAgo,
                publishedAt = eightDaysAgo,
            )

        // 2. Очень старое PUBLISHED сообщение (10 дней назад) -> должно быть удалено
        val oldPublished2 =
            OutboxMessageEntity(
                id = UUID.randomUUID(),
                aggregateType = "UserProfile",
                aggregateId = "user_old_2",
                topic = "user.registered",
                payload = byteArrayOf(4, 5, 6),
                status = OutboxStatus.PUBLISHED,
                createdAt = tenDaysAgo,
                publishedAt = tenDaysAgo,
            )

        // 3. Свежее PUBLISHED сообщение (1 день назад) -> должно остаться
        val freshPublished =
            OutboxMessageEntity(
                id = UUID.randomUUID(),
                aggregateType = "UserProfile",
                aggregateId = "user_fresh",
                topic = "user.registered",
                payload = byteArrayOf(7, 8, 9),
                status = OutboxStatus.PUBLISHED,
                createdAt = oneDayAgo,
                publishedAt = oneDayAgo,
            )

        // 4. Старое PENDING сообщение (10 дней назад) -> должно остаться (не опубликовано)
        val oldPending =
            OutboxMessageEntity(
                id = UUID.randomUUID(),
                aggregateType = "UserProfile",
                aggregateId = "user_pending",
                topic = "user.registered",
                payload = byteArrayOf(10, 11),
                status = OutboxStatus.PENDING,
                createdAt = tenDaysAgo,
            )

        // 5. Старое FAILED сообщение (10 дней назад) -> должно остаться (требует внимания)
        val oldFailed =
            OutboxMessageEntity(
                id = UUID.randomUUID(),
                aggregateType = "UserProfile",
                aggregateId = "user_failed",
                topic = "user.registered",
                payload = byteArrayOf(12, 13),
                status = OutboxStatus.FAILED,
                createdAt = tenDaysAgo,
            )

        outboxRepository.saveAll(listOf(oldPublished1, oldPublished2, freshPublished, oldPending, oldFailed))
        assertEquals(5, outboxRepository.count())

        // Выполняем очистку (retention 7 дней) через настроенную стратегию
        val deletedCount = outboxCleanupJob.cleanupOldMessages()
        assertEquals(2, deletedCount)

        assertEquals(3, outboxRepository.count())
        assertFalse(outboxRepository.existsById(oldPublished1.id))
        assertFalse(outboxRepository.existsById(oldPublished2.id))
        assertTrue(outboxRepository.existsById(freshPublished.id))
        assertTrue(outboxRepository.existsById(oldPending.id))
        assertTrue(outboxRepository.existsById(oldFailed.id))
    }

    @Test
    fun `BatchChunkCleanupStrategy deletes messages in chunks without locking errors`() {
        val tenDaysAgo = Instant.now().minus(Duration.ofDays(10))
        val messages =
            (1..5).map { i ->
                OutboxMessageEntity(
                    id = UUID.randomUUID(),
                    aggregateType = "UserProfile",
                    aggregateId = "user_batch_$i",
                    topic = "user.registered",
                    payload = byteArrayOf(i.toByte()),
                    status = OutboxStatus.PUBLISHED,
                    createdAt = tenDaysAgo,
                    publishedAt = tenDaysAgo,
                )
            }
        outboxRepository.saveAll(messages)
        assertEquals(5, outboxRepository.count())

        // Тестируем стратегию с размером пачки batchSize = 2 (потребует 3 итерации цикла)
        val customBatchStrategy =
            dev.andre.nutritionist.profile.outbox.cleanup.BatchChunkCleanupStrategy(
                outboxRepository = outboxRepository,
                transactionTemplate = transactionTemplate,
                batchSize = 2,
            )

        val result = customBatchStrategy.cleanup(Instant.now().minus(Duration.ofDays(7)))
        assertTrue(result is dev.andre.nutritionist.profile.outbox.cleanup.CleanupResult.Success)
        val success = result as dev.andre.nutritionist.profile.outbox.cleanup.CleanupResult.Success
        assertEquals(5, success.deletedCount)
        assertEquals(0, outboxRepository.count())
    }

    @Test
    fun `DirectDeleteCleanupStrategy deletes all eligible messages in a single query`() {
        val tenDaysAgo = Instant.now().minus(Duration.ofDays(10))
        val messages =
            (1..3).map { i ->
                OutboxMessageEntity(
                    id = UUID.randomUUID(),
                    aggregateType = "UserProfile",
                    aggregateId = "user_direct_$i",
                    topic = "user.registered",
                    payload = byteArrayOf(i.toByte()),
                    status = OutboxStatus.PUBLISHED,
                    createdAt = tenDaysAgo,
                    publishedAt = tenDaysAgo,
                )
            }
        outboxRepository.saveAll(messages)
        assertEquals(3, outboxRepository.count())

        val directStrategy =
            dev.andre.nutritionist.profile.outbox.cleanup.DirectDeleteCleanupStrategy(
                outboxRepository = outboxRepository,
            )

        val result = directStrategy.cleanup(Instant.now().minus(Duration.ofDays(7)))
        assertTrue(result is dev.andre.nutritionist.profile.outbox.cleanup.CleanupResult.Success)
        val success = result as dev.andre.nutritionist.profile.outbox.cleanup.CleanupResult.Success
        assertEquals(3, success.deletedCount)
        assertEquals(0, outboxRepository.count())
    }
}
