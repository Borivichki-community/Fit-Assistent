package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.Recipe

sealed interface AiRecipeEvent {
    sealed interface Ui : AiRecipeEvent {
        data class QueryChanged(val query: String) : Ui
        data class CaloriesChanged(val calories: Int) : Ui {
            companion object {
                fun fromSlider(value: Float) = CaloriesChanged(value.toInt())
            }
        }
        data class FitRemainingChanged(val enabled: Boolean) : Ui
        data class BudgetChanged(val budgetRub: Int) : Ui
        data class MealTypeSelected(val mealType: MealType) : Ui
        data class DietaryPreferenceSelected(val preference: String) : Ui
        data object GenerateClicked : Ui
        data object RegenerateClicked : Ui
        data object AddRecipeToDiaryClicked : Ui
        data object CancelClicked : Ui
        data object BackClicked : Ui
    }

    sealed interface Internal : AiRecipeEvent {
        data class ProgressUpdated(val phaseText: String, val progress: Float) : Internal
        data class RecipeGenerated(val recipe: Recipe) : Internal
        data class RecipeAddedToDiary(val record: MealRecord) : Internal
        data class GenerationFailed(val error: Throwable) : Internal
        data class SaveFailed(val error: Throwable) : Internal
    }
}
