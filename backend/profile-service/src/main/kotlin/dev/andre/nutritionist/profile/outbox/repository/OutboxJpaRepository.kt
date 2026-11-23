package dev.andre.nutritionist.profile.outbox.repository

import dev.andre.nutritionist.profile.outbox.entity.OutboxMessageEntity
import dev.andre.nutritionist.profile.outbox.model.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface OutboxJpaRepository : JpaRepository<OutboxMessageEntity, UUID> {
    /**
     * Выборка сообщений с пессимистической блокировкой и пропуском заблокированных строк
     * (SELECT ... FOR UPDATE SKIP LOCKED).
     * Честный нативный SQL гарантирует переносимость и отсутствие гонок между подами.
     */
    @Query(
        value = """
            SELECT * FROM outbox_messages
            WHERE status = :status
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true,
    )
    fun findByStatusWithLock(
        @Param("status") status: String,
        @Param("limit") limit: Int,
    ): List<OutboxMessageEntity>

    /**
     * Очистка устаревших опубликованных сообщений (retention policy).
     */
    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query(
        value = """
            DELETE FROM outbox_messages
            WHERE status = 'PUBLISHED' AND published_at < :cutoff
        """,
        nativeQuery = true,
    )
    fun deletePublishedBefore(
        @Param("cutoff") cutoff: Instant,
    ): Int

    /**
     * Пакетная очистка устаревших опубликованных сообщений порциями по [batchSize].
     * Предотвращает раздувание WAL и долгие блокировки строк в PostgreSQL.
     */
    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query(
        value = """
            DELETE FROM outbox_messages
            WHERE id IN (
                SELECT id FROM outbox_messages
                WHERE status = 'PUBLISHED' AND published_at < :cutoff
                LIMIT :batchSize
            )
        """,
        nativeQuery = true,
    )
    fun deletePublishedBeforeBatch(
        @Param("cutoff") cutoff: Instant,
        @Param("batchSize") batchSize: Int,
    ): Int

    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query("UPDATE OutboxMessageEntity m SET m.status = :status, m.publishedAt = :publishedAt WHERE m.id = :id")
    fun markPublished(
        @Param("id") id: UUID,
        @Param("status") status: OutboxStatus = OutboxStatus.PUBLISHED,
        @Param("publishedAt") publishedAt: Instant = Instant.now(),
    ): Int

    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query(
        """
        UPDATE OutboxMessageEntity m
        SET m.attempts = m.attempts + 1,
            m.status = CASE WHEN (m.attempts + 1) >= :maxAttempts THEN :failedStatus ELSE m.status END
        WHERE m.id = :id
        """,
    )
    fun recordFailure(
        @Param("id") id: UUID,
        @Param("maxAttempts") maxAttempts: Int,
        @Param("failedStatus") failedStatus: OutboxStatus = OutboxStatus.FAILED,
    ): Int

    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query(
        """
        UPDATE OutboxMessageEntity m
        SET m.status = :pendingStatus,
            m.attempts = 0
        WHERE m.status = :failedStatus
        """,
    )
    fun resetFailedToPending(
        @Param("pendingStatus") pendingStatus: OutboxStatus = OutboxStatus.PENDING,
        @Param("failedStatus") failedStatus: OutboxStatus = OutboxStatus.FAILED,
    ): Int

    @Query("SELECT COUNT(m) FROM OutboxMessageEntity m WHERE m.status = :status")
    fun countByStatus(
        @Param("status") status: OutboxStatus,
    ): Long
}
