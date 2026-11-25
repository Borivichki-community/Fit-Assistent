package dev.andre.nutritionist.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.andre.nutritionist.core.kotea.KoteaStoreFactory
import dev.andre.nutritionist.core.kotea.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CatalogSearchViewModel(
    update: CatalogSearchUpdate,
    commandsFlowHandler: CatalogSearchCommandsFlowHandler,
    private val uiStateMapper: CatalogSearchUiStateMapper,
) : ViewModel() {

    private val store: Store<CatalogSearchState, CatalogSearchEvent.Ui, CatalogSearchNews> =
        KoteaStoreFactory.create(
            initialState = CatalogSearchState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val state: StateFlow<CatalogSearchUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<CatalogSearchNews> = store.news

    fun dispatch(event: CatalogSearchEvent.Ui) {
        store.dispatch(event)
    }
}
