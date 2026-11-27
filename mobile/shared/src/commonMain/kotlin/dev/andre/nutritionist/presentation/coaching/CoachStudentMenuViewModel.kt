package dev.andre.nutritionist.presentation.coaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.andre.nutritionist.core.kotea.KoteaStoreFactory
import dev.andre.nutritionist.core.kotea.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CoachStudentMenuViewModel(
    update: CoachStudentMenuUpdate,
    commandsFlowHandler: CoachStudentMenuCommandsFlowHandler,
    private val uiStateMapper: CoachStudentMenuUiStateMapper,
) : ViewModel() {

    private val store: Store<CoachStudentMenuState, CoachStudentMenuEvent.Ui, CoachStudentMenuNews> =
        KoteaStoreFactory.create(
            initialState = CoachStudentMenuState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val state: StateFlow<CoachStudentMenuUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<CoachStudentMenuNews> = store.news

    fun dispatch(event: CoachStudentMenuEvent.Ui) {
        store.dispatch(event)
    }
}
