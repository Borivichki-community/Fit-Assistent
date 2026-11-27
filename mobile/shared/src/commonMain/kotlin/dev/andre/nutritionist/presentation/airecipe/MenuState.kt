package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.presentation.model.MealPlanUiModel

data class MenuState(
    val period: TargetPeriod = TargetPeriod.DAY,
    val targetCalories: Int = 2000,
    val budgetRub: Double = 500.0,
    val preferences: List<String> = emptyList(),
    val content: Content = Content.Idle,
) {
    sealed interface Content {
        data object Idle : Content
        data class Generating(val statusLabel: String = "Подбираем меню...") : Content
        data class Success(val plan: MealPlanUiModel) : Content
        data class Error(val message: String) : Content
    }
}
