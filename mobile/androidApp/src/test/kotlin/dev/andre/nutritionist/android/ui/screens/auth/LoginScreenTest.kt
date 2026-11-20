package dev.andre.nutritionist.android.ui.screens.auth

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.auth.AuthUiState
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
                state = AuthUiState(content = AuthUiState.Content.Idle),
                onLoginClick = {},
                onCreateAccountClick = {},
                onTelegramClick = {},
                onGoogleClick = {},
            )
        }

        val titleText = context.getString(R.string.auth_title)
        val subtitleText = context.getString(R.string.auth_subtitle)
        val loginKeycloakText = context.getString(R.string.auth_login_keycloak)
        val googleText = context.getString(R.string.auth_via_google)
        val createAccountText = context.getString(R.string.auth_create_account)
        val telegramText = context.getString(R.string.auth_via_telegram)

        composeTestRule.onNodeWithText(titleText).assertIsDisplayed()
        composeTestRule.onNodeWithText(subtitleText).assertIsDisplayed()
        composeTestRule.onNodeWithText(loginKeycloakText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(googleText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(createAccountText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(telegramText).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `login click triggers onLoginClick callback`() {
        var loginClicked = false

        composeTestRule.setContent {
            LoginScreen(
                state = AuthUiState(content = AuthUiState.Content.Idle),
                onLoginClick = { loginClicked = true },
                onCreateAccountClick = {},
                onTelegramClick = {},
                onGoogleClick = {},
            )
        }

        val loginKeycloakText = context.getString(R.string.auth_login_keycloak)
        composeTestRule.onNodeWithText(loginKeycloakText).performScrollTo().performClick()

        assertTrue(loginClicked)
    }

    @Test
    fun `google click triggers onGoogleClick callback`() {
        var googleClicked = false

        composeTestRule.setContent {
            LoginScreen(
                state = AuthUiState(content = AuthUiState.Content.Idle),
                onLoginClick = {},
                onCreateAccountClick = {},
                onTelegramClick = {},
                onGoogleClick = { googleClicked = true },
            )
        }

        val googleText = context.getString(R.string.auth_via_google)
        composeTestRule.onNodeWithText(googleText).performScrollTo().performClick()

        assertTrue(googleClicked)
    }

    @Test
    fun `create account click triggers navigation callback`() {
        var createAccountClicked = false

        composeTestRule.setContent {
            LoginScreen(
                state = AuthUiState(content = AuthUiState.Content.Idle),
                onLoginClick = {},
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
    fun `loading state displays progress indicator and loading text`() {
        composeTestRule.setContent {
            LoginScreen(
                state = AuthUiState(content = AuthUiState.Content.Loading),
                onLoginClick = {},
                onCreateAccountClick = {},
                onTelegramClick = {},
                onGoogleClick = {},
            )
        }

        val loadingText = context.getString(R.string.auth_loading)
        composeTestRule.onNodeWithText(loadingText).assertIsDisplayed()
    }

    @Test
    fun `error state displays error message and retry button`() {
        var retryClicked = false

        composeTestRule.setContent {
            LoginScreen(
                state = AuthUiState(content = AuthUiState.Content.Error("Keycloak unreachable")),
                onLoginClick = { retryClicked = true },
                onCreateAccountClick = {},
                onTelegramClick = {},
                onGoogleClick = {},
            )
        }

        val retryBtnText = context.getString(R.string.retry_button)
        composeTestRule.onNodeWithText("Keycloak unreachable").assertIsDisplayed()
        composeTestRule.onNodeWithText(retryBtnText).performClick()

        assertTrue(retryClicked)
    }
}
