package dev.andre.nutritionist.presentation.planmanager

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
 * ViewModel экрана менеджера рационов питания.
 * Инкапсулирует KoTEA Store и предоставляет StateFlow<MealPlanManagerUiState>.
 */
class MealPlanManagerViewModel(
    update: MealPlanManagerUpdate,
    commandsFlowHandler: MealPlanManagerCommandsFlowHandler,
    private val uiStateMapper: MealPlanManagerUiStateMapper,
) : ViewModel() {

    private val store: Store<MealPlanManagerState, MealPlanManagerEvent.Ui, MealPlanManagerNews> =
        KoteaStoreFactory.create(
            initialState = MealPlanManagerState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val managerState: StateFlow<MealPlanManagerState> = store.state

    val state: StateFlow<MealPlanManagerUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<MealPlanManagerNews> = store.news

    fun dispatch(event: MealPlanManagerEvent.Ui) {
        store.dispatch(event)
    }
}
