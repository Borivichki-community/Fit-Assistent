package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.airecipe.AiRecipeGeneratingUiModel
import dev.andre.nutritionist.presentation.airecipe.AiRecipePromptUiModel
import dev.andre.nutritionist.presentation.airecipe.AiRecipeUiState
import dev.andre.nutritionist.presentation.airecipe.RecipeIngredientUiModel
import dev.andre.nutritionist.presentation.airecipe.RecipeResultUiModel
import dev.andre.nutritionist.presentation.airecipe.RecipeStepUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiRecipeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val samplePrompt = AiRecipePromptUiModel(
        query = "",
        targetCalories = 500,
        targetCaloriesText = "500 ккал",
        targetBudgetRub = 200,
        targetBudgetText = "200 ₽",
        mealType = MealType.LUNCH,
        mealTypeTitle = "Обед",
        dietaryPreference = "Все",
        availablePreferences = listOf("Все", "Высокий белок", "Эконом", "Вегетарианское"),
    )

    private val sampleRecipe = RecipeResultUiModel(
        id = "recipe-gen-1",
        title = "Куриное филе су-вид с гречкой",
        description = "Нежное диетическое филе с ароматной ядрицей",
        mealTypeBadge = "Обед",
        caloriesBadge = "520 ккал",
        budgetBadge = "~210 ₽",
        cookingTimeBadge = "25 мин",
        macroSummaryText = "Б: 46г • Ж: 11г • У: 58г",
        proteinText = "46 г белка",
        fatsText = "11 г жиров",
        carbsText = "58 г углев.",
        proteinFraction = 0.40f,
        fatsFraction = 0.22f,
        carbsFraction = 0.38f,
        ingredients = listOf(
            RecipeIngredientUiModel("Грудка индейки", "180 г"),
            RecipeIngredientUiModel("Крупа гречневая", "70 г"),
        ),
        steps = listOf(
            RecipeStepUiModel(1, "Отварить гречневую крупу"),
            RecipeStepUiModel(2, "Обжарить филе на гриле"),
        ),
        isSaved = false,
    )

    @Test
    fun configuring_state_displays_inputs_and_clicking_generate_invokes_callback() {
        var generateClicked = false
        composeTestRule.setContent {
            AiRecipeScreen(
                state = AiRecipeUiState(AiRecipeUiState.Content.Configuring(samplePrompt)),
                onGenerateClicked = { generateClicked = true },
            )
        }

        composeTestRule.onNodeWithTag("ai_recipe_top_bar_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_recipe_query_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_recipe_generate_button").performScrollTo().performClick()
        assertTrue(generateClicked)
    }

    @Test
    fun configuring_state_typing_query_invokes_callback() {
        var changedQuery = ""
        composeTestRule.setContent {
            AiRecipeScreen(
                state = AiRecipeUiState(AiRecipeUiState.Content.Configuring(samplePrompt)),
                onQueryChanged = { changedQuery = it },
            )
        }

        composeTestRule.onNodeWithTag("ai_recipe_query_input").performTextInput("Панкейки")
        assertEquals("Панкейки", changedQuery)
    }

    @Test
    fun configuring_state_clicking_meal_type_invokes_callback() {
        var selectedMealType: MealType? = null
        composeTestRule.setContent {
            AiRecipeScreen(
                state = AiRecipeUiState(AiRecipeUiState.Content.Configuring(samplePrompt)),
                onMealTypeSelected = { selectedMealType = it },
            )
        }

        composeTestRule.onNodeWithTag("ai_recipe_meal_type_DINNER").performClick()
        assertEquals(MealType.DINNER, selectedMealType)
    }

    @Test
    fun configuring_state_clicking_calories_invokes_callback() {
        var selectedCalories: Int? = null
        composeTestRule.setContent {
            AiRecipeScreen(
                state = AiRecipeUiState(AiRecipeUiState.Content.Configuring(samplePrompt)),
                onCaloriesChanged = { selectedCalories = it },
            )
        }

        composeTestRule.onNodeWithTag("ai_recipe_calories_row").performScrollTo()
        composeTestRule.onNodeWithTag("ai_recipe_cal_650").performClick()
        assertEquals(650, selectedCalories)
    }

    @Test
    fun generating_state_displays_breathing_orb_and_phase_text() {
        val generatingModel = AiRecipeGeneratingUiModel(
            phaseText = "Шеф составляет пошаговый рецепт...",
            progress = 0.75f,
            progressPercentText = "75%",
        )
        composeTestRule.setContent {
            AiRecipeScreen(
                state = AiRecipeUiState(AiRecipeUiState.Content.Generating(generatingModel)),
            )
        }

        composeTestRule.onNodeWithTag("ai_recipe_breathing_orb").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_recipe_phase_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Шеф составляет пошаговый рецепт...").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_recipe_progress_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_recipe_progress_percent").assertIsDisplayed()
    }

    @Test
    fun success_state_displays_recipe_details_and_action_clicks_invoke_callbacks() {
        var addDiaryClicked = false
        var saveRecipeClicked = false
        var resetClicked = false

        composeTestRule.setContent {
            AiRecipeScreen(
                state = AiRecipeUiState(AiRecipeUiState.Content.Success(sampleRecipe)),
                onAddRecipeToDiaryClicked = { addDiaryClicked = true },
                onSaveRecipeClicked = { saveRecipeClicked = true },
                onResetClicked = { resetClicked = true },
            )
        }

        composeTestRule.onNodeWithTag("ai_recipe_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Куриное филе су-вид с гречкой").assertIsDisplayed()
        composeTestRule.onNodeWithText("520 ккал").assertIsDisplayed()

        composeTestRule.onNodeWithTag("ai_recipe_add_diary_button").performScrollTo().performClick()
        assertTrue(addDiaryClicked)

        composeTestRule.onNodeWithTag("ai_recipe_save_button").performScrollTo().performClick()
        assertTrue(saveRecipeClicked)

        composeTestRule.onNodeWithTag("ai_recipe_reset_button").performScrollTo().performClick()
        assertTrue(resetClicked)
    }

    @Test
    fun error_state_displays_message_and_retry_invokes_callback() {
        var retryClicked = false
        composeTestRule.setContent {
            AiRecipeScreen(
                state = AiRecipeUiState(AiRecipeUiState.Content.Error("Ошибка генерации")),
                onRetryClicked = { retryClicked = true },
            )
        }

        composeTestRule.onNodeWithTag("ai_recipe_error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ошибка генерации").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_recipe_retry_button").performClick()
        assertTrue(retryClicked)
    }

    @Test
    fun back_button_invokes_callback() {
        var backClicked = false
        composeTestRule.setContent {
            AiRecipeScreen(
                state = AiRecipeUiState(AiRecipeUiState.Content.Configuring(samplePrompt)),
                onNavigateBack = { backClicked = true },
            )
        }

        composeTestRule.onNodeWithTag("ai_recipe_back_button").performClick()
        assertTrue(backClicked)
    }
}
