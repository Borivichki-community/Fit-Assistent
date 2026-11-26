package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId

/**
 * Внутреннее состояние модального меню выбора блюда (@fitapp_4).
 */
data class AddMealState(
    val mealType: MealType = MealType.LUNCH,
    val selectedDateEpochMs: Long = 0L,
    val query: String = "",
    val items: List<AddMealSearchItemUiModel> = emptyList(),
    val domainProducts: Map<ProductId, Product> = emptyMap(),
    val domainRecipes: Map<RecipeId, Recipe> = emptyMap(),
    val content: Content = Content.Idle,
) {
    sealed interface Content {
        data object Idle : Content
        data object Loading : Content
        data class Success(val isSaving: Boolean = false) : Content
        data object Empty : Content
        data class Error(val message: String) : Content
    }
}
