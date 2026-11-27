package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.*
import kotlin.test.*

class AiRecipeUiStateMapperTest {
    @Test
    fun formatsServingAndSlot() {
        val state = AiRecipeState(AiRecipePrompt(MealType.DINNER,400),0L,AiRecipeState.Content.Success(Recipe(RecipeId("recipe"), "Каша", totalWeightGrams = 200.0, nutrition = NutritionFacts(200.0, 10.0, 4.0, 31.0), estimatedCost = Money.fromMinor(15000))))
        val result = assertIs<AiRecipeUiState.Content.Success>(AiRecipeUiStateMapper().map(state).content)
        assertEquals("400 ккал • 150 ₽",result.badges)
        assertEquals("Добавить в ужин",result.addLabel)
        assertEquals(1f,result.macros.sumOf { it.second.toDouble() }.toFloat(),0.001f)
    }
}
