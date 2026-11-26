package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.formatters.DateFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter
import dev.andre.nutritionist.presentation.model.ActivePlanChipUiModel
import dev.andre.nutritionist.presentation.model.DiaryMealSectionUiModel
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class DiaryUiStateMapper(
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : UiStateMapper<DiaryState, DiaryUiState> {

    override fun map(state: DiaryState): DiaryUiState {
        val uiContent = when (val c = state.content) {
            DiaryState.Content.Loading -> DiaryUiState.Content.Loading
            DiaryState.Content.Success -> DiaryUiState.Content.Success
            DiaryState.Content.Empty -> DiaryUiState.Content.Empty
            is DiaryState.Content.Error -> DiaryUiState.Content.Error(c.message)
        }
        val nowEpochMs = nowProvider()
        val effectiveDateEpochMs = if (state.selectedDateEpochMs > 0L) state.selectedDateEpochMs else nowEpochMs

        val dateLabel = DateFormatter.formatDayHeader(
            epochMs = effectiveDateEpochMs,
            nowEpochMs = nowEpochMs,
            timeZone = timeZone,
        )
        val monthYearLabel = DateFormatter.formatMonthYear(
            epochMs = effectiveDateEpochMs,
            timeZone = timeZone,
        )
        val weekNumberLabel = DateFormatter.formatIsoWeekLabel(
            epochMs = effectiveDateEpochMs,
            timeZone = timeZone,
        )
        val weekDays = DateFormatter.weekDaysForDate(
            selectedDateEpochMs = effectiveDateEpochMs,
            nowEpochMs = nowEpochMs,
            datesWithMeals = state.loggedDatesInWeek,
            timeZone = timeZone,
        )

        val mealSections = MealType.entries.map { mealType ->
            val slotEntries = state.items.filter { it.mealType == mealType }
            val slotCalories = slotEntries.sumOf { it.caloriesInt }
            DiaryMealSectionUiModel(
                type = mealType,
                title = mealType.toSlotTitle(),
                caloriesTotal = slotCalories,
                caloriesLabel = NutritionFormatter.formatCalories(slotCalories.toDouble()),
                entries = slotEntries,
                isExpanded = state.expandedMeals.contains(mealType),
            )
        }

        return DiaryUiState(
            selectedDateLabel = dateLabel,
            monthYearLabel = monthYearLabel,
            weekNumberLabel = weekNumberLabel,
            weekDays = weekDays,
            activePlans = DEFAULT_PLAN_CHIPS,
            items = state.items,
            mealSections = mealSections,
            dailySummary = state.dailySummary,
            showCompleteProfileBanner = state.showCompleteProfileBanner,
            content = uiContent,
        )
    }

    private fun MealType.toSlotTitle(): String = when (this) {
        MealType.BREAKFAST -> "Утренний слот"
        MealType.LUNCH -> "Дневной приём"
        MealType.DINNER -> "Вечерний приём"
        MealType.SNACK -> "Перекус & Другое"
    }

    private companion object {
        val DEFAULT_PLAN_CHIPS = listOf(
            ActivePlanChipUiModel(
                id = "plan-dry-2400",
                title = "СУШКА 2400 ККАЛ",
                isPrimaryAccent = true,
            ),
            ActivePlanChipUiModel(
                id = "plan-keto",
                title = "КЕТО БАЛАНС",
                isPrimaryAccent = false,
            ),
        )
    }
}
