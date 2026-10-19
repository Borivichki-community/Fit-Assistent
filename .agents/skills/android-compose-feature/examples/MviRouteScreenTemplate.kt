/**
 * Эталонный MVI-код для фичи "Рецепты".
 * Демонстрирует полный стек: Intent → ViewState → Effect → ViewModel → Route → Screen.
 *
 * ПРАВИЛА (из AGENTS.md):
 * - ViewState: вложенный sealed interface Content, нет isLoading/error флагов
 * - ViewModel: StateFlow + Channel(BUFFERED), диспетчеры через конструктор, нет GlobalScope
 * - Route: stateful (koinViewModel + LaunchedEffect для эффектов)
 * - Screen: stateless (только state + onIntent, нет логики, нет форматирования)
 * - when(sealed): нет ветки else
 * - UiModel: все строки (цены, калории) форматируются в :shared, не здесь
 */

// ─── Intent ───────────────────────────────────────────────────────────────────

package com.budgetntr.android.feature.recipe

import com.budgetntr.shared.domain.model.RecipeId

/** Все намерения пользователя — exhaustive sealed interface. */
sealed interface RecipeIntent {
    /** Поиск рецептов по строке. */
    data class Search(val query: String) : RecipeIntent

    /** Выбор рецепта для просмотра деталей. */
    data class SelectRecipe(val id: RecipeId) : RecipeIntent  // value class, не String!

    /** Очистка поискового запроса. */
    data object ClearSearch : RecipeIntent

    /** Повторная попытка после ошибки. */
    data object Retry : RecipeIntent
}

// ─── ViewState ────────────────────────────────────────────────────────────────

/**
 * Состояние экрана Рецептов.
 *
 * ЗАПРЕЩЕНО добавлять:
 * - isLoading: Boolean (используй Content.Loading)
 * - error: String? (используй Content.Error)
 * - data: List<*>? (используй Content.Success)
 */
data class RecipeViewState(
    val content: Content = Content.Loading,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
) {
    /**
     * Вложенный sealed interface — описывает исчерпывающий набор состояний контента.
     * Компилятор защитит when-выражения при добавлении нового состояния.
     */
    sealed interface Content {
        /** Загрузка данных. Показывать индикатор. */
        data object Loading : Content

        /** Данные получены. Показывать список. */
        data class Success(
            val recipes: List<RecipeUiModel>,
            val hasMore: Boolean = false,
        ) : Content

        /** Произошла ошибка. Показывать сообщение + кнопку Retry. */
        data class Error(
            val message: String,
            val isNetworkError: Boolean = false,
        ) : Content

        /** Нет результатов по запросу. */
        data object Empty : Content
    }
}

/**
 * UI-модель рецепта.
 * ВСЕ строки (caloriesLabel, priceLabel) уже отформатированы в :shared.
 * В Composable никакого String.format, NumberFormat и пр.
 */
data class RecipeUiModel(
    val id: RecipeId,             // value class — не сырой String
    val name: String,
    val imageUrl: String?,
    val caloriesLabel: String,    // "320 ккал" — из :shared
    val priceLabel: String,       // "85 ₽"    — из :shared
    val proteinsLabel: String,    // "24 г белка"
    val tags: List<String>,
)

// ─── Effect ───────────────────────────────────────────────────────────────────

/** Разовые события, не хранящиеся в состоянии. */
sealed interface RecipeEffect {
    data class NavigateToDetail(val recipeId: RecipeId) : RecipeEffect
    data class ShowSnackbar(val message: String) : RecipeEffect
    data object ScrollToTop : RecipeEffect
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetntr.shared.domain.coroutines.CoroutineDispatchers
import com.budgetntr.shared.domain.usecase.recipe.SearchRecipesUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val searchRecipesUseCase: SearchRecipesUseCase,
    private val dispatchers: CoroutineDispatchers,  // инжектируется, не хардкодится
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeViewState())
    /** Публичный StateFlow — UI подписывается через collectAsStateWithLifecycle(). */
    val state: StateFlow<RecipeViewState> = _state.asStateFlow()

    /**
     * Channel.BUFFERED — эффекты не теряются, даже если UI временно не подписан
     * (например, при пересоздании composition).
     * SharedFlow здесь ЗАПРЕЩЁН — риск пропуска события.
     */
    private val _effects = Channel<RecipeEffect>(Channel.BUFFERED)
    val effects: Flow<RecipeEffect> = _effects.receiveAsFlow()

    // Храним последний запрос для Retry
    private var lastQuery: String = ""

    /** Единственная точка входа для всех намерений пользователя. */
    fun onIntent(intent: RecipeIntent) {
        // НЕТ ветки else — exhaustive when для sealed interface
        when (intent) {
            is RecipeIntent.Search       -> handleSearch(intent.query)
            is RecipeIntent.SelectRecipe -> handleSelectRecipe(intent.id)
            RecipeIntent.ClearSearch     -> handleClearSearch()
            RecipeIntent.Retry           -> handleSearch(lastQuery)
        }
    }

    private fun handleSearch(query: String) {
        lastQuery = query
        _state.update { it.copy(searchQuery = query, content = RecipeViewState.Content.Loading) }

        viewModelScope.launch(dispatchers.default) {
            searchRecipesUseCase(query)
                .onSuccess { recipes ->
                    val uiModels = recipes.map { it.toUiModel() }
                    _state.update {
                        it.copy(
                            content = if (uiModels.isEmpty()) {
                                RecipeViewState.Content.Empty
                            } else {
                                RecipeViewState.Content.Success(uiModels)
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            content = RecipeViewState.Content.Error(
                                message = error.message ?: "Неизвестная ошибка",
                                isNetworkError = error is java.net.UnknownHostException,
                            )
                        )
                    }
                }
        }
    }

    private fun handleSelectRecipe(id: RecipeId) {
        viewModelScope.launch {
            _effects.send(RecipeEffect.NavigateToDetail(id))
        }
    }

    private fun handleClearSearch() {
        lastQuery = ""
        _state.update { RecipeViewState() }
    }
}

// Функция-расширение для маппинга domain → UiModel (в presentation/mapper или здесь)
private fun com.budgetntr.shared.domain.model.Recipe.toUiModel(): RecipeUiModel = RecipeUiModel(
    id = id,
    name = name,
    imageUrl = imageUrl,
    caloriesLabel = caloriesLabel,   // поле уже отформатировано в :shared
    priceLabel = priceLabel,         // "85 ₽" — из :shared
    proteinsLabel = proteinsLabel,
    tags = tags,
)

// ─── Route (stateful) ─────────────────────────────────────────────────────────

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful Composable — единственный, кто знает о ViewModel.
 * Подключается к Koin, собирает эффекты, управляет навигацией.
 */
@Composable
fun RecipeRoute(
    navController: NavController,
    viewModel: RecipeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Обработка разовых эффектов — в LaunchedEffect(Unit) или LaunchedEffect(viewModel)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            // НЕТ ветки else — exhaustive when для sealed interface
            when (effect) {
                is RecipeEffect.NavigateToDetail ->
                    navController.navigate("recipe_detail/${effect.recipeId.raw}")

                is RecipeEffect.ShowSnackbar ->
                    // snackbarHostState.showSnackbar(effect.message) — пробросить через LocalSnackbar
                    Unit

                RecipeEffect.ScrollToTop ->
                    // lazyListState.animateScrollToItem(0)
                    Unit
            }
        }
    }

    RecipeScreen(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

// ─── Screen (stateless) ───────────────────────────────────────────────────────

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Stateless Composable — не знает о ViewModel.
 * Тестируется изолированно через createComposeRule().
 *
 * ЗАПРЕЩЕНО здесь:
 * - koinViewModel() или viewModel()
 * - Бизнес-логика, расчёты
 * - String.format(), NumberFormat, форматирование цен/дат
 * - Прямые вызовы UseCase или Repository
 */
@Composable
fun RecipeScreen(
    state: RecipeViewState,
    onIntent: (RecipeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { onIntent(RecipeIntent.Search(it)) },
                onClear = { onIntent(RecipeIntent.ClearSearch) },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            // НЕТ ветки else — если добавится новое Content состояние, компилятор предупредит
            when (val content = state.content) {
                RecipeViewState.Content.Loading ->
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("LoadingIndicator"),
                    )

                is RecipeViewState.Content.Success ->
                    RecipeListContent(
                        recipes = content.recipes,
                        onRecipeClick = { id -> onIntent(RecipeIntent.SelectRecipe(id)) },
                    )

                is RecipeViewState.Content.Error ->
                    ErrorContent(
                        message = content.message,
                        isNetworkError = content.isNetworkError,
                        onRetry = { onIntent(RecipeIntent.Retry) },
                    )

                RecipeViewState.Content.Empty ->
                    EmptyContent(query = state.searchQuery)
            }
        }
    }
}

// ─── Вспомогательные Composable-компоненты ───────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Material 3 SearchBar или TextField
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Найти рецепт...") },
        modifier = modifier.fillMaxWidth(),
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = onClear) { Icon(/* X icon */) } }
        } else null,
    )
}

@Composable
private fun RecipeListContent(
    recipes: List<RecipeUiModel>,
    onRecipeClick: (RecipeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.lazy.LazyColumn(modifier = modifier) {
        items(recipes, key = { it.id.raw }) { recipe ->
            RecipeCard(
                recipe = recipe,
                onClick = { onRecipeClick(recipe.id) },
            )
        }
    }
}

@Composable
private fun RecipeCard(
    recipe: RecipeUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column {
            // Показываем уже отформатированные строки из :shared — НЕ форматируем здесь
            Text(text = recipe.name, style = MaterialTheme.typography.titleMedium)
            Text(text = recipe.caloriesLabel)   // "320 ккал"
            Text(text = recipe.priceLabel)      // "85 ₽"
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    isNetworkError: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = if (isNetworkError) "Нет подключения к сети" else message)
        Button(onClick = onRetry) { Text("Повторить") }
    }
}

@Composable
private fun EmptyContent(
    query: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = if (query.isBlank()) "Введите запрос для поиска" else "Рецепты по запросу «$query» не найдены",
        modifier = modifier,
    )
}

// ─── Preview ─────────────────────────────────────────────────────────────────

import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Loading", showBackground = true)
@Composable
private fun RecipeScreenLoadingPreview() {
    MaterialTheme {
        RecipeScreen(
            state = RecipeViewState(content = RecipeViewState.Content.Loading),
            onIntent = {},
        )
    }
}

@Preview(name = "Success", showBackground = true)
@Composable
private fun RecipeScreenSuccessPreview() {
    MaterialTheme {
        RecipeScreen(
            state = RecipeViewState(
                content = RecipeViewState.Content.Success(
                    recipes = listOf(
                        RecipeUiModel(
                            id = RecipeId("preview-1"),
                            name = "Гречка с курицей",
                            imageUrl = null,
                            caloriesLabel = "185 ккал / 100 г",
                            priceLabel = "45 ₽ / 100 г",
                            proteinsLabel = "18 г белка",
                            tags = listOf("горячее", "белок"),
                        )
                    )
                ),
                searchQuery = "гречка",
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun RecipeScreenErrorPreview() {
    MaterialTheme {
        RecipeScreen(
            state = RecipeViewState(
                content = RecipeViewState.Content.Error(
                    message = "Нет подключения к интернету",
                    isNetworkError = true,
                )
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun RecipeScreenEmptyPreview() {
    MaterialTheme {
        RecipeScreen(
            state = RecipeViewState(
                content = RecipeViewState.Content.Empty,
                searchQuery = "стейк из мраморной говядины",
            ),
            onIntent = {},
        )
    }
}
