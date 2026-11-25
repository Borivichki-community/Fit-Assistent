package dev.andre.nutritionist.android.ui.screens.role

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import dev.andre.nutritionist.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoleSelectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `role selection screen renders title subtitle and strictly two role buttons`() {
        composeTestRule.setContent {
            FitAssistantTheme {
                RoleSelectionScreen(onRoleSelected = {})
            }
        }

        val title = context.getString(R.string.role_selection_title)
        val subtitle = context.getString(R.string.role_selection_subtitle)
        val clientTitle = context.getString(R.string.role_client_title)
        val coachTitle = context.getString(R.string.role_coach_title)

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(subtitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(clientTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(coachTitle).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROLE_CARD_CLIENT_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROLE_CARD_COACH_TAG).assertIsDisplayed()
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(2)
    }

    @Test
    fun `clicking client card emits CLIENT role`() {
        var selectedRole: UserRole? = null

        composeTestRule.setContent {
            FitAssistantTheme {
                RoleSelectionScreen(onRoleSelected = { selectedRole = it })
            }
        }

        composeTestRule.onNodeWithTag(ROLE_CARD_CLIENT_TAG).performClick()

        assertEquals(UserRole.CLIENT, selectedRole)
    }

    @Test
    fun `clicking coach card emits COACH role`() {
        var selectedRole: UserRole? = null

        composeTestRule.setContent {
            FitAssistantTheme {
                RoleSelectionScreen(onRoleSelected = { selectedRole = it })
            }
        }

        composeTestRule.onNodeWithTag(ROLE_CARD_COACH_TAG).performClick()

        assertEquals(UserRole.COACH, selectedRole)
    }
}
