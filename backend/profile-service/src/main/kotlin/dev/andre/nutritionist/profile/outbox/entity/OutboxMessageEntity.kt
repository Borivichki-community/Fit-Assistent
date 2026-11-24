package dev.andre.nutritionist.profile.outbox.entity

import dev.andre.nutritionist.profile.outbox.model.OutboxStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Suppress("LongParameterList")
@Entity
@Table(name = "outbox_messages")
class OutboxMessageEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,
    @Column(name = "aggregate_type", nullable = false, length = 64)
    val aggregateType: String,
    @Column(name = "aggregate_id", nullable = false, length = 64)
    val aggregateId: String,
    @Column(name = "topic", nullable = false, length = 128)
    val topic: String,
    @Column(name = "payload", nullable = false, columnDefinition = "bytea")
    val payload: ByteArray,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OutboxStatus = OutboxStatus.PENDING, // mutable by design: JPA managed entity
    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0, // mutable by design: JPA managed entity
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "published_at")
    var publishedAt: Instant? = null, // mutable by design: JPA managed entity
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OutboxMessageEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
