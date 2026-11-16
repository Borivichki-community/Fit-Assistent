package dev.andre.nutritionist.android.presentation.profile.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.presentation.profile.validation.ProfileFieldsValidation
import dev.andre.nutritionist.android.presentation.profile.validation.ProfileValidationError
import dev.andre.nutritionist.android.ui.screens.ProfileSuccessContent
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
        assertEquals(ProfileValidationError.Empty, ProfileFieldsValidation.validateName(""))
        assertEquals(ProfileValidationError.Empty, ProfileFieldsValidation.validateName("   "))
        assertEquals(ProfileValidationError.TooShort(2), ProfileFieldsValidation.validateName("A"))
        assertNull(ProfileFieldsValidation.validateName("Aleksey"))

        // Age validation
        assertEquals(ProfileValidationError.Empty, ProfileFieldsValidation.validateAge(""))
        assertEquals(ProfileValidationError.NotANumber, ProfileFieldsValidation.validateAge("abc"))
        assertEquals(ProfileValidationError.NegativeOrZero, ProfileFieldsValidation.validateAge("-5"))
        assertEquals(ProfileValidationError.NegativeOrZero, ProfileFieldsValidation.validateAge("0"))
        assertEquals(ProfileValidationError.OutOfRange(14, 120), ProfileFieldsValidation.validateAge("10"))
        assertEquals(ProfileValidationError.OutOfRange(14, 120), ProfileFieldsValidation.validateAge("150"))
        assertNull(ProfileFieldsValidation.validateAge("25"))

        // Weight validation
        assertEquals(ProfileValidationError.Empty, ProfileFieldsValidation.validateTargetWeight(""))
        assertEquals(ProfileValidationError.NotANumber, ProfileFieldsValidation.validateTargetWeight("invalid"))
        assertEquals(ProfileValidationError.NegativeOrZero, ProfileFieldsValidation.validateTargetWeight("-70.0"))
        assertEquals(ProfileValidationError.OutOfRange(30.0, 300.0), ProfileFieldsValidation.validateTargetWeight("20.0"))
        assertEquals(ProfileValidationError.OutOfRange(30.0, 300.0), ProfileFieldsValidation.validateTargetWeight("350.0"))
        assertNull(ProfileFieldsValidation.validateTargetWeight("75.5"))

        // Calories validation
        assertEquals(ProfileValidationError.Empty, ProfileFieldsValidation.validateDailyCalories(""))
        assertEquals(ProfileValidationError.NotANumber, ProfileFieldsValidation.validateDailyCalories("xyz"))
        assertEquals(ProfileValidationError.NegativeOrZero, ProfileFieldsValidation.validateDailyCalories("0"))
        assertEquals(ProfileValidationError.OutOfRange(500, 10000), ProfileFieldsValidation.validateDailyCalories("300"))
        assertEquals(ProfileValidationError.OutOfRange(500, 10000), ProfileFieldsValidation.validateDailyCalories("12000"))
        assertNull(ProfileFieldsValidation.validateDailyCalories("2200"))
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

    private fun createTestProfileUiModel() = ProfileUiModel(
        id = ProfileId("p-test-01"),
        userId = UserId("u-test-01"),
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
    )
}
