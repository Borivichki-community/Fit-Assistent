package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.model.DailyNutritionUiModel
import dev.andre.nutritionist.presentation.model.MealRecordUiModel
import kotlinx.datetime.LocalDate

data class DiaryState(
    val selectedDateEpochMs: Long = 0L,
    val currentUserId: UserId? = null,
    val items: List<MealRecordUiModel> = emptyList(),
    val dailySummary: DailyNutritionUiModel = DailyNutritionUiModel(),
    val expandedMeals: Set<MealType> = MealType.entries.toSet(),
    val loggedDatesInWeek: Set<LocalDate> = emptySet(),
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
