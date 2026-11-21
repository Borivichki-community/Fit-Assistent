package dev.andre.nutritionist.presentation.savedrecipes

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId

/**
 * События экрана сохранённых рецептов.
 */
sealed interface SavedRecipesEvent {

    sealed interface Ui : SavedRecipesEvent {
        data object Init : Ui
        data class SearchQueryChanged(val query: String) : Ui
        data class MealTypeFilterSelected(val mealType: MealType?) : Ui
        data class RecipeClicked(val recipeId: RecipeId) : Ui
        data object RecipeDetailDismissed : Ui
        data class ToggleFavoriteClicked(val recipeId: RecipeId) : Ui
        data class AddRecipeToDiaryClicked(val recipeId: RecipeId) : Ui
        data object RetryClicked : Ui
        data object GenerateRecipeClicked : Ui
    }

    sealed interface Internal : SavedRecipesEvent {
        data class RecipesLoaded(val recipes: List<Recipe>) : Internal
        data class RecipesLoadFailed(val throwable: Throwable) : Internal
        data class RecipeToggled(val recipe: Recipe) : Internal
        data class ToggleFailed(val throwable: Throwable) : Internal
        data class RecipeAddedToDiary(val recipeId: RecipeId) : Internal
        data class AddToDiaryFailed(val throwable: Throwable) : Internal
    }
}
