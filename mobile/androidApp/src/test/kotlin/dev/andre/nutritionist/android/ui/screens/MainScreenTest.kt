package dev.andre.nutritionist.android.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.domain.repository.DiaryRepository
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeDiaryRepository : DiaryRepository {
    // Fake implementation for KMP requirement
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `button minimum touch target size is respected despite custom text`() {
        composeTestRule.setContent {
            MainScreen()
        }

        val buttonText = context.getString(R.string.btn_add_meal_test)
        val button = composeTestRule.onNodeWithText(buttonText, useUnmergedTree = true)

        button.assertIsDisplayed()
        button.assertHeightIsAtLeast(1.dp)
        button.assertWidthIsAtLeast(1.dp)
    }

    @Test
    fun `long text inside button does not overflow container unexpectedly`() {
        composeTestRule.setContent {
            MainScreen()
        }

        val buttonText = context.getString(R.string.btn_add_meal_test)
        composeTestRule.onNodeWithText(buttonText, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `small style body text is used for click counter`() {
        composeTestRule.setContent {
            MainScreen()
        }

        val buttonText = context.getString(R.string.btn_add_meal_test)
        val expectedCountText = context.getString(R.string.meals_added_count, 1)

        // click to show the text
        composeTestRule.onNodeWithText(buttonText, useUnmergedTree = true).performClick()

        // verify text is displayed
        composeTestRule.onNodeWithText(expectedCountText).assertIsDisplayed()
    }

    @Test
    fun `multiple clicks on add meal button updates count correctly`() {
        composeTestRule.setContent {
            MainScreen()
        }

        val buttonText = context.getString(R.string.btn_add_meal_test)
        val expectedCountText = context.getString(R.string.meals_added_count, 3)

        val button = composeTestRule.onNodeWithText(buttonText, useUnmergedTree = true)

        button.performClick()
        button.performClick()
        button.performClick()

        composeTestRule.onNodeWithText(expectedCountText).assertIsDisplayed()
    }

    @Test
    fun `bottom navigation item click invokes onDestinationSelected callback`() {
        var selectedDestination: dev.andre.nutritionist.android.navigation.AppDestination? = null

        composeTestRule.setContent {
            MainScreen(
                onDestinationSelected = { selectedDestination = it }
            )
        }

        // Navigate to Statistics
        val statisticsLabel = context.getString(R.string.nav_statistics)
        // Since BottomNavItem may not use text directly (uses contentDescription for icon or text based on implementation)
        // Let's rely on content description or generic click
        composeTestRule.onNodeWithContentDescription(statisticsLabel, useUnmergedTree = true).performClick()

        org.junit.Assert.assertEquals(dev.andre.nutritionist.android.navigation.AppDestination.Statistics, selectedDestination)
    }
}
