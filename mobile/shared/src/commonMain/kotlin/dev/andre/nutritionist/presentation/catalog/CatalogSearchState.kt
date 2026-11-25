package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.presentation.model.ProductUiModel

data class CatalogSearchState(
    val query: String = "",
    val category: String? = null,
    val items: List<ProductUiModel> = emptyList(),
    val isFavoriteMap: Map<ProductId, Boolean> = emptyMap(),
    val pageToken: String = "",
    val domainProducts: Map<ProductId, Product> = emptyMap(),
    val content: Content = Content.Empty,
) {
    sealed interface Content {
        data object Loading : Content
        data object Success : Content
        data object Empty : Content
        data class Error(val message: String) : Content
    }
}
