package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.model.DailyDiary
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.FoodItemId
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class DashboardUpdateTest {

    private val update = DashboardUpdate()
    private val testUserId = UserId("user-100")
    private val testDate = LocalDate(2026, 3, 15)

    private val sampleEntry = DiaryEntry(
        id = DiaryEntryId("entry-1"),
        userId = testUserId,
        foodItemId = FoodItemId("food-1"),
        foodName = "Гречка",
        portionGrams = 150.0,
        calories = 180,
        proteins = 6.0,
        fats = 2.0,
        carbs = 35.0,
        mealType = MealType.LUNCH,
        date = testDate,
        recordedAt = Instant.fromEpochMilliseconds(0),
        idempotencyKey = IdempotencyKey("idem-1"),
    )

    @Test
    fun `LoadDashboard sets Loading content and emits Load command`() {
        val initial = DashboardState()
        val next = update.update(
            initial,
            DashboardEvent.Ui.LoadDashboard(
                userId = testUserId,
                date = testDate,
                showCompleteProfileBanner = true,
            ),
        )

        assertIs<DashboardState.Content.Loading>(next.state?.content)
        assertEquals(testUserId, next.state?.currentUserId)
        assertEquals(testDate, next.state?.selectedDate)
        assertEquals(listOf(DashboardCommand.Load(testUserId, testDate)), next.commands)
    }

    @Test
    fun `SelectDate updates selectedDate, sets Loading and emits Load command when userId exists`() {
        val initial = DashboardState(
            currentUserId = testUserId,
            selectedDate = testDate,
            content = DashboardState.Content.Success(
                profile = null,
                diary = DailyDiary(userId = testUserId, date = testDate),
            ),
        )
        val newDate = LocalDate(2026, 3, 16)
        val next = update.update(initial, DashboardEvent.Ui.SelectDate(newDate))

        assertIs<DashboardState.Content.Loading>(next.state?.content)
        assertEquals(newDate, next.state?.selectedDate)
        assertEquals(listOf(DashboardCommand.Load(testUserId, newDate)), next.commands)
    }

    @Test
    fun `ToggleMealSection toggles expansion in state`() {
        val initial = DashboardState(
            expandedMeals = setOf(MealType.DINNER),
            content = DashboardState.Content.Success(
                profile = null,
                diary = DailyDiary(userId = testUserId, date = testDate),
                expandedMeals = setOf(MealType.DINNER),
            ),
        )

        val nextExpand = update.update(initial, DashboardEvent.Ui.ToggleMealSection(MealType.BREAKFAST))
        val state1 = requireNotNull(nextExpand.state)
        assertTrue(state1.expandedMeals.contains(MealType.BREAKFAST))
        assertTrue(state1.expandedMeals.contains(MealType.DINNER))

        val nextCollapse = update.update(state1, DashboardEvent.Ui.ToggleMealSection(MealType.DINNER))
        val state2 = requireNotNull(nextCollapse.state)
        assertTrue(!state2.expandedMeals.contains(MealType.DINNER))
        assertTrue(state2.expandedMeals.contains(MealType.BREAKFAST))
    }

    @Test
    fun `AddMealOptimistic updates diary entries in Success state and emits AddMeal command`() {
        val initialDiary = DailyDiary(userId = testUserId, date = testDate, entries = emptyList())
        val initial = DashboardState(
            currentUserId = testUserId,
            selectedDate = testDate,
            content = DashboardState.Content.Success(
                profile = null,
                diary = initialDiary,
            ),
        )

        val next = update.update(initial, DashboardEvent.Ui.AddMealOptimistic(sampleEntry))
        val content = next.state?.content
        assertIs<DashboardState.Content.Success>(content)
        assertEquals(listOf(sampleEntry), content.diary.entries)
        assertEquals(listOf(DashboardCommand.AddMeal(sampleEntry)), next.commands)
    }

    @Test
    fun `DeleteMeal removes entry in Success state and emits DeleteMeal command`() {
        val initialDiary = DailyDiary(userId = testUserId, date = testDate, entries = listOf(sampleEntry))
        val initial = DashboardState(
            currentUserId = testUserId,
            selectedDate = testDate,
            content = DashboardState.Content.Success(
                profile = null,
                diary = initialDiary,
            ),
        )

        val next = update.update(initial, DashboardEvent.Ui.DeleteMeal(sampleEntry.id))
        val content = next.state?.content
        assertIs<DashboardState.Content.Success>(content)
        assertTrue(content.diary.entries.isEmpty())
        assertEquals(listOf(DashboardCommand.DeleteMeal(sampleEntry.id, testUserId, testDate)), next.commands)
    }

    @Test
    fun `Retry emits Load command when userId and selectedDate present`() {
        val initial = DashboardState(
            currentUserId = testUserId,
            selectedDate = testDate,
            content = DashboardState.Content.Error("Ошибка"),
        )

        val next = update.update(initial, DashboardEvent.Ui.Retry)
        assertIs<DashboardState.Content.Loading>(next.state?.content)
        assertEquals(listOf(DashboardCommand.Load(testUserId, testDate)), next.commands)
    }

    @Test
    fun `Internal Loaded transitions to Success state`() {
        val initial = DashboardState(content = DashboardState.Content.Loading)
        val diary = DailyDiary(userId = testUserId, date = testDate, entries = listOf(sampleEntry))

        val next = update.update(initial, DashboardEvent.Internal.Loaded(profile = null, diary = diary))
        val content = next.state?.content
        assertIs<DashboardState.Content.Success>(content)
        assertEquals(diary, content.diary)
    }

    @Test
    fun `Internal LoadFailed transitions to Error state`() {
        val initial = DashboardState(content = DashboardState.Content.Loading)

        val next = update.update(initial, DashboardEvent.Internal.LoadFailed(IllegalStateException("Network error")))
        val content = next.state?.content
        assertIs<DashboardState.Content.Error>(content)
        assertEquals("Network error", content.message)
    }

    @Test
    fun `Internal MutationFailed produces ShowSnackbar news`() {
        val initial = DashboardState()

        val next = update.update(initial, DashboardEvent.Internal.MutationFailed(IllegalStateException("Disk full")))
        assertEquals(listOf(DashboardNews.ShowSnackbar("Disk full")), next.news)
    }
}
