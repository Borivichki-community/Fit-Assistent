package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId

sealed interface CatalogSearchEvent {

    sealed interface Ui : CatalogSearchEvent {
        data class QueryChanged(val query: String) : Ui
        data class CategorySelected(val category: String?) : Ui
        data class ToggleFavorite(val productId: ProductId) : Ui
        data object LoadNextPage : Ui
        data object Retry : Ui
        data object ObserveFavorites : Ui
    }

    sealed interface Internal : CatalogSearchEvent {
        data class Loaded(val products: List<Product>, val nextPageToken: String) : Internal
        data class NextPageLoaded(val products: List<Product>, val nextPageToken: String) : Internal
        data class FavoritesUpdated(val favoriteIds: Set<ProductId>) : Internal
        data class FavoriteToggled(val productId: ProductId, val isFavorite: Boolean) : Internal
        data class Failed(val error: Throwable) : Internal
    }
}
