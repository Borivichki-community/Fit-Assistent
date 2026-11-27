package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.*
import kotlin.test.*

class AiRecipeUpdateTest {
    private val update = AiRecipeUpdate()
    private val initial = AiRecipeState(AiRecipePrompt(MealType.DINNER, 400), 1000L)
    @Test
    fun generationSaveAndRetry() {
        val generating = requireNotNull(update.update(initial, AiRecipeEvent.Ui.GenerateClicked).state)
        assertIs<AiRecipeState.Content.Generating>(generating.content)
        val recipe = Recipe(RecipeId("recipe"), "Каша", totalWeightGrams = 200.0, nutrition = NutritionFacts(200.0, 10.0, 4.0, 31.0), estimatedCost = Money.fromMinor(15000))
        val success = requireNotNull(update.update(generating, AiRecipeEvent.Internal.RecipeGenerated(recipe)).state)
        val retry = update.update(success, AiRecipeEvent.Ui.RegenerateClicked)
        assertEquals(listOf(AiRecipeCommand.Generate(initial.prompt)),retry.commands)
        val save = update.update(success,AiRecipeEvent.Ui.AddRecipeToDiaryClicked)
        assertEquals(listOf(AiRecipeCommand.AddRecipeToDiary(recipe,MealType.DINNER,1000L)),save.commands)
        assertTrue(update.update(requireNotNull(save.state),AiRecipeEvent.Ui.AddRecipeToDiaryClicked).commands.isEmpty())
        val failure = update.update(generating,AiRecipeEvent.Internal.GenerationFailed(IllegalStateException("Нет сети")))
        assertEquals(AiRecipeState.Content.Error("Не удалось сгенерировать блюдо. Попробуйте ещё раз."),failure.state?.content)
        assertEquals(initial,update.update(generating,AiRecipeEvent.Ui.CancelClicked).state)
    }
}
