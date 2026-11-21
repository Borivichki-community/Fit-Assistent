package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.AiRecipePrompt
import dev.andre.nutritionist.domain.model.Recipe

/**
 * Команды экрана генерации AI-рецепта в архитектуре KoTEA MVI.
 */
sealed interface AiRecipeCommand {
    data class Generate(val prompt: AiRecipePrompt) : AiRecipeCommand
    data class SaveRecipe(val recipe: Recipe) : AiRecipeCommand
    data class AddRecipeToDiary(val recipe: Recipe) : AiRecipeCommand
}
