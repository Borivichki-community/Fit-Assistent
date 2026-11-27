package dev.andre.nutritionist.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.andre.nutritionist.core.kotea.KoteaStoreFactory
import dev.andre.nutritionist.core.kotea.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DiaryViewModel(
    update: DiaryUpdate,
    commandsFlowHandler: DiaryCommandsFlowHandler,
    private val uiStateMapper: DiaryUiStateMapper,
) : ViewModel() {

    private val store: Store<DiaryState, DiaryEvent.Ui, DiaryNews> =
        KoteaStoreFactory.create(
            initialState = DiaryState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val state: StateFlow<DiaryUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<DiaryNews> = store.news

    fun dispatch(event: DiaryEvent.Ui) {
        store.dispatch(event)
    }
}
