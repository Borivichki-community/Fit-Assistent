package dev.andre.nutritionist.android.auth

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
 * ViewModel авторизации на базе KoTEA Store.
 * Управляет жизненным циклом Store, маппингом состояния в [AuthUiState] и доставкой [AuthNews].
 */
class AuthViewModel(
    update: AuthUpdate,
    commandsFlowHandler: AuthCommandsFlowHandler,
    private val uiStateMapper: AuthUiStateMapper,
) : ViewModel() {

    private val store: Store<AuthState, AuthEvent.Ui, AuthNews> =
        KoteaStoreFactory.create(
            initialState = AuthState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val state: StateFlow<AuthUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<AuthNews> = store.news

    fun dispatch(event: AuthEvent.Ui) {
        store.dispatch(event)
    }
}
