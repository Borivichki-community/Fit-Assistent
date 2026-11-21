package dev.andre.nutritionist.presentation.savedrecipes

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedRecipesUpdateTest {

    private val update = SavedRecipesUpdate()

    private val sampleRecipe1 = Recipe(
        id = RecipeId("rec-1"),
        title = "Белковый омлет со шпинатом",
        description = "Воздушный омлет с зеленью и сыром",
        mealType = MealType.BREAKFAST,
        calories = 340,
        proteins = 28.0,
        fats = 16.0,
        carbs = 6.0,
        estimatedCostRub = 120,
        cookingTimeMinutes = 12,
        ingredients = listOf(RecipeIngredient("Яйца С0", "3 шт")),
        steps = listOf("Взбить яйца, вылить на сковороду"),
        isFavorite = true,
    )

    private val sampleRecipe2 = Recipe(
        id = RecipeId("rec-2"),
        title = "Лосось на пару с брокколи",
        description = "Диетический ужин",
        mealType = MealType.DINNER,
        calories = 450,
        proteins = 42.0,
        fats = 14.0,
        carbs = 12.0,
        estimatedCostRub = 350,
        cookingTimeMinutes = 20,
        ingredients = listOf(RecipeIngredient("Филе лосося", "200 г")),
        steps = listOf("Приготовить в пароварке"),
        isFavorite = false,
    )

    @Test
    fun init_triggers_observeSavedRecipes_command() {
        val initial = SavedRecipesState()
        val next = update.update(initial, SavedRecipesEvent.Ui.Init)

        assertTrue(next.commands.contains(SavedRecipesCommand.ObserveSavedRecipes))
    }

    @Test
    fun searchQueryChanged_updates_filter_query() {
        val initial = SavedRecipesState()
        val next = update.update(initial, SavedRecipesEvent.Ui.SearchQueryChanged("Лосось"))

        val state = requireNotNull(next.state)
        assertEquals("Лосось", state.filter.query)
    }

    @Test
    fun mealTypeFilterSelected_updates_filter_mealType() {
        val initial = SavedRecipesState()
        val next = update.update(initial, SavedRecipesEvent.Ui.MealTypeFilterSelected(MealType.DINNER))

        val state = requireNotNull(next.state)
        assertEquals(MealType.DINNER, state.filter.selectedMealType)
    }

    @Test
    fun recipeClicked_in_success_sets_selectedRecipe() {
        val initial = SavedRecipesState(
            content = SavedRecipesState.Content.Success(
                allRecipes = listOf(sampleRecipe1, sampleRecipe2),
                selectedRecipe = null,
            ),
        )
        val next = update.update(initial, SavedRecipesEvent.Ui.RecipeClicked(RecipeId("rec-2")))

        val state = requireNotNull(next.state)
        val content = assertIs<SavedRecipesState.Content.Success>(state.content)
        assertEquals(sampleRecipe2, content.selectedRecipe)
    }

    @Test
    fun recipeDetailDismissed_clears_selectedRecipe() {
        val initial = SavedRecipesState(
            content = SavedRecipesState.Content.Success(
                allRecipes = listOf(sampleRecipe1, sampleRecipe2),
                selectedRecipe = sampleRecipe1,
            ),
        )
        val next = update.update(initial, SavedRecipesEvent.Ui.RecipeDetailDismissed)

        val state = requireNotNull(next.state)
        val content = assertIs<SavedRecipesState.Content.Success>(state.content)
        assertNull(content.selectedRecipe)
    }

    @Test
    fun toggleFavoriteClicked_emits_command() {
        val initial = SavedRecipesState()
        val next = update.update(initial, SavedRecipesEvent.Ui.ToggleFavoriteClicked(RecipeId("rec-1")))

        assertTrue(next.commands.contains(SavedRecipesCommand.ToggleFavorite(RecipeId("rec-1"))))
    }

    @Test
    fun addRecipeToDiaryClicked_emits_command_with_recipe() {
        val initial = SavedRecipesState(
            content = SavedRecipesState.Content.Success(
                allRecipes = listOf(sampleRecipe1),
            ),
        )
        val next = update.update(initial, SavedRecipesEvent.Ui.AddRecipeToDiaryClicked(RecipeId("rec-1")))

        assertTrue(next.commands.contains(SavedRecipesCommand.AddRecipeToDiary(sampleRecipe1)))
    }

    @Test
    fun retryClicked_resets_to_loading_and_emits_observeSavedRecipes() {
        val initial = SavedRecipesState(
            content = SavedRecipesState.Content.Error("Ошибка"),
        )
        val next = update.update(initial, SavedRecipesEvent.Ui.RetryClicked)

        val state = requireNotNull(next.state)
        assertIs<SavedRecipesState.Content.Loading>(state.content)
        assertTrue(next.commands.contains(SavedRecipesCommand.ObserveSavedRecipes))
    }

    @Test
    fun generateRecipeClicked_emits_NavigateToAiGenerator_news() {
        val initial = SavedRecipesState()
        val next = update.update(initial, SavedRecipesEvent.Ui.GenerateRecipeClicked)

        assertTrue(next.news.contains(SavedRecipesNews.NavigateToAiGenerator))
    }

    @Test
    fun recipesLoaded_empty_sets_Empty_state() {
        val initial = SavedRecipesState()
        val next = update.update(initial, SavedRecipesEvent.Internal.RecipesLoaded(emptyList()))

        val state = requireNotNull(next.state)
        assertIs<SavedRecipesState.Content.Empty>(state.content)
    }

    @Test
    fun recipesLoaded_with_items_sets_Success_state_and_preserves_selected() {
        val initial = SavedRecipesState(
            content = SavedRecipesState.Content.Success(
                allRecipes = listOf(sampleRecipe1),
                selectedRecipe = sampleRecipe1,
            ),
        )
        val updatedRecipe1 = sampleRecipe1.copy(isFavorite = false)
        val next = update.update(initial, SavedRecipesEvent.Internal.RecipesLoaded(listOf(updatedRecipe1, sampleRecipe2)))

        val state = requireNotNull(next.state)
        val content = assertIs<SavedRecipesState.Content.Success>(state.content)
        assertEquals(2, content.allRecipes.size)
        assertEquals(updatedRecipe1, content.selectedRecipe)
    }

    @Test
    fun recipesLoadFailed_sets_Error_state() {
        val initial = SavedRecipesState()
        val next = update.update(initial, SavedRecipesEvent.Internal.RecipesLoadFailed(RuntimeException("Network error")))

        val state = requireNotNull(next.state)
        val content = assertIs<SavedRecipesState.Content.Error>(state.content)
        assertEquals("Network error", content.message)
    }

    @Test
    fun recipeToggled_emits_ShowMessage_news() {
        val initial = SavedRecipesState()
        val next = update.update(initial, SavedRecipesEvent.Internal.RecipeToggled(sampleRecipe1))

        val newsItem = next.news.filterIsInstance<SavedRecipesNews.ShowMessage>().firstOrNull()
        requireNotNull(newsItem)
        assertTrue(newsItem.message.contains("Белковый омлет со шпинатом"))
    }

    @Test
    fun recipeAddedToDiary_emits_ShowMessage_news() {
        val initial = SavedRecipesState()
        val next = update.update(initial, SavedRecipesEvent.Internal.RecipeAddedToDiary(RecipeId("rec-1")))

        val newsItem = next.news.filterIsInstance<SavedRecipesNews.ShowMessage>().firstOrNull()
        requireNotNull(newsItem)
        assertTrue(newsItem.message.contains("добавлен в дневник"))
    }

    @Test
    fun addToDiaryFailed_emits_ShowMessage_news() {
        val initial = SavedRecipesState()
        val next = update.update(initial, SavedRecipesEvent.Internal.AddToDiaryFailed(RuntimeException("Failed diary insert")))

        val newsItem = next.news.filterIsInstance<SavedRecipesNews.ShowMessage>().firstOrNull()
        requireNotNull(newsItem)
        assertEquals("Failed diary insert", newsItem.message)
    }
}
