package dev.andre.nutritionist.android.ui.screens.statistics

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.domain.model.AnalyticsPeriod
import dev.andre.nutritionist.presentation.statistics.DailyBarUiModel
import dev.andre.nutritionist.presentation.statistics.StatisticsSuccessUiModel
import dev.andre.nutritionist.presentation.statistics.StatisticsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StatisticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleModel = StatisticsSuccessUiModel(
        caloriesSummaryText = "2 175 ккал",
        targetCaloriesText = "из 2 200 ккал/день",
        complianceBadgeText = "96% норма",
        isComplianceHigh = true,
        caloriesChartBars = listOf(
            DailyBarUiModel("Пн", "2100", 0.95f, true),
            DailyBarUiModel("Вт", "2250", 1.0f, true),
            DailyBarUiModel("Ср", "2180", 0.98f, true),
        ),
        macroProteinsText = "135 г",
        macroFatsText = "68 г",
        macroCarbsText = "245 г",
        proteinsFraction = 0.30f,
        fatsFraction = 0.25f,
        carbsFraction = 0.45f,
        totalSpentText = "2 580 ₽",
        totalBudgetText = "2 800 ₽",
        savedText = "+220 ₽ экономия",
        budgetBurnPercentText = "92% бюджета",
        budgetBurnFraction = 0.92f,
        averageMealCostText = "~92 ₽ / порция",
    )

    @Test
    fun loading_state_displays_progress_indicator() {
        composeTestRule.setContent {
            StatisticsScreen(
                state = StatisticsUiState(
                    content = StatisticsUiState.Content.Loading,
                ),
            )
        }

        composeTestRule.onNodeWithTag("statistics_loading").assertIsDisplayed()
    }

    @Test
    fun success_state_displays_metrics_and_cards() {
        composeTestRule.setContent {
            StatisticsScreen(
                state = StatisticsUiState(
                    selectedPeriod = AnalyticsPeriod.WEEK,
                    content = StatisticsUiState.Content.Success(sampleModel),
                ),
            )
        }

        composeTestRule.onNodeWithTag("statistics_top_bar_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("statistics_period_selector").assertIsDisplayed()
        composeTestRule.onNodeWithTag("statistics_calories_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("statistics_calories_summary_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 175 ккал").assertIsDisplayed()
        composeTestRule.onNodeWithText("96% норма").assertIsDisplayed()

        composeTestRule.onNodeWithTag("statistics_macros_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("statistics_macro_proteins_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("135 г").assertIsDisplayed()

        composeTestRule.onNodeWithTag("statistics_budget_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("statistics_budget_spent_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 580 ₽").assertIsDisplayed()
        composeTestRule.onNodeWithText("+220 ₽ экономия").assertIsDisplayed()
    }

    @Test
    fun success_state_clicking_period_month_invokes_callback() {
        var selectedPeriod: AnalyticsPeriod? = null
        composeTestRule.setContent {
            StatisticsScreen(
                state = StatisticsUiState(
                    selectedPeriod = AnalyticsPeriod.WEEK,
                    content = StatisticsUiState.Content.Success(sampleModel),
                ),
                onPeriodSelected = { selectedPeriod = it },
            )
        }

        composeTestRule.onNodeWithTag("statistics_period_MONTH").performClick()
        assertEquals(AnalyticsPeriod.MONTH, selectedPeriod)
    }

    @Test
    fun error_state_displays_message_and_retry_invokes_callback() {
        var retryClicked = false
        composeTestRule.setContent {
            StatisticsScreen(
                state = StatisticsUiState(
                    content = StatisticsUiState.Content.Error("Ошибка получения аналитики"),
                ),
                onRetryClicked = { retryClicked = true },
            )
        }

        composeTestRule.onNodeWithTag("statistics_error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ошибка получения аналитики").assertIsDisplayed()
        composeTestRule.onNodeWithTag("statistics_retry_button").performClick()
        assertTrue(retryClicked)
    }

    @Test
    fun bottom_bar_navigation_invokes_callback() {
        val context: Context = ApplicationProvider.getApplicationContext()
        var selectedDest: AppDestination? = null
        composeTestRule.setContent {
            StatisticsScreen(
                state = StatisticsUiState(
                    content = StatisticsUiState.Content.Loading,
                ),
                onDestinationSelected = { selectedDest = it },
            )
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.nav_diary)).performClick()
        assertEquals(AppDestination.Dashboard, selectedDest)
    }
}
