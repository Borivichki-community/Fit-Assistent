package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.AiRecipePrompt
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AiRecipeUpdateTest {

    private val update = AiRecipeUpdate()

    private val testRecipe = Recipe(
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
        isFavorite = false,
    )

    @Test
    fun queryChanged_updates_prompt_query() {
        val initial = AiRecipeState(AiRecipeState.Content.Configuring(AiRecipePrompt()))
        val next = update.update(initial, AiRecipeEvent.Ui.QueryChanged("Сырники"))

        val state = requireNotNull(next.state)
        val content = assertIs<AiRecipeState.Content.Configuring>(state.content)
        assertEquals("Сырники", content.prompt.query)
    }

    @Test
    fun caloriesChanged_updates_target_calories() {
        val initial = AiRecipeState(AiRecipeState.Content.Configuring(AiRecipePrompt()))
        val next = update.update(initial, AiRecipeEvent.Ui.CaloriesChanged(750))

        val state = requireNotNull(next.state)
        val content = assertIs<AiRecipeState.Content.Configuring>(state.content)
        assertEquals(750, content.prompt.targetCalories)
    }

    @Test
    fun budgetChanged_updates_target_budget() {
        val initial = AiRecipeState(AiRecipeState.Content.Configuring(AiRecipePrompt()))
        val next = update.update(initial, AiRecipeEvent.Ui.BudgetChanged(350))

        val state = requireNotNull(next.state)
        val content = assertIs<AiRecipeState.Content.Configuring>(state.content)
        assertEquals(350, content.prompt.targetBudgetRub)
    }

    @Test
    fun mealTypeSelected_updates_meal_type() {
        val initial = AiRecipeState(AiRecipeState.Content.Configuring(AiRecipePrompt()))
        val next = update.update(initial, AiRecipeEvent.Ui.MealTypeSelected(MealType.DINNER))

        val state = requireNotNull(next.state)
        val content = assertIs<AiRecipeState.Content.Configuring>(state.content)
        assertEquals(MealType.DINNER, content.prompt.mealType)
    }

    @Test
    fun dietaryPreferenceSelected_updates_preference() {
        val initial = AiRecipeState(AiRecipeState.Content.Configuring(AiRecipePrompt()))
        val next = update.update(initial, AiRecipeEvent.Ui.DietaryPreferenceSelected("Высокий белок"))

        val state = requireNotNull(next.state)
        val content = assertIs<AiRecipeState.Content.Configuring>(state.content)
        assertEquals("Высокий белок", content.prompt.dietaryPreference)
    }

    @Test
    fun generateClicked_transitions_to_generating_and_emits_command() {
        val prompt = AiRecipePrompt(query = "Курица", targetCalories = 600)
        val initial = AiRecipeState(AiRecipeState.Content.Configuring(prompt))
        val next = update.update(initial, AiRecipeEvent.Ui.GenerateClicked)

        val state = requireNotNull(next.state)
        val content = assertIs<AiRecipeState.Content.Generating>(state.content)
        assertEquals(prompt, content.prompt)
        assertEquals(listOf(AiRecipeCommand.Generate(prompt)), next.commands)
    }

    @Test
    fun generateClicked_when_offline_emits_snackbar_and_blocks_command() {
        val prompt = AiRecipePrompt(query = "Курица", targetCalories = 600)
        val initial = AiRecipeState(
            content = AiRecipeState.Content.Configuring(prompt),
            isOnline = false,
        )
        val next = update.update(initial, AiRecipeEvent.Ui.GenerateClicked)

        assertTrue(next.commands.isEmpty())
        assertEquals(1, next.news.size)
        val snackbar = assertIs<AiRecipeNews.ShowSnackbar>(next.news.first())
        assertEquals("Генерация рецептов недоступна без интернета", snackbar.message)
    }

    @Test
    fun networkStatusChanged_updates_isOnline_state() {
        val initial = AiRecipeState(isOnline = true)
        val next = update.update(initial, AiRecipeEvent.Ui.NetworkStatusChanged(false))

        val state = requireNotNull(next.state)
        assertEquals(false, state.isOnline)
    }

    @Test
    fun generationProgressUpdated_updates_phase_and_progress() {
        val prompt = AiRecipePrompt()
        val initial = AiRecipeState(AiRecipeState.Content.Generating(prompt, "Старт", 0.1f))
        val next = update.update(initial, AiRecipeEvent.Internal.GenerationProgressUpdated("Шеф готовит рецепт...", 0.85f))

        val state = requireNotNull(next.state)
        val content = assertIs<AiRecipeState.Content.Generating>(state.content)
        assertEquals("Шеф готовит рецепт...", content.phaseText)
        assertEquals(0.85f, content.progress)
    }

    @Test
    fun recipeGenerated_transitions_to_success() {
        val prompt = AiRecipePrompt()
        val initial = AiRecipeState(AiRecipeState.Content.Generating(prompt))
        val next = update.update(initial, AiRecipeEvent.Internal.RecipeGenerated(testRecipe))

        val state = requireNotNull(next.state)
        val content = assertIs<AiRecipeState.Content.Success>(state.content)
        assertEquals(testRecipe, content.recipe)
        assertEquals(false, content.isSaved)
    }

    @Test
    fun saveRecipeClicked_and_saved_flow() {
        val initial = AiRecipeState(AiRecipeState.Content.Success(testRecipe, isSaved = false))
        val next1 = update.update(initial, AiRecipeEvent.Ui.SaveRecipeClicked)
        assertEquals(listOf(AiRecipeCommand.SaveRecipe(testRecipe)), next1.commands)

        val next2 = update.update(initial, AiRecipeEvent.Internal.RecipeSaved(testRecipe))
        val state = requireNotNull(next2.state)
        val content = assertIs<AiRecipeState.Content.Success>(state.content)
        assertTrue(content.isSaved)
        assertTrue(next2.news.any { it is AiRecipeNews.ShowSnackbar })
    }

    @Test
    fun addRecipeToDiary_flow() {
        val initial = AiRecipeState(AiRecipeState.Content.Success(testRecipe))
        val next1 = update.update(initial, AiRecipeEvent.Ui.AddRecipeToDiaryClicked)
        assertEquals(listOf(AiRecipeCommand.AddRecipeToDiary(testRecipe)), next1.commands)

        val next2 = update.update(initial, AiRecipeEvent.Internal.RecipeAddedToDiary)
        assertTrue(next2.news.any { it is AiRecipeNews.RecipeAddedToDiarySuccess })
    }

    @Test
    fun resetClicked_returns_to_configuring() {
        val initial = AiRecipeState(AiRecipeState.Content.Success(testRecipe))
        val next = update.update(initial, AiRecipeEvent.Ui.ResetClicked)

        val state = requireNotNull(next.state)
        assertIs<AiRecipeState.Content.Configuring>(state.content)
    }

    @Test
    fun generationFailed_transitions_to_error_and_retry_re_triggers_generation() {
        val prompt = AiRecipePrompt(query = "Суп", targetCalories = 450)
        val initial = AiRecipeState(AiRecipeState.Content.Generating(prompt))
        val nextError = update.update(initial, AiRecipeEvent.Internal.GenerationFailed(IllegalStateException("AI server busy")))

        val stateError = requireNotNull(nextError.state)
        val errorContent = assertIs<AiRecipeState.Content.Error>(stateError.content)
        assertEquals("AI server busy", errorContent.message)
        assertEquals(prompt, errorContent.lastPrompt)

        val nextRetry = update.update(stateError, AiRecipeEvent.Ui.RetryClicked)
        val stateRetry = requireNotNull(nextRetry.state)
        assertIs<AiRecipeState.Content.Generating>(stateRetry.content)
        assertEquals(listOf(AiRecipeCommand.Generate(prompt)), nextRetry.commands)
    }
}
