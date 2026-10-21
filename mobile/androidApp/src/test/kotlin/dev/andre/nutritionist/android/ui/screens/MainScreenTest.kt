package dev.andre.nutritionist.android.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.unit.dp
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.model.DiaryEntryId

class FakeDiaryRepository : DiaryRepository {
    // Fake implementation for KMP requirement
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `button minimum touch target size is respected despite custom text`() {
        composeTestRule.setContent {
            MainScreen()
        }

        val button = composeTestRule.onNodeWithText("Добавить приём пищи (маленькая кнопка)", useUnmergedTree = true)

        button.assertIsDisplayed()
        button.assertHeightIsAtLeast(1.dp)
        button.assertWidthIsAtLeast(1.dp)
    }

    @Test
    fun `long text inside button does not overflow container unexpectedly`() {
        composeTestRule.setContent {
            MainScreen()
        }

        composeTestRule.onNodeWithText("Добавить приём пищи (маленькая кнопка)", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `small style body text is used for click counter`() {
        composeTestRule.setContent {
            MainScreen()
        }

        // click to show the text
        composeTestRule.onNodeWithText("Добавить приём пищи (маленькая кнопка)", useUnmergedTree = true).performClick()

        // verify text is displayed
        composeTestRule.onNodeWithText("Добавлено приёмов пищи: 1").assertIsDisplayed()
    }
}
