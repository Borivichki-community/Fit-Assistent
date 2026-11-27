package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.core.kotea.UiStateMapper

class CatalogSearchUiStateMapper : UiStateMapper<CatalogSearchState, CatalogSearchUiState> {

    override fun map(state: CatalogSearchState): CatalogSearchUiState {
        val uiContent = when (val c = state.content) {
            CatalogSearchState.Content.Loading -> CatalogSearchUiState.Content.Loading
            CatalogSearchState.Content.Success -> CatalogSearchUiState.Content.Success
            CatalogSearchState.Content.Empty -> CatalogSearchUiState.Content.Empty
            is CatalogSearchState.Content.Error -> CatalogSearchUiState.Content.Error(c.message)
        }
        return CatalogSearchUiState(
            query = state.query,
            selectedCategory = state.category,
            items = state.items,
            canLoadMore = state.pageToken.isNotEmpty(),
            content = uiContent,
        )
    }
}
