package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.presentation.model.DailyNutritionUiModel
import dev.andre.nutritionist.presentation.model.MealRecordUiModel

data class DiaryUiState(
    val selectedDateLabel: String = "",
    val items: List<MealRecordUiModel> = emptyList(),
    val dailySummary: DailyNutritionUiModel = DailyNutritionUiModel(),
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data object Success : Content
        data object Empty : Content
        data class Error(val message: String) : Content
    }
}
