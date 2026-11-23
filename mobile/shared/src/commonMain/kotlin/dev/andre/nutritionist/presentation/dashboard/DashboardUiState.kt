package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.model.MealType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/**
 * Вспомогательная функция форматирования граммов макронутриентов.
 */
fun formatMacroGrams(grams: Double): String {
    val rounded = kotlin.math.round(grams * 100) / 100.0
    val str = rounded.toString().replace('.', ',')
    return when {
        str.endsWith(",0") -> str.substringBefore(',')
        str.contains(',') && str.endsWith("0") -> str.dropLast(1)
        else -> str
    }
}

/**
 * UI-модель одного макронутриента (Б/Ж/У).
 */
data class MacroNutrientUiModel(
    val label: String,
    val currentGrams: Double,
    val targetGrams: Double,
    val remainingGrams: Double = 0.0,
    val isExceeded: Boolean = false,
    val formattedValue: String,
    val displayCurrent: String = formatMacroGrams(currentGrams),
    val displayTarget: String = "${formatMacroGrams(targetGrams)} г",
    val displayRemaining: String = "Осталось: ${formatMacroGrams(remainingGrams)} г",
)

/**
 * Сводная UI-модель целевых и употреблённых нутриентов (КБЖУ).
 */
data class NutritionSummaryUiModel(
    val caloriesConsumed: Int = 0,
    val caloriesTarget: Int = 2000,
    val caloriesRemaining: Int = 2000,
    val percentCompleted: Int = 0,
    val isCaloriesExceeded: Boolean = false,
    val caloriesRemainingLabel: String = "Осталось $caloriesRemaining ккал",
    val fats: MacroNutrientUiModel,
    val carbs: MacroNutrientUiModel,
    val proteins: MacroNutrientUiModel,
)

/**
 * UI-модель отдельного блюда в приёме пищи.
 */
data class MealEntryUiModel(
    val id: String,
    val title: String,
    val portionDescription: String,
    val calories: Int,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
)

/**
 * UI-модель секции приёма пищи (Завтрак, Обед, Ужин, Перекус/Другое).
 */
data class MealSectionUiModel(
    val type: MealType,
    val title: String,
    val caloriesTotal: Int = 0,
    val entries: List<MealEntryUiModel> = emptyList(),
    val isExpanded: Boolean = false,
)

/**
 * UI-модель дня недели в полосе календаря.
 */
data class DayOfWeekUiModel(
    val dayName: String,
    val isSelected: Boolean = false,
    val isCompleted: Boolean = false,
    val date: LocalDate? = null,
    val dayNumber: String = "",
)

fun defaultWeekDays(): List<DayOfWeekUiModel> = listOf(
    DayOfWeekUiModel(dayName = "Пн", isSelected = false, isCompleted = true, dayNumber = "02"),
    DayOfWeekUiModel(dayName = "Вт", isSelected = false, isCompleted = true, dayNumber = "03"),
    DayOfWeekUiModel(dayName = "Ср", isSelected = false, isCompleted = true, dayNumber = "04"),
    DayOfWeekUiModel(dayName = "Чт", isSelected = false, isCompleted = true, dayNumber = "05"),
    DayOfWeekUiModel(dayName = "Пт", isSelected = false, isCompleted = true, dayNumber = "06"),
    DayOfWeekUiModel(dayName = "Сб", isSelected = true, isCompleted = true, dayNumber = "07"),
    DayOfWeekUiModel(dayName = "Вс", isSelected = false, isCompleted = false, dayNumber = "08"),
)

fun weekDaysForDate(selectedDate: LocalDate, anchorDate: LocalDate = selectedDate): List<DayOfWeekUiModel> {
    val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val isoDay = anchorDate.dayOfWeek.isoDayNumber // 1 = Monday, 7 = Sunday
    val mondayEpochDays = anchorDate.toEpochDays() - (isoDay - 1)
    val todayEpochDays = selectedDate.toEpochDays()

    return dayNames.mapIndexed { index, name ->
        val date = LocalDate.fromEpochDays(mondayEpochDays + index)
        DayOfWeekUiModel(
            dayName = name,
            isSelected = date == selectedDate,
            isCompleted = date.toEpochDays() <= todayEpochDays,
            date = date,
            dayNumber = date.day.toString().padStart(2, '0'),
        )
    }
}

/**
 * Успешные агрегированные данные для отображения в UI.
 */
data class DashboardSuccessUiModel(
    val selectedDateLabel: String = "Сегодня",
    val streakDays: Int = 1,
    val streakLabel: String = "Серия записи еды: 1 дн.",
    val weekDays: List<DayOfWeekUiModel> = defaultWeekDays(),
    val nutritionSummary: NutritionSummaryUiModel,
    val meals: List<MealSectionUiModel>,
    val showCompleteProfileBanner: Boolean = false,
)

/**
 * Презентационное состояние главного экрана (Dashboard / Diary).
 * В соответствии с KoTEA и AGENTS.md §3.1 содержит sealed interface Content.
 */
data class DashboardUiState(
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val data: DashboardSuccessUiModel) : Content
        data class Error(val message: String) : Content
    }
}
