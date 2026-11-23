package dev.andre.nutritionist.android.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.presentation.dashboard.DashboardUiStateMapper
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeDiaryRepository : DiaryRepository {
    private val entries = mutableListOf<dev.andre.nutritionist.domain.model.DiaryEntry>()

    override suspend fun getDailyDiary(
        userId: UserId,
        date: LocalDate,
    ): Result<dev.andre.nutritionist.domain.model.DailyDiary> {
        val list = entries.filter { it.userId == userId && it.date == date }
        return Result.success(dev.andre.nutritionist.domain.model.DailyDiary(userId = userId, date = date, entries = list))
    }

    override fun observeDailyDiary(
        userId: UserId,
        date: LocalDate,
    ): kotlinx.coroutines.flow.Flow<dev.andre.nutritionist.domain.model.DailyDiary> = kotlinx.coroutines.flow.flow {
        val list = entries.filter { it.userId == userId && it.date == date }
        emit(dev.andre.nutritionist.domain.model.DailyDiary(userId = userId, date = date, entries = list))
    }

    override suspend fun addEntry(entry: dev.andre.nutritionist.domain.model.DiaryEntry): Result<dev.andre.nutritionist.domain.model.DiaryEntry> {
        entries.add(entry)
        return Result.success(entry)
    }

    override suspend fun deleteEntry(id: dev.andre.nutritionist.domain.model.DiaryEntryId): Result<Unit> {
        entries.removeAll { it.id == id }
        return Result.success(Unit)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `fitApp top bar title is displayed`() {
        composeTestRule.setContent {
            MainScreen()
        }

        composeTestRule.onNodeWithText("FitApp").assertIsDisplayed()
    }

    @Test
    fun `complete profile banner is displayed when showCompleteProfileBanner is true and click invokes callback`() {
        var completeClicked = false
        composeTestRule.setContent {
            MainScreen(
                showCompleteProfileBanner = true,
                onCompleteProfileClick = { completeClicked = true },
            )
        }

        val bannerTitle = context.getString(R.string.banner_complete_profile_title)
        val bannerSubtitle = context.getString(R.string.banner_complete_profile_subtitle)
        val bannerButton = context.getString(R.string.banner_complete_profile_button)

        composeTestRule.onNodeWithText(bannerTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(bannerSubtitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(bannerButton).performClick()

        assertTrue(completeClicked)
    }

    @Test
    fun `complete profile banner is not displayed when showCompleteProfileBanner is false`() {
        composeTestRule.setContent {
            MainScreen(
                showCompleteProfileBanner = false,
            )
        }

        val bannerTitle = context.getString(R.string.banner_complete_profile_title)
        composeTestRule.onNodeWithText(bannerTitle).assertDoesNotExist()
    }

    @Test
    fun `kpi bar displays nutrition targets and consumed macros from profile`() {
        val profile = Profile(
            id = ProfileId("p-test-1"),
            userId = UserId("u-test-1"),
            heightCm = 180,
            weightKg = 75.0,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATE,
            goalType = GoalType.MAINTAIN,
            nutritionGoal = NutritionGoal(
                targetCalories = 2200,
                targetProteinsGrams = 135.0,
                targetFatsGrams = 75.0,
                targetCarbsGrams = 260.0,
                targetWeightKg = 75.0,
            ),
            weeklyBudget = 5000.0,
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
        )
        val mapper = DashboardUiStateMapper()
        val state = mapper.map(profile = profile)

        composeTestRule.setContent {
            MainScreen(state = state)
        }

        // Проверяем отображение заголовков КБЖУ
        composeTestRule.onNodeWithText(context.getString(R.string.macro_fats)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.macro_carbs)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.macro_proteins)).performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText(context.getString(R.string.macro_calories)).onFirst().performScrollTo().assertIsDisplayed()

        // Проверяем отображение целевых показателей из профиля
        composeTestRule.onNodeWithText("/ 2200").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("/ 75 г").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("/ 260 г").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("/ 135 г").performScrollTo().assertIsDisplayed()

        // Проверяем текущее значение употреблённых калорий
        composeTestRule.onAllNodesWithText("85").onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `all four meal sections are displayed in order`() {
        val mapper = DashboardUiStateMapper()
        val state = mapper.map(profile = null)

        composeTestRule.setContent {
            MainScreen(state = state)
        }

        val breakfast = context.getString(R.string.meal_breakfast)
        val lunch = context.getString(R.string.meal_lunch)
        val dinner = context.getString(R.string.meal_dinner)
        val snack = context.getString(R.string.meal_snack)

        composeTestRule.onNodeWithText(breakfast).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(lunch).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(dinner).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(snack).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `dinner section expanded displays added dish with calories and quick actions`() {
        val mapper = DashboardUiStateMapper()
        val state = mapper.map(profile = null, expandedMeals = setOf(MealType.DINNER))

        composeTestRule.setContent {
            MainScreen(state = state)
        }

        composeTestRule.onNodeWithText("Йогурт епика").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1 порция").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.btn_quick_save_food)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.btn_quick_camera)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `clicking plus button on meal section invokes onAddMealClick with corresponding MealType`() {
        val mapper = DashboardUiStateMapper()
        val state = mapper.map(profile = null)

        var addedMealType: MealType? = null

        composeTestRule.setContent {
            MainScreen(
                state = state,
                onAddMealClick = { addedMealType = it },
            )
        }

        composeTestRule.onNodeWithTag("add_meal_BREAKFAST").performScrollTo().performClick()
        assertEquals(MealType.BREAKFAST, addedMealType)

        composeTestRule.onNodeWithTag("add_meal_LUNCH").performScrollTo().performClick()
        assertEquals(MealType.LUNCH, addedMealType)

        composeTestRule.onNodeWithTag("add_meal_DINNER").performScrollTo().performClick()
        assertEquals(MealType.DINNER, addedMealType)

        composeTestRule.onNodeWithTag("add_meal_SNACK").performScrollTo().performClick()
        assertEquals(MealType.SNACK, addedMealType)
    }

    @Test
    fun `clicking meal item invokes onMealItemClick with item id`() {
        val mapper = DashboardUiStateMapper()
        val state = mapper.map(profile = null, expandedMeals = setOf(MealType.DINNER))

        var clickedId: String? = null

        composeTestRule.setContent {
            MainScreen(
                state = state,
                onMealItemClick = { clickedId = it },
            )
        }

        composeTestRule.onNodeWithText("Йогурт епика").performScrollTo().performClick()
        assertEquals("dinner-epica-1", clickedId)
    }

    @Test
    fun `dashboard streak bar displays date and streak format`() {
        val mapper = DashboardUiStateMapper()
        val state = mapper.map(profile = null, streakDays = 3)

        composeTestRule.setContent {
            MainScreen(state = state)
        }

        composeTestRule.onNodeWithText("Сегодня").assertIsDisplayed()
        val expectedStreakText = context.getString(R.string.streak_full_format, 3)
        composeTestRule.onNodeWithText(expectedStreakText).assertIsDisplayed()
    }
}
