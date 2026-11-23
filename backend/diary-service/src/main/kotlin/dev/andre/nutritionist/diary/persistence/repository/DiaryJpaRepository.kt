package dev.andre.nutritionist.diary.persistence.repository

import dev.andre.nutritionist.diary.domain.model.DiaryEntryId
import dev.andre.nutritionist.diary.domain.model.UserId
import dev.andre.nutritionist.diary.persistence.entity.DiaryEntryEntity
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import kotlin.time.Instant

@Repository
class DiaryJpaRepository(
    private val entityManager: EntityManager,
) {
    fun insert(entity: DiaryEntryEntity): DiaryEntryEntity {
        entityManager.persist(entity)
        entityManager.flush()
        entityManager.refresh(entity)
        return entity
    }

    fun delete(
        userId: UserId,
        entryId: DiaryEntryId,
    ): Boolean =
        entityManager
            .createQuery("DELETE FROM DiaryEntryEntity e WHERE e.userId = :userId AND e.id = :entryId")
            .setParameter("userId", userId.raw)
            .setParameter("entryId", entryId.raw)
            .executeUpdate() == 1

    fun getRange(
        userId: UserId,
        from: Instant,
        until: Instant,
    ): List<DiaryEntryEntity> =
        entityManager
            .createQuery(
                """
                SELECT e FROM DiaryEntryEntity e
                WHERE e.userId = :userId AND e.recordedAt >= :from AND e.recordedAt < :until
                ORDER BY e.recordedAt, e.id
                """.trimIndent(),
                DiaryEntryEntity::class.java,
            ).setParameter("userId", userId.raw)
            .setParameter("from", java.time.Instant.ofEpochSecond(from.epochSeconds, from.nanosecondsOfSecond.toLong()))
            .setParameter("until", java.time.Instant.ofEpochSecond(until.epochSeconds, until.nanosecondsOfSecond.toLong()))
            .resultList
}
