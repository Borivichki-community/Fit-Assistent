package dev.andre.nutritionist.android.ui.screens.auth

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
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
                onRegisterKeycloakClick = {},
                onNavigateBack = {},
                onGoogleSignUpClick = {},
            )
        }

        val titleText = context.getString(R.string.signup_title)
        val subtitleText = context.getString(R.string.signup_subtitle)
        val registerKeycloakText = context.getString(R.string.signup_register_keycloak)
        val googleBtnText = context.getString(R.string.signup_via_google)
        val backBtnText = context.getString(R.string.signup_back_button)

        composeTestRule.onNodeWithText(titleText).assertIsDisplayed()
        composeTestRule.onNodeWithText(subtitleText).assertIsDisplayed()
        composeTestRule.onNodeWithText(registerKeycloakText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(googleBtnText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(backBtnText).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `register keycloak click triggers callback`() {
        var registerClicked = false

        composeTestRule.setContent {
            SignUpScreen(
                onRegisterKeycloakClick = { registerClicked = true },
                onNavigateBack = {},
                onGoogleSignUpClick = {},
            )
        }

        val registerKeycloakText = context.getString(R.string.signup_register_keycloak)
        composeTestRule.onNodeWithText(registerKeycloakText).performScrollTo().performClick()

        assertTrue(registerClicked)
    }

    @Test
    fun `google sign up triggers callback directly`() {
        var googleClicked = false

        composeTestRule.setContent {
            SignUpScreen(
                onRegisterKeycloakClick = {},
                onNavigateBack = {},
                onGoogleSignUpClick = { googleClicked = true },
            )
        }

        val googleBtnText = context.getString(R.string.signup_via_google)
        composeTestRule.onNodeWithText(googleBtnText).performScrollTo().performClick()

        assertTrue(googleClicked)
    }

    @Test
    fun `back button triggers navigation back`() {
        var backClicked = false

        composeTestRule.setContent {
            SignUpScreen(
                onRegisterKeycloakClick = {},
                onNavigateBack = { backClicked = true },
                onGoogleSignUpClick = {},
            )
        }

        val backBtnText = context.getString(R.string.signup_back_button)
        composeTestRule.onNodeWithText(backBtnText).performScrollTo().performClick()

        assertTrue(backClicked)
    }
}
