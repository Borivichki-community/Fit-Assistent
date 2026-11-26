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

class CoachStudentsViewModel(
    update: CoachStudentsUpdate,
    commandsFlowHandler: CoachStudentsCommandsFlowHandler,
    private val uiStateMapper: CoachStudentsUiStateMapper,
) : ViewModel() {

    private val store: Store<CoachStudentsState, CoachStudentsEvent.Ui, CoachStudentsNews> =
        KoteaStoreFactory.create(
            initialState = CoachStudentsState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val state: StateFlow<CoachStudentsUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<CoachStudentsNews> = store.news

    fun dispatch(event: CoachStudentsEvent.Ui) {
        store.dispatch(event)
    }
}
