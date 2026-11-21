package dev.andre.nutritionist.presentation.savedrecipes

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.airecipe.RecipeResultUiModel

/**
 * UI-модель чипса фильтрации по приёму пищи.
 */
data class MealTypeFilterUiModel(
    val mealType: MealType?,
    val title: String,
    val isSelected: Boolean,
)

/**
 * UI-модель карточки сохранённого рецепта.
 */
data class SavedRecipeItemUiModel(
    val id: String,
    val title: String,
    val description: String,
    val mealTypeBadge: String,
    val caloriesBadge: String,
    val budgetBadge: String,
    val cookingTimeBadge: String,
    val macroSummaryText: String,
    val isFavorite: Boolean,
)

/**
 * Состояние экрана сохранённых рецептов для UI слоя.
 */
data class SavedRecipesUiState(
    val searchQuery: String = "",
    val mealTypeFilters: List<MealTypeFilterUiModel> = emptyList(),
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content

        data class Empty(
            val title: String,
            val description: String,
            val actionTitle: String,
        ) : Content

        data class Success(
            val recipes: List<SavedRecipeItemUiModel>,
            val countText: String,
            val selectedRecipeDetail: RecipeResultUiModel? = null,
        ) : Content

        data class Error(val message: String) : Content
    }
}
