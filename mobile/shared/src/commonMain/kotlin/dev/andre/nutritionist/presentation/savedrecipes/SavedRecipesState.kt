package dev.andre.nutritionist.presentation.savedrecipes

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe

/**
 * Фильтр для списка сохранённых рецептов.
 */
data class SavedRecipesFilter(
    val query: String = "",
    val selectedMealType: MealType? = null,
)

/**
 * MVI-состояние экрана избранных и сохранённых рецептов.
 */
data class SavedRecipesState(
    val filter: SavedRecipesFilter = SavedRecipesFilter(),
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content

        data class Success(
            val allRecipes: List<Recipe>,
            val selectedRecipe: Recipe? = null,
        ) : Content

        data class Empty(
            val filter: SavedRecipesFilter,
        ) : Content

        data class Error(val message: String) : Content
    }
}
