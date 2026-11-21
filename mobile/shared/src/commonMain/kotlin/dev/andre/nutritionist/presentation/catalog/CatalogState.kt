package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanId

/**
 * Неделимый снимок состояния экрана каталога рационов питания.
 */
data class CatalogState(
    val query: String = "",
    val selectedCategory: String = "Все",
    val selectedPlanId: MealPlanId? = null,
    val isOnline: Boolean = true,
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val plans: List<MealPlan>) : Content
        data class Error(val message: String) : Content
    }
}
