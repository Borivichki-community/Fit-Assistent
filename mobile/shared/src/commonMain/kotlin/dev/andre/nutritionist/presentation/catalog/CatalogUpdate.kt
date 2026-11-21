package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.core.kotea.DslUpdate

/**
 * Синхронный чистый редьюсер экрана каталога рационов.
 */
class CatalogUpdate : DslUpdate<CatalogState, CatalogEvent, CatalogCommand, CatalogNews>() {

    override fun NextBuilder<CatalogState, CatalogCommand, CatalogNews>.update(event: CatalogEvent) {
        when (event) {
            CatalogEvent.Ui.Load -> {
                state { copy(content = CatalogState.Content.Loading) }
                commands(CatalogCommand.LoadPlans(query = state.query, category = state.selectedCategory))
            }

            is CatalogEvent.Ui.SearchQueryChanged -> {
                state {
                    copy(
                        query = event.query,
                        content = CatalogState.Content.Loading,
                    )
                }
                commands(CatalogCommand.LoadPlans(query = event.query, category = state.selectedCategory))
            }

            is CatalogEvent.Ui.CategorySelected -> {
                state {
                    copy(
                        selectedCategory = event.category,
                        content = CatalogState.Content.Loading,
                    )
                }
                commands(CatalogCommand.LoadPlans(query = state.query, category = event.category))
            }

            is CatalogEvent.Ui.PlanSelected -> {
                state { copy(selectedPlanId = event.planId) }
                news(CatalogNews.NavigateToPlanDetail(event.planId))
            }

            is CatalogEvent.Ui.ToggleFavorite -> {
                val currentContent = state.content
                if (currentContent is CatalogState.Content.Success) {
                    val updated = currentContent.plans.map { plan ->
                        if (plan.id == event.planId) plan.copy(isFavorite = !plan.isFavorite) else plan
                    }
                    state { copy(content = CatalogState.Content.Success(updated)) }
                }
                commands(CatalogCommand.ToggleFavorite(event.planId))
            }

            CatalogEvent.Ui.GenerateAiPlanClick -> {
                news(CatalogNews.NavigateToAiGenerator)
            }

            CatalogEvent.Ui.Retry -> {
                state { copy(content = CatalogState.Content.Loading) }
                commands(CatalogCommand.LoadPlans(query = state.query, category = state.selectedCategory))
            }

            is CatalogEvent.Internal.PlansLoaded -> {
                state { copy(content = CatalogState.Content.Success(event.plans)) }
            }

            is CatalogEvent.Internal.FavoriteUpdated -> {
                // Оптимистичное обновление уже применено
            }

            is CatalogEvent.Internal.LoadFailed -> {
                state {
                    copy(
                        content = CatalogState.Content.Error(
                            event.error.message ?: "Ошибка загрузки каталога",
                        ),
                    )
                }
            }
        }
    }
}
