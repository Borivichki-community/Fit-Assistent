# 03 — Mobile Stack

> **Платформы:** Android (основная) · iOS (целевая совместимость через KMP)
> **Стек:** Jetpack Compose · Material 3 · Navigation Compose · Koin · WorkManager · Coil 3 · AppAuth · Android Keystore · Tink · DataStore · kotlinx-datetime · Room KMP 2.8.x

---

## 1. Стек `androidApp`

### 1.1 Зависимости (`libs.versions.toml`, секция versions)
```toml
[versions]
kotlin                   = "2.4.0"
compose-bom              = "2025.05.00"
material3                = "1.3.0"
navigation-compose       = "2.8.0"
koin-android             = "4.0.0"
koin-compose             = "4.0.0"
workmanager              = "2.10.0"
coil                     = "3.0.0"
appauth                  = "0.11.1"
tink                     = "1.15.0"
datastore                = "1.1.0"
room                     = "2.8.0"
kotlinx-datetime         = "0.7.0"
kotlinx-coroutines       = "1.10.0"
grpc-kotlin              = "1.4.1"
protobuf-kotlin          = "4.29.0"
turbine                  = "1.2.0"
coroutines-test          = "1.10.0"

[libraries]
compose-bom              = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui               = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling       = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-material3        = { group = "androidx.compose.material3", name = "material3", version.ref = "material3" }
navigation-compose       = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
koin-android             = { group = "io.insert-koin", name = "koin-android", version.ref = "koin-android" }
koin-androidx-compose    = { group = "io.insert-koin", name = "koin-androidx-compose", version.ref = "koin-compose" }
workmanager-ktx          = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workmanager" }
coil-compose             = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp      = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
appauth                  = { group = "net.openid", name = "appauth", version.ref = "appauth" }
tink-android             = { group = "com.google.crypto.tink", name = "tink-android", version.ref = "tink" }
datastore-preferences    = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
kotlinx-datetime         = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinx-datetime" }
turbine                  = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
coroutines-test          = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines-test" }
```

### 1.2 Структура модуля `androidApp`
```
app/androidApp/src/main/
├── MainActivity.kt                        # единственная Activity, edge-to-edge
├── BudgetNutritionistApp.kt               # Application, KoinApplication
├── navigation/
│   ├── AppNavGraph.kt                     # NavHost верхнего уровня
│   └── Destinations.kt                    # sealed interface Screen
├── feature/
│   ├── recipe/
│   │   ├── RecipeIntent.kt
│   │   ├── RecipeViewState.kt             # вложенный sealed interface Content
│   │   ├── RecipeEffect.kt
│   │   ├── RecipeViewModel.kt
│   │   ├── RecipeRoute.kt                 # stateful
│   │   └── RecipeScreen.kt               # stateless
│   ├── diary/
│   │   ├── DiaryIntent.kt
│   │   ├── DiaryViewState.kt
│   │   ├── DiaryEffect.kt
│   │   ├── DiaryViewModel.kt
│   │   ├── DiaryRoute.kt
│   │   └── DiaryScreen.kt
│   └── profile/
│       ├── ProfileIntent.kt
│       ├── ProfileViewState.kt
│       ├── ProfileViewModel.kt
│       ├── ProfileRoute.kt
│       └── ProfileScreen.kt
│   └── favorites/
│       ├── FavoritesIntent.kt
│       ├── FavoritesViewState.kt
│       ├── FavoritesViewModel.kt
│       ├── FavoritesRoute.kt
│       └── FavoritesScreen.kt
├── di/
│   └── AppModule.kt                       # Koin ViewModels
└── sync/
    ├── SyncDiaryWorker.kt                 # WorkManager Worker дневника
    └── SyncFavoritesWorker.kt             # WorkManager Worker избранного
```

---

## 2. Эталонный код MVI

### 2.1 Intent
```kotlin
// app/androidApp/src/main/feature/recipe/RecipeIntent.kt
package com.budgetntr.android.feature.recipe

sealed interface RecipeIntent {
    data class Search(val query: String) : RecipeIntent
    data class SelectRecipe(val id: RecipeId) : RecipeIntent  // value class!
    data object Retry : RecipeIntent
    data object ClearSearch : RecipeIntent
}
```

### 2.2 ViewState (с вложенным sealed interface Content)
```kotlin
// app/androidApp/src/main/feature/recipe/RecipeViewState.kt
package com.budgetntr.android.feature.recipe

data class RecipeViewState(
    val content: Content = Content.Loading,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
) {
    sealed interface Content {
        /** Первоначальная загрузка или загрузка при смене запроса */
        data object Loading : Content

        /** Данные успешно получены */
        data class Success(
            val recipes: List<RecipeUiModel>,
            val hasMore: Boolean = false,
        ) : Content

        /** Произошла ошибка */
        data class Error(
            val message: String,
            val isNetworkError: Boolean = false,
        ) : Content

        /** Нет результатов по запросу */
        data object Empty : Content
    }
}

data class RecipeUiModel(
    val id: RecipeId,
    val name: String,
    val imageUrl: String?,
    val caloriesLabel: String,       // "320 ккал" — сформировано в :shared
    val priceLabel: String,          // "85 ₽" — сформировано в :shared
    val proteinsLabel: String,       // "24 г белка"
    val tags: List<String>,
)
```

### 2.3 Effect
```kotlin
// app/androidApp/src/main/feature/recipe/RecipeEffect.kt
package com.budgetntr.android.feature.recipe

sealed interface RecipeEffect {
    data class NavigateToDetail(val recipeId: RecipeId) : RecipeEffect
    data class ShowError(val message: String) : RecipeEffect
    data object ScrollToTop : RecipeEffect
}
```

### 2.4 ViewModel
```kotlin
// app/androidApp/src/main/feature/recipe/RecipeViewModel.kt
package com.budgetntr.android.feature.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val searchRecipesUseCase: SearchRecipesUseCase,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeViewState())
    val state: StateFlow<RecipeViewState> = _state.asStateFlow()

    // Channel.BUFFERED — не теряем эффекты, если UI временно не подписан
    private val _effects = Channel<RecipeEffect>(Channel.BUFFERED)
    val effects: Flow<RecipeEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: RecipeIntent) {
        when (intent) {
            is RecipeIntent.Search      -> handleSearch(intent.query)
            is RecipeIntent.SelectRecipe -> handleSelectRecipe(intent.id)
            RecipeIntent.Retry          -> retryLastSearch()
            RecipeIntent.ClearSearch    -> clearSearch()
        }
    }

    private fun handleSearch(query: String) {
        _state.update { it.copy(searchQuery = query, content = RecipeViewState.Content.Loading) }
        viewModelScope.launch(dispatchers.default) {
            searchRecipesUseCase(query)
                .onSuccess { recipes ->
                    val uiModels = recipes.map { it.toUiModel() }
                    _state.update {
                        it.copy(
                            content = if (uiModels.isEmpty())
                                RecipeViewState.Content.Empty
                            else
                                RecipeViewState.Content.Success(uiModels),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            content = RecipeViewState.Content.Error(
                                message = error.message ?: "Неизвестная ошибка",
                                isNetworkError = error is NetworkException,
                            ),
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

    private fun retryLastSearch() = handleSearch(_state.value.searchQuery)

    private fun clearSearch() {
        _state.update { RecipeViewState() }
    }
}
```

### 2.5 RecipeRoute (stateful)
```kotlin
// app/androidApp/src/main/feature/recipe/RecipeRoute.kt
@Composable
fun RecipeRoute(
    navController: NavController,
    viewModel: RecipeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RecipeEffect.NavigateToDetail ->
                    navController.navigate(RecipeDetailDestination(effect.recipeId.raw))
                is RecipeEffect.ShowError ->
                    // показать SnackBar через SnackbarHostState из Scaffold
                    Unit
                RecipeEffect.ScrollToTop ->
                    Unit
            }
        }
    }

    RecipeScreen(
        state = state,
        onIntent = viewModel::onIntent,
    )
}
```

### 2.6 RecipeScreen (stateless)
```kotlin
// app/androidApp/src/main/feature/recipe/RecipeScreen.kt
@Composable
fun RecipeScreen(
    state: RecipeViewState,
    onIntent: (RecipeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            SearchTopBar(
                query = state.searchQuery,
                onQueryChange = { onIntent(RecipeIntent.Search(it)) },
                onClear = { onIntent(RecipeIntent.ClearSearch) },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val content = state.content) {
                RecipeViewState.Content.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is RecipeViewState.Content.Success ->
                    RecipeList(
                        recipes = content.recipes,
                        onRecipeClick = { onIntent(RecipeIntent.SelectRecipe(it)) },
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
```

---

## 3. Offline-first дневник с IdempotencyKey

### 3.1 Схема работы
```
Пользователь вводит приём пищи
            │
            ▼
  AddDiaryEntryUseCase.invoke()
            │
    ┌───────▼────────────────────────────┐
    │  1. idempotencyKey = IdempotencyKey │
    │     .generate()  [ОДИН РАЗ!]        │
    │  2. DiaryEntryEntity(              │
    │       syncStatus = PENDING,        │
    │       idempotencyKey = key,        │
    │     )                              │
    │  3. DiaryDao.insertOrReplace()     │
    └───────┬────────────────────────────┘
            │ Room (in-memory + WAL)
            │
   UI показывает запись немедленно
            │
   SyncScheduler.scheduleSync()
            │
            ▼
  ┌─────────────────────────────────────┐
  │  SyncDiaryWorker (WorkManager)      │
  │  - Читает PENDING из Room           │
  │  - Отправляет gRPC с метаданными:  │
  │    idempotency-key: <key.raw>       │
  │  - BackoffPolicy.EXPONENTIAL        │
  │  - При успехе: syncStatus=SYNCED    │
  │  - При ошибке: syncStatus=FAILED,  │
  │    оставляет ТОТ ЖЕ idempotencyKey  │
  └─────────────────────────────────────┘
```

### 3.2 AddDiaryEntryUseCase
```kotlin
class AddDiaryEntryUseCase(
    private val diaryRepository: DiaryRepository,
    private val syncScheduler: SyncScheduler,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend operator fun invoke(
        foodItemId: FoodItemId,
        quantityGrams: Double,
        recordedAt: Instant,
    ): Result<DiaryEntry> = withContext(dispatchers.io) {
        runCatching {
            val entry = DiaryEntry(
                id = DiaryEntryId(Uuid.random().toString()),
                idempotencyKey = IdempotencyKey.generate(), // генерируется ОДИН раз
                foodItemId = foodItemId,
                quantityGrams = quantityGrams,
                recordedAt = recordedAt,
                syncStatus = SyncStatus.PENDING,
            )
            diaryRepository.save(entry)
            syncScheduler.scheduleSync()
            entry
        }
    }
}
```

### 3.3 SyncDiaryWorker (WorkManager)
```kotlin
// androidMain
class SyncDiaryWorker(
    context: Context,
    params: WorkerParameters,
    private val diaryRepository: DiaryRepository,
    private val diaryGrpcDataSource: DiaryGrpcDataSource,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendingEntries = diaryRepository.getPendingEntries()
        if (pendingEntries.isEmpty()) return Result.success()

        pendingEntries.forEach { entry ->
            runCatching {
                val metadata = Metadata().apply {
                    put(
                        Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER),
                        entry.idempotencyKey.raw, // ВСЕГДА тот же ключ, что при создании
                    )
                }
                diaryGrpcDataSource.addEntry(entry, metadata)
                diaryRepository.updateSyncStatus(entry.id, SyncStatus.SYNCED)
            }.onFailure { ex ->
                diaryRepository.updateSyncStatus(entry.id, SyncStatus.FAILED)
                if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                    return Result.retry() // WorkManager применит BackoffPolicy.EXPONENTIAL
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val MAX_RETRY_ATTEMPTS = 5
        const val WORK_NAME = "sync_diary_worker"

        fun buildRequest(): PeriodicWorkRequest =
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

### 3.4 Оффлайн-синхронизация Избранного (SyncFavoritesWorker)
Принцип аналогичен дневнику:
1. Пользователь нажимает иконку «Сердечко» в каталоге (продукт, рецепт или готовое меню на день).
2. Запись добавляется / удаляется в Room KMP (`FavoriteItemEntity`) со статусом `SyncStatus.PENDING` и уникальным `idempotencyKey = IdempotencyKey(Uuid.random().toString())`. UI переключается мгновенно (0 мс).
3. `SyncFavoritesWorker` (OneTimeWorkRequest с триггером сетевого подключения или PeriodicWorkRequest) вычитывает несинхронизированные элементы и вызывает gRPC-метод `CatalogService.AddFavorite` / `RemoveFavorite` с метаданными `idempotency-key`.
4. После успешного ответа сервера статус в Room обновляется до `SyncStatus.SYNCED`.

```kotlin
// androidMain
class SyncFavoritesWorker(
    context: Context,
    params: WorkerParameters,
    private val favoritesRepository: FavoritesRepository,
    private val catalogGrpcDataSource: CatalogGrpcDataSource,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = favoritesRepository.getPendingSyncItems()
        if (pending.isEmpty()) return Result.success()

        pending.forEach { item ->
            runCatching {
                val metadata = Metadata().apply {
                    put(
                        Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER),
                        item.idempotencyKey.raw,
                    )
                }
                catalogGrpcDataSource.syncFavorite(item, metadata)
                favoritesRepository.markSynced(item.id)
            }.onFailure {
                if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                    return Result.retry()
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val MAX_RETRY_ATTEMPTS = 5
        const val WORK_NAME = "sync_favorites_worker"
    }
}
```

---

## 4. Авторизация OAuth 2.1 + AppAuth + Tink/Keystore

### 4.1 Поток авторизации
```
MainActivity / Auth Screen
        │
        ▼ AppAuth.AuthorizationService
Keycloak Authorization Endpoint
  (Authorization Code + PKCE)
        │
        ▼ Redirect URI
AppAuth.AuthorizationResponse
        │
        ▼ TokenRequest (code exchange)
Keycloak Token Endpoint
        │
        ▼
TokenResponse { access_token, refresh_token }
        │
        ▼ AppAuthTokenProvider.saveTokens()
Android Keystore / Tink AeadKeysetHandle
  → EncryptedFile или зашифрованный DataStore
        │
        ▼
AuthTokenProvider.getAccessToken()  ← gRPC AuthInterceptor
```

### 4.2 AppAuthTokenProvider (реализация интерфейса из :shared/domain)
```kotlin
// androidMain/auth/AppAuthTokenProvider.kt
class AppAuthTokenProvider(
    private val context: Context,
    private val authStateManager: AuthStateManager,
) : AuthTokenProvider {

    override fun getAccessToken(): String? =
        authStateManager.current.accessToken

    override fun isAuthenticated(): Boolean =
        authStateManager.current.isAuthorized

    override suspend fun refreshIfNeeded(): Result<String> = runCatching {
        val authState = authStateManager.current
        if (!authState.needsTokenRefresh) {
            return@runCatching requireNotNull(authState.accessToken) {
                "Access token is null despite authState.isAuthorized"
            }
        }
        suspendCancellableCoroutine { continuation ->
            val authService = AuthorizationService(context)
            authState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
                authService.dispose()
                if (ex != null) {
                    continuation.resumeWithException(ex)
                } else {
                    continuation.resume(requireNotNull(accessToken) { "Token refresh returned null" })
                }
            }
        }
    }
}
```

### 4.3 Хранение AuthState с Tink (зашифрованный файл)
```kotlin
class AuthStateManager(context: Context) {
    private val keysetHandle: KeysetHandle =
        AndroidKeysetManager.Builder()
            .withSharedPref(context, AUTH_KEYSET_NAME, AUTH_PREF_FILE)
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://$AUTH_MASTER_KEY_ALIAS")
            .build()
            .keysetHandle

    private val aead: Aead = keysetHandle.getPrimitive(Aead::class.java)
    private val file = File(context.filesDir, "auth_state.enc")

    var current: AuthState
        get() {
            if (!file.exists()) return AuthState()
            val encrypted = file.readBytes()
            val decrypted = aead.decrypt(encrypted, AUTH_AAD)
            return AuthState.jsonDeserialize(String(decrypted))
        }
        set(value) {
            val json = value.jsonSerializeString().toByteArray()
            val encrypted = aead.encrypt(json, AUTH_AAD)
            file.writeBytes(encrypted)
        }

    companion object {
        private const val AUTH_KEYSET_NAME = "auth_keyset"
        private const val AUTH_PREF_FILE = "auth_prefs"
        private const val AUTH_MASTER_KEY_ALIAS = "budget_nutritionist_auth_master"
        private val AUTH_AAD = "auth_state".toByteArray()
    }
}
```

---

## 5. Таблица миграции компонентов

| Источник | Компонент | Куда переносится | Изменения при миграции |
|---|---|---|---|
| `VKEducation` (XML Views) | `RecipeListFragment` | `RecipeScreen.kt` (Compose) | XML → Composable, RecyclerView → LazyColumn |
| `VKEducation` | `RecipeAdapter` | Удалить | LazyColumn items DSL |
| `VKEducation` | `RecipeViewModel` (LiveData) | `RecipeViewModel` (StateFlow) | `LiveData<T>` → `StateFlow<T>`, добавить `sealed interface Content` |
| `VKEducation` | `RecipeRepository` (Retrofit) | `RecipeRepositoryImpl` (:shared/data) | Retrofit → gRPC DataSource, добавить offline Room |
| `VKEducation` | `SessionManager` (SharedPreferences) | `AuthStateManager` (Tink) | SharedPreferences → Tink+Keystore |
| `ai-calorie-tracker-backend` | `FoodController` (REST) | `food-cv-service` (gRPC) | REST → gRPC proto, добавить Buf |
| `ai-calorie-tracker-backend` | `CalorieService` (бизнес-логика) | `catalog-price-service` | Spring Boot 3 → 4, добавить Outbox |
| `ai-calorie-tracker-backend` | `UserEntity` (JPA) | `profile-service` | перенести сущность, добавить Liquibase миграцию |
| `ai-calorie-tracker-backend` | `RecipeController` | `recipe-ai-service` | добавить Resilience4j + Kafka events |
| Оба проекта | Тесты с H2 | Testcontainers PostgreSQL | удалить H2-зависимость, добавить @Container |
| Оба проекта | `Thread.sleep()` в тестах | `advanceTimeBy()` / `runTest` | рефакторинг coroutine тестов |

---

## 6. Тестирование Mobile

### 6.1 commonTest — FakeRepository + Turbine
```kotlin
// :shared/commonTest/domain/usecase/GetRecipeUseCaseTest.kt
class GetRecipeUseCaseTest {

    private lateinit var fakeRepository: FakeRecipeRepository
    private lateinit var dispatchers: TestCoroutineDispatchers
    private lateinit var useCase: GetRecipeUseCase

    @BeforeTest
    fun setUp() {
        fakeRepository = FakeRecipeRepository()
        dispatchers = TestCoroutineDispatchers()
        useCase = GetRecipeUseCase(fakeRepository, dispatchers)
    }

    @Test
    fun `invoke returns success when recipe exists`() = runTest {
        val recipe = Recipe(
            id = RecipeId("recipe-1"),
            name = "Гречка с курицей",
            caloriesPer100g = 185.0,
            pricePer100g = 15.5,
        )
        fakeRepository.recipes.add(recipe)

        val result = useCase(RecipeId("recipe-1"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().name).isEqualTo("Гречка с курицей")
    }

    @Test
    fun `invoke returns failure when repository throws`() = runTest {
        fakeRepository.shouldThrow = RuntimeException("Network error")

        val result = useCase(RecipeId("not-found"))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(RuntimeException::class)
    }
}
```

### 6.2 ViewModel тест с Turbine
```kotlin
class RecipeViewModelTest {
    private val fakeSearchUseCase = FakeSearchRecipesUseCase()
    private val dispatchers = TestCoroutineDispatchers()
    private lateinit var viewModel: RecipeViewModel

    @BeforeTest
    fun setUp() {
        viewModel = RecipeViewModel(fakeSearchUseCase, dispatchers)
    }

    @Test
    fun `searching emits Loading then Success states`() = runTest {
        val recipes = listOf(
            Recipe(RecipeId("1"), "Борщ", 80.0, 25.0),
        )
        fakeSearchUseCase.recipesToReturn = recipes

        viewModel.state.test {
            // Initial state
            val initial = awaitItem()
            assertThat(initial.content).isInstanceOf(RecipeViewState.Content.Loading::class)

            viewModel.onIntent(RecipeIntent.Search("борщ"))

            val loading = awaitItem()
            assertThat(loading.content).isInstanceOf(RecipeViewState.Content.Loading::class)

            val success = awaitItem()
            assertThat(success.content).isInstanceOf(RecipeViewState.Content.Success::class)
            val successContent = success.content as RecipeViewState.Content.Success
            assertThat(successContent.recipes).hasSize(1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting recipe emits NavigateToDetail effect`() = runTest {
        viewModel.effects.test {
            viewModel.onIntent(RecipeIntent.SelectRecipe(RecipeId("recipe-42")))

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(RecipeEffect.NavigateToDetail::class)
            assertThat((effect as RecipeEffect.NavigateToDetail).recipeId.raw).isEqualTo("recipe-42")
        }
    }
}
```

### 6.3 Room KMP DAO тест (BundledSQLiteDriver)
```kotlin
// :shared/commonTest/data/dao/DiaryDaoTest.kt
class DiaryDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: DiaryDao

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = db.diaryDao()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insertOrReplace and observePending returns pending entries`() = runTest {
        val entry = DiaryEntryEntity(
            id = "entry-1",
            idempotencyKey = "idem-key-1",
            foodItemId = "food-1",
            calories = 250,
            proteins = 20.0,
            fats = 8.0,
            carbs = 30.0,
            recordedAt = 1_700_000_000_000L,
            syncStatus = "PENDING",
        )
        dao.insertOrReplace(entry)

        dao.observePending().test {
            val pending = awaitItem()
            assertThat(pending).hasSize(1)
            assertThat(pending.first().idempotencyKey).isEqualTo("idem-key-1")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

---

## 7. Чек-лист готовности к iOS

### 7.1 Проверки перед добавлением iOS-таргета
- [ ] В `commonMain` нет ни одного импорта `java.*`, `android.*`.
- [ ] Не используется `String.format()` — только `buildString`, интерполяция, Okio.
- [ ] Даты: `kotlinx-datetime` (`Instant`, `LocalDate`), не `java.time.*`.
- [ ] UUID: `kotlin.uuid.Uuid`, не `java.util.UUID`.
- [ ] Нет прямых зависимостей на `Context`, `Resources`, `R.string.*`.
- [ ] `expect/actual` сведены к минимуму, платформенный код за интерфейсами.
- [ ] `GrpcChannelFactory`, `DatabaseBuilder`, `AuthTokenProvider`, `SyncScheduler` — только интерфейсы в `commonMain`.
- [ ] `CoroutineDispatchers` — интерфейс, реализации в `androidMain` / `iosMain`.
- [ ] Room KMP настроен с KSP для iOS-таргетов (`kspIosArm64`, `kspIosSimulatorArm64`).
- [ ] Koin: iOS-модуль `iosModule` создан в `iosMain`.
- [ ] `commonTest` не использует MockK / Mockito — только Fakes.
- [ ] `BundledSQLiteDriver` используется для тестов (работает на обеих платформах).

### 7.2 iOS Module (iosMain — заготовка)
```kotlin
// :shared/iosMain/di/IosModule.kt
val iosModule = module {
    single<CoroutineDispatchers> { IosCoroutineDispatchers() }
    single<GrpcChannelFactory> { GrpcNioChannelFactory() }
    // DatabaseBuilder для iOS через Room KMP
    // AuthTokenProvider — через iOS Keychain
    // SyncScheduler — через BackgroundTasks framework
}
```

### 7.3 Kotlin/Native Memory Management
- В `commonMain` избегать объектов с циклическими ссылками без `@OptIn(FreezingIsDeprecated::class)`.
- Предпочитать `val` и иммутабельные структуры данных.
- `StateFlow` и `Channel` безопасны для Kotlin/Native (коопсрутины поддерживают новый MM).
