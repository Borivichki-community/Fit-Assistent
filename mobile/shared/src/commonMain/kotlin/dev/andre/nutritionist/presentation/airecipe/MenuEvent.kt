package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.domain.usecase.MenuGenerationProgress

sealed interface MenuEvent {

    sealed interface Ui : MenuEvent {
        data class CaloriesChanged(val calories: Int) : Ui
        data class BudgetChanged(val budgetRub: Double) : Ui
        data class PeriodSelected(val period: TargetPeriod) : Ui
        data class PreferenceToggled(val preferenceId: String) : Ui
        data object GenerateClicked : Ui
        data object CancelClicked : Ui
    }

    sealed interface Internal : MenuEvent {
        data class ProgressUpdated(val progress: MenuGenerationProgress) : Internal
        data class GenerationCompleted(val plan: MealPlan) : Internal
        data class GenerationFailed(val error: Throwable) : Internal
    }
}
