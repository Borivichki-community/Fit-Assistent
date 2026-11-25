package dev.andre.nutritionist.presentation.airecipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.andre.nutritionist.core.kotea.KoteaStoreFactory
import dev.andre.nutritionist.core.kotea.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MenuViewModel(
    update: MenuUpdate,
    commandsFlowHandler: MenuCommandsFlowHandler,
    private val uiStateMapper: MenuUiStateMapper,
) : ViewModel() {

    private val store: Store<MenuState, MenuEvent.Ui, MenuNews> =
        KoteaStoreFactory.create(
            initialState = MenuState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val state: StateFlow<MenuUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<MenuNews> = store.news

    fun dispatch(event: MenuEvent.Ui) {
        store.dispatch(event)
    }
}
