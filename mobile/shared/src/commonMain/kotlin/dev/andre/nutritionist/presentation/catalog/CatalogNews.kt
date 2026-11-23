package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.domain.model.MealPlanId

/**
 * Одноразовые эффекты экрана каталога рационов.
 */
sealed interface CatalogNews {
    data class ShowSnackbar(val message: String) : CatalogNews
    data object NavigateToAiGenerator : CatalogNews
    data class NavigateToPlanDetail(val planId: MealPlanId) : CatalogNews
}
