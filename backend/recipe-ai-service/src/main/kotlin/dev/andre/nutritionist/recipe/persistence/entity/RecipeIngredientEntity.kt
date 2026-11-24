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
@Table(name = "recipe_ingredients")
class RecipeIngredientEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    var recipe: RecipeEntity? = null,
    @Column(name = "product_id")
    var productId: UUID? = null,
    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    var productNameSnapshot: String = "",
    @Column(name = "weight_grams", nullable = false, precision = 7, scale = 2)
    var weightGrams: BigDecimal = BigDecimal.ZERO,
    @Column(name = "calories_per_100g", precision = 6, scale = 2)
    var caloriesPer100g: BigDecimal? = null,
    @Column(name = "proteins_per_100g", precision = 6, scale = 2)
    var proteinsPer100g: BigDecimal? = null,
    @Column(name = "fats_per_100g", precision = 6, scale = 2)
    var fatsPer100g: BigDecimal? = null,
    @Column(name = "carbs_per_100g", precision = 6, scale = 2)
    var carbsPer100g: BigDecimal? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecipeIngredientEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
