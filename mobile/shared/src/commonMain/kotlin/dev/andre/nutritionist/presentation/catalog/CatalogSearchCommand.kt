package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.domain.model.Product

sealed interface CatalogSearchCommand {
    data class Search(
        val query: String,
        val category: String?,
        val pageToken: String = "",
        val forceRefresh: Boolean = false,
    ) : CatalogSearchCommand

    data class ToggleFavorite(val product: Product) : CatalogSearchCommand

    data object ObserveFavorites : CatalogSearchCommand
}
