package dev.andre.nutritionist.presentation.airecipe


sealed interface AiRecipeNews {
    data class ShowSnackbar(val message: String) : AiRecipeNews
    data object RecipeAddedSuccess : AiRecipeNews
    data object Dismiss : AiRecipeNews
}
