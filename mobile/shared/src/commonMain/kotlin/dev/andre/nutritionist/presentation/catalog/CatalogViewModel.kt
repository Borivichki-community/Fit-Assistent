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

/**
 * ViewModel экрана каталога рационов питания.
 * Инкапсулирует KoTEA Store и предоставляет StateFlow<CatalogUiState>.
 */
class CatalogViewModel(
    update: CatalogUpdate,
    commandsFlowHandler: CatalogCommandsFlowHandler,
    private val uiStateMapper: CatalogUiStateMapper,
) : ViewModel() {

    private val store: Store<CatalogState, CatalogEvent.Ui, CatalogNews> =
        KoteaStoreFactory.create(
            initialState = CatalogState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val catalogState: StateFlow<CatalogState> = store.state

    val state: StateFlow<CatalogUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<CatalogNews> = store.news

    fun dispatch(event: CatalogEvent.Ui) {
        store.dispatch(event)
    }
}
