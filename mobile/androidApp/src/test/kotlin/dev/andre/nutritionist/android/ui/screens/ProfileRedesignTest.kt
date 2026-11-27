package dev.andre.nutritionist.android.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.model.UserRole
import dev.andre.nutritionist.presentation.profile.ProfileUiModel
import dev.andre.nutritionist.presentation.profile.ProfileUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProfileRedesignTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `profile screen renders fitapp_8 header with eyebrow title and notifications button`() {
        val state = ProfileUiState(
            content = ProfileUiState.Content.Success(sampleProfile(userRole = UserRole.CLIENT)),
        )
        var notificationsClicked = false

        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = true) {
                ProfileScreen(
                    state = state,
                    onEvent = {},
                    onNotificationsClick = { notificationsClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.profile_eyebrow)).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(context.getString(R.string.profile_title)).onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_diary)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_recipes)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_statistics)).assertIsDisplayed()
        composeTestRule.onNodeWithText("FitApp").assertDoesNotExist()

        composeTestRule.onAllNodesWithContentDescription(context.getString(R.string.profile_menu_notifications))
            .onFirst()
            .performClick()
        assertTrue(notificationsClicked)
    }

    @Test
    fun `identity card renders CLIENT badge real email biometrics and edit data trigger`() {
        var fillSurveyClicked = false

        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = true) {
                ProfileSuccessContent(
                    profile = sampleProfile(
                        email = "anna@nutrition.dev",
                        userRole = UserRole.CLIENT,
                    ),
                    onFillSurveyClick = { fillSurveyClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Анна Петрова").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.role_badge_client)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.role_badge_coach)).assertDoesNotExist()
        composeTestRule.onNodeWithText("anna@nutrition.dev").assertIsDisplayed()
        composeTestRule.onNodeWithText("172 см • Женский").assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.profile_edit_data)).performClick()
        assertTrue(fillSurveyClicked)
    }

    @Test
    fun `identity card renders COACH badge when userRole is COACH`() {
        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = false) {
                ProfileSuccessContent(
                    profile = sampleProfile(userRole = UserRole.COACH),
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.role_badge_coach)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.role_badge_client)).assertDoesNotExist()
    }

    @Test
    fun `identity card hides role badge and email when absent`() {
        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = false) {
                ProfileSuccessContent(
                    profile = sampleProfile(email = "", userRole = null),
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.role_badge_client)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.role_badge_coach)).assertDoesNotExist()
        composeTestRule.onNodeWithText("anna@nutrition.dev").assertDoesNotExist()
    }

    @Test
    fun `profile success content renders kbju card formatted budget and no hardcoded fake strings`() {
        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = true) {
                ProfileSuccessContent(
                    profile = sampleProfile(userRole = UserRole.CLIENT),
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.profile_health_params))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("65 ${context.getString(R.string.profile_unit_kg)}")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Умеренная").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("3–5 тренировок / нед").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Похудение").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_per_day_format, "500 ₽"))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("~3 500 ₽ в неделю").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_target_weight_format, "62 кг"))
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.profile_daily_kbju_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("2 200 ккал").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("140 г").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("70 г").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("250 г").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.profile_menu_meal_plan))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Похудение • 2 200 ккал").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_notifications_on))
            .performScrollTo()
            .assertIsDisplayed()

        // Запрещённые фейковые заглушки из референса / KAN-75 не должны присутствовать
        listOf(
            "PRO",
            "john.athlete@fitapp.io",
            "14 дней подряд",
            "Уровень 4",
            "↓ 1.4 кг",
            "Синхронизировано с Health",
            "v2.4.1",
        ).forEach { forbidden ->
            composeTestRule.onNodeWithText(forbidden).assertDoesNotExist()
        }
    }

    @Test
    fun `profile success content renders cleanly in light theme`() {
        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = false) {
                ProfileSuccessContent(
                    profile = sampleProfile(userRole = UserRole.CLIENT),
                )
            }
        }

        composeTestRule.onNodeWithText("Анна Петрова").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_logout_button))
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun sampleProfile(
        email: String = "anna@nutrition.dev",
        userRole: UserRole? = UserRole.CLIENT,
    ) = ProfileUiModel(
        id = ProfileId("p-redesign-1"),
        userId = UserId("u-redesign-1"),
        displayName = "Анна Петрова",
        email = email,
        userRole = userRole,
        heightLabel = "172 см",
        weightLabel = "65.0 кг",
        weightValueLabel = "65",
        genderLabel = "Женский",
        activityLabel = "Умеренная активность",
        activityShortLabel = "Умеренная",
        activitySubtitle = "3–5 тренировок / нед",
        goalLabel = "Снижение веса",
        goalShortLabel = "Похудение",
        caloriesGoalLabel = "2200 ккал",
        proteinsGoalLabel = "140 г",
        fatsGoalLabel = "70 г",
        carbsGoalLabel = "250 г",
        weightGoalLabel = "62 кг",
        weeklyBudgetLabel = "3500 ₽ / нед.",
        dailyBudgetLabel = "500 ₽",
        dailyBudgetValueLabel = "500",
        weeklyBudgetFormattedLabel = "3 500 ₽",
        weeklyBudgetApproxLabel = "~3 500 ₽ в неделю",
        caloriesGoalFormatted = "2 200 ккал",
        proteinsGoalFormatted = "140 г",
        fatsGoalFormatted = "70 г",
        carbsGoalFormatted = "250 г",
        planSummaryLabel = "Похудение • 2 200 ккал",
        rawWeightKg = 65.0,
        rawActivityLevel = ActivityLevel.MODERATE,
        rawGoalType = GoalType.LOSE_WEIGHT,
        rawWeeklyBudget = 3500.0,
    )
}
