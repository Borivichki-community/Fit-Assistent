package dev.andre.nutritionist.recipe.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "meal_plan_items")
class MealPlanItemEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    var plan: MealPlanEntity? = null,
    @Column(name = "day_number", nullable = false)
    var dayNumber: Int = 1,
    @Column(name = "meal_slot", nullable = false, length = 20)
    var mealSlot: String = "BREAKFAST",
    @Column(name = "item_type", nullable = false, length = 20)
    var itemType: String = "RECIPE",
    @Column(name = "item_id", nullable = false)
    var itemId: UUID = UUID.randomUUID(),
    @Column(name = "name_snapshot", nullable = false, length = 255)
    var nameSnapshot: String = "",
    @Column(name = "weight_grams", nullable = false, precision = 7, scale = 2)
    var weightGrams: BigDecimal = BigDecimal.ZERO,
    @Column(name = "calories", precision = 6, scale = 2)
    var calories: BigDecimal? = null,
    @Column(name = "proteins", precision = 6, scale = 2)
    var proteins: BigDecimal? = null,
    @Column(name = "fats", precision = 6, scale = 2)
    var fats: BigDecimal? = null,
    @Column(name = "carbs", precision = 6, scale = 2)
    var carbs: BigDecimal? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MealPlanItemEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
