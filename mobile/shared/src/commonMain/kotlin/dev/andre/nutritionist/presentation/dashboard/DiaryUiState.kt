package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.presentation.model.ActivePlanChipUiModel
import dev.andre.nutritionist.presentation.model.DailyNutritionUiModel
import dev.andre.nutritionist.presentation.model.DayOfWeekUiModel
import dev.andre.nutritionist.presentation.model.DiaryMealSectionUiModel
import dev.andre.nutritionist.presentation.model.MealRecordUiModel

data class DiaryUiState(
    val selectedDateEpochMs: Long = 0L,
    val selectedDateLabel: String = "",
    val monthYearLabel: String = "",
    val weekNumberLabel: String = "",
    val weekDays: List<DayOfWeekUiModel> = emptyList(),
    val activePlans: List<ActivePlanChipUiModel> = emptyList(),
    val items: List<MealRecordUiModel> = emptyList(),
    val mealSections: List<DiaryMealSectionUiModel> = emptyList(),
    val dailySummary: DailyNutritionUiModel = DailyNutritionUiModel(),
    val showCompleteProfileBanner: Boolean = false,
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data object Success : Content
        data object Empty : Content
        data class Error(val message: String) : Content
    }
}
