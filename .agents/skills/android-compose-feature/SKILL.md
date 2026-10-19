---
name: android-compose-feature
description: >
  Разработка ViewModel и тонкого UI в androidApp. Используй этот Skill при создании или
  изменении: ViewModel в :shared/presentation или androidApp (MVI: StateFlow + Channel + sealed
  interface Content, разделение *Route/*Screen), Jetpack Compose + Material 3 экранов,
  SyncDiaryWorker (WorkManager + idempotency-key + BackoffPolicy.EXPONENTIAL), авторизации
  OAuth 2.1 PKCE (AppAuth + Android Keystore/Tink). Запрещает бизнес-логику и форматирование
  в @Composable, флаги isLoading/error в ViewState.
---

# Skill: Android Compose Feature (MVI)

## Назначение

Этот Skill регулирует разработку всего, что живёт в `androidApp` и связанных Compose-слоях:
ViewModel, State/Intent/Effect, Route/Screen-разделение, WorkManager и авторизацию.

---

## Workflow

### Шаг 1. Прочитать STATE.md и архитектурный документ

```
1. Прочитать STATE.md — понять текущий статус.
2. Прочитать docs/architecture/03-mobile-stack.md — MVI-эталоны, WorkManager, AppAuth.
3. Убедиться, что UseCase и Repository уже реализованы в :shared (если нет — сначала kmp-shared-feature).
```

### Шаг 2. MVI — создать Intent, ViewState, Effect

#### 2.1 Intent
```kotlin
// androidApp/feature/<name>/<Name>Intent.kt
sealed interface <Name>Intent {
    data class <Action>(val param: ParamType) : <Name>Intent
    data object Retry : <Name>Intent
    // НЕТ else при обработке в ViewModel
}
```

#### 2.2 ViewState — вложенный sealed interface Content (ОБЯЗАТЕЛЬНО)
```kotlin
// androidApp/feature/<name>/<Name>ViewState.kt
data class <Name>ViewState(
    val content: Content = Content.Loading,
    // дополнительные поля (НЕ isLoading, НЕ error: String?)
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val items: List<<Name>UiModel>) : Content
        data class Error(val message: String) : Content
        // добавлять новые варианты здесь — компилятор защитит when-выражения
    }
}

// UiModel — ВСЕ строки уже отформатированы в :shared
data class <Name>UiModel(
    val id: <Name>Id,           // value class
    val title: String,
    val priceLabel: String,     // "350 ₽" — из :shared, НЕ форматировать здесь
    val caloriesLabel: String,  // "320 ккал" — из :shared
)
```

#### 2.3 Effect — разовые события
```kotlin
sealed interface <Name>Effect {
    data class NavigateTo(val id: <Name>Id) : <Name>Effect
    data class ShowSnackbar(val message: String) : <Name>Effect
}
```

### Шаг 3. ViewModel

```kotlin
class <Name>ViewModel(
    private val useCase: Get<Name>UseCase,     // UseCase из :shared/domain
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(<Name>ViewState())
    val state: StateFlow<<Name>ViewState> = _state.asStateFlow()

    // Channel.BUFFERED — события не теряются при временном отсутствии подписчика
    private val _effects = Channel<<Name>Effect>(Channel.BUFFERED)
    val effects: Flow<<Name>Effect> = _effects.receiveAsFlow()

    fun onIntent(intent: <Name>Intent) {
        // НЕТ else в when!
        when (intent) {
            is <Name>Intent.<Action> -> handle<Action>(intent.param)
            <Name>Intent.Retry       -> retry()
        }
    }

    private fun handle<Action>(param: ParamType) {
        _state.update { it.copy(content = <Name>ViewState.Content.Loading) }
        viewModelScope.launch(dispatchers.default) {
            useCase(param)
                .onSuccess { data ->
                    _state.update {
                        it.copy(content = <Name>ViewState.Content.Success(data.toUiModels()))
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            content = <Name>ViewState.Content.Error(
                                error.message ?: "Неизвестная ошибка"
                            )
                        )
                    }
                }
        }
    }
}
```

**Запрещено в ViewModel:**
- `GlobalScope`
- Хардкод диспетчеров (`Dispatchers.IO` напрямую)
- Форматирование строк (`"$price ₽"`)
- Прямые вызовы DAO или DataSource

### Шаг 4. Route (stateful) и Screen (stateless)

#### 4.1 Route — единственный stateful Composable
```kotlin
@Composable
fun <Name>Route(
    navController: NavController,
    viewModel: <Name>ViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            // НЕТ else в when!
            when (effect) {
                is <Name>Effect.NavigateTo ->
                    navController.navigate(/* destination */)
                is <Name>Effect.ShowSnackbar ->
                    /* snackbarHostState.showSnackbar(effect.message) */
            }
        }
    }

    <Name>Screen(
        state = state,
        onIntent = viewModel::onIntent,
    )
}
```

#### 4.2 Screen — чистый stateless Composable
```kotlin
@Composable
fun <Name>Screen(
    state: <Name>ViewState,
    onIntent: (<Name>Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // НЕТ вызовов UseCase
    // НЕТ форматирования строк
    // НЕТ расчётов
    Scaffold(modifier = modifier) { padding ->
        Box(Modifier.padding(padding)) {
            // НЕТ else в when!
            when (val content = state.content) {
                <Name>ViewState.Content.Loading  -> LoadingContent()
                is <Name>ViewState.Content.Success -> SuccessContent(content.items, onIntent)
                is <Name>ViewState.Content.Error   -> ErrorContent(content.message, onIntent)
            }
        }
    }
}
```

**Запрещено в Screen:**
- Любая логика (условия кроме `when (state.content)`)
- `viewModel()` или `koinViewModel()` — только в Route
- Форматирование (`"$x ₽"`, `DateTimeFormatter`, `String.format`)

### Шаг 5. Koin — регистрация ViewModel

```kotlin
// androidApp/di/AppModule.kt
val appModule = module {
    viewModel { <Name>ViewModel(get(), get()) }
    // ... другие ViewModels
}
```

### Шаг 6. SyncDiaryWorker (WorkManager) — только если задача касается дневника

```kotlin
// androidApp/sync/SyncDiaryWorker.kt
class SyncDiaryWorker(
    context: Context,
    params: WorkerParameters,
    private val diaryRepository: DiaryRepository,
    private val diaryGrpcDataSource: DiaryGrpcDataSource,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = diaryRepository.getPendingEntries()
        if (pending.isEmpty()) return Result.success()

        pending.forEach { entry ->
            runCatching {
                val metadata = Metadata().apply {
                    // idempotencyKey — ВСЕГДА из Room, НИКОГДА не генерировать заново
                    put(IDEMPOTENCY_KEY_HEADER, entry.idempotencyKey.raw)
                }
                diaryGrpcDataSource.addEntry(entry, metadata)
                diaryRepository.updateSyncStatus(entry.id, SyncStatus.SYNCED)
            }.onFailure {
                diaryRepository.updateSyncStatus(entry.id, SyncStatus.FAILED)
                if (runAttemptCount < MAX_RETRY_ATTEMPTS) return Result.retry()
            }
        }
        return Result.success()
    }

    companion object {
        val IDEMPOTENCY_KEY_HEADER: Metadata.Key<String> =
            Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
        const val MAX_RETRY_ATTEMPTS = 5
        const val WORK_NAME = "sync_diary_worker"

        // BackoffPolicy.EXPONENTIAL — обязательно
        fun buildPeriodicRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<SyncDiaryWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
    }
}
```

### Шаг 7. Авторизация OAuth 2.1 — только если задача касается auth

- Использовать `AppAuth` (net.openid:appauth).
- Токены хранить через `AuthStateManager` с Tink (`AeadKeysetHandle` + Android Keystore).
- **ЗАПРЕЩЕНО**: `SharedPreferences`, `DataStore<Preferences>`, `EncryptedSharedPreferences`.
- `AuthTokenProvider` — интерфейс из `:shared/domain`, реализация `AppAuthTokenProvider` в `androidMain`.

### Шаг 8. @Preview для Screen

```kotlin
@Preview(showBackground = true, name = "Loading")
@Composable
private fun <Name>ScreenLoadingPreview() {
    BudgetNutritionistTheme {
        <Name>Screen(
            state = <Name>ViewState(content = <Name>ViewState.Content.Loading),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, name = "Success")
@Composable
private fun <Name>ScreenSuccessPreview() {
    BudgetNutritionistTheme {
        <Name>Screen(
            state = <Name>ViewState(
                content = <Name>ViewState.Content.Success(
                    items = listOf(/* preview data */),
                ),
            ),
            onIntent = {},
        )
    }
}
```

### Шаг 9. Проверка и тесты

```bash
./gradlew :app:androidApp:assembleDebug   # Компиляция
./gradlew ktlintCheck                      # Форматирование
./gradlew detekt                           # Статический анализ
```

Написать тест ViewModel с Turbine — см. `.agents/skills/testing-and-verification/SKILL.md §2.3`.

### Шаг 10. Обновить STATE.md

---

## Чеклист перед коммитом

```
[ ] ViewState: вложенный sealed interface Content (нет isLoading/error флагов)
[ ] ViewModel: StateFlow для state, Channel(BUFFERED) для effects
[ ] ViewModel: диспетчеры через конструктор, нет GlobalScope
[ ] ViewModel: when(intent) без else
[ ] Route: только подключение к ViewModel + обработка effects
[ ] Screen: только state + onIntent, нет логики, нет форматирования
[ ] Screen: when(state.content) без else
[ ] UiModel: все строки (цены, калории) уже отформатированы в :shared
[ ] SyncDiaryWorker: idempotencyKey берётся из Room, НЕ генерируется заново
[ ] SyncDiaryWorker: BackoffPolicy.EXPONENTIAL
[ ] Koin: viewModel { } зарегистрирован
[ ] @Preview добавлен для каждого состояния Content
[ ] Тест ViewModel написан (Turbine)
[ ] ./gradlew :app:androidApp:assembleDebug — GREEN
```
