package dev.andre.nutritionist.catalog.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "regions")
class RegionEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "name", nullable = false, length = 255)
    var name: String,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
