package dev.andre.nutritionist.android.presentation.profile.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.presentation.profile.validation.toMessageRes
import dev.andre.nutritionist.android.ui.screens.ProfileSuccessContent
import dev.andre.nutritionist.domain.validation.FieldValidationError
import dev.andre.nutritionist.domain.validation.ProfileValidator
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileState
import dev.andre.nutritionist.presentation.profile.ProfileUiModel
import dev.andre.nutritionist.presentation.profile.ProfileUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProfileScreenUnitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testCase1_displaysValidationErrorForInvalidName() {
        val expectedErrorText = context.getString(R.string.validation_error_name_empty)
        val fixedStateWithValidationError = ProfileState(
            content = ProfileState.Content.ValidationError(
                name = "",
                age = "25",
                targetWeight = "70.0",
                dailyCalories = "2000",
                nameError = expectedErrorText,
            )
        )

        composeTestRule.setContent {
            ProfileScreen(
                state = fixedStateWithValidationError,
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText(expectedErrorText).assertIsDisplayed()
    }

    @Test
    fun testCase2_clickSaveButtonDispatchesCorrectSaveProfileUiEvent() {
        val fixedValidState = ProfileState(
            content = ProfileState.Content.Form(
                name = "Aleksey",
                age = "25",
                targetWeight = "75.0",
                dailyCalories = "2200",
            )
        )
        var capturedEvent: ProfileEvent.Ui? = null
        var dispatchCount = 0

        composeTestRule.setContent {
            ProfileScreen(
                state = fixedValidState,
                onEvent = { event ->
                    capturedEvent = event
                    dispatchCount++
                },
            )
        }

        val saveButtonText = context.getString(R.string.profile_save_button)
        composeTestRule.onNodeWithText(saveButtonText).performScrollTo().performClick()

        assertEquals(1, dispatchCount)
        assertNotNull(capturedEvent)
        assertTrue(capturedEvent is ProfileEvent.Ui.SaveProfile)

        val saveEvent = capturedEvent as ProfileEvent.Ui.SaveProfile
        assertEquals("Aleksey", saveEvent.name)
        assertEquals("25", saveEvent.age)
        assertEquals("75.0", saveEvent.targetWeight)
        assertEquals("2200", saveEvent.dailyCalories)
    }

    @Test
    fun testValidation_pureFunctionsVerifyAllConstraints() {
        // Name validation
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateName(""))
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateName("   "))
        assertEquals(FieldValidationError.TooShort(2), ProfileValidator.validateName("A"))
        assertNull(ProfileValidator.validateName("Aleksey"))

        // Age validation
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateAge(""))
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateAge("abc"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateAge("-5"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateAge("0"))
        assertEquals(FieldValidationError.OutOfRange(14, 120), ProfileValidator.validateAge("10"))
        assertEquals(FieldValidationError.OutOfRange(14, 120), ProfileValidator.validateAge("150"))
        assertNull(ProfileValidator.validateAge("25"))

        // Weight validation (20.0..500.0 aligned with backend)
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateTargetWeight(""))
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateTargetWeight("invalid"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateTargetWeight("-70.0"))
        assertEquals(FieldValidationError.OutOfRange(20.0, 500.0), ProfileValidator.validateTargetWeight("15.0"))
        assertEquals(FieldValidationError.OutOfRange(20.0, 500.0), ProfileValidator.validateTargetWeight("550.0"))
        assertNull(ProfileValidator.validateTargetWeight("75.5"))

        // Calories validation
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateDailyCalories(""))
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateDailyCalories("xyz"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateDailyCalories("0"))
        assertEquals(FieldValidationError.OutOfRange(500, 10000), ProfileValidator.validateDailyCalories("300"))
        assertEquals(FieldValidationError.OutOfRange(500, 10000), ProfileValidator.validateDailyCalories("12000"))
        assertNull(ProfileValidator.validateDailyCalories("2200"))

        // String resource mapping
        assertEquals(R.string.validation_error_empty, FieldValidationError.Empty.toMessageRes())
        assertEquals(R.string.validation_error_not_a_number, FieldValidationError.NotANumber.toMessageRes())
        assertEquals(R.string.validation_error_negative_or_zero, FieldValidationError.NegativeOrZero.toMessageRes())
        assertEquals(R.string.validation_error_out_of_range, FieldValidationError.OutOfRange(1, 10).toMessageRes())
        assertEquals(R.string.validation_error_too_short, FieldValidationError.TooShort(2).toMessageRes())
    }

    @Test
    fun testCase3_displaysLogoutButtonOnProfileScreen() {
        val fixedValidState = ProfileState(
            content = ProfileState.Content.Form(
                name = "Aleksey",
                age = "25",
                targetWeight = "75.0",
                dailyCalories = "2200",
            )
        )

        composeTestRule.setContent {
            ProfileScreen(
                state = fixedValidState,
                onEvent = {},
                onLogoutClick = {},
            )
        }

        val logoutButtonText = context.getString(R.string.profile_logout_button)
        composeTestRule.onNodeWithText(logoutButtonText).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testCase4_clickLogoutButtonInvokesOnLogoutClick() {
        val fixedValidState = ProfileState(
            content = ProfileState.Content.Form(
                name = "Aleksey",
                age = "25",
                targetWeight = "75.0",
                dailyCalories = "2200",
            )
        )
        var logoutClicked = false

        composeTestRule.setContent {
            ProfileScreen(
                state = fixedValidState,
                onEvent = {},
                onLogoutClick = { logoutClicked = true },
            )
        }

        val logoutButtonText = context.getString(R.string.profile_logout_button)
        composeTestRule.onNodeWithText(logoutButtonText).performScrollTo().performClick()

        assertTrue(logoutClicked)
    }

    @Test
    fun testCase5_logoutButtonDisabledWhenIsLoggingOut() {
        val fixedValidState = ProfileState(
            content = ProfileState.Content.Form(
                name = "Aleksey",
                age = "25",
                targetWeight = "75.0",
                dailyCalories = "2200",
            )
        )
        var logoutClicked = false

        composeTestRule.setContent {
            ProfileScreen(
                state = fixedValidState,
                onEvent = {},
                onLogoutClick = { logoutClicked = true },
                isLoggingOut = true,
            )
        }

        val logoutButtonText = context.getString(R.string.profile_logout_button)
        composeTestRule.onNodeWithText(logoutButtonText).performScrollTo().performClick()

        assertFalse(logoutClicked)
    }

    @Test
    fun testCase6_displaysLogoutRowOnProfileSuccessContent() {
        val profile = createTestProfileUiModel()
        var logoutClicked = false

        composeTestRule.setContent {
            ProfileSuccessContent(
                profile = profile,
                onEditClick = {},
                onLogoutClick = { logoutClicked = true },
            )
        }

        val logoutText = context.getString(R.string.profile_logout_button)
        composeTestRule.onNodeWithText(logoutText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(logoutText).performClick()

        assertTrue(logoutClicked)
    }

    @Test
    fun testCase7_logoutRowDisabledWhenLoggingOutOnProfileSuccessContent() {
        val profile = createTestProfileUiModel()
        var logoutClicked = false

        composeTestRule.setContent {
            ProfileSuccessContent(
                profile = profile,
                onEditClick = {},
                isLoggingOut = true,
                onLogoutClick = { logoutClicked = true },
            )
        }

        val logoutText = context.getString(R.string.profile_logout_button)
        composeTestRule.onNodeWithText(logoutText).performScrollTo().performClick()

        assertFalse(logoutClicked)
    }

    @Test
    fun testCase8_displaysAndTriggersLogoutOnFullProfileScreen() {
        val profile = createTestProfileUiModel()
        var logoutClicked = false
        val successState = ProfileUiState(
            content = ProfileUiState.Content.Success(profile = profile),
        )

        composeTestRule.setContent {
            dev.andre.nutritionist.android.ui.screens.ProfileScreen(
                state = successState,
                onEvent = {},
                onLogoutClick = { logoutClicked = true },
            )
        }

        val logoutText = context.getString(R.string.profile_logout_button)
        composeTestRule.onNodeWithText(logoutText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(logoutText).performClick()

        assertTrue(logoutClicked)
    }

    @Test
    fun testCase9_displaysActualDisplayNameNotDefaultJohnOnProfileSuccessContent() {
        val testName = "Aleksey Codintsev"
        val profile = createTestProfileUiModel(displayName = testName)

        composeTestRule.setContent {
            ProfileSuccessContent(
                profile = profile,
                onEditClick = {},
                onLogoutClick = {},
            )
        }

        // Must display actual name
        composeTestRule.onNodeWithText(testName).assertIsDisplayed()
        // Must NOT display hardcoded "John"
        val defaultJohn = context.getString(R.string.profile_default_user_name)
        composeTestRule.onNodeWithText(defaultJohn).assertDoesNotExist()
    }

    @Test
    fun testCase10_displaysMetricsMatchingProfileUiModel() {
        val profile = createTestProfileUiModel(
            displayName = "Maria Ivanova",
        ).copy(
            weightLabel = "62.5 кг",
            heightLabel = "168 см",
            activityLabel = "Лёгкая активность",
            goalLabel = "Снижение веса",
            weeklyBudgetLabel = "4500 ₽ / нед.",
        )

        composeTestRule.setContent {
            ProfileSuccessContent(
                profile = profile,
                onEditClick = {},
                onLogoutClick = {},
            )
        }

        composeTestRule.onNodeWithText("62.5 кг").assertIsDisplayed()
        composeTestRule.onNodeWithText("Лёгкая активность").assertIsDisplayed()
        composeTestRule.onNodeWithText("Снижение веса").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("4500 ₽ / нед.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testCase11_displaysActualDisplayNameAndAgeNotJohnAnd28OnProfileScreenSuccessState() {
        val testProfile = dev.andre.nutritionist.domain.mapper.OnboardingDataMapper.toProfile(
            data = dev.andre.nutritionist.domain.model.OnboardingData(
                firstName = "Alexey",
                heightCm = "180",
                weightKg = "75.0",
                age = "32",
            ),
            userId = UserId("user-alex-123"),
        )

        val state = ProfileState(
            content = ProfileState.Content.Success(
                profile = testProfile,
                displayName = "Alexey",
                age = "32",
            ),
            displayName = "Alexey",
            age = "32",
        )

        composeTestRule.setContent {
            ProfileScreen(
                state = state,
                onEvent = {},
            )
        }

        // Must display "Alexey" and "32"
        composeTestRule.onNodeWithText("Alexey").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("32").performScrollTo().assertIsDisplayed()

        // Must NOT display "John" and "28"
        val defaultJohn = context.getString(R.string.profile_default_user_name)
        composeTestRule.onNodeWithText(defaultJohn).assertDoesNotExist()
        composeTestRule.onNodeWithText("28").assertDoesNotExist()
    }

    @Test
    fun testCase12_displaysSetupProfileButtonWhenProfileNotFound() {
        var createProfileClicked = false
        val notFoundState = ProfileUiState(
            content = ProfileUiState.Content.Error(message = "NOT_FOUND: Profile not found"),
        )

        composeTestRule.setContent {
            dev.andre.nutritionist.android.ui.screens.ProfileScreen(
                state = notFoundState,
                onEvent = {},
                onCreateProfileClick = { createProfileClicked = true },
            )
        }

        val expectedMessage = context.getString(R.string.profile_not_found_message)
        composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()

        val fillSurveyText = context.getString(R.string.profile_fill_survey_again)
        composeTestRule.onNodeWithText(fillSurveyText).performClick()

        assertTrue(createProfileClicked)
    }

    @Test
    fun testCase13_displaysFillSurveyBannerWhenProfileIsIncomplete() {
        var fillSurveyClicked = false
        val incompleteProfile = createTestProfileUiModel().copy(isComplete = false)
        val successState = ProfileUiState(
            content = ProfileUiState.Content.Success(incompleteProfile),
        )

        composeTestRule.setContent {
            dev.andre.nutritionist.android.ui.screens.ProfileScreen(
                state = successState,
                onEvent = {},
                onCreateProfileClick = { fillSurveyClicked = true },
            )
        }

        val bannerTitle = context.getString(R.string.banner_complete_profile_title)
        composeTestRule.onNodeWithText(bannerTitle).assertIsDisplayed()

        val fillSurveyText = context.getString(R.string.profile_fill_survey_again)
        composeTestRule.onAllNodesWithText(fillSurveyText).onFirst().performClick()

        assertTrue(fillSurveyClicked)
    }

    private fun createTestProfileUiModel(displayName: String = "Aleksey Codintsev") = ProfileUiModel(
        id = ProfileId("p-test-01"),
        userId = UserId("u-test-01"),
        displayName = displayName,
        heightLabel = "180 см",
        weightLabel = "75 кг",
        genderLabel = "Мужской",
        activityLabel = "Умеренная",
        goalLabel = "Похудение",
        caloriesGoalLabel = "2200 ккал",
        proteinsGoalLabel = "140 г",
        fatsGoalLabel = "70 г",
        carbsGoalLabel = "250 г",
        weightGoalLabel = "70 кг",
        weeklyBudgetLabel = "3500 ₽",
        rawWeightKg = 75.0,
        rawActivityLevel = ActivityLevel.MODERATE,
        rawGoalType = GoalType.LOSE_WEIGHT,
        rawWeeklyBudget = 3500.0,
    )

    @Test
    fun testCase14_clickWeightCardOpensEditWeightDialogAndDispatchesChangeWeight() {
        val profile = createTestProfileUiModel()
        var capturedEvent: ProfileEvent.Ui? = null

        composeTestRule.setContent {
            ProfileSuccessContent(
                profile = profile,
                onEvent = { capturedEvent = it },
            )
        }

        // Клик по карточке веса
        composeTestRule.onNodeWithText("75 кг").performClick()

        // Проверяем отображение диалога
        val dialogTitle = context.getString(R.string.dialog_edit_weight_title)
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        // Вводим новый вес
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("80.5")

        // Нажимаем Сохранить
        val saveText = context.getString(R.string.common_save)
        composeTestRule.onNodeWithText(saveText).performClick()

        assertNotNull(capturedEvent)
        assertTrue(capturedEvent is ProfileEvent.Ui.ChangeWeight)
        assertEquals(80.5, (capturedEvent as ProfileEvent.Ui.ChangeWeight).weightKg, 0.001)
    }

    @Test
    fun testCase15_clickActivityCardOpensEditActivityDialogAndDispatchesChangeActivity() {
        val profile = createTestProfileUiModel()
        var capturedEvent: ProfileEvent.Ui? = null

        composeTestRule.setContent {
            ProfileSuccessContent(
                profile = profile,
                onEvent = { capturedEvent = it },
            )
        }

        // Клик по карточке активности
        composeTestRule.onNodeWithText("Умеренная").performClick()

        // Диалог выбора активности открыт
        val dialogTitle = context.getString(R.string.dialog_edit_activity_title)
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        // Выбираем Высокую активность
        val veryActiveOption = context.getString(R.string.onboarding_activity_very_active)
        composeTestRule.onNodeWithText(veryActiveOption).performClick()

        // Нажимаем Сохранить
        val saveText = context.getString(R.string.common_save)
        composeTestRule.onNodeWithText(saveText).performClick()

        assertNotNull(capturedEvent)
        assertTrue(capturedEvent is ProfileEvent.Ui.ChangeActivity)
        assertEquals(ActivityLevel.VERY_ACTIVE, (capturedEvent as ProfileEvent.Ui.ChangeActivity).activityLevel)
    }

    @Test
    fun testCase16_clickGoalCardOpensEditGoalDialogAndDispatchesChangeGoal() {
        val profile = createTestProfileUiModel()
        var capturedEvent: ProfileEvent.Ui? = null

        composeTestRule.setContent {
            ProfileSuccessContent(
                profile = profile,
                onEvent = { capturedEvent = it },
            )
        }

        // Клик по карточке цели
        composeTestRule.onNodeWithText("Похудение").performClick()

        // Диалог цели открыт
        val dialogTitle = context.getString(R.string.dialog_edit_goal_title)
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        // Выбираем Набор массы
        val gainGoalOption = context.getString(R.string.onboarding_goal_gain)
        composeTestRule.onNodeWithText(gainGoalOption).performClick()

        // Нажимаем Сохранить
        val saveText = context.getString(R.string.common_save)
        composeTestRule.onNodeWithText(saveText).performClick()

        assertNotNull(capturedEvent)
        assertTrue(capturedEvent is ProfileEvent.Ui.ChangeGoal)
        assertEquals(GoalType.GAIN_WEIGHT, (capturedEvent as ProfileEvent.Ui.ChangeGoal).goalType)
    }

    @Test
    fun testCase17_clickBudgetCardOpensEditBudgetDialogAndDispatchesChangeBudget() {
        val profile = createTestProfileUiModel()
        var capturedEvent: ProfileEvent.Ui? = null

        composeTestRule.setContent {
            ProfileSuccessContent(
                profile = profile,
                onEvent = { capturedEvent = it },
            )
        }

        // Клик по карточке бюджета
        composeTestRule.onNodeWithText("3500 ₽").performClick()

        // Диалог бюджета открыт
        val dialogTitle = context.getString(R.string.dialog_edit_budget_title)
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        // Вводим новый бюджет
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("6500")

        // Нажимаем Сохранить
        val saveText = context.getString(R.string.common_save)
        composeTestRule.onNodeWithText(saveText).performClick()

        assertNotNull(capturedEvent)
        assertTrue(capturedEvent is ProfileEvent.Ui.ChangeBudget)
        assertEquals(6500.0, (capturedEvent as ProfileEvent.Ui.ChangeBudget).weeklyBudget, 0.001)
    }

    @Test
    fun testCase18_invalidWeightShowsErrorAndDoesNotDispatch() {
        val profile = createTestProfileUiModel()
        var capturedEvent: ProfileEvent.Ui? = null

        composeTestRule.setContent {
            ProfileSuccessContent(
                profile = profile,
                onEvent = { capturedEvent = it },
            )
        }

        // Клик по карточке веса
        composeTestRule.onNodeWithText("75 кг").performClick()

        // Вводим некорректный вес (5.0 кг < 20.0 кг)
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("5.0")

        // Нажимаем Сохранить
        val saveText = context.getString(R.string.common_save)
        composeTestRule.onNodeWithText(saveText).performClick()

        // Ошибка валидации отображается, событие не отправлено
        val errorText = context.getString(R.string.validation_invalid_weight)
        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()
        assertNull(capturedEvent)
    }
}
