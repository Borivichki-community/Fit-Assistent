package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.calculator.DailyNutritionBalanceCalculator
import dev.andre.nutritionist.domain.model.ConsumedNutrition
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.presentation.mappers.DailyNutritionUiMapper.toUiModel
import dev.andre.nutritionist.presentation.model.MealRecordUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class DiaryUiStateMapperTest {

    private val satNov7Ms = Instant.parse("2026-11-07T12:00:00Z").toEpochMilliseconds()
    private val mapper = DiaryUiStateMapper(
        nowProvider = { satNov7Ms },
        timeZone = TimeZone.UTC,
    )

    @Test
    fun `map formats calendar week, monthYear, weekNumber and four meal sections from state`() {
        val epicaUi = MealRecordUiModel(
            id = MealRecordId("seed-epica-lunch-1"),
            mealType = MealType.LUNCH,
            mealTypeLabel = "Обед",
            title = "Йогурт Epica",
            weightLabel = "130 г",
            caloriesLabel = "85 ккал",
            caloriesInt = 85,
            proteinsLabel = "4,9 г",
            fatsLabel = "1,3 г",
            carbsLabel = "13,8 г",
            macrosLabel = "Б 4,9 г • Ж 1,3 г • У 13,8 г",
            compactMacrosLabel = "Б: 4,93г • Ж: 1,25г • У: 13,8г",
            costLabel = "95 ₽",
            dailyBalanceShareLabel = "4% от суточной нормы",
            durationLabel = "1 мин",
            syncStatus = SyncStatus.SYNCED,
            syncStatusLabel = "Синхронизировано",
            timeLabel = "12:00",
        )
        val balance = DailyNutritionBalanceCalculator.calculate(
            goal = NutritionGoal(2400, 170.0, 85.0, 378.0),
            consumed = ConsumedNutrition(85, 4.93, 1.25, 13.8),
        )
        val state = DiaryState(
            selectedDateEpochMs = satNov7Ms,
            items = listOf(epicaUi),
            dailySummary = balance.toUiModel(
                totalCost = Money.fromMinor(9500L),
                weeklyBudgetRubles = 3500.0,
            ),
            loggedDatesInWeek = setOf(LocalDate(2026, 11, 7)),
            content = DiaryState.Content.Success,
        )

        val uiState = mapper.map(state)

        assertEquals("Ноябрь 2026", uiState.monthYearLabel)
        assertEquals("НЕДЕЛЯ 45", uiState.weekNumberLabel)
        assertEquals(7, uiState.weekDays.size)
        assertTrue(uiState.weekDays[5].isSelected)
        assertTrue(uiState.weekDays[5].hasLoggedMeals)

        assertEquals(4, uiState.mealSections.size)
        assertEquals("Утренний слот", uiState.mealSections[0].title)
        assertTrue(uiState.mealSections[0].entries.isEmpty())

        assertEquals("Дневной приём", uiState.mealSections[1].title)
        assertEquals(85, uiState.mealSections[1].caloriesTotal)
        assertEquals(1, uiState.mealSections[1].entries.size)
        assertEquals("Йогурт Epica", uiState.mealSections[1].entries.first().title)

        assertEquals("95 ₽", uiState.dailySummary.totalMealCostLabel)
        assertEquals("Бюджет: ~500 ₽", uiState.dailySummary.dailyBudgetLabel)
        assertEquals("4%", uiState.dailySummary.caloriesProgressLabel)
    }

    @Test
    fun `map preserves empty day with zero metrics when items list is empty`() {
        val balance = DailyNutritionBalanceCalculator.calculate(
            goal = NutritionGoal(2400, 170.0, 85.0, 378.0),
            consumed = ConsumedNutrition(0, 0.0, 0.0, 0.0),
        )
        val state = DiaryState(
            selectedDateEpochMs = satNov7Ms,
            items = emptyList(),
            dailySummary = balance.toUiModel(
                totalCost = Money.ZERO,
                weeklyBudgetRubles = 3500.0,
            ),
            loggedDatesInWeek = emptySet(),
            content = DiaryState.Content.Empty,
        )

        val uiState = mapper.map(state)

        assertIsEmptySections(uiState)
        assertEquals("0 ₽", uiState.dailySummary.totalMealCostLabel)
        assertEquals("0%", uiState.dailySummary.caloriesProgressLabel)
        assertEquals("0", uiState.dailySummary.caloriesConsumedValue)
        assertFalse(uiState.weekDays.any { it.hasLoggedMeals })
    }

    private fun assertIsEmptySections(uiState: DiaryUiState) {
        assertTrue(uiState.mealSections.all { it.entries.isEmpty() && it.caloriesTotal == 0 })
    }
}
