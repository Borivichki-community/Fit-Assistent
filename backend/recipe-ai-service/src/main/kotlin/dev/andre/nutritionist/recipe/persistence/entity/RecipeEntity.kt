package dev.andre.nutritionist.recipe.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "recipes")
class RecipeEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "author_type", nullable = false, length = 32)
    var authorType: String = "SYSTEM",
    @Column(name = "author_id", length = 64)
    var authorId: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_recipe_id")
    var originRecipe: RecipeEntity? = null,
    @Column(name = "title", nullable = false, length = 255)
    var title: String = "",
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,
    @Column(name = "instructions", columnDefinition = "TEXT")
    var instructions: String? = null,
    @Column(name = "visibility", nullable = false, length = 32)
    var visibility: String = "PRIVATE",
    @Column(name = "servings_count", nullable = false)
    var servingsCount: Int = 1,
    @Column(name = "total_weight_g", nullable = false, precision = 8, scale = 2)
    var totalWeightG: BigDecimal = BigDecimal.ZERO,
    @Column(name = "calories_per_100g", nullable = false, precision = 6, scale = 2)
    var caloriesPer100g: BigDecimal = BigDecimal.ZERO,
    @Column(name = "proteins_per_100g", nullable = false, precision = 6, scale = 2)
    var proteinsPer100g: BigDecimal = BigDecimal.ZERO,
    @Column(name = "fats_per_100g", nullable = false, precision = 6, scale = 2)
    var fatsPer100g: BigDecimal = BigDecimal.ZERO,
    @Column(name = "carbs_per_100g", nullable = false, precision = 6, scale = 2)
    var carbsPer100g: BigDecimal = BigDecimal.ZERO,
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @OneToMany(
        mappedBy = "recipe",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    val ingredients: MutableList<RecipeIngredientEntity> = mutableListOf(),
) {
    fun addIngredient(ingredient: RecipeIngredientEntity) {
        ingredients.add(ingredient)
        ingredient.recipe = this
    }

    fun removeIngredient(ingredient: RecipeIngredientEntity) {
        ingredients.remove(ingredient)
        ingredient.recipe = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecipeEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
