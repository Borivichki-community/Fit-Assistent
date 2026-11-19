package dev.andre.nutritionist.android.ui.screens.onboarding

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `step 0 shows body step with personalized name and empty height displays error on forward click`() {
        composeTestRule.setContent {
            RegistrationWizardScreen(
                onFinish = {},
                initialData = OnboardingData(firstName = "Aleksey"),
            )
        }

        // Verify title with personalized Keycloak name is displayed on step 0
        val expectedTitle = context.getString(R.string.onboarding_title_about_format, "Aleksey")
        composeTestRule.onNodeWithText(expectedTitle).assertIsDisplayed()

        // Click next with empty height
        composeTestRule.onNodeWithText("→").performClick()

        // Error message for empty height should appear
        val emptyError = context.getString(R.string.validation_error_empty)
        composeTestRule.onNodeWithText(emptyError).assertIsDisplayed()
    }

    @Test
    fun `step 0 with valid body data navigates to step 1 activity`() {
        composeTestRule.setContent {
            RegistrationWizardScreen(
                onFinish = {},
                initialData = OnboardingData(
                    firstName = "Aleksey",
                    heightCm = "180",
                    weightKg = "75.0",
                    age = "25",
                ),
            )
        }

        // Click next on step 0 with valid body params
        composeTestRule.onNodeWithText("→").performClick()

        // Step 1 should display activity step with personalized title
        val expectedStep1Title = context.getString(R.string.onboarding_title_activity_format, "Aleksey")
        composeTestRule.onNodeWithText(expectedStep1Title).assertIsDisplayed()
    }

    @Test
    fun `completing all 4 steps invokes onFinish with collected data`() {
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

        // Step 0 (Body) -> Step 1 (Activity)
        composeTestRule.onNodeWithText("→").performClick()

        // Step 1 (Activity) -> Step 2 (Budget)
        composeTestRule.onNodeWithText("→").performClick()

        // Step 2 (Budget) -> Step 3 (Goal)
        composeTestRule.onNodeWithText("→").performClick()

        // Step 3 (Finish step: Goal): click finish "✓"
        composeTestRule.onNodeWithText("✓").performClick()

        assertNotNull(finishedData)
        val data = checkNotNull(finishedData)
        assertEquals("Aleksey", data.firstName)
        assertEquals("Codintsev", data.lastName)
        assertEquals("180", data.heightCm)
        assertEquals("75.0", data.weightKg)
        assertEquals("25", data.age)
        assertEquals("5000", data.weeklyBudget)
    }
}
