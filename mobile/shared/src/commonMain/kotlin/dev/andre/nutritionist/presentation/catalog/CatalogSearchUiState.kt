package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.presentation.model.ProductUiModel

data class CatalogSearchUiState(
    val query: String = "",
    val selectedCategory: String? = null,
    val items: List<ProductUiModel> = emptyList(),
    val canLoadMore: Boolean = false,
    val content: Content = Content.Empty,
) {
    sealed interface Content {
        data object Loading : Content
        data object Success : Content
        data object Empty : Content
        data class Error(val message: String) : Content
    }
}
