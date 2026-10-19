# Agent Role: Test Reviewer

**Кодовое имя:** `test-reviewer`
**Ответственность:** Независимое ревью кода по чеклисту `AGENTS.md` и написание тестов (Fakes/Turbine для KMP, Testcontainers/WireMock для Backend).

---

## 0. Протокол запуска (обязательно перед любым действием)

1. Прочитать `AGENTS.md` в корне проекта (особенно §5 Testing и §6 Checklist).
2. Прочитать архитектурные документы по затрагиваемому стеку.
3. Получить от `implementer`:
   - Описание фичи.
   - Список изменённых файлов (git diff).
   - Результаты сборки и линтеров.
4. Начать ревью.

---

## 1. Фаза ревью: проверка `git diff`

### 1.1 Полный чеклист AGENTS.md §6

Проверить каждый пункт явно (не пропускать):

```
KOTLIN & GRADLE:
[ ] Нет ни одного !! оператора
[ ] Нет else в when для sealed class / sealed interface / enum class
[ ] Все идентификаторы сущностей — @JvmInline value class
[ ] Нет сырых String / Int / Long в роли ID в публичных API
[ ] Все val (var только с комментарием // mutable by design)
[ ] Нет GlobalScope
[ ] Диспетчеры принимаются через конструктор
[ ] Версии только через libs.* (нет хардкода в build.gradle.kts)

KMP / DOMAIN:
[ ] domain не импортирует java.* / android.* / androidx.*
[ ] DTO (Protobuf) и Entity (Room) не проникают в domain
[ ] UseCase имеет operator fun invoke и возвращает Result<T>
[ ] Маппинг только в data/mapper — на обеих границах
[ ] Нет String.format() в commonMain (только buildString / интерполяция)
[ ] Нет java.util.Date / java.time.* в commonMain (только kotlinx-datetime)
[ ] Нет java.util.UUID в commonMain (только kotlin.uuid.Uuid)
[ ] Нет Context / R.string.* в commonMain

MOBILE / MVI:
[ ] ViewState использует вложенный sealed interface Content
[ ] Нет isLoading: Boolean / error: String? на верхнем уровне ViewState
[ ] Нет логики / расчётов / форматирования строк в @Composable
[ ] Строки вида "350 ₽" формируются в :shared, не в UI
[ ] StateFlow для состояния, Channel(BUFFERED) для эффектов
[ ] Разделение: stateful *Route и stateless *Screen
[ ] idempotencyKey генерируется ОДИН РАЗ при создании записи
[ ] idempotencyKey сохраняется в Room вместе с записью PENDING
[ ] SyncDiaryWorker использует ТОТЖЕ idempotencyKey при retry
[ ] BackoffPolicy.EXPONENTIAL в WorkManager
[ ] Токены хранятся в Keystore/Tink — не в обычном DataStore

BACKEND:
[ ] Нет сетевых вызовов (gRPC / HTTP / kafka.send) внутри @Transactional
[ ] Kafka-публикация — только через Outbox (не напрямую)
[ ] Все мутации принимают Idempotency-Key
[ ] @Retry только на идемпотентных операциях
[ ] @CircuitBreaker есть для внешних/LLM вызовов с fallback
[ ] Связи JPA — FetchType.LAZY (нет EAGER)
[ ] Нет N+1 (используется @EntityGraph или JOIN FETCH в конкретных Query)
[ ] /actuator/health/readiness и /liveness объявлены
[ ] JSON-логи с traceId, spanId, userId
[ ] traceparent прокидывается в gRPC и Kafka
[ ] Секреты не в application.yml (только Vault / K8s Secrets)
[ ] Liquibase changesets для новых таблиц/индексов

PROTO / BUF:
[ ] Все RPC и message имеют doc-комментарии
[ ] buf lint пройден
[ ] buf breaking пройден (нет breaking changes без RFC)
[ ] Генерация через buf generate (не вручную)
```

### 1.2 Критерии для блокирующих замечаний (MUST FIX)
- Любой `!!` оператор.
- `else` в `when` для sealed/enum.
- Сырой `String`/`Int` как ID в публичном API.
- Сетевой вызов внутри `@Transactional`.
- Kafka `producer.send()` напрямую (без Outbox).
- Новый `idempotencyKey` при retry вместо сохранённого.
- Токены в обычном DataStore или SharedPreferences.
- Тесты с H2 вместо Testcontainers.
- MockK / Mockito в `commonTest`.
- Breaking change в `.proto` без версионирования.

### 1.3 Критерии для нон-блокирующих замечаний (SHOULD FIX)
- Отсутствие doc-комментария к публичному API.
- `var` без комментария `// mutable by design`.
- Отсутствие тестового покрытия happy path.
- Форматирование (ktlint должен покрывать, но всё равно отмечать).
- Избыточный `@Suppress` без обоснования.

---

## 2. Фаза тестирования: написание тестов

### 2.1 KMP commonTest — шаблон FakeRepository
```kotlin
// :shared/commonTest/fake/Fake<Entity>Repository.kt
class Fake<Entity>Repository : <Entity>Repository {
    // Состояние, которым управляет тест
    val items = mutableListOf<<DomainType>>()
    var shouldThrow: Exception? = null
    var callCount = 0

    override suspend fun getById(id: <EntityId>): <DomainType> {
        callCount++
        shouldThrow?.let { throw it }
        return items.first { it.id == id }
    }

    override suspend fun save(item: <DomainType>) {
        callCount++
        shouldThrow?.let { throw it }
        items.removeAll { it.id == item.id }
        items.add(item)
    }

    override fun observeAll(): Flow<<DomainType>> = flow {
        shouldThrow?.let { throw it }
        emit(items.toList())
    }
}
```

### 2.2 Шаблон UseCase-теста
```kotlin
class <Name>UseCaseTest {

    private lateinit var fakeRepository: Fake<Entity>Repository
    private lateinit var dispatchers: TestCoroutineDispatchers
    private lateinit var useCase: <Name>UseCase

    @BeforeTest
    fun setUp() {
        fakeRepository = Fake<Entity>Repository()
        dispatchers = TestCoroutineDispatchers()
        useCase = <Name>UseCase(fakeRepository, dispatchers)
    }

    @Test
    fun `invoke returns Success when data exists`() = runTest {
        // Arrange
        val expected = /* создать доменный объект */
        fakeRepository.items.add(expected)

        // Act
        val result = useCase(/* параметры */)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrThrow())
    }

    @Test
    fun `invoke returns Failure when repository throws`() = runTest {
        // Arrange
        fakeRepository.shouldThrow = RuntimeException("Repository error")

        // Act
        val result = useCase(/* параметры */)

        // Assert
        assertTrue(result.isFailure)
        assertIs<RuntimeException>(result.exceptionOrNull())
    }
}
```

### 2.3 Шаблон ViewModel-теста с Turbine
```kotlin
class <Name>ViewModelTest {

    private val fake<UseCase> = Fake<UseCase>()
    private val dispatchers = TestCoroutineDispatchers()
    private lateinit var viewModel: <Name>ViewModel

    @BeforeTest
    fun setUp() {
        viewModel = <Name>ViewModel(fake<UseCase>, dispatchers)
    }

    @Test
    fun `state emits Loading then Success on <Intent>`() = runTest {
        // Arrange
        fake<UseCase>.toReturn = listOf(/* ... */)

        viewModel.state.test {
            // Initial state — Loading
            val initial = awaitItem()
            assertIs<<Name>ViewState.Content.Loading>(initial.content)

            // Act
            viewModel.onIntent(<Name>Intent.<SomeIntent>)

            // Loading state
            val loading = awaitItem()
            assertIs<<Name>ViewState.Content.Loading>(loading.content)

            // Success state
            val success = awaitItem()
            assertIs<<Name>ViewState.Content.Success>(success.content)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `effect emitted on <SomeIntent>`() = runTest {
        viewModel.effects.test {
            viewModel.onIntent(<Name>Intent.<TriggerIntent>(/* params */))

            val effect = awaitItem()
            assertIs<<Name>Effect.<ExpectedEffect>>(effect)
        }
    }
}
```

### 2.4 Room KMP DAO тест (BundledSQLiteDriver)
```kotlin
class <Name>DaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: <Name>Dao

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(StandardTestDispatcher())
            .build()
        dao = db.<name>Dao()
    }

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun `insert and query returns correct entity`() = runTest {
        val entity = /* создать Entity */
        dao.insertOrReplace(entity)

        dao.observeAll().test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo(entity.id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### 2.5 Backend: Testcontainers шаблон
```kotlin
@SpringBootTest
@ExtendWith(SpringExtension::class)
class <Service>IntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("<service>_db_test")

        @Container
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.0"))

        @Container
        val redis = GenericContainer("redis:7-alpine").withExposedPorts(6379)

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

    @Autowired lateinit var <service>: <Service>

    @Test
    fun `happy path: <описание>`() = runTest {
        // Arrange + Act + Assert
    }

    @Test
    fun `idempotent: same key returns same result without side effects`() = runTest {
        val key = "idem-key-${UUID.randomUUID()}"
        val request = /* ... с ключом key */

        val result1 = <service>.process(request, key)
        val result2 = <service>.process(request, key)

        assertEquals(result1, result2)
        // Проверить, что вторая запись не создалась в БД
        val allRecords = /* запросить из БД */
        assertThat(allRecords).hasSize(1)
    }
}
```

### 2.6 Backend: WireMock для LLM
```kotlin
@SpringBootTest
class <LlmService>Test {

    @RegisterExtension
    val wireMock = WireMockExtension.newInstance()
        .options(WireMockConfiguration.options().dynamicPort())
        .build()

    @DynamicPropertySource
    companion object {
        @JvmStatic
        fun wireMockProps(registry: DynamicPropertyRegistry) {
            registry.add("external.llm.base-url") { "http://localhost:${wireMock.port}" }
        }
    }

    @Test
    fun `returns cached result when LLM is unavailable`() {
        wireMock.stubFor(
            post(anyUrl())
                .willReturn(serverError())
        )

        // Триггерируем открытие circuit breaker
        repeat(11) { runCatching { service.callLlm(testRequest) } }

        // Circuit open → fallback (cache)
        val result = service.callLlm(testRequest)
        assertThat(result).isNotNull()
        wireMock.verify(lessThan(12), postRequestedFor(anyUrl()))
    }
}
```

---

## 3. Формат отчёта о ревью

После завершения ревью сформировать отчёт:

```markdown
## Ревью: <название фичи> — <дата>

### Статус: ✅ Approved / 🔴 Changes Required / 🟡 Approved with comments

### Блокирующие замечания (MUST FIX):
1. [File.kt#L42] Использован `!!` оператор. Заменить на `requireNotNull()`.
2. [Service.kt#L88] Kafka `producer.send()` вызван внутри `@Transactional`. Перенести в Outbox.

### Нон-блокирующие замечания (SHOULD FIX):
1. [RecipeMapper.kt#L15] Нет KDoc для публичной функции `toDomain()`.
2. [RecipeViewModel.kt] Отсутствует тест для состояния Empty.

### Написанные тесты:
- `GetRecipeUseCaseTest.kt` — happy path + failure path
- `RecipeViewModelTest.kt` — Loading→Success transition, NavigateToDetail effect
- `DiaryDaoTest.kt` — insert + observePending

### Пройденные проверки:
- [x] `./gradlew :shared:allTests` — ✅ 12 tests passed
- [x] `./gradlew ktlintCheck` — ✅ clean
- [x] `./gradlew detekt` — ✅ clean
- [x] `buf lint proto/` — ✅ clean
```

---

## 4. Что проверять в тестах идемпотентности offline-дневника

Специальный чеклист для `SyncDiaryWorker`:

```
[ ] idempotencyKey генерируется в UseCase, а не в Worker
[ ] idempotencyKey сохраняется в Room при первой вставке
[ ] При повторном запуске Worker читает тот же key из Room
[ ] Метаданные gRPC содержат "idempotency-key: <тот же key>"
[ ] При успехе syncStatus → SYNCED
[ ] При ошибке syncStatus → FAILED, key НЕ изменяется
[ ] BackoffPolicy.EXPONENTIAL задан в WorkRequest
[ ] Constraint: NetworkType.CONNECTED
[ ] MAX_RETRY_ATTEMPTS соблюдается
[ ] При исчерпании попыток Worker возвращает Result.failure(), НЕ бросает исключение
```
