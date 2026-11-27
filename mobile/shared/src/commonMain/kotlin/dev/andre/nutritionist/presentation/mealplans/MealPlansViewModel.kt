package dev.andre.nutritionist.presentation.mealplans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.andre.nutritionist.core.kotea.KoteaStoreFactory
import dev.andre.nutritionist.core.kotea.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MealPlansViewModel(
    update: MealPlansUpdate,
    commandsFlowHandler: MealPlansCommandsFlowHandler,
    private val uiStateMapper: MealPlansUiStateMapper,
) : ViewModel() {

    private val store: Store<MealPlansState, MealPlansEvent.Ui, MealPlansNews> =
        KoteaStoreFactory.create(
            initialState = MealPlansState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val state: StateFlow<MealPlansUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<MealPlansNews> = store.news

    fun dispatch(event: MealPlansEvent.Ui) {
        store.dispatch(event)
    }
}
