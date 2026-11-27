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

class StudentCoachingViewModel(
    update: StudentCoachingUpdate,
    commandsFlowHandler: StudentCoachingCommandsFlowHandler,
    private val uiStateMapper: StudentCoachingUiStateMapper,
) : ViewModel() {

    private val store: Store<StudentCoachingState, StudentCoachingEvent.Ui, StudentCoachingNews> =
        KoteaStoreFactory.create(
            initialState = StudentCoachingState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val state: StateFlow<StudentCoachingUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<StudentCoachingNews> = store.news

    fun dispatch(event: StudentCoachingEvent.Ui) {
        store.dispatch(event)
    }
}
