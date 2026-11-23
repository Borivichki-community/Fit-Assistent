package dev.andre.nutritionist.presentation.airecipe

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
 * ViewModel экрана генерации AI-рецептов, инкапсулирующая KoTEA Store.
 */
class AiRecipeViewModel(
    update: AiRecipeUpdate,
    commandsFlowHandler: AiRecipeCommandsFlowHandler,
    private val uiStateMapper: AiRecipeUiStateMapper,
) : ViewModel() {

    private val store: Store<AiRecipeState, AiRecipeEvent.Ui, AiRecipeNews> =
        KoteaStoreFactory.create(
            initialState = AiRecipeState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val recipeState: StateFlow<AiRecipeState> = store.state

    val state: StateFlow<AiRecipeUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<AiRecipeNews> = store.news

    fun dispatch(event: AiRecipeEvent.Ui) {
        store.dispatch(event)
    }
}
