package dev.andre.nutritionist.domain.model

import kotlinx.datetime.Instant

enum class RecipeAuthorType {
    UNSPECIFIED,
    SYSTEM,
    USER,
    COACH,
    AI,
}

enum class ContentVisibility {
    UNSPECIFIED,
    PRIVATE,
    LINK_ONLY,
    PUBLIC,
}

data class RecipeIngredient(
    val id: RecipeIngredientId = RecipeIngredientId(""),
    val productId: ProductId? = null,
    val name: String,
    val weightGrams: Double,
    val nutritionPer100g: NutritionFacts = NutritionFacts.ZERO,
    val cost: Money? = null,
)

data class Recipe(
    val id: RecipeId,
    val title: String,
    val authorType: RecipeAuthorType = RecipeAuthorType.USER,
    val authorId: UserId? = null,
    val originRecipeId: RecipeId? = null,
    val description: String? = null,
    val instructions: String? = null,
    val visibility: ContentVisibility = ContentVisibility.PRIVATE,
    val servingsCount: Int = 1,
    val totalWeightGrams: Double = 0.0,
    val nutrition: NutritionFacts,
    val mealType: MealType = MealType.LUNCH,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val estimatedCost: Money = Money.ZERO,
    val cookingTimeMinutes: Int = 0,
    val version: Long = 0L,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)
