package dev.andre.nutritionist.recipe.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "access_grants",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_access_grant", columnNames = ["user_id", "target_type", "target_id"]),
    ],
)
class AccessGrantEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false, length = 64)
    var userId: String = "",
    @Column(name = "target_type", nullable = false, length = 32)
    var targetType: String = "",
    @Column(name = "target_id", nullable = false)
    var targetId: UUID = UUID.randomUUID(),
    @Column(name = "grant_source", nullable = false, length = 32)
    var grantSource: String = "PURCHASE",
    @Column(name = "granted_by", length = 64)
    var grantedBy: String? = null,
    @Column(name = "expires_at")
    var expiresAt: Instant? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccessGrantEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
