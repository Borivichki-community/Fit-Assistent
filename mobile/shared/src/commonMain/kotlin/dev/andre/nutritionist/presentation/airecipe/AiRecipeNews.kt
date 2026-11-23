package dev.andre.nutritionist.presentation.airecipe

/**
 * Одноразовые эффекты экрана генерации AI-рецепта в архитектуре KoTEA MVI.
 */
sealed interface AiRecipeNews {
    data class ShowSnackbar(val message: String) : AiRecipeNews
    data object RecipeAddedToDiarySuccess : AiRecipeNews
    data object NavigateBack : AiRecipeNews
}
