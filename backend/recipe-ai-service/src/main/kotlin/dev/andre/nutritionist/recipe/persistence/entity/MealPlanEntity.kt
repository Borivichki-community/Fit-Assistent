package dev.andre.nutritionist.recipe.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "meal_plans")
class MealPlanEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "author_id", nullable = false, length = 64)
    var authorId: String = "",
    @Column(name = "author_type", nullable = false, length = 32)
    var authorType: String = "USER",
    @Column(name = "title", nullable = false, length = 255)
    var title: String = "",
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,
    @Column(name = "duration_days", nullable = false)
    var durationDays: Int = 1,
    @Column(name = "visibility", nullable = false, length = 32)
    var visibility: String = "PRIVATE",
    @Column(name = "access_mode", nullable = false, length = 32)
    var accessMode: String = "FREE",
    @Column(name = "price_amount_minor", nullable = false)
    var priceAmountMinor: Long = 0,
    @Column(name = "currency", nullable = false, length = 8)
    var currency: String = "RUB",
    @Column(name = "avg_daily_calories", precision = 6, scale = 2)
    var avgDailyCalories: BigDecimal? = null,
    @Column(name = "avg_daily_proteins", precision = 6, scale = 2)
    var avgDailyProteins: BigDecimal? = null,
    @Column(name = "avg_daily_fats", precision = 6, scale = 2)
    var avgDailyFats: BigDecimal? = null,
    @Column(name = "avg_daily_carbs", precision = 6, scale = 2)
    var avgDailyCarbs: BigDecimal? = null,
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @OneToMany(
        mappedBy = "plan",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    val items: MutableList<MealPlanItemEntity> = mutableListOf(),
) {
    fun addItem(item: MealPlanItemEntity) {
        items.add(item)
        item.plan = this
    }

    fun removeItem(item: MealPlanItemEntity) {
        items.remove(item)
        item.plan = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MealPlanEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
