package dev.andre.nutritionist.presentation.statistics

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
 * ViewModel экрана аналитики питания и бюджета.
 * Инкапсулирует KoTEA Store и предоставляет StateFlow<StatisticsUiState>.
 */
class StatisticsViewModel(
    update: StatisticsUpdate,
    commandsFlowHandler: StatisticsCommandsFlowHandler,
    private val uiStateMapper: StatisticsUiStateMapper,
) : ViewModel() {

    private val store: Store<StatisticsState, StatisticsEvent.Ui, StatisticsNews> =
        KoteaStoreFactory.create(
            initialState = StatisticsState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val statisticsState: StateFlow<StatisticsState> = store.state

    val state: StateFlow<StatisticsUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<StatisticsNews> = store.news

    fun dispatch(event: StatisticsEvent.Ui) {
        store.dispatch(event)
    }
}
