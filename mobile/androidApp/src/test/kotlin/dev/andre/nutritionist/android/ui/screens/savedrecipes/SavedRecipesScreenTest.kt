package dev.andre.nutritionist.android.ui.screens.savedrecipes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.presentation.airecipe.RecipeIngredientUiModel
import dev.andre.nutritionist.presentation.airecipe.RecipeResultUiModel
import dev.andre.nutritionist.presentation.airecipe.RecipeStepUiModel
import dev.andre.nutritionist.presentation.savedrecipes.MealTypeFilterUiModel
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipeItemUiModel
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipesUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SavedRecipesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleFilters = listOf(
        MealTypeFilterUiModel(mealType = null, title = "Все", isSelected = true),
        MealTypeFilterUiModel(mealType = MealType.BREAKFAST, title = "Завтрак", isSelected = false),
        MealTypeFilterUiModel(mealType = MealType.LUNCH, title = "Обед", isSelected = false),
    )

    private val sampleItem1 = SavedRecipeItemUiModel(
        id = "recipe-1",
        title = "Овсяная каша с яблоком",
        description = "Нежная овсянка долгой варки с корицей",
        mealTypeBadge = "Завтрак",
        caloriesBadge = "380 ккал",
        budgetBadge = "~95 ₽",
        cookingTimeBadge = "15 мин",
        macroSummaryText = "Б: 12г • Ж: 9г • У: 62г",
        isFavorite = true,
    )

    private val sampleItem2 = SavedRecipeItemUiModel(
        id = "recipe-2",
        title = "Филе индейки с гречкой",
        description = "Сочное филе индейки на гриле",
        mealTypeBadge = "Обед",
        caloriesBadge = "520 ккал",
        budgetBadge = "~210 ₽",
        cookingTimeBadge = "25 мин",
        macroSummaryText = "Б: 46г • Ж: 11г • У: 58г",
        isFavorite = false,
    )

    private val sampleRecipeDetail = RecipeResultUiModel(
        id = "recipe-1",
        title = "Овсяная каша с яблоком",
        description = "Нежная овсянка долгой варки с корицей",
        mealTypeBadge = "Завтрак",
        caloriesBadge = "380 ккал",
        budgetBadge = "~95 ₽",
        cookingTimeBadge = "15 мин",
        macroSummaryText = "Б: 12г • Ж: 9г • У: 62г",
        proteinText = "12 г белка",
        fatsText = "9 г жиров",
        carbsText = "62 г углев.",
        proteinFraction = 0.15f,
        fatsFraction = 0.20f,
        carbsFraction = 0.65f,
        ingredients = listOf(
            RecipeIngredientUiModel("Овсяные хлопья", "70 г"),
            RecipeIngredientUiModel("Яблоко", "1 шт"),
        ),
        steps = listOf(
            RecipeStepUiModel(1, "Сварить овсянку"),
            RecipeStepUiModel(2, "Добавить яблоко"),
        ),
        isSaved = true,
    )

    @Test
    fun loading_state_displays_progress_indicator() {
        composeTestRule.setContent {
            SavedRecipesScreen(
                state = SavedRecipesUiState(
                    content = SavedRecipesUiState.Content.Loading,
                ),
            )
        }

        composeTestRule.onNodeWithTag("saved_recipes_loading").assertIsDisplayed()
    }

    @Test
    fun empty_state_displays_title_and_clicking_action_invokes_callback() {
        var actionClicked = false
        composeTestRule.setContent {
            SavedRecipesScreen(
                state = SavedRecipesUiState(
                    content = SavedRecipesUiState.Content.Empty(
                        title = "Коллекция рецептов пуста",
                        description = "Сохраняйте любимые блюда из каталога",
                        actionTitle = "Сгенерировать с AI",
                    ),
                ),
                onEmptyActionClicked = { actionClicked = true },
            )
        }

        composeTestRule.onNodeWithTag("saved_recipes_empty").assertIsDisplayed()
        composeTestRule.onNodeWithTag("saved_recipes_empty_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Коллекция рецептов пуста").assertIsDisplayed()
        composeTestRule.onNodeWithTag("saved_recipes_empty_action_button").performClick()
        assertTrue(actionClicked)
    }

    @Test
    fun success_state_displays_recipes_and_count() {
        composeTestRule.setContent {
            SavedRecipesScreen(
                state = SavedRecipesUiState(
                    mealTypeFilters = sampleFilters,
                    content = SavedRecipesUiState.Content.Success(
                        recipes = listOf(sampleItem1, sampleItem2),
                        countText = "2 рецептов",
                    ),
                ),
            )
        }

        composeTestRule.onNodeWithTag("saved_recipes_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("saved_recipes_count_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 рецептов").assertIsDisplayed()
        composeTestRule.onNodeWithText("Овсяная каша с яблоком").assertIsDisplayed()
    }

    @Test
    fun success_state_typing_in_search_invokes_callback() {
        var query = ""
        composeTestRule.setContent {
            SavedRecipesScreen(
                state = SavedRecipesUiState(
                    searchQuery = "",
                    mealTypeFilters = sampleFilters,
                    content = SavedRecipesUiState.Content.Success(
                        recipes = listOf(sampleItem1),
                        countText = "1 рецептов",
                    ),
                ),
                onSearchQueryChanged = { query = it },
            )
        }

        composeTestRule.onNodeWithTag("saved_recipes_search_input").performTextInput("Овсянка")
        assertEquals("Овсянка", query)
    }

    @Test
    fun success_state_clicking_filter_invokes_callback() {
        var selectedMealType: MealType? = null
        composeTestRule.setContent {
            SavedRecipesScreen(
                state = SavedRecipesUiState(
                    mealTypeFilters = sampleFilters,
                    content = SavedRecipesUiState.Content.Success(
                        recipes = listOf(sampleItem1),
                        countText = "1 рецептов",
                    ),
                ),
                onMealTypeFilterSelected = { selectedMealType = it },
            )
        }

        composeTestRule.onNodeWithTag("saved_recipes_filter_Завтрак").performClick()
        assertEquals(MealType.BREAKFAST, selectedMealType)
    }

    @Test
    fun success_state_clicking_card_invokes_callback() {
        var clickedId: RecipeId? = null
        composeTestRule.setContent {
            SavedRecipesScreen(
                state = SavedRecipesUiState(
                    mealTypeFilters = sampleFilters,
                    content = SavedRecipesUiState.Content.Success(
                        recipes = listOf(sampleItem1),
                        countText = "1 рецептов",
                    ),
                ),
                onRecipeClicked = { clickedId = it },
            )
        }

        composeTestRule.onNodeWithTag("saved_recipe_card_recipe-1").performClick()
        assertEquals(RecipeId("recipe-1"), clickedId)
    }

    @Test
    fun success_state_clicking_favorite_invokes_callback() {
        var toggledId: RecipeId? = null
        composeTestRule.setContent {
            SavedRecipesScreen(
                state = SavedRecipesUiState(
                    mealTypeFilters = sampleFilters,
                    content = SavedRecipesUiState.Content.Success(
                        recipes = listOf(sampleItem1),
                        countText = "1 рецептов",
                    ),
                ),
                onToggleFavorite = { toggledId = it },
            )
        }

        composeTestRule.onNodeWithTag("saved_recipe_favorite_recipe-1").performClick()
        assertEquals(RecipeId("recipe-1"), toggledId)
    }

    @Test
    fun detail_content_displays_details_and_action_clicks_invoke_callbacks() {
        var added = false
        var dismissed = false

        composeTestRule.setContent {
            SavedRecipeDetailContent(
                recipe = sampleRecipeDetail,
                onDismiss = { dismissed = true },
                onAddToDiary = { added = true },
            )
        }

        composeTestRule.onNodeWithTag("saved_recipe_detail_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("saved_recipe_detail_add_diary_button").performScrollTo().performClick()
        assertTrue(added)

        composeTestRule.onNodeWithTag("saved_recipe_detail_close_button").performScrollTo().performClick()
        assertTrue(dismissed)
    }

    @Test
    fun success_state_with_detail_displays_detail_title() {
        composeTestRule.setContent {
            SavedRecipesScreen(
                state = SavedRecipesUiState(
                    mealTypeFilters = sampleFilters,
                    content = SavedRecipesUiState.Content.Success(
                        recipes = listOf(sampleItem1),
                        countText = "1 рецептов",
                        selectedRecipeDetail = sampleRecipeDetail,
                    ),
                ),
            )
        }

        composeTestRule.onNodeWithTag("saved_recipe_detail_title").assertIsDisplayed()
    }

    @Test
    fun error_state_displays_message_and_retry_invokes_callback() {
        var retried = false
        composeTestRule.setContent {
            SavedRecipesScreen(
                state = SavedRecipesUiState(
                    content = SavedRecipesUiState.Content.Error("Ошибка базы данных"),
                ),
                onRetryClicked = { retried = true },
            )
        }

        composeTestRule.onNodeWithTag("saved_recipes_error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ошибка базы данных").assertIsDisplayed()
        composeTestRule.onNodeWithTag("saved_recipes_retry_button").performClick()
        assertTrue(retried)
    }

    @Test
    fun top_bar_back_and_ai_generator_clicks_invoke_callbacks() {
        var backClicked = false
        var genClicked = false

        composeTestRule.setContent {
            SavedRecipesScreen(
                state = SavedRecipesUiState(
                    content = SavedRecipesUiState.Content.Loading,
                ),
                onNavigateBack = { backClicked = true },
                onGenerateRecipeClicked = { genClicked = true },
            )
        }

        composeTestRule.onNodeWithTag("saved_recipes_top_bar_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("saved_recipes_back_button").performClick()
        assertTrue(backClicked)

        composeTestRule.onNodeWithTag("saved_recipes_ai_gen_button").performClick()
        assertTrue(genClicked)
    }
}
