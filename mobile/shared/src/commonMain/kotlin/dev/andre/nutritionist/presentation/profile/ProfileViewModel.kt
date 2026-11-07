package dev.andre.nutritionist.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.andre.nutritionist.core.kotea.KoteaStoreFactory
import dev.andre.nutritionist.core.kotea.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    update: ProfileUpdate,
    commandsFlowHandler: ProfileCommandsFlowHandler,
    private val uiStateMapper: ProfileUiStateMapper,
) : ViewModel() {

    private val store: Store<ProfileState, ProfileEvent.Ui, ProfileNews> =
        KoteaStoreFactory.create(
            initialState = ProfileState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val profileState: StateFlow<ProfileState> = store.state

    val state: StateFlow<ProfileUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<ProfileNews> = store.news

    fun dispatch(event: ProfileEvent.Ui) {
        store.dispatch(event)
    }
}
