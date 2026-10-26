/**
 * Эталонный код фичи по паттерну The Elm Architecture (KoTEA).
 * Демонстрирует полный стек:
 * State / Event / Command / News → DslUpdate → CommandsFlowHandler → UiStateMapper → ViewModel → Route → Screen.
 *
 * ПРАВИЛА (из AGENTS.md):
 * - State / UiState: вложенный sealed interface Content, нет isLoading/error флагов
 * - DslUpdate: чистый синхронный редьюсер, никаких корутин и сайд-эффектов
 * - CommandsFlowHandler: асинхронные команды, вызов репозитория напрямую (Anti-Boilerplate)
 * - UiStateMapper: все строки формируются в :shared
 * - Route: stateful (koinViewModel + LaunchedEffect для news)
 * - Screen: stateless (только uiState + onEvent, нет логики, нет форматирования)
 * - when(sealed): нет ветки else
 */

package com.budgetntr.android.feature.recipe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.core.kotea.KoteaStoreFactory
import dev.andre.nutritionist.core.kotea.Store
import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

// ─── Value Class ID ─────────────────────────────────────────────────────────

@JvmInline
value class RecipeId(val raw: String)

data class Recipe(val id: RecipeId, val title: String, val calories: Int, val priceRubles: Double)

interface RecipeRepository {
    suspend fun getRecipes(): List<Recipe>
}

// ─── State ──────────────────────────────────────────────────────────────────

data class RecipeState(
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val recipes: List<Recipe>) : Content
        data class Error(val message: String) : Content
    }
}

// ─── Event ──────────────────────────────────────────────────────────────────

sealed interface RecipeEvent {
    sealed interface Ui : RecipeEvent {
        data object Load : Ui
        data object Retry : Ui
        data class SelectRecipe(val id: RecipeId) : Ui
    }
    sealed interface Internal : RecipeEvent {
        data class Loaded(val recipes: List<Recipe>) : Internal
        data class Failed(val error: Throwable) : Internal
    }
}

// ─── Command ────────────────────────────────────────────────────────────────

sealed interface RecipeCommand {
    data object LoadRecipes : RecipeCommand
}

// ─── News ───────────────────────────────────────────────────────────────────

sealed interface RecipeNews {
    data class NavigateToDetail(val id: RecipeId) : RecipeNews
    data class ShowSnackbar(val message: String) : RecipeNews
}

// ─── DslUpdate ──────────────────────────────────────────────────────────────

class RecipeUpdate : DslUpdate<RecipeState, RecipeEvent, RecipeCommand, RecipeNews>() {
    override fun NextBuilder<RecipeState, RecipeCommand, RecipeNews>.update(event: RecipeEvent) {
        when (event) {
            RecipeEvent.Ui.Load -> {
                state { copy(content = RecipeState.Content.Loading) }
                commands(RecipeCommand.LoadRecipes)
            }
            RecipeEvent.Ui.Retry -> {
                state { copy(content = RecipeState.Content.Loading) }
                commands(RecipeCommand.LoadRecipes)
            }
            is RecipeEvent.Ui.SelectRecipe -> {
                news(RecipeNews.NavigateToDetail(event.id))
            }
            is RecipeEvent.Internal.Loaded -> {
                state { copy(content = RecipeState.Content.Success(event.recipes)) }
            }
            is RecipeEvent.Internal.Failed -> {
                state { copy(content = RecipeState.Content.Error(event.error.message ?: "Ошибка загрузки")) }
                news(RecipeNews.ShowSnackbar("Не удалось загрузить рецепты"))
            }
        }
    }
}

// ─── CommandsFlowHandler ───────────────────────────────────────────────────

class RecipeCommandsFlowHandler(
    private val repository: RecipeRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<RecipeCommand, RecipeEvent> {

    override fun handle(commands: Flow<RecipeCommand>): Flow<RecipeEvent> = flow {
        commands.collect { command ->
            when (command) {
                RecipeCommand.LoadRecipes -> {
                    val event = withContext(dispatchers.io) {
                        runCatching { repository.getRecipes() }
                            .fold(
                                onSuccess = { RecipeEvent.Internal.Loaded(it) },
                                onFailure = { RecipeEvent.Internal.Failed(it) },
                            )
                    }
                    emit(event)
                }
            }
        }
    }
}

// ─── UiState & UiStateMapper ────────────────────────────────────────────────

data class RecipeUiModel(
    val id: RecipeId,
    val title: String,
    val caloriesLabel: String,
    val priceLabel: String,
)

data class RecipeUiState(
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val recipes: List<RecipeUiModel>) : Content
        data class Error(val message: String) : Content
    }
}

class RecipeUiStateMapper : UiStateMapper<RecipeState, RecipeUiState> {
    override fun map(state: RecipeState): RecipeUiState {
        val content = when (val c = state.content) {
            RecipeState.Content.Loading -> RecipeUiState.Content.Loading
            is RecipeState.Content.Success -> RecipeUiState.Content.Success(
                recipes = c.recipes.map { recipe ->
                    RecipeUiModel(
                        id = recipe.id,
                        title = recipe.title,
                        caloriesLabel = "${recipe.calories} ккал",
                        priceLabel = "${recipe.priceRubles.toInt()} ₽",
                    )
                }
            )
            is RecipeState.Content.Error -> RecipeUiState.Content.Error(c.message)
        }
        return RecipeUiState(content = content)
    }
}

// ─── ViewModel ─────────────────────────────────────────────────────────────

class RecipeViewModel(
    update: RecipeUpdate,
    commandsFlowHandler: RecipeCommandsFlowHandler,
    private val uiStateMapper: RecipeUiStateMapper,
) : ViewModel() {

    private val store: Store<RecipeState, RecipeEvent.Ui, RecipeNews> =
        KoteaStoreFactory.create(
            initialState = RecipeState(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val state: StateFlow<RecipeUiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<RecipeNews> = store.news

    fun dispatch(event: RecipeEvent.Ui) {
        store.dispatch(event)
    }
}

// ─── Route & Screen ─────────────────────────────────────────────────────────

@Composable
fun RecipeRoute(
    onNavigateToDetail: (RecipeId) -> Unit,
    viewModel: RecipeViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.news.collect { news ->
            when (news) {
                is RecipeNews.NavigateToDetail -> onNavigateToDetail(news.id)
                is RecipeNews.ShowSnackbar -> { /* snackbarHostState.showSnackbar(news.message) */ }
            }
        }
    }

    RecipeScreen(state = state, onEvent = viewModel::dispatch)
}

@Composable
fun RecipeScreen(
    state: RecipeUiState,
    onEvent: (RecipeEvent.Ui) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Box(Modifier.padding(padding)) {
            when (val content = state.content) {
                RecipeUiState.Content.Loading -> Text("Загрузка...")
                is RecipeUiState.Content.Success -> Text("Найдено: ${content.recipes.size}")
                is RecipeUiState.Content.Error -> Text("Ошибка: ${content.message}")
            }
        }
    }
}
