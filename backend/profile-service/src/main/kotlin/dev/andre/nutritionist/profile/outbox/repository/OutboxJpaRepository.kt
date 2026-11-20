package dev.andre.nutritionist.profile.outbox.repository

import dev.andre.nutritionist.profile.outbox.entity.OutboxMessageEntity
import dev.andre.nutritionist.profile.outbox.model.OutboxStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface OutboxJpaRepository : JpaRepository<OutboxMessageEntity, UUID> {
    /**
     * Выборка сообщений с пессимистической блокировкой и пропуском заблокированных строк
     * (SELECT ... FOR UPDATE SKIP LOCKED). Гарантирует отсутствие гонок между параллельными подами/воркерами.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
        QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"),
    )
    @Query("SELECT m FROM OutboxMessageEntity m WHERE m.status = :status ORDER BY m.createdAt ASC")
    fun findByStatusWithLock(
        @Param("status") status: OutboxStatus,
        pageable: Pageable,
    ): List<OutboxMessageEntity>

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

    @Query("SELECT COUNT(m) FROM OutboxMessageEntity m WHERE m.status = :status")
    fun countByStatus(
        @Param("status") status: OutboxStatus,
    ): Long
}
