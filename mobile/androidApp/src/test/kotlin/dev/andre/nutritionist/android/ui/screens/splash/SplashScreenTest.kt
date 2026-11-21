package dev.andre.nutritionist.android.ui.screens.splash

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.andre.nutritionist.android.fake.FakeAuthRepository
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SplashScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeAuthRepository = FakeAuthRepository()

    @Test
    fun `splash screen displays logo title and greeting`() {
        composeTestRule.setContent {
            FitAssistantTheme {
                SplashScreen()
            }
        }

        composeTestRule.onNodeWithText("fit-assistent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Здравствуйте!").assertIsDisplayed()
    }

    @Test
    fun `splash route navigates to dashboard when user is authenticated`() {
        fakeAuthRepository.setAuthenticated(true)
        var navigatedToDashboard = false
        var navigatedToAuth = false

        composeTestRule.setContent {
            FitAssistantTheme {
                SplashRoute(
                    onNavigateToDashboard = { navigatedToDashboard = true },
                    onNavigateToAuth = { navigatedToAuth = true },
                    authRepository = fakeAuthRepository,
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500L)

        assertTrue("Must navigate to dashboard when authenticated", navigatedToDashboard)
        assertFalse("Must not navigate to auth when authenticated", navigatedToAuth)
    }

    @Test
    fun `splash route navigates to auth when user is not authenticated`() {
        fakeAuthRepository.setAuthenticated(false)
        var navigatedToDashboard = false
        var navigatedToAuth = false

        composeTestRule.setContent {
            FitAssistantTheme {
                SplashRoute(
                    onNavigateToDashboard = { navigatedToDashboard = true },
                    onNavigateToAuth = { navigatedToAuth = true },
                    authRepository = fakeAuthRepository,
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500L)

        assertTrue("Must navigate to auth when not authenticated", navigatedToAuth)
        assertFalse("Must not navigate to dashboard when not authenticated", navigatedToDashboard)
    }
}
