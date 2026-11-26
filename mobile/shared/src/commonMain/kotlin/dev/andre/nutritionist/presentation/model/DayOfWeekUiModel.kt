package dev.andre.nutritionist.presentation.model

import dev.andre.nutritionist.domain.model.MealType
import kotlinx.datetime.LocalDate

/**
 * UI-модель дня в 7-дневной календарной полосе дневника.
 */
data class DayOfWeekUiModel(
    val date: LocalDate,
    val dateEpochMs: Long,
    val dayOfWeekName: String,
    val dayOfMonth: String,
    val isSelected: Boolean = false,
    val isToday: Boolean = false,
    val hasLoggedMeals: Boolean = false,
)

/**
 * UI-модель секции приёма пищи на экране дневника.
 */
data class DiaryMealSectionUiModel(
    val type: MealType,
    val title: String,
    val caloriesTotal: Int = 0,
    val caloriesLabel: String = "0 ккал",
    val entries: List<MealRecordUiModel> = emptyList(),
    val isExpanded: Boolean = true,
)

/**
 * UI-модель чипа активного плана питания («Мои планы»).
 */
data class ActivePlanChipUiModel(
    val id: String,
    val title: String,
    val isPrimaryAccent: Boolean,
)
