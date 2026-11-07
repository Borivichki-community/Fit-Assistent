package dev.andre.nutritionist.android.presentation.profile.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Изолированные UI-тесты компонента [ProfileScreen].
 * Выполняются на фиксированном State без использования мок-библиотек (NO MockK).
 */
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testCase1_displaysValidationErrorForInvalidName() {
        // Arrange
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

        // Act
        composeTestRule.setContent {
            ProfileScreen(
                state = fixedStateWithValidationError,
                onEvent = {},
            )
        }

        // Assert
        composeTestRule.onNodeWithText(expectedErrorText).assertIsDisplayed()
    }

    @Test
    fun testCase2_clickSaveButtonDispatchesCorrectSaveProfileUiEvent() {
        // Arrange
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

        // Act
        val saveButtonText = context.getString(R.string.profile_save_button)
        composeTestRule.onNodeWithText(saveButtonText).performScrollTo().performClick()

        // Assert
        assertEquals(1, dispatchCount)
        assertNotNull(capturedEvent)
        assertTrue(capturedEvent is ProfileEvent.Ui.SaveProfile)

        val saveEvent = capturedEvent as ProfileEvent.Ui.SaveProfile
        assertEquals("Aleksey", saveEvent.name)
        assertEquals("25", saveEvent.age)
        assertEquals("75.0", saveEvent.targetWeight)
        assertEquals("2200", saveEvent.dailyCalories)
    }
}
