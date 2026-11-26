package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.NutritionFacts

data class AiRecipePrompt(
    val mealType: MealType,
    val targetCalories: Int,
    val targetBudgetRub: Int = 350,
    val query: String = "",
    val dietaryPreference: String = "",
    val remainingNutrition: NutritionFacts = NutritionFacts.ZERO,
    val fitRemaining: Boolean = true,
) {
    val nutritionTarget: NutritionFacts
        get() = if (fitRemaining && remainingNutrition.calories > 0) {
            val fraction = (targetCalories / remainingNutrition.calories).coerceIn(0.0, 1.0)
            remainingNutrition * fraction
        } else {
            NutritionFacts(calories = targetCalories.toDouble())
        }

    companion object {
        const val NO_BUDGET_LIMIT = 0
        const val MAX_SUPPORTED_BUDGET_RUB = 10_000_000
    }
}

data class AiRecipeState(val prompt: AiRecipePrompt, val dateEpochMs: Long, val content: Content = Content.Configuring) {
    sealed interface Content {
        data object Configuring : Content
        data class Generating(val phaseText: String, val progress: Float) : Content
        data class Success(val recipe: Recipe, val isSaving: Boolean = false) : Content
        data class Error(val message: String) : Content
    }
}
