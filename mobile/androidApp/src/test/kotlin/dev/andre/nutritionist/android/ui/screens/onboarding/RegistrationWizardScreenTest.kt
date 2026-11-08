package dev.andre.nutritionist.android.ui.screens.onboarding

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RegistrationWizardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `step 0 shows welcome title and empty first name displays error on forward click`() {
        composeTestRule.setContent {
            RegistrationWizardScreen(
                onFinish = {},
                initialData = OnboardingData(firstName = "", lastName = ""),
            )
        }

        // Verify title is displayed
        val welcomeTitle = context.getString(R.string.onboarding_title_welcome)
        composeTestRule.onNodeWithText(welcomeTitle).assertIsDisplayed()

        // Click next with empty first name
        val nextContentDesc = context.getString(R.string.onboarding_nav_next)
        composeTestRule.onNodeWithText("→").performClick()

        // Error message should appear
        val emptyError = context.getString(R.string.validation_error_empty)
        composeTestRule.onNodeWithText(emptyError).assertIsDisplayed()
    }

    @Test
    fun `step 0 enters first name and navigates to step 1`() {
        composeTestRule.setContent {
            RegistrationWizardScreen(
                onFinish = {},
                initialData = OnboardingData(firstName = "Aleksey", lastName = "Codintsev"),
            )
        }

        // Click next with valid name
        composeTestRule.onNodeWithText("→").performClick()

        // Step 1 should display "Aleksey, расскажи о себе"
        val expectedStep1Title = context.getString(R.string.onboarding_title_about_format, "Aleksey")
        composeTestRule.onNodeWithText(expectedStep1Title).assertIsDisplayed()
    }

    @Test
    fun `completing all steps invokes onFinish with collected data`() {
        var finishedData: OnboardingData? = null

        composeTestRule.setContent {
            RegistrationWizardScreen(
                onFinish = { finishedData = it },
                initialData = OnboardingData(
                    firstName = "Aleksey",
                    lastName = "Codintsev",
                    heightCm = "180",
                    weightKg = "75.0",
                    age = "25",
                    weeklyBudget = "5000",
                ),
            )
        }

        // Advance through steps
        // Step 0 -> 1
        composeTestRule.onNodeWithText("→").performClick()

        // Step 1 -> 2
        composeTestRule.onNodeWithText("→").performClick()

        // Step 2 -> 3
        composeTestRule.onNodeWithText("→").performClick()

        // Step 3 -> 4
        composeTestRule.onNodeWithText("→").performClick()

        // Step 4 (Finish step): click finish "✓"
        composeTestRule.onNodeWithText("✓").performClick()

        assertNotNull(finishedData)
        val data = finishedData as OnboardingData
        assertEquals("Aleksey", data.firstName)
        assertEquals("Codintsev", data.lastName)
        assertEquals("180", data.heightCm)
        assertEquals("75.0", data.weightKg)
        assertEquals("25", data.age)
    }
}
