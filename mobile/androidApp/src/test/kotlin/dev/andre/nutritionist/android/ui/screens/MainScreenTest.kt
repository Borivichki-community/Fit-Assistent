package dev.andre.nutritionist.android.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
    fun `fitApp top bar title is displayed`() {
        composeTestRule.setContent {
            MainScreen()
        }

        composeTestRule.onNodeWithText("FitApp").assertIsDisplayed()
    }

    @Test
    fun `my plans section header is displayed`() {
        composeTestRule.setContent {
            MainScreen()
        }

        val sectionText = context.getString(R.string.section_my_plans)
        composeTestRule.onNodeWithText(sectionText).assertIsDisplayed()
    }

    @Test
    fun `breakfast section and empty state are displayed`() {
        composeTestRule.setContent {
            MainScreen()
        }

        val sectionText = context.getString(R.string.section_breakfast)
        val emptyTitle = context.getString(R.string.empty_plan_title)
        composeTestRule.onNodeWithText(sectionText).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(emptyTitle).performScrollTo().assertIsDisplayed()
    }
}
