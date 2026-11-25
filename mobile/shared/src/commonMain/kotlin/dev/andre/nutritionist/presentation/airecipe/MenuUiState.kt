package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.presentation.model.MealPlanUiModel

data class MenuUiState(
    val selectedPeriod: TargetPeriod = TargetPeriod.DAY,
    val periodLabel: String = "День",
    val targetCalories: Int = 2000,
    val targetCaloriesLabel: String = "2 000 ккал",
    val budgetRub: Double = 500.0,
    val budgetLabel: String = "500 ₽",
    val preferences: List<String> = emptyList(),
    val content: Content = Content.Idle,
) {
    sealed interface Content {
        data object Idle : Content
        data class Generating(val statusLabel: String) : Content
        data class Success(val plan: MealPlanUiModel) : Content
        data class Error(val message: String) : Content
    }
}
