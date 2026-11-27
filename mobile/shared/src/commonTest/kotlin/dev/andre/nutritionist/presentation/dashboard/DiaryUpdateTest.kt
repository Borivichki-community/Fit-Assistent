package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.calculator.DailyNutritionBalanceCalculator
import dev.andre.nutritionist.domain.model.ConsumedNutrition
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class DiaryUpdateTest {

    private val update = DiaryUpdate(timeZone = TimeZone.UTC)

    private fun epicaRecord(id: String = "seed-epica-lunch-1") = MealRecord(
        id = MealRecordId(id),
        mealType = MealType.LUNCH,
        recordedAt = Instant.parse("2026-11-07T12:00:00Z"),
        sourceType = EntrySourceType.PRODUCT,
        sourceId = "20000000-0000-0000-0000-000000000112",
        nameSnapshot = "Йогурт Epica",
        weightGrams = 130.0,
        nutrition = NutritionFacts(calories = 85.0, proteins = 4.93, fats = 1.25, carbs = 13.8),
        cost = Money.fromMinor(9500L),
        idempotencyKey = IdempotencyKey(id),
        syncStatus = SyncStatus.SYNCED,
    )

    @Test
    fun `DateSelected sets Loading, preserves userId and emits ObserveDate command`() {
        val userId = UserId("user-1")
        val next = update.update(
            DiaryState(),
            DiaryEvent.Ui.DateSelected(1_700_000_000_000L, userId),
        )

        assertEquals(1_700_000_000_000L, next.state?.selectedDateEpochMs)
        assertEquals(userId, next.state?.currentUserId)
        assertIs<DiaryState.Content.Loading>(next.state?.content)
        assertEquals(listOf(DiaryCommand.ObserveDate(1_700_000_000_000L, userId)), next.commands)
    }

    @Test
    fun `PreviousWeekClicked and NextWeekClicked shift date by 7 days and emit ObserveDate`() {
        val satNov7Ms = Instant.parse("2026-11-07T12:00:00Z").toEpochMilliseconds()
        val userId = UserId("user-1")
        val initial = DiaryState(selectedDateEpochMs = satNov7Ms, currentUserId = userId)

        val prevNext = update.update(initial, DiaryEvent.Ui.PreviousWeekClicked)
        val expectedPrevMs = Instant.parse("2026-10-31T12:00:00Z").toEpochMilliseconds()
        assertEquals(expectedPrevMs, prevNext.state?.selectedDateEpochMs)
        assertEquals(listOf(DiaryCommand.ObserveDate(expectedPrevMs, userId)), prevNext.commands)

        val forwardNext = update.update(initial, DiaryEvent.Ui.NextWeekClicked)
        val expectedForwardMs = Instant.parse("2026-11-14T12:00:00Z").toEpochMilliseconds()
        assertEquals(expectedForwardMs, forwardNext.state?.selectedDateEpochMs)
        assertEquals(listOf(DiaryCommand.ObserveDate(expectedForwardMs, userId)), forwardNext.commands)

        val retryNext = update.update(
            initial.copy(content = DiaryState.Content.Error("Ошибка сети")),
            DiaryEvent.Ui.Retry,
        )
        assertIs<DiaryState.Content.Loading>(retryNext.state?.content)
        assertEquals(satNov7Ms, retryNext.state?.selectedDateEpochMs)
        assertEquals(listOf(DiaryCommand.ObserveDate(satNov7Ms, userId)), retryNext.commands)
    }

    @Test
    fun `ToggleMealSection toggles section expansion state`() {
        val initial = DiaryState()
        assertTrue(initial.expandedMeals.contains(MealType.BREAKFAST))

        val collapsed = update.update(initial, DiaryEvent.Ui.ToggleMealSection(MealType.BREAKFAST))
        val collapsedState = requireNotNull(collapsed.state)
        assertFalse(collapsedState.expandedMeals.contains(MealType.BREAKFAST))

        val reExpanded = update.update(
            collapsedState,
            DiaryEvent.Ui.ToggleMealSection(MealType.BREAKFAST),
        )
        assertTrue(requireNotNull(reExpanded.state).expandedMeals.contains(MealType.BREAKFAST))
    }

    @Test
    fun `DeleteRecord and ManualSyncClicked emit corresponding commands`() {
        val deleteNext = update.update(
            DiaryState(),
            DiaryEvent.Ui.DeleteRecord(MealRecordId("rec-1")),
        )
        assertEquals(listOf(DiaryCommand.DeleteRecord(MealRecordId("rec-1"))), deleteNext.commands)

        val syncNext = update.update(DiaryState(), DiaryEvent.Ui.ManualSyncClicked)
        assertEquals(listOf(DiaryCommand.SyncPending), syncNext.commands)
    }

    @Test
    fun `RecordsLoaded maps items, costs, budget and balance share from profile goal`() {
        val balance = DailyNutritionBalanceCalculator.calculate(
            goal = NutritionGoal(
                targetCalories = 2957,
                targetProteinsGrams = 170.0,
                targetFatsGrams = 85.0,
                targetCarbsGrams = 378.0,
            ),
            consumed = ConsumedNutrition(
                calories = 85,
                proteinsGrams = 4.93,
                fatsGrams = 1.25,
                carbsGrams = 13.8,
            ),
        )

        val successNext = update.update(
            DiaryState(),
            DiaryEvent.Internal.RecordsLoaded(
                records = listOf(epicaRecord()),
                balance = balance,
                totalCost = Money.fromMinor(9500L),
                weeklyBudget = 4900.0,
                targetCalories = 2957,
                loggedDatesInWeek = setOf(LocalDate(2026, 11, 7)),
            ),
        )
        val updatedState = requireNotNull(successNext.state)
        assertIs<DiaryState.Content.Success>(updatedState.content)
        assertEquals(1, updatedState.items.size)
        assertEquals("95 ₽", updatedState.dailySummary.totalMealCostLabel)
        assertEquals("Бюджет: ~700 ₽", updatedState.dailySummary.dailyBudgetLabel)
        assertEquals("1,25", updatedState.dailySummary.fatsConsumedValue)
        assertEquals("/ 85 г", updatedState.dailySummary.fatsTargetShortLabel)
        assertEquals("3%", updatedState.dailySummary.caloriesProgressLabel)
        assertEquals("85", updatedState.dailySummary.caloriesConsumedValue)
        assertEquals("/ 2 957", updatedState.dailySummary.caloriesTargetValue)
        assertEquals("3% от суточной нормы", updatedState.items.first().dailyBalanceShareLabel)

        val emptyBalance = DailyNutritionBalanceCalculator.calculate(
            goal = NutritionGoal(2957, 170.0, 85.0, 378.0),
            consumed = ConsumedNutrition(0, 0.0, 0.0, 0.0),
        )
        val emptyNext = update.update(
            DiaryState(),
            DiaryEvent.Internal.RecordsLoaded(
                records = emptyList(),
                balance = emptyBalance,
                totalCost = Money.ZERO,
                weeklyBudget = 4900.0,
                targetCalories = 2957,
            ),
        )
        assertIs<DiaryState.Content.Empty>(emptyNext.state?.content)
        assertEquals("0 ₽", emptyNext.state?.dailySummary?.totalMealCostLabel)
        assertEquals("0%", emptyNext.state?.dailySummary?.caloriesProgressLabel)
    }

    @Test
    fun `SyncFinished and Failed emit expected snackbar news`() {
        val syncDone = update.update(DiaryState(), DiaryEvent.Internal.SyncFinished(syncedCount = 2))
        assertEquals(listOf(DiaryNews.ShowSnackbar("Синхронизировано записей: 2")), syncDone.news)

        val syncZero = update.update(DiaryState(), DiaryEvent.Internal.SyncFinished(syncedCount = 0))
        assertEquals(listOf(DiaryNews.ShowSnackbar("Все записи синхронизированы")), syncZero.news)

        val failed = update.update(DiaryState(), DiaryEvent.Internal.Failed(RuntimeException("Ошибка БД")))
        val error = assertIs<DiaryState.Content.Error>(failed.state?.content)
        assertEquals("Ошибка БД", error.message)
        assertEquals(listOf(DiaryNews.ShowSnackbar("Ошибка БД")), failed.news)
    }
}
