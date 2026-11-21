package dev.andre.nutritionist.presentation.addmeal

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
 * ViewModel экрана добавления блюда в дневник питания.
 * Управляет жизненным циклом KoTEA Store и предоставляет StateFlow<AddMealUiState>.
 */
class AddMealViewModel(
    update: AddMealUpdate,
    commandsFlowHandler: AddMealCommandsFlowHandler,
    private val uiStateMapper: AddMealUiStateMapper,
) : ViewModel() {

    private val store: Store<AddMealState, AddMealEvent.Ui, AddMealNews> =
        KoteaStoreFactory.create(
            initialState = AddMealState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val addMealState: StateFlow<AddMealState> = store.state

    val state: StateFlow<AddMealUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<AddMealNews> = store.news

    fun dispatch(event: AddMealEvent.Ui) {
        store.dispatch(event)
    }
}
