package dev.andre.nutritionist.presentation.savedrecipes

/**
 * Одноразовые эффекты/уведомления для экрана сохранённых рецептов.
 */
sealed interface SavedRecipesNews {
    data class ShowSnackbar(val message: String) : SavedRecipesNews
    data class ShowMessage(val message: String) : SavedRecipesNews
    data object NavigateToAiGenerator : SavedRecipesNews
}
