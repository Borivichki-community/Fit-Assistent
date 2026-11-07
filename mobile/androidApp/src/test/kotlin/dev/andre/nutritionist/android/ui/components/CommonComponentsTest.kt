package dev.andre.nutritionist.android.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CommonComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `LoadingContent renders successfully within theme`() {
        composeTestRule.setContent {
            FitAssistantTheme {
                LoadingContent()
            }
        }
        // Verification that LoadingContent composes without throwing exceptions
    }

    @Test
    fun `ErrorContent displays error message and triggers onRetry`() {
        var retryClicked = false
        val errorMessage = context.getString(R.string.error_default_message)
        val retryText = context.getString(R.string.retry_button)

        composeTestRule.setContent {
            FitAssistantTheme {
                ErrorContent(
                    message = errorMessage,
                    onRetry = { retryClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText(retryText).performClick()

        assertTrue(retryClicked)
    }

    @Test
    fun `ErrorContent multiple retry clicks increments callback`() {
        var retryCount = 0
        val errorMessage = context.getString(R.string.error_default_message)
        val retryText = context.getString(R.string.retry_button)

        composeTestRule.setContent {
            FitAssistantTheme {
                ErrorContent(
                    message = errorMessage,
                    onRetry = { retryCount++ },
                )
            }
        }

        val retryButton = composeTestRule.onNodeWithText(retryText)
        retryButton.performClick()
        retryButton.performClick()
        retryButton.performClick()

        org.junit.Assert.assertEquals(3, retryCount)
    }

    @Test
    fun `OfflineBanner displays message and subtitle`() {
        val title = context.getString(R.string.offline_banner_title)
        val subtitle = context.getString(R.string.offline_banner_subtitle)

        composeTestRule.setContent {
            FitAssistantTheme {
                OfflineBanner(
                    message = title,
                    subtitle = subtitle,
                )
            }
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(subtitle).assertIsDisplayed()
    }
}
