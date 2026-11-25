package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.presentation.mappers.ProductUiMapper.toUiModel

class CatalogSearchUpdate :
    DslUpdate<CatalogSearchState, CatalogSearchEvent, CatalogSearchCommand, CatalogSearchNews>() {

    override fun NextBuilder<CatalogSearchState, CatalogSearchCommand, CatalogSearchNews>.update(
        event: CatalogSearchEvent,
    ) {
        when (event) {
            is CatalogSearchEvent.Ui.QueryChanged -> {
                state {
                    copy(
                        query = event.query,
                        pageToken = "",
                        content = CatalogSearchState.Content.Loading,
                    )
                }
                commands(
                    CatalogSearchCommand.Search(
                        query = event.query,
                        category = state.category,
                        pageToken = "",
                    ),
                )
            }

            is CatalogSearchEvent.Ui.CategorySelected -> {
                state {
                    copy(
                        category = event.category,
                        pageToken = "",
                        content = CatalogSearchState.Content.Loading,
                    )
                }
                commands(
                    CatalogSearchCommand.Search(
                        query = state.query,
                        category = event.category,
                        pageToken = "",
                    ),
                )
            }

            is CatalogSearchEvent.Ui.ToggleFavorite -> {
                val product = state.domainProducts[event.productId]
                if (product != null) {
                    val nextFavorite = !(state.isFavoriteMap[event.productId] ?: false)
                    val updatedMap = state.isFavoriteMap + (event.productId to nextFavorite)
                    state {
                        copy(
                            isFavoriteMap = updatedMap,
                            items = items.map { item ->
                                if (item.id == event.productId) item.copy(isFavorite = nextFavorite) else item
                            },
                        )
                    }
                    commands(CatalogSearchCommand.ToggleFavorite(product))
                }
            }

            CatalogSearchEvent.Ui.LoadNextPage -> {
                if (state.pageToken.isNotEmpty() && state.content is CatalogSearchState.Content.Success) {
                    commands(
                        CatalogSearchCommand.Search(
                            query = state.query,
                            category = state.category,
                            pageToken = state.pageToken,
                        ),
                    )
                }
            }

            CatalogSearchEvent.Ui.Retry -> {
                state { copy(content = CatalogSearchState.Content.Loading) }
                commands(
                    CatalogSearchCommand.Search(
                        query = state.query,
                        category = state.category,
                        pageToken = "",
                        forceRefresh = true,
                    ),
                )
            }

            CatalogSearchEvent.Ui.ObserveFavorites -> {
                commands(CatalogSearchCommand.ObserveFavorites)
            }

            is CatalogSearchEvent.Internal.Loaded -> {
                val domainMap = event.products.associateBy { it.id }
                val uiItems = event.products.map { product ->
                    product.toUiModel(isFavorite = state.isFavoriteMap[product.id] == true)
                }
                state {
                    copy(
                        items = uiItems,
                        domainProducts = domainMap,
                        pageToken = event.nextPageToken,
                        content = if (uiItems.isEmpty()) {
                            CatalogSearchState.Content.Empty
                        } else {
                            CatalogSearchState.Content.Success
                        },
                    )
                }
            }

            is CatalogSearchEvent.Internal.NextPageLoaded -> {
                val existingIds = state.items.mapTo(mutableSetOf()) { it.id }
                val newProducts = event.products.filterNot { it.id in existingIds }
                val mergedDomain = state.domainProducts + newProducts.associateBy { it.id }
                val mergedUi = state.items + newProducts.map { product ->
                    product.toUiModel(isFavorite = state.isFavoriteMap[product.id] == true)
                }
                state {
                    copy(
                        items = mergedUi,
                        domainProducts = mergedDomain,
                        pageToken = event.nextPageToken,
                        content = if (mergedUi.isEmpty()) {
                            CatalogSearchState.Content.Empty
                        } else {
                            CatalogSearchState.Content.Success
                        },
                    )
                }
            }

            is CatalogSearchEvent.Internal.FavoritesUpdated -> {
                val updatedMap = event.favoriteIds.associateWith { true }
                state {
                    copy(
                        isFavoriteMap = updatedMap,
                        items = items.map { item ->
                            item.copy(isFavorite = item.id in event.favoriteIds)
                        },
                    )
                }
            }

            is CatalogSearchEvent.Internal.FavoriteToggled -> {
                val updatedMap = if (event.isFavorite) {
                    state.isFavoriteMap + (event.productId to true)
                } else {
                    state.isFavoriteMap - event.productId
                }
                state {
                    copy(
                        isFavoriteMap = updatedMap,
                        items = items.map { item ->
                            if (item.id == event.productId) item.copy(isFavorite = event.isFavorite) else item
                        },
                    )
                }
            }

            is CatalogSearchEvent.Internal.Failed -> {
                val message = event.error.message?.takeIf { it.isNotBlank() }
                    ?: "Не удалось загрузить продукты"
                if (state.items.isEmpty()) {
                    state { copy(content = CatalogSearchState.Content.Error(message)) }
                }
                news(CatalogSearchNews.ShowSnackbar(message))
            }
        }
    }
}
