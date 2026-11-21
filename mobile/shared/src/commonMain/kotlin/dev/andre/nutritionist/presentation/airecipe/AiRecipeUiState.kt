package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.MealType

/**
 * UI-модель ингредиента.
 */
data class RecipeIngredientUiModel(
    val name: String,
    val amount: String,
)

/**
 * UI-модель шага приготовления.
 */
data class RecipeStepUiModel(
    val stepNumber: Int,
    val instruction: String,
)

/**
 * UI-модель формы настройки генерации.
 */
data class AiRecipePromptUiModel(
    val query: String,
    val targetCalories: Int,
    val targetCaloriesText: String,
    val targetBudgetRub: Int,
    val targetBudgetText: String,
    val mealType: MealType,
    val mealTypeTitle: String,
    val dietaryPreference: String,
    val availablePreferences: List<String>,
)

/**
 * UI-модель фазы генерации с анимацией.
 */
data class AiRecipeGeneratingUiModel(
    val phaseText: String,
    val progress: Float,
    val progressPercentText: String,
)

/**
 * UI-модель готового рецепта.
 */
data class RecipeResultUiModel(
    val id: String,
    val title: String,
    val description: String,
    val mealTypeBadge: String,
    val caloriesBadge: String,
    val budgetBadge: String,
    val cookingTimeBadge: String,
    val macroSummaryText: String,
    val proteinText: String,
    val fatsText: String,
    val carbsText: String,
    val proteinFraction: Float,
    val fatsFraction: Float,
    val carbsFraction: Float,
    val ingredients: List<RecipeIngredientUiModel>,
    val steps: List<RecipeStepUiModel>,
    val isSaved: Boolean,
)

/**
 * Состояние экрана генерации рецепта для UI слоя.
 */
data class AiRecipeUiState(
    val content: Content,
) {
    sealed interface Content {
        data class Configuring(val prompt: AiRecipePromptUiModel) : Content
        data class Generating(val generating: AiRecipeGeneratingUiModel) : Content
        data class Success(val recipe: RecipeResultUiModel) : Content
        data class Error(val message: String) : Content
    }
}
