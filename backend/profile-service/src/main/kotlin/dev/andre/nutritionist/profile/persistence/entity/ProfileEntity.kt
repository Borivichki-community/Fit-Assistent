package dev.andre.nutritionist.profile.persistence.entity

import dev.andre.nutritionist.profile.domain.model.ActivityLevel
import dev.andre.nutritionist.profile.domain.model.Gender
import dev.andre.nutritionist.profile.domain.model.GoalType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Suppress("LongParameterList")
@Entity
@Table(name = "profiles")
class ProfileEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,
    @Column(name = "user_id", nullable = false, length = 64, unique = true)
    val userId: String,
    @Column(name = "height_cm", nullable = false)
    var heightCm: Int, // mutable by design: JPA managed entity
    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    var weightKg: BigDecimal, // mutable by design: JPA managed entity
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    var gender: Gender, // mutable by design: JPA managed entity
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false, length = 30)
    var activityLevel: ActivityLevel, // mutable by design: JPA managed entity
    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 30)
    var goalType: GoalType, // mutable by design: JPA managed entity
    @Embedded
    var nutritionGoal: NutritionGoalEmbeddable, // mutable by design: JPA managed entity
    @Column(name = "weekly_budget", nullable = false, precision = 10, scale = 2)
    var weeklyBudget: BigDecimal, // mutable by design: JPA managed entity
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(), // mutable by design: JPA managed entity
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0, // mutable by design: JPA managed entity
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProfileEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
