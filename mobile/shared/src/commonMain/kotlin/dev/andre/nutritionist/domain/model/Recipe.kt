package dev.andre.nutritionist.domain.model

/**
 * Ингредиент рецепта с наименованием и количеством.
 */
data class RecipeIngredient(
    val name: String,
    val amount: String,
)

/**
 * Доменная модель рецепта блюда с нутриентами, стоимостью и пошаговым приготовлением.
 */
data class Recipe(
    val id: RecipeId,
    val title: String,
    val description: String,
    val mealType: MealType,
    val calories: Int,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val estimatedCostRub: Int,
    val cookingTimeMinutes: Int,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val isFavorite: Boolean = false,
)

/**
 * Параметры генерации персонального рецепта с помощью AI.
 */
data class AiRecipePrompt(
    val query: String = "",
    val targetCalories: Int = 550,
    val targetBudgetRub: Int = 220,
    val mealType: MealType = MealType.LUNCH,
    val dietaryPreference: String = "Все",
)
