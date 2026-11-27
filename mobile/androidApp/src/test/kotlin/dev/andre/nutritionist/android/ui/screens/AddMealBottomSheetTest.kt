package dev.andre.nutritionist.android.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.addmeal.AddMealEvent
import dev.andre.nutritionist.presentation.addmeal.AddMealItemKind
import dev.andre.nutritionist.presentation.addmeal.AddMealSearchItemUiModel
import dev.andre.nutritionist.presentation.addmeal.AddMealUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AddMealBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `idle state matches fitapp_4 layout and stub buttons emit stub events`() {
        val capturedEvents = mutableListOf<AddMealEvent.Ui>()

        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = true) {
                AddMealBottomSheetContent(
                    uiState = AddMealUiState(
                        mealType = MealType.LUNCH,
                        content = AddMealUiState.Content.Idle,
                    ),
                    onEvent = { capturedEvents.add(it) },
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.add_meal_search_placeholder)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.add_meal_action_ai)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.add_meal_action_photo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.add_meal_action_assistant)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.add_meal_saved_food_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.add_meal_saved_food_subtitle)).assertIsDisplayed()

        composeTestRule.onNodeWithTag("add_meal_stub_ai").performClick()
        composeTestRule.onNodeWithTag("add_meal_stub_photo").performClick()
        composeTestRule.onNodeWithTag("add_meal_stub_assistant").performClick()
        composeTestRule.onNodeWithTag("add_meal_stub_saved_food").performClick()
        composeTestRule.onNodeWithTag("add_meal_close_button").performClick()

        assertTrue(capturedEvents.contains(AddMealEvent.Ui.AiGenerateClicked))
        assertTrue(capturedEvents.contains(AddMealEvent.Ui.TakePhotoClicked))
        assertTrue(capturedEvents.contains(AddMealEvent.Ui.VoiceAssistantClicked))
        assertTrue(capturedEvents.contains(AddMealEvent.Ui.SavedFoodClicked))
        assertTrue(capturedEvents.contains(AddMealEvent.Ui.Dismiss))
    }

    @Test
    fun `typing in search input dispatches QueryChanged and clicking result dispatches ItemClicked`() {
        val capturedEvents = mutableListOf<AddMealEvent.Ui>()
        val items = listOf(
            AddMealSearchItemUiModel(
                id = "prod-epica",
                kind = AddMealItemKind.PRODUCT,
                title = "Йогурт Epica",
                typeLabel = "Продукт",
                portionLabel = "100 г",
                caloriesLabel = "65 ккал",
                macrosLabel = "Б: 3,79г • Ж: 0,96г • У: 10,62г",
                priceLabel = "95 ₽",
            ),
            AddMealSearchItemUiModel(
                id = "rec-buckwheat",
                kind = AddMealItemKind.RECIPE,
                title = "Гречневая каша с куриным филе",
                typeLabel = "Рецепт",
                portionLabel = "365 г",
                caloriesLabel = "808 ккал",
                macrosLabel = "Б: 66,17г • Ж: 21,13г • У: 94,06г",
                priceLabel = null,
            ),
        )

        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = true) {
                AddMealBottomSheetContent(
                    uiState = AddMealUiState(
                        mealType = MealType.DINNER,
                        query = "йогурт",
                        items = items,
                        content = AddMealUiState.Content.Success(),
                    ),
                    onEvent = { capturedEvents.add(it) },
                )
            }
        }

        composeTestRule.onNodeWithText("Йогурт Epica").assertIsDisplayed()
        composeTestRule.onNodeWithText("Продукт").assertIsDisplayed()
        composeTestRule.onNodeWithText("95 ₽").assertIsDisplayed()
        composeTestRule.onNodeWithText("Гречневая каша с куриным филе").assertIsDisplayed()
        composeTestRule.onNodeWithText("Рецепт").assertIsDisplayed()

        composeTestRule.onNodeWithTag("add_meal_result_RECIPE_rec-buckwheat").performClick()
        assertEquals(
            AddMealEvent.Ui.ItemClicked(id = "rec-buckwheat", kind = AddMealItemKind.RECIPE),
            capturedEvents.last(),
        )

        composeTestRule.onNodeWithTag("add_meal_search_input").performTextInput(" каша")
        assertTrue(capturedEvents.any { it is AddMealEvent.Ui.QueryChanged })
    }

    @Test
    fun `error state displays retry button and dispatches Retry`() {
        var retried = false

        composeTestRule.setContent {
            FitAssistantTheme(darkTheme = false) {
                AddMealBottomSheetContent(
                    uiState = AddMealUiState(
                        query = "омлет",
                        content = AddMealUiState.Content.Error("Сервис недоступен"),
                    ),
                    onEvent = {
                        if (it == AddMealEvent.Ui.Retry) retried = true
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Сервис недоступен").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.retry_button)).performClick()
        assertTrue(retried)
    }
}
