package dev.andre.nutritionist.presentation.savedrecipes

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
 * ViewModel экрана сохранённых и избранных рецептов.
 * Инкапсулирует KoTEA Store и предоставляет StateFlow<SavedRecipesUiState>.
 */
class SavedRecipesViewModel(
    update: SavedRecipesUpdate,
    commandsFlowHandler: SavedRecipesCommandsFlowHandler,
    private val uiStateMapper: SavedRecipesUiStateMapper,
) : ViewModel() {

    private val store: Store<SavedRecipesState, SavedRecipesEvent.Ui, SavedRecipesNews> =
        KoteaStoreFactory.create(
            initialState = SavedRecipesState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val savedRecipesState: StateFlow<SavedRecipesState> = store.state

    val state: StateFlow<SavedRecipesUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<SavedRecipesNews> = store.news

    fun dispatch(event: SavedRecipesEvent.Ui) {
        store.dispatch(event)
    }
}
