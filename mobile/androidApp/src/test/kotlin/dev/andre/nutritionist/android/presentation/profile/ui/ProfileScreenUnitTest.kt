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
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileState
import org.junit.Assert.assertEquals
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
}
