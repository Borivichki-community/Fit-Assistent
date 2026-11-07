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
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `login screen renders all primary elements properly`() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginClick = { _, _ -> },
                onCreateAccountClick = {},
                onTelegramClick = {},
                onGoogleClick = {},
            )
        }

        val titleText = context.getString(R.string.auth_title)
        val loginBtnText = context.getString(R.string.auth_login_button)
        val createAccountText = context.getString(R.string.auth_create_account)
        val telegramText = context.getString(R.string.auth_via_telegram)
        val googleText = context.getString(R.string.auth_via_google)

        composeTestRule.onNodeWithText(titleText).assertIsDisplayed()
        composeTestRule.onNodeWithText(loginBtnText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(createAccountText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(telegramText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(googleText).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `create account click triggers navigation callback`() {
        var createAccountClicked = false

        composeTestRule.setContent {
            LoginScreen(
                onLoginClick = { _, _ -> },
                onCreateAccountClick = { createAccountClicked = true },
                onTelegramClick = {},
                onGoogleClick = {},
            )
        }

        val createAccountText = context.getString(R.string.auth_create_account)
        composeTestRule.onNodeWithText(createAccountText).performScrollTo().performClick()

        assertTrue(createAccountClicked)
    }

    @Test
    fun `invalid email or short password displays validation error`() {
        var loginCalled = false

        composeTestRule.setContent {
            LoginScreen(
                onLoginClick = { _, _ -> loginCalled = true },
                onCreateAccountClick = {},
                onTelegramClick = {},
                onGoogleClick = {},
            )
        }

        val loginBtnText = context.getString(R.string.auth_login_button)
        composeTestRule.onNodeWithText(loginBtnText).performScrollTo().performClick()

        val invalidEmailText = context.getString(R.string.validation_error_invalid_email)
        composeTestRule.onNodeWithText(invalidEmailText).performScrollTo().assertIsDisplayed()
        assertEquals(false, loginCalled)
    }

    @Test
    fun `valid credentials triggers login callback`() {
        var loggedEmail = ""
        var loggedPassword = ""

        composeTestRule.setContent {
            LoginScreen(
                onLoginClick = { email, pass ->
                    loggedEmail = email
                    loggedPassword = pass
                },
                onCreateAccountClick = {},
                onTelegramClick = {},
                onGoogleClick = {},
            )
        }

        val emailHint = context.getString(R.string.auth_email_hint)
        val passHint = context.getString(R.string.auth_password_hint)
        val loginBtnText = context.getString(R.string.auth_login_button)

        composeTestRule.onNodeWithText(emailHint).performScrollTo().performTextInput("test@mail.com")
        composeTestRule.onNodeWithText(passHint).performScrollTo().performTextInput("secret123")
        composeTestRule.onNodeWithText(loginBtnText).performScrollTo().performClick()

        assertEquals("test@mail.com", loggedEmail)
        assertEquals("secret123", loggedPassword)
    }
}
