package dev.andre.nutritionist.profile.outbox.service

import dev.andre.nutritionist.profile.outbox.entity.OutboxMessageEntity
import dev.andre.nutritionist.profile.outbox.model.OutboxStatus
import dev.andre.nutritionist.profile.outbox.repository.OutboxJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Сервис сохранения событий в таблицу transactional outbox.
 * Согласно AGENTS.md §4.1 и §4.3, сохранение выполняется строго в рамках той же транзакции,
 * что и доменные сущности, без прямых сетевых вызовов в Kafka.
 */
@Service
class OutboxService(
    private val outboxRepository: OutboxJpaRepository,
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun save(
        id: UUID = UUID.randomUUID(),
        aggregateType: String,
        aggregateId: String,
        topic: String,
        payload: ByteArray,
    ): OutboxMessageEntity {
        val entity =
            OutboxMessageEntity(
                id = id,
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                topic = topic,
                payload = payload,
                status = OutboxStatus.PENDING,
                attempts = 0,
                createdAt = Instant.now(),
                publishedAt = null,
            )
        return outboxRepository.save(entity)
    }
}
