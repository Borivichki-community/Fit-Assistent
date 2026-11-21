package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe

/**
 * События экрана генерации AI-рецепта в архитектуре KoTEA MVI.
 */
sealed interface AiRecipeEvent {

    sealed interface Ui : AiRecipeEvent {
        data class QueryChanged(val query: String) : Ui
        data class CaloriesChanged(val calories: Int) : Ui
        data class BudgetChanged(val budgetRub: Int) : Ui
        data class MealTypeSelected(val mealType: MealType) : Ui
        data class DietaryPreferenceSelected(val preference: String) : Ui
        data object GenerateClicked : Ui
        data object SaveRecipeClicked : Ui
        data object AddRecipeToDiaryClicked : Ui
        data object ResetClicked : Ui
        data object RetryClicked : Ui
        data class NetworkStatusChanged(val isConnected: Boolean) : Ui
    }

    sealed interface Internal : AiRecipeEvent {
        data class GenerationProgressUpdated(val phaseText: String, val progress: Float) : Internal
        data class RecipeGenerated(val recipe: Recipe) : Internal
        data class RecipeSaved(val recipe: Recipe) : Internal
        data object RecipeAddedToDiary : Internal
        data class GenerationFailed(val error: Throwable) : Internal
    }
}
