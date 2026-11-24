package dev.andre.nutritionist.catalog.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "regions")
@BatchSize(size = 50)
class RegionEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "name", nullable = false, length = 255)
    var name: String, // mutable by design: JPA managed entity
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true, // mutable by design: JPA managed entity
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RegionEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
