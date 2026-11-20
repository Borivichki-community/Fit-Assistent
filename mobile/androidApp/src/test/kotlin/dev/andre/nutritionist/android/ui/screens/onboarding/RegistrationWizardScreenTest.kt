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
                initialData = OnboardingData(
                    firstName = "Aleksey",
                    weightKg = "75.0",
                    age = "25",
                ),
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

    @Test
    fun `step 0 with out of range height displays error and does not navigate to step 1`() {
        composeTestRule.setContent {
            RegistrationWizardScreen(
                onFinish = {},
                initialData = OnboardingData(
                    firstName = "Aleksey",
                    heightCm = "350",
                    weightKg = "75.0",
                    age = "25",
                ),
            )
        }

        // Click next on step 0 with invalid height (350 > 300)
        composeTestRule.onNodeWithText("→").performClick()

        // Out of range error should be displayed
        val outOfRangeError = context.getString(R.string.validation_error_out_of_range)
        composeTestRule.onNodeWithText(outOfRangeError).assertIsDisplayed()

        // Wizard stays on step 0 (personalized about title still displayed)
        val expectedStep0Title = context.getString(R.string.onboarding_title_about_format, "Aleksey")
        composeTestRule.onNodeWithText(expectedStep0Title).assertIsDisplayed()
    }

    @Test
    fun `step 2 with invalid weekly budget displays error and does not navigate to step 3`() {
        composeTestRule.setContent {
            RegistrationWizardScreen(
                onFinish = {},
                initialData = OnboardingData(
                    firstName = "Aleksey",
                    heightCm = "180",
                    weightKg = "75.0",
                    age = "25",
                    weeklyBudget = "-500",
                ),
            )
        }

        // Step 0 -> Step 1
        composeTestRule.onNodeWithText("→").performClick()

        // Step 1 -> Step 2
        composeTestRule.onNodeWithText("→").performClick()

        // Step 2 (Budget): click next with negative budget
        composeTestRule.onNodeWithText("→").performClick()

        // Error message for negative or zero budget should be displayed
        val budgetError = context.getString(R.string.validation_error_negative_or_zero)
        composeTestRule.onNodeWithText(budgetError).assertIsDisplayed()

        // Label for budget still visible on screen (stays on step 2)
        val budgetLabel = context.getString(R.string.onboarding_label_budget)
        composeTestRule.onNodeWithText(budgetLabel).assertIsDisplayed()
    }
}
