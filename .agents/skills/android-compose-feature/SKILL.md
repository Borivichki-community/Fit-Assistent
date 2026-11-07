---
name: android-compose-feature
description: >
  Разработка Presentation (KoTEA: Store, State, UiEvent/InternalEvent, Command, News, DslUpdate,
  CommandsFlowHandler, UiStateMapper) и тонкого UI в androidApp (Compose Material 3: разделение
  *Route/*Screen), SyncDiaryWorker (WorkManager + idempotency-key + BackoffPolicy.EXPONENTIAL),
  авторизации OAuth 2.1 PKCE (AppAuth + Android Keystore/Tink). Запрещает бизнес-логику и
  форматирование в @Composable, флаги isLoading/error в State/UiState, побочные эффекты в DslUpdate.
---

# Skill: Android Compose Feature (KoTEA & Compose)

## Назначение

Этот Skill регулирует разработку презентационного слоя и UI:
архитектура The Elm Architecture на базе ядра KoTEA (`dev.andre.nutritionist.core.kotea.*`),
чистые редьюсеры `DslUpdate`, обработчики команд `CommandsFlowHandler`, мапперы `UiStateMapper`,
разбиение `*Route` / `*Screen` в Jetpack Compose, WorkManager и авторизацию OAuth 2.1.

---

## Workflow

### Шаг 1. Прочитать STATE.md и архитектурный документ

```
1. Прочитать STATE.md — понять текущий статус.
2. Прочитать docs/architecture/03-mobile-stack.md и AGENTS.md §3.
3. Убедиться, что Repository уже реализован в :shared (при отсутствии сложной доменной логики UseCase не создаётся).
```

### Шаг 2. KoTEA Контракты — State, Event, Command, News

#### 2.1 State (с вложенным `sealed interface Content`)
```kotlin
data class <Name>State(
    val content: Content = Content.Loading,
    val currentId: <Name>Id? = null,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val item: <Name>) : Content
        data class Error(val message: String) : Content
    }
}
```

#### 2.2 Event — разделение на Ui и Internal
```kotlin
sealed interface <Name>Event {
    sealed interface Ui : <Name>Event {
        data class Load(val id: <Name>Id) : Ui
        data object Retry : Ui
        data object ActionClicked : Ui
    }
    sealed interface Internal : <Name>Event {
        data class Loaded(val data: <Name>) : Internal
        data class Failed(val error: Throwable) : Internal
    }
}
```

#### 2.3 Command — асинхронные команды
```kotlin
sealed interface <Name>Command {
    data class Load(val id: <Name>Id) : <Name>Command
}
```

#### 2.4 News — разовые UI-эффекты (навигация, snackbar)
```kotlin
sealed interface <Name>News {
    data class NavigateToDetails(val id: <Name>Id) : <Name>News
    data class ShowSnackbar(val message: String) : <Name>News
}
```

### Шаг 3. Чистый редьюсер DslUpdate

```kotlin
class <Name>Update : DslUpdate<<Name>State, <Name>Event, <Name>Command, <Name>News>() {
    override fun NextBuilder<<Name>State, <Name>Command, <Name>News>.update(event: <Name>Event) {
        // НЕТ else в when!
        when (event) {
            is <Name>Event.Ui.Load -> {
                state { copy(content = <Name>State.Content.Loading, currentId = event.id) }
                commands(<Name>Command.Load(event.id))
            }
            <Name>Event.Ui.Retry -> {
                val id = state.currentId
                if (id != null) {
                    state { copy(content = <Name>State.Content.Loading) }
                    commands(<Name>Command.Load(id))
                }
            }
            <Name>Event.Ui.ActionClicked -> {
                val id = state.currentId
                if (id != null) {
                    news(<Name>News.NavigateToDetails(id))
                }
            }
            is <Name>Event.Internal.Loaded -> {
                state { copy(content = <Name>State.Content.Success(event.data)) }
            }
            is <Name>Event.Internal.Failed -> {
                state { copy(content = <Name>State.Content.Error(event.error.message ?: "Ошибка")) }
            }
        }
    }
}
```

**Строгие правила для DslUpdate:**
- Полная синхронность и детерминированность.
- **СТРОГО ЗАПРЕЩЕНЫ**: корутины (`launch`), сетевые запросы, обращение к БД, `Dispatchers`, `System.currentTimeMillis()`.

### Шаг 4. Асинхронный обработчик команд CommandsFlowHandler

```kotlin
class <Name>CommandsFlowHandler(
    private val repository: <Name>Repository, // при отсутствии доменной логики вызываем репозиторий напрямую
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<<Name>Command, <Name>Event> {

    override fun handle(commands: Flow<<Name>Command>): Flow<<Name>Event> = flow {
        commands.collect { command ->
            when (command) {
                is <Name>Command.Load -> {
                    val event = withContext(dispatchers.io) {
                        runCatching { repository.getById(command.id) }
                            .fold(
                                onSuccess = { <Name>Event.Internal.Loaded(it) },
                                onFailure = { <Name>Event.Internal.Failed(it) },
                            )
                    }
                    emit(event)
                }
            }
        }
    }
}
```

### Шаг 5. UiState и UiStateMapper

```kotlin
data class <Name>UiState(
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val model: <Name>UiModel) : Content
        data class Error(val message: String) : Content
    }
}

class <Name>UiStateMapper : UiStateMapper<<Name>State, <Name>UiState> {
    override fun map(state: <Name>State): <Name>UiState {
        val content = when (val c = state.content) {
            <Name>State.Content.Loading -> <Name>UiState.Content.Loading
            is <Name>State.Content.Success -> <Name>UiState.Content.Success(c.item.toUiModel())
            is <Name>State.Content.Error -> <Name>UiState.Content.Error(c.message)
        }
        return <Name>UiState(content = content)
    }
}
```

### Шаг 6. ViewModel и интеграция с Store

```kotlin
class <Name>ViewModel(
    update: <Name>Update,
    commandsFlowHandler: <Name>CommandsFlowHandler,
    private val uiStateMapper: <Name>UiStateMapper,
) : ViewModel() {

    private val store: Store<<Name>State, <Name>Event.Ui, <Name>News> =
        KoteaStoreFactory.create(
            initialState = <Name>State(),
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val state: StateFlow<<Name>UiState> =
        store.state
            .map(uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = uiStateMapper.map(store.state.value),
            )

    val news: Flow<<Name>News> = store.news

    fun dispatch(event: <Name>Event.Ui) {
        store.dispatch(event)
    }
}
```

### Шаг 7. Compose UI: Route и Screen

```kotlin
@Composable
fun <Name>Route(
    navController: NavController,
    viewModel: <Name>ViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.news.collect { news ->
            when (news) {
                is <Name>News.NavigateToDetails ->
                    navController.navigate(/* details */)
                is <Name>News.ShowSnackbar ->
                    /* show snackbar */
            }
        }
    }

    <Name>Screen(
        state = state,
        onEvent = viewModel::dispatch,
    )
}

@Composable
fun <Name>Screen(
    state: <Name>UiState,
    onEvent: (<Name>Event.Ui) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Box(Modifier.padding(padding)) {
            when (val content = state.content) {
                <Name>UiState.Content.Loading -> LoadingView()
                is <Name>UiState.Content.Success -> SuccessView(content.model, onEvent)
                is <Name>UiState.Content.Error -> ErrorView(content.message, onEvent)
            }
        }
    }
}
```

### Шаг 8. SyncDiaryWorker и Auth (при необходимости)
- `SyncDiaryWorker`: WorkManager с `BackoffPolicy.EXPONENTIAL`, `idempotencyKey` строго из Room.
- `Auth`: AppAuth + Android Keystore/Tink. Запрет на SharedPreferences/DataStore для токенов.

### Шаг 9. Material 3 Theme & Design Tokens
- Палитра определяется через `darkColorScheme` и `lightColorScheme` в `Theme.kt` с использованием констант из `Color.kt`.
- Цвета в UI-компонентах берутся строго из `MaterialTheme.colorScheme.*` (`primary`, `surfaceVariant`, `errorContainer`, `tertiaryContainer` и т.д.). Запрещён хардкод `Color(0xFF...)` внутри экранов.
- Отступы и размеры структурируются через токены (например, `Spacing.extraSmall`, `Spacing.small`, `Spacing.medium`, `Spacing.large`, `Spacing.extraLarge`) для устранения «магических чисел».

### Шаг 10. Общие UI-компоненты (CommonComponents)
- `LoadingContent(modifier)`: центрированный `CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)`.
- `ErrorContent(message, onRetry, modifier)`: M3 `Card` с `errorContainer`, сообщением об ошибке и кнопкой повтора действия.
- `OfflineBanner(modifier, message, subtitle)`: закрепленная плашка M3 `Surface` с цветом `tertiaryContainer` или `errorContainer`, информирующая пользователя об использовании локального кэша Room при недоступности сети.

### Шаг 11. Каркас навигации (AppNavHost)
- Дестинации объявляются как `sealed class AppDestination(val route: String)` с типизированными аргументами.
- `AppNavHost` принимает `startDestination: Any` и изолирует переходы между экранами.
- Экраны `*Route` инкапсулируют обработку навигационных коллбэков (`onNavigateBack`, `onNavigateToDetails`), передавая Typed ID (`UserId`, `RecipeId`) без раскрытия логики графа навигации внутри `*Screen`.

### Шаг 12. Двуязычная локализация (RU / EN)
- Все статические строки выносятся в XML-ресурсы:
  - `mobile/androidApp/src/main/res/values/strings.xml` — базовый язык (English).
  - `mobile/androidApp/src/main/res/values-ru/strings.xml` — русская локализация (Russian).
- Приложение автоматически переключает язык в зависимости от системных настроек пользователя. В Compose используется `stringResource(R.string.<id>)`.

### Шаг 13. Правило «Один файл — одна функция / класс» и переиспользование
- **Строгое правило структуры:** на один Kotlin-файл приходится ровно один класс/объект или одна корневая Composable-функция.
- **Вынос переиспользуемой логики:**
  - `FitTopBar.kt` — единая шапка (FitApp + меню) для всех экранов;
  - `MetricCard.kt` — переиспользуемая карточка метрик (профиль, статистика, калории);
  - `FitBottomNavigationBar.kt` и `BottomNavItem.kt` — нижняя навигационная панель;
  - `Spacing.kt` — константы отступов и скруглений.
- Разделение экранов: `*Route.kt` (stateful), `*Screen.kt` (stateless), подкомпоненты контента (`*SuccessContent.kt`, `CalendarCard.kt`, `PlanChip.kt`) выносятся в отдельные файлы.

---

## Чеклист перед коммитом

```
[ ] На один файл — строго одна функция или один класс (Single Responsibility File Rule)
[ ] Переиспользуемые компоненты (FitTopBar, MetricCard, BottomBar) вынесены в ui/components/
[ ] State / UiState: вложенный sealed interface Content (нет isLoading/error флагов)
[ ] DslUpdate: полностью синхронный, без корутин, без сайд-эффектов
[ ] DslUpdate: when(event) без else
[ ] CommandsFlowHandler: все сайд-эффекты инкапсулированы здесь, диспетчеры инжектируются
[ ] UiStateMapper: все строки для UI формируются в :shared
[ ] Route: только stateful подключение к Store/ViewModel + collect news
[ ] Screen: чистый stateless UI (state + onEvent), нет вызовов репозиториев/форматирования
[ ] Anti-Boilerplate: нет проходных UseCase без доменной логики
[ ] Material 3 Tokens: цвета только из MaterialTheme.colorScheme, отступы через Spacing
[ ] CommonComponents: LoadingContent, ErrorContent, OfflineBanner переиспользуются
[ ] Localization: строки вынесены в strings.xml (values и values-ru)
[ ] Unit-тесты: Update протестирован синхронно, CommandsFlowHandler протестирован с Turbine
[ ] ./gradlew :mobile:shared:allTests — GREEN
[ ] ./gradlew :mobile:androidApp:testDebugUnitTest — GREEN
[ ] ./gradlew :mobile:androidApp:assembleDebug — GREEN
```
