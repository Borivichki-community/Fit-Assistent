package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe

sealed interface AiRecipeCommand {
    data class Generate(val prompt: AiRecipePrompt) : AiRecipeCommand
    data class AddRecipeToDiary(val recipe: Recipe, val mealType: MealType, val dateEpochMs: Long) : AiRecipeCommand
    data object Cancel : AiRecipeCommand
}
