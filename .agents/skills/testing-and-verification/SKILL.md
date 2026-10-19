---
name: testing-and-verification
description: >
  Написание тестов и верификация кода. Используй этот Skill при создании тестов для
  любого слоя: commonTest в :shared (Fakes + Turbine + BundledSQLiteDriver, запрет MockK),
  Compose UI (createComposeRule для stateless *Screen), backend (JUnit 5 + Testcontainers
  для PostgreSQL/Redis/Kafka + WireMock для LLM). Также используй для запуска тест-проверок
  и формирования отчёта о покрытии.
---

# Skill: Testing & Verification

## Назначение

Этот Skill регулирует написание тестов для всех слоёв проекта.
Главное правило: **никаких моков инфраструктуры** — только реальные контейнеры (Testcontainers)
и ручные Fake-реализации (commonTest).

---

## Workflow

### Шаг 1. Прочитать STATE.md

```
1. Прочитать STATE.md — понять, какие компоненты готовы к тестированию.
2. Определить слой: KMP commonTest / Android instrumented / Backend.
3. Проверить наличие Fake-реализаций в commonTest/fake/.
```

---

## 2.1 KMP commonTest — Fake Repositories

### Правило
- MockK, Mockito — **ЗАПРЕЩЕНЫ** в `commonTest`.
- Создавать ручные `Fake*`-реализации для каждого Repository.

### Шаблон FakeRepository
```kotlin
// app/shared/src/commonTest/kotlin/com/budgetntr/shared/fake/FakeRecipeRepository.kt
class FakeRecipeRepository : RecipeRepository {
    // Состояние, которым управляет тест — mutable by design: test control
    val recipes = mutableListOf<Recipe>()
    var shouldThrow: Exception? = null
    var saveCallCount = 0

    override suspend fun getById(id: RecipeId): Recipe {
        shouldThrow?.let { throw it }
        return recipes.firstOrNull { it.id == id }
            ?: error("Recipe $id not found in fake")
    }

    override suspend fun save(recipe: Recipe) {
        shouldThrow?.let { throw it }
        saveCallCount++
        recipes.removeAll { it.id == recipe.id }
        recipes.add(recipe)
    }

    override fun observeAll(): Flow<List<Recipe>> = flow {
        shouldThrow?.let { throw it }
        emit(recipes.toList())
    }

    // Вспомогательные методы для тестов
    fun givenRecipes(vararg items: Recipe) { recipes.addAll(items) }
    fun givenError(error: Exception) { shouldThrow = error }
    fun reset() { recipes.clear(); shouldThrow = null; saveCallCount = 0 }
}
```

### TestCoroutineDispatchers
```kotlin
// app/shared/src/commonTest/kotlin/com/budgetntr/shared/fake/TestCoroutineDispatchers.kt
class TestCoroutineDispatchers(
    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
) : CoroutineDispatchers {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}
```

---

## 2.2 UseCase Tests (commonTest)

```kotlin
// app/shared/src/commonTest/kotlin/com/budgetntr/shared/domain/usecase/GetRecipeUseCaseTest.kt
class GetRecipeUseCaseTest {

    private val fakeRepository = FakeRecipeRepository()
    private val dispatchers = TestCoroutineDispatchers()
    private val useCase = GetRecipeUseCase(fakeRepository, dispatchers)

    @BeforeTest
    fun setUp() = fakeRepository.reset()

    @Test
    fun `invoke returns Success when recipe exists`() = runTest {
        // Arrange
        val expected = Recipe(
            id = RecipeId("recipe-1"),
            name = "Гречка с курицей",
            caloriesPer100g = 185.0,
            pricePer100g = 15.5,
        )
        fakeRepository.givenRecipes(expected)

        // Act
        val result = useCase(RecipeId("recipe-1"))

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrThrow())
    }

    @Test
    fun `invoke returns Failure when repository throws`() = runTest {
        // Arrange
        fakeRepository.givenError(RuntimeException("Network error"))

        // Act
        val result = useCase(RecipeId("nonexistent"))

        // Assert
        assertTrue(result.isFailure)
        assertIs<RuntimeException>(result.exceptionOrNull())
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke calls repository exactly once`() = runTest {
        fakeRepository.givenRecipes(
            Recipe(RecipeId("r1"), "Борщ", 80.0, 25.0)
        )
        useCase(RecipeId("r1"))
        // Проверить через счётчик, если нужно (для более сложных сценариев)
    }
}
```

---

## 2.3 ViewModel Tests — Turbine

```kotlin
// app/androidApp/src/.../feature/recipe/RecipeViewModelTest.kt
class RecipeViewModelTest {

    private val fakeUseCase = FakeSearchRecipesUseCase()
    private val dispatchers = TestCoroutineDispatchers()

    @get:Rule
    val coroutineRule = MainCoroutineRule()  // если нужен Dispatchers.Main

    private val viewModel by lazy {
        RecipeViewModel(fakeUseCase, dispatchers)
    }

    @Test
    fun `initial state is Loading`() = runTest {
        viewModel.state.test {
            val initial = awaitItem()
            assertIs<RecipeViewState.Content.Loading>(initial.content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Search intent transitions Loading then Success`() = runTest {
        fakeUseCase.recipesToReturn = listOf(
            Recipe(RecipeId("1"), "Борщ", 80.0, 25.0),
        )

        viewModel.state.test {
            awaitItem() // initial Loading

            viewModel.onIntent(RecipeIntent.Search("борщ"))

            val loading = awaitItem()
            assertIs<RecipeViewState.Content.Loading>(loading.content)

            val success = awaitItem()
            assertIs<RecipeViewState.Content.Success>(success.content)
            assertEquals(1, (success.content as RecipeViewState.Content.Success).recipes.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Search intent with error transitions to Error state`() = runTest {
        fakeUseCase.errorToThrow = RuntimeException("No network")

        viewModel.state.test {
            awaitItem() // initial
            viewModel.onIntent(RecipeIntent.Search("test"))
            awaitItem() // loading

            val error = awaitItem()
            assertIs<RecipeViewState.Content.Error>(error.content)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SelectRecipe intent emits NavigateToDetail effect`() = runTest {
        viewModel.effects.test {
            viewModel.onIntent(RecipeIntent.SelectRecipe(RecipeId("recipe-42")))

            val effect = awaitItem()
            assertIs<RecipeEffect.NavigateToDetail>(effect)
            assertEquals("recipe-42", (effect as RecipeEffect.NavigateToDetail).recipeId.raw)
        }
    }

    @Test
    fun `Empty results transitions to Empty state`() = runTest {
        fakeUseCase.recipesToReturn = emptyList()

        viewModel.state.test {
            awaitItem()
            viewModel.onIntent(RecipeIntent.Search("ничего"))
            awaitItem() // loading

            val result = awaitItem()
            assertIs<RecipeViewState.Content.Empty>(result.content)

            cancelAndIgnoreRemainingEvents()
        }
    }
}

// FakeSearchRecipesUseCase — без MockK!
class FakeSearchRecipesUseCase : SearchRecipesUseCase(
    repository = FakeRecipeRepository(),
    dispatchers = TestCoroutineDispatchers(),
) {
    var recipesToReturn: List<Recipe> = emptyList()
    var errorToThrow: Exception? = null

    override suspend fun invoke(query: String): Result<List<Recipe>> =
        errorToThrow?.let { Result.failure(it) } ?: Result.success(recipesToReturn)
}
```

---

## 2.4 Room KMP DAO Tests (BundledSQLiteDriver)

```kotlin
// app/shared/src/commonTest/kotlin/com/budgetntr/shared/data/dao/DiaryDaoTest.kt
class DiaryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DiaryDao

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())      // работает на JVM + iOS!
            .setQueryCoroutineContext(UnconfinedTestDispatcher())
            .build()
        dao = db.diaryDao()
    }

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun `insertOrReplace then observePending returns entry with PENDING status`() = runTest {
        val entry = DiaryEntryEntity(
            id = "entry-1",
            idempotencyKey = "idem-key-abc123",
            foodItemId = "apple-001",
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
            assertEquals(1, pending.size)
            assertEquals("idem-key-abc123", pending.first().idempotencyKey)
            assertEquals("PENDING", pending.first().syncStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateSyncStatus changes status to SYNCED`() = runTest {
        val entry = DiaryEntryEntity(
            id = "entry-2",
            idempotencyKey = "idem-key-xyz",
            foodItemId = "banana-001",
            calories = 100,
            proteins = 1.0,
            fats = 0.5,
            carbs = 23.0,
            recordedAt = 1_700_000_000_001L,
            syncStatus = "PENDING",
        )
        dao.insertOrReplace(entry)
        dao.updateSyncStatus("entry-2", "SYNCED")

        dao.observePending().test {
            val pending = awaitItem()
            assertTrue(pending.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insertOrReplace with same id updates existing entry`() = runTest {
        val original = DiaryEntryEntity(
            id = "entry-3", idempotencyKey = "key-1", foodItemId = "food-1",
            calories = 100, proteins = 10.0, fats = 5.0, carbs = 12.0,
            recordedAt = 1_700_000_000_002L, syncStatus = "PENDING",
        )
        val updated = original.copy(syncStatus = "SYNCED")

        dao.insertOrReplace(original)
        dao.insertOrReplace(updated)

        dao.observePending().test {
            val pending = awaitItem()
            assertTrue(pending.isEmpty()) // updated to SYNCED
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

---

## 2.5 Backend Integration Tests — Testcontainers

```kotlin
// server/src/test/kotlin/com/budgetntr/diary/DiaryServiceIntegrationTest.kt
@SpringBootTest
@ExtendWith(SpringExtension::class)
@Testcontainers
class DiaryServiceIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("diary_db_test")
            .withUsername("test")
            .withPassword("test")

        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.0"))

        @Container
        @JvmStatic
        val redis = GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }

    @Autowired lateinit var diaryService: DiaryService
    @Autowired lateinit var outboxRepository: OutboxMessageRepository
    @Autowired lateinit var diaryRepository: DiaryJpaRepository

    @Test
    fun `addEntry saves entry and outbox message in same transaction`() {
        val idempotencyKey = "test-key-${UUID.randomUUID()}"
        val request = AddEntryServiceRequest(
            userId = "user-test-1",
            foodItemId = UUID.randomUUID(),
            quantityGrams = 150.0,
            recordedAt = OffsetDateTime.now(),
            idempotencyKey = idempotencyKey,
        )

        diaryService.addEntry(request)

        // Проверить бизнес-запись
        val entries = diaryRepository.findAll()
        assertThat(entries).hasSize(1)
        assertThat(entries.first().userId).isEqualTo("user-test-1")

        // Проверить Outbox (в той же транзакции)
        val outboxMessages = outboxRepository.findAll()
        assertThat(outboxMessages).hasSize(1)
        assertThat(outboxMessages.first().aggregateType).isEqualTo("DiaryEntry")
        assertThat(outboxMessages.first().status).isEqualTo("PENDING")
    }

    @Test
    fun `addEntry is idempotent — same key returns same result without duplicate`() {
        val idempotencyKey = "idem-key-${UUID.randomUUID()}"
        val request = AddEntryServiceRequest(
            userId = "user-idem-test",
            foodItemId = UUID.randomUUID(),
            quantityGrams = 100.0,
            recordedAt = OffsetDateTime.now(),
            idempotencyKey = idempotencyKey,
        )

        val result1 = diaryService.addEntry(request)
        val result2 = diaryService.addEntry(request)  // повторный запрос

        assertEquals(result1.entryId, result2.entryId)
        // Только одна запись в БД
        assertThat(diaryRepository.findAll()).hasSize(1)
    }
}
```

---

## 2.6 WireMock для LLM/внешних HTTP

```kotlin
// server/src/test/kotlin/com/budgetntr/recipe/RecipeAiServiceTest.kt
@SpringBootTest
class RecipeAiServiceTest {

    @RegisterExtension
    @JvmField
    val wireMock: WireMockExtension = WireMockExtension.newInstance()
        .options(WireMockConfiguration.options().dynamicPort())
        .build()

    @Autowired lateinit var recipeAiService: RecipeAiService

    @DynamicPropertySource
    companion object {
        @JvmStatic
        fun wireMockProperties(registry: DynamicPropertyRegistry) {
            // Устанавливается через companion object — порт доступен позже
        }
    }

    @Test
    fun `circuit breaker activates on repeated LLM failures and returns cached result`() {
        wireMock.stubFor(
            WireMock.post(WireMock.anyUrl())
                .willReturn(WireMock.serverError().withBody("LLM unavailable"))
        )

        // Открытие circuit breaker: N неудачных попыток
        repeat(11) {
            runCatching { recipeAiService.generateRecipes(testProfile) }
        }

        // После открытия circuit — должен вернуть кэшированный результат
        val result = recipeAiService.generateRecipes(testProfile)
        assertThat(result).isNotEmpty()

        // LLM вызывался не более 10 раз (circuit открылся)
        wireMock.verify(
            WireMock.lessThan(12),
            WireMock.postRequestedFor(WireMock.anyUrl()),
        )
    }

    @Test
    fun `returns LLM result when service is available`() {
        wireMock.stubFor(
            WireMock.post(WireMock.anyUrl())
                .willReturn(
                    WireMock.okJson("""{"recipes": [{"name": "Гречка", "calories": 180}]}""")
                )
        )

        val result = recipeAiService.generateRecipes(testProfile)

        assertThat(result).isNotEmpty()
        wireMock.verify(1, WireMock.postRequestedFor(WireMock.anyUrl()))
    }
}
```

---

## 2.7 Compose UI Tests (stateless Screen)

```kotlin
// androidApp/src/androidTest/.../feature/recipe/RecipeScreenTest.kt
class RecipeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `loading state shows progress indicator`() {
        composeTestRule.setContent {
            BudgetNutritionistTheme {
                RecipeScreen(
                    state = RecipeViewState(content = RecipeViewState.Content.Loading),
                    onIntent = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("LoadingIndicator").assertIsDisplayed()
    }

    @Test
    fun `success state shows recipe list`() {
        val recipes = listOf(
            RecipeUiModel(
                id = RecipeId("1"),
                name = "Борщ",
                imageUrl = null,
                caloriesLabel = "320 ккал",
                priceLabel = "85 ₽",
                proteinsLabel = "12 г",
                tags = listOf("суп"),
            )
        )
        composeTestRule.setContent {
            BudgetNutritionistTheme {
                RecipeScreen(
                    state = RecipeViewState(
                        content = RecipeViewState.Content.Success(recipes)
                    ),
                    onIntent = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Борщ").assertIsDisplayed()
        composeTestRule.onNodeWithText("85 ₽").assertIsDisplayed()
    }

    @Test
    fun `retry button click emits Retry intent`() {
        var capturedIntent: RecipeIntent? = null
        composeTestRule.setContent {
            BudgetNutritionistTheme {
                RecipeScreen(
                    state = RecipeViewState(
                        content = RecipeViewState.Content.Error("Нет сети")
                    ),
                    onIntent = { capturedIntent = it },
                )
            }
        }
        composeTestRule.onNodeWithText("Повторить").performClick()
        assertIs<RecipeIntent.Retry>(capturedIntent)
    }
}
```

---

## Запуск всех тестов

```bash
# KMP commonTest (JVM + iOS Simulator)
./gradlew :app:shared:allTests

# Android instrumented (нужен эмулятор/устройство)
./gradlew :app:androidApp:connectedAndroidTest

# Backend (Testcontainers — нужен Docker)
./gradlew :server:test

# Все вместе (без instrumented)
./gradlew :app:shared:allTests :server:test

# Coverage report
./gradlew :app:shared:koverReport
./gradlew :server:jacocoTestReport
```

---

## Чеклист перед коммитом

```
[ ] commonTest: нет MockK / Mockito — только Fakes
[ ] Все FakeRepository реализуют весь интерфейс репозитория
[ ] TestCoroutineDispatchers создан и инжектируется в UseCase
[ ] Turbine: cancelAndIgnoreRemainingEvents() в конце каждого Flow-теста
[ ] BundledSQLiteDriver используется для Room DAO-тестов
[ ] Backend: Testcontainers (PostgreSQL + Redis + Kafka) — нет H2
[ ] Backend: нет Thread.sleep() — runTest + advanceTimeBy()
[ ] WireMock: все внешние HTTP-вызовы перехвачены
[ ] Compose: Screen тестируется в изоляции (без ViewModel)
[ ] Compose: onNodeWithTag используется для структурных элементов
[ ] ./gradlew :app:shared:allTests — GREEN
[ ] ./gradlew :server:test — GREEN
```
