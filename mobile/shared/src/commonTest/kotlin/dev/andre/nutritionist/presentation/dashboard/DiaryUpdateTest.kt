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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class DiaryUpdateTest {

    private val update = DiaryUpdate()

    private fun sampleRecord(id: String) = MealRecord(
        id = MealRecordId(id),
        mealType = MealType.BREAKFAST,
        recordedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        sourceType = EntrySourceType.PRODUCT,
        nameSnapshot = "Овсянка",
        weightGrams = 200.0,
        nutrition = NutritionFacts(calories = 300.0, proteins = 10.0, fats = 5.0, carbs = 52.0),
        cost = Money.fromRubles(45.0),
        idempotencyKey = IdempotencyKey("idem-1"),
        syncStatus = SyncStatus.PENDING,
    )

    @Test
    fun `DateSelected sets Loading and emits ObserveDate command`() {
        val next = update.update(
            DiaryState(),
            DiaryEvent.Ui.DateSelected(1_700_000_000_000L),
        )

        assertEquals(1_700_000_000_000L, next.state?.selectedDateEpochMs)
        assertIs<DiaryState.Content.Loading>(next.state?.content)
        assertEquals(listOf(DiaryCommand.ObserveDate(1_700_000_000_000L)), next.commands)
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
    fun `RecordsLoaded maps items and daily summary into Success or Empty state`() {
        val balance = DailyNutritionBalanceCalculator.calculate(
            goal = NutritionGoal(
                targetCalories = 2000,
                targetProteinsGrams = 100.0,
                targetFatsGrams = 60.0,
                targetCarbsGrams = 250.0,
            ),
            consumed = ConsumedNutrition(
                calories = 300,
                proteinsGrams = 10.0,
                fatsGrams = 5.0,
                carbsGrams = 52.0,
            ),
        )

        val successNext = update.update(
            DiaryState(),
            DiaryEvent.Internal.RecordsLoaded(records = listOf(sampleRecord("rec-1")), balance = balance),
        )
        assertIs<DiaryState.Content.Success>(successNext.state?.content)
        assertEquals(1, successNext.state?.items?.size)
        assertEquals("300 ккал", successNext.state?.dailySummary?.consumedCaloriesLabel)
        assertEquals("Осталось 1 700 ккал", successNext.state?.dailySummary?.remainingCaloriesLabel)

        val emptyNext = update.update(
            DiaryState(),
            DiaryEvent.Internal.RecordsLoaded(records = emptyList(), balance = balance),
        )
        assertIs<DiaryState.Content.Empty>(emptyNext.state?.content)
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
