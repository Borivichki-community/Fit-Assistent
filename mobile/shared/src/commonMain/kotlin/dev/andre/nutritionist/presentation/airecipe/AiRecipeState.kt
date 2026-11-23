package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.AiRecipePrompt
import dev.andre.nutritionist.domain.model.Recipe

/**
 * Состояние экрана генерации AI-рецепта в архитектуре KoTEA MVI.
 */
data class AiRecipeState(
    val content: Content = Content.Configuring(AiRecipePrompt()),
    val isOnline: Boolean = true,
) {
    sealed interface Content {
        data class Configuring(
            val prompt: AiRecipePrompt,
            val isGenerating: Boolean = false,
        ) : Content

        data class Generating(
            val prompt: AiRecipePrompt,
            val phaseText: String = "Анализируем нутриенты...",
            val progress: Float = 0.2f,
        ) : Content

        data class Success(
            val recipe: Recipe,
            val isSaved: Boolean = false,
        ) : Content

        data class Error(
            val message: String,
            val lastPrompt: AiRecipePrompt? = null,
        ) : Content
    }
}
