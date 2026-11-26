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
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
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
import dev.andre.nutritionist.presentation.dashboard.DiaryState
import dev.andre.nutritionist.presentation.dashboard.DiaryUiState
import dev.andre.nutritionist.presentation.dashboard.DiaryUiStateMapper
import dev.andre.nutritionist.presentation.mappers.DailyNutritionUiMapper.toUiModel
import dev.andre.nutritionist.presentation.mappers.MealRecordUiMapper.toUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val satNov7Ms = Instant.parse("2026-11-07T12:00:00Z").toEpochMilliseconds()
    private val mapper = DiaryUiStateMapper(
        nowProvider = { satNov7Ms },
        timeZone = TimeZone.UTC,
    )

    @After
    fun teardown() {
        stopKoin()
    }

    private fun sampleEpicaUiState(
        showCompleteProfileBanner: Boolean = false,
        withEpicaRecord: Boolean = true,
    ): DiaryUiState {
        val goal = NutritionGoal(
            targetCalories = 2957,
            targetProteinsGrams = 170.0,
            targetFatsGrams = 85.0,
            targetCarbsGrams = 378.0,
        )
        val records = if (withEpicaRecord) {
            listOf(
                MealRecord(
                    id = MealRecordId("seed-epica-lunch-1"),
                    mealType = MealType.LUNCH,
                    recordedAt = Instant.fromEpochMilliseconds(satNov7Ms),
                    sourceType = EntrySourceType.PRODUCT,
                    sourceId = "20000000-0000-0000-0000-000000000112",
                    nameSnapshot = "Йогурт Epica",
                    weightGrams = 130.0,
                    nutrition = NutritionFacts(calories = 85.0, proteins = 4.93, fats = 1.25, carbs = 13.8),
                    cost = Money.fromMinor(9500L),
                    idempotencyKey = IdempotencyKey("seed-epica-lunch-1"),
                    syncStatus = SyncStatus.SYNCED,
                ),
            )
        } else {
            emptyList()
        }
        val consumed = ConsumedNutrition(
            calories = records.sumOf { it.nutrition.calories }.toInt(),
            proteinsGrams = records.sumOf { it.nutrition.proteins },
            fatsGrams = records.sumOf { it.nutrition.fats },
            carbsGrams = records.sumOf { it.nutrition.carbs },
        )
        val balance = DailyNutritionBalanceCalculator.calculate(goal = goal, consumed = consumed)
        val totalCost = if (withEpicaRecord) Money.fromMinor(9500L) else Money.ZERO
        val uiItems = records.map { it.toUiModel(targetCalories = goal.targetCalories, timeZone = TimeZone.UTC) }

        return mapper.map(
            DiaryState(
                selectedDateEpochMs = satNov7Ms,
                items = uiItems,
                dailySummary = balance.toUiModel(totalCost = totalCost, weeklyBudgetRubles = 5000.0),
                loggedDatesInWeek = if (withEpicaRecord) setOf(LocalDate(2026, 11, 7)) else emptySet(),
                showCompleteProfileBanner = showCompleteProfileBanner,
                content = if (withEpicaRecord) DiaryState.Content.Success else DiaryState.Content.Empty,
            ),
        )
    }

    @Test
    fun `fitApp top bar title and calendar header are displayed`() {
        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = true) {
                MainScreen(state = sampleEpicaUiState())
            }
        }

        composeTestRule.onNodeWithText("FitApp").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ноябрь 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("НЕДЕЛЯ 45").assertIsDisplayed()
    }

    @Test
    fun `complete profile banner shown when enabled and triggers callback`() {
        var clicked = false
        composeTestRule.setContent {
            FitAssistantTheme {
                MainScreen(
                    state = sampleEpicaUiState(showCompleteProfileBanner = true),
                    onCompleteProfileClick = { clicked = true },
                )
            }
        }

        val bannerButton = context.getString(R.string.banner_complete_profile_button)
        composeTestRule.onNodeWithText(bannerButton).performClick()
        assertTrue(clicked)
    }

    @Test
    fun `kpi bar displays real consumed macros, profile targets, cost and budget`() {
        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = true) {
                MainScreen(state = sampleEpicaUiState())
            }
        }

        composeTestRule.onNodeWithText("1,25").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("/ 85 г").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("13,8").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("/ 378 г").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("4,93").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("/ 170 г").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("3%").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("85").onFirst().performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("/ 2 957").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Бюджет: ~714 ₽").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `lunch section renders Epica Yogurt card and delete triggers callback`() {
        var deletedId: MealRecordId? = null
        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = false) {
                MainScreen(
                    state = sampleEpicaUiState(),
                    onDeleteRecordClick = { deletedId = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Йогурт Epica").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("85 ккал").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("95 ₽").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("3% от суточной нормы").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("delete_meal_seed-epica-lunch-1").performScrollTo().performClick()
        assertEquals(MealRecordId("seed-epica-lunch-1"), deletedId)
    }

    @Test
    fun `calendar navigation arrows and day selection invoke callbacks`() {
        var selectedEpoch: Long? = null
        var prevClicked = false
        var nextClicked = false

        composeTestRule.setContent {
            FitAssistantTheme {
                MainScreen(
                    state = sampleEpicaUiState(),
                    onDaySelected = { selectedEpoch = it },
                    onPreviousWeekClick = { prevClicked = true },
                    onNextWeekClick = { nextClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("calendar_prev_week").performClick()
        assertTrue(prevClicked)

        composeTestRule.onNodeWithTag("calendar_next_week").performClick()
        assertTrue(nextClicked)

        composeTestRule.onNodeWithTag("calendar_day_2026-11-05").performClick()
        assertEquals(Instant.parse("2026-11-05T12:00:00Z").toEpochMilliseconds(), selectedEpoch)
    }

    @Test
    fun `empty day displays zero metrics and compact slot placeholders`() {
        composeTestRule.setContent {
            FitAssistantTheme {
                MainScreen(state = sampleEpicaUiState(withEpicaRecord = false))
            }
        }

        composeTestRule.onAllNodesWithText("0").onFirst().performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("0%").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.diary_empty_ai_title)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.diary_empty_snack_title)).performScrollTo().assertIsDisplayed()
    }
}
