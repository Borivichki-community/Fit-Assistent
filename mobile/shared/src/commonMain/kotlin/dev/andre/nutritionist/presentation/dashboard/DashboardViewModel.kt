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

/**
 * ViewModel главного экрана (Dashboard / Diary).
 * Управляет жизненным циклом KoTEA Store и предоставляет StateFlow<DashboardUiState>.
 */
class DashboardViewModel(
    update: DashboardUpdate,
    commandsFlowHandler: DashboardCommandsFlowHandler,
    private val uiStateMapper: DashboardUiStateMapper,
) : ViewModel() {

    private val store: Store<DashboardState, DashboardEvent.Ui, DashboardNews> =
        KoteaStoreFactory.create(
            initialState = DashboardState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val dashboardState: StateFlow<DashboardState> = store.state

    val state: StateFlow<DashboardUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<DashboardNews> = store.news

    fun dispatch(event: DashboardEvent.Ui) {
        store.dispatch(event)
    }
}
