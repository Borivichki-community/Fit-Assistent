package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.domain.model.MealPlanId

/**
 * Асинхронные команды экрана каталога рационов.
 */
sealed interface CatalogCommand {
    data class LoadPlans(val query: String, val category: String) : CatalogCommand
    data class ToggleFavorite(val planId: MealPlanId) : CatalogCommand
}
