package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanId

/**
 * Все события экрана каталога рационов питания.
 */
sealed interface CatalogEvent {

    sealed interface Ui : CatalogEvent {
        data object Load : Ui

        data class SearchQueryChanged(val query: String) : Ui

        data class CategorySelected(val category: String) : Ui

        data class PlanSelected(val planId: MealPlanId) : Ui

        data class ToggleFavorite(val planId: MealPlanId) : Ui

        data object GenerateAiPlanClick : Ui

        data object Retry : Ui

        data class NetworkStatusChanged(val isConnected: Boolean) : Ui
    }

    sealed interface Internal : CatalogEvent {
        data class PlansLoaded(val plans: List<MealPlan>) : Internal

        data class FavoriteUpdated(val planId: MealPlanId, val isFavorite: Boolean) : Internal

        data class LoadFailed(val error: Throwable) : Internal
    }
}
