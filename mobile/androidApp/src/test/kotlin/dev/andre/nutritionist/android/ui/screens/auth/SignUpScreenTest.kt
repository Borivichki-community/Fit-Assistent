package dev.andre.nutritionist.android.ui.screens.auth

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SignUpScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `signup screen renders all primary elements properly`() {
        composeTestRule.setContent {
            SignUpScreen(
                onRegistrationSuccess = {},
                onNavigateBack = {},
                onGoogleSignUpClick = {},
            )
        }

        val titleText = context.getString(R.string.signup_title)
        val registerBtnText = context.getString(R.string.signup_register_button)
        val backBtnText = context.getString(R.string.signup_back_button)
        val googleBtnText = context.getString(R.string.signup_via_google)

        composeTestRule.onNodeWithText(titleText).assertIsDisplayed()
        composeTestRule.onNodeWithText(registerBtnText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(googleBtnText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(backBtnText).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `back button triggers navigation back`() {
        var backClicked = false

        composeTestRule.setContent {
            SignUpScreen(
                onRegistrationSuccess = {},
                onNavigateBack = { backClicked = true },
                onGoogleSignUpClick = {},
            )
        }

        val backBtnText = context.getString(R.string.signup_back_button)
        composeTestRule.onNodeWithText(backBtnText).performScrollTo().performClick()

        assertTrue(backClicked)
    }

    @Test
    fun `valid email and password advances to code verification and verifies`() {
        var registeredEmail = ""

        composeTestRule.setContent {
            SignUpScreen(
                onRegistrationSuccess = { registeredEmail = it },
                onNavigateBack = {},
                onGoogleSignUpClick = {},
            )
        }

        val emailHint = context.getString(R.string.auth_email_hint)
        val passHint = context.getString(R.string.auth_password_hint)
        val registerBtnText = context.getString(R.string.signup_register_button)

        composeTestRule.onNodeWithText(emailHint).performScrollTo().performTextInput("user@test.com")
        composeTestRule.onNodeWithText(passHint).performScrollTo().performTextInput("password123")
        composeTestRule.onNodeWithText(registerBtnText).performScrollTo().performClick()

        // Verify code screen is displayed
        val codeTitle = context.getString(R.string.signup_code_title)
        val codeHint = context.getString(R.string.signup_code_hint)
        val confirmBtnText = context.getString(R.string.signup_confirm_code)

        composeTestRule.onNodeWithText(codeTitle).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(codeHint).performScrollTo().performTextInput("1234")
        composeTestRule.onNodeWithText(confirmBtnText).performScrollTo().performClick()

        assertEquals("user@test.com", registeredEmail)
    }

    @Test
    fun `google sign up triggers callback directly`() {
        var googleClicked = false

        composeTestRule.setContent {
            SignUpScreen(
                onRegistrationSuccess = {},
                onNavigateBack = {},
                onGoogleSignUpClick = { googleClicked = true },
            )
        }

        val googleBtnText = context.getString(R.string.signup_via_google)
        composeTestRule.onNodeWithText(googleBtnText).performScrollTo().performClick()

        assertTrue(googleClicked)
    }
}
