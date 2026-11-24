package dev.andre.nutritionist.domain.model

data class RecipeIngredient(
    val productId: ProductId? = null,
    val name: String,
    val weightGrams: Double,
    val cost: Money? = null,
)

data class Recipe(
    val id: RecipeId,
    val title: String,
    val mealType: MealType,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val nutrition: NutritionFacts,
    val estimatedCost: Money,
    val cookingTimeMinutes: Int = 0,
    val instructions: String? = null,
)
