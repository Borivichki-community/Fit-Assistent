package dev.andre.nutritionist.presentation.savedrecipes

import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId

/**
 * Команды для выполнения в фоновом потоке экрана сохранённых рецептов.
 */
sealed interface SavedRecipesCommand {
    data object ObserveSavedRecipes : SavedRecipesCommand
    data class ToggleFavorite(val recipeId: RecipeId) : SavedRecipesCommand
    data class AddRecipeToDiary(val recipe: Recipe) : SavedRecipesCommand
}
