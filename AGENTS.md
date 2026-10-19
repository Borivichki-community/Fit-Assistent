# AGENTS.md — «Бюджетный нутрициолог» Agent Rulebook

> **Статус:** Обязательно к прочтению перед любой реализацией.
> Этот файл является единственным источником истины для всех агентов и разработчиков.
> Любое отступление требует явного RFC-решения и правки этого файла.

---

## 0. Context & Skills (читать первым делом)

Перед любым действием агент ОБЯЗАН:

1. Прочитать все три архитектурных документа:
   - `docs/architecture/01-overview-kmp-infra.md` — KMP-структура, Coroutines, Koin, Room, gRPC, инфраструктура, CI/CD.
   - `docs/architecture/02-backend-stack.md` — бэкенд-компоненты, Buf, Kafka/Avro, Resilience4j, OpenTelemetry, тестирование.
   - `docs/architecture/03-mobile-stack.md` — мобильный стек, MVI-эталоны, Offline-first, AppAuth, таблица миграции, iOS-чеклист.

2. Определить, какой Skill из `.agents/skills/` наиболее подходит для задачи, и активировать его.

3. Прочитать роль из `.agents/agents/`:
   - **Реализация фичи** → `.agents/agents/implementer.md`
   - **Ревью и тесты** → `.agents/agents/test-reviewer.md`

---

## 1. Kotlin 2.4.x & Gradle 9.x — жёсткие правила языка

### 1.1 Null Safety
- **ЗАПРЕЩЕНО** использовать оператор `!!` в любом месте кода.
- Разрешённые альтернативы:
  - Safe call `?.`
  - Elvis-оператор `?:`
  - `requireNotNull(value) { "Описание почему null недопустим" }`
  - `checkNotNull(value) { "Описание" }`

### 1.2 `when` exhaustiveness
- **ЗАПРЕЩЕНА** ветка `else` в `when`-выражениях, если субъект является `sealed class`, `sealed interface` или `enum class`.
- Причина: ветка `else` маскирует добавление нового варианта и ломает принцип исчерпывающего анализа.
- Correct:
  ```kotlin
  when (content) {
      is RecipeViewState.Content.Loading -> { /* ... */ }
      is RecipeViewState.Content.Success -> { /* ... */ }
      is RecipeViewState.Content.Error   -> { /* ... */ }
      // НЕТ else — компилятор защитит при добавлении нового состояния
  }
  ```

### 1.3 Typed IDs — Value Classes
- Все идентификаторы сущностей ОБЯЗАНЫ быть обёрнуты в `@JvmInline value class`.
- **ЗАПРЕЩЕНЫ** сырые `String`, `Int`, `Long` в роли ID в сигнатурах функций и DTO.
- Реестр value-классов (пополнять по мере добавления сущностей):

  ```kotlin
  // :shared/domain/model/ids.kt
  @JvmInline value class UserId(val raw: String)
  @JvmInline value class RecipeId(val raw: String)
  @JvmInline value class DiaryEntryId(val raw: String)
  @JvmInline value class FoodItemId(val raw: String)
  @JvmInline value class ProfileId(val raw: String)
  @JvmInline value class IdempotencyKey(val raw: String)
  ```

### 1.4 Immutability
- По умолчанию все переменные `val`.
- `var` допустим только в локальном скоупе алгоритмического кода с коротким временем жизни и с явным комментарием `// mutable by design: <причина>`.
- `MutableList`, `MutableMap` и аналоги возвращать из функций **запрещено** — возвращать `List`, `Map` и т.д.

### 1.5 Coroutines & Dispatchers
- **ЗАПРЕЩЁН** `GlobalScope`.
- Диспетчеры (`CoroutineDispatchers`) инжектируются через конструктор, никогда не хардкодятся:
  ```kotlin
  class GetRecipesUseCase(
      private val repository: RecipeRepository,
      private val dispatchers: CoroutineDispatchers,
  ) {
      suspend operator fun invoke(query: String): Result<List<Recipe>> =
          withContext(dispatchers.io) {
              runCatching { repository.search(query) }
          }
  }
  ```
- Интерфейс диспетчеров:
  ```kotlin
  // :shared/domain/coroutines/CoroutineDispatchers.kt
  interface CoroutineDispatchers {
      val main: CoroutineDispatcher
      val io: CoroutineDispatcher
      val default: CoroutineDispatcher
      val unconfined: CoroutineDispatcher
  }
  ```
- В `commonMain` — expect/actual или Koin-биндинг платформенной реализации.
- В тестах — инжектировать `TestCoroutineDispatchers(StandardTestDispatcher())`.

### 1.6 Версии зависимостей
- ВСЕ версии библиотек хранятся **исключительно** в `gradle/libs.versions.toml` под секцией `[versions]`.
- В `build.gradle.kts` использовать только ссылки `libs.*`, ни одна версия не должна быть захардкожена строкой.
- Пример запрещённого кода: `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")`
- Правильно: `implementation(libs.kotlinx.coroutines.core)`

---

## 2. KMP `:shared` — Clean Architecture & iOS Readiness

### 2.1 Структура слоёв
```
:shared/
├── domain/          # Чистый Kotlin + kotlinx.coroutines — НИКАКИХ внешних зависимостей
│   ├── model/       # Entity-модели (Value Objects, Aggregates)
│   ├── repository/  # Интерфейсы репозиториев (только domain-типы)
│   ├── usecase/     # UseCases с operator fun invoke(): Result<T>
│   └── coroutines/  # CoroutineDispatchers interface
├── data/
│   ├── local/       # Room DAO, Entity, Migrations
│   ├── remote/      # gRPC stubs, Retrofit (если нужно)
│   ├── mapper/      # DTO/Entity → Domain, Domain → DTO/Entity
│   └── repository/  # Реализации репозиториев
└── presentation/    # (необязательно для KMP) SharedViewModel / State
```

### 2.2 Граница слоёв — строгие правила
- `domain` зависит **только** от чистого Kotlin stdlib и `kotlinx.coroutines`.
- **ЗАПРЕЩЕНО** импортировать в `domain`: `androidx.*`, `android.*`, `java.*`, `com.google.protobuf.*`, Room-аннотации.
- Protobuf DTO и Room Entity **никогда** не попадают в `domain`.
  - Правильный поток: `gRPC Response (DTO)` → `mapper` → `domain.model.*` → `domain.repository`.
  - Правильный поток: `domain.model.*` → `mapper` → `Room Entity` → `DAO`.

### 2.3 UseCase — контракт
```kotlin
// :shared/domain/usecase/recipe/GetRecipeUseCase.kt
class GetRecipeUseCase(
    private val repository: RecipeRepository,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend operator fun invoke(id: RecipeId): Result<Recipe> =
        withContext(dispatchers.io) {
            runCatching { repository.getById(id) }
        }
}
```
- Возвращаемый тип всегда `Result<T>`.
- Принцип Fail Fast: `runCatching` оборачивает весь блок.
- **ЗАПРЕЩЕНО** возвращать `null` или использовать кастомные sealed-результаты вместо `Result<T>` без явного обоснования.

### 2.4 commonMain — запрещённые импорты
В любом файле в `commonMain` **ЗАПРЕЩЕНЫ**:
- `import java.*`
- `import android.*`
- `String.format(...)` — использовать `buildString` / `"${}"` / Okio
- `import android.content.Context`
- Обращения к `R.string.*`

### 2.5 Платформозависимый код
- Минимизировать `expect/actual`.
- Платформенный код прятать за интерфейсами, биндинг через Koin:

  | Абстракция | Android | Описание |
  |---|---|---|
  | `GrpcChannelFactory` | `OkHttpChannelFactory` | Создание gRPC-канала |
  | `DatabaseBuilder<T>` | `AndroidDatabaseBuilder` | Room databaseBuilder |
  | `AuthTokenProvider` | `AppAuthTokenProvider` | Получение токена OAuth |
  | `SyncScheduler` | `WorkManagerSyncScheduler` | Планирование синхронизации |

### 2.6 Типы данных для платформонезависимых нужд
- **Даты/время**: `kotlinx-datetime` (`Instant`, `LocalDate`, `LocalDateTime`) — запрет на `java.util.Date`, `java.time.*` в `commonMain`.
- **UUID**: `kotlin.uuid.Uuid` (Kotlin 2.0+) — запрет на `java.util.UUID` в `commonMain`.

---

## 3. Mobile UI (`androidApp`) — MVI & Compose

### 3.1 Тонкий UI — обязательное правило
- В `@Composable` функциях **ЗАПРЕЩЕНЫ**:
  - Вызовы UseCase напрямую.
  - Бизнес-логика, расчёты, форматирование.
  - Любое форматирование цен, дат, единиц (`"350 ₽"`, `"1 200 ккал"`).
- Все строки для отображения **формируются в `:shared`** (например, `FormattedRecipe.priceLabel: String`).

### 3.2 ViewState — запрет флагов
- **ЗАПРЕЩЕНО** использовать `isLoading: Boolean`, `error: String?` как поля верхнего уровня `ViewState`.
- Обязательный паттерн с вложенным `sealed interface Content`:

  ```kotlin
  data class RecipeViewState(
      val content: Content = Content.Loading,
      val query: String = "",
  ) {
      sealed interface Content {
          data object Loading : Content
          data class Success(val recipes: List<RecipeUiModel>) : Content
          data class Error(val message: String) : Content
      }
  }
  ```

### 3.3 Разделение Route / Screen
- **`*Route`** (stateful): подключается к Koin ViewModel, собирает эффекты, управляет навигацией.
- **`*Screen`** (stateless): принимает только `state: ViewState` и `onIntent: (Intent) -> Unit`. Тестируется изолированно.

  ```kotlin
  @Composable
  fun RecipeRoute(navController: NavController) {
      val viewModel: RecipeViewModel = koinViewModel()
      val state by viewModel.state.collectAsStateWithLifecycle()

      LaunchedEffect(Unit) {
          viewModel.effects.collect { effect ->
              when (effect) {
                  is RecipeEffect.NavigateToDetail ->
                      navController.navigate(RecipeDetailRoute(effect.recipeId))
              }
          }
      }

      RecipeScreen(state = state, onIntent = viewModel::onIntent)
  }

  @Composable
  fun RecipeScreen(
      state: RecipeViewState,
      onIntent: (RecipeIntent) -> Unit,
  ) {
      // Чистый декларативный UI
  }
  ```

### 3.4 State & Effects
- Состояние: `StateFlow<ViewState>` → `collectAsStateWithLifecycle()`.
- Разовые эффекты (навигация, snackbar): `Channel<Effect>(Channel.BUFFERED)` + `receiveAsFlow()`.
- **ЗАПРЕЩЕНО** использовать `SharedFlow` для эффектов (риск пропуска события при отсутствии подписчика).

### 3.5 Offline-first дневник — IdempotencyKey
1. При создании записи дневника (до любого сетевого вызова) сгенерировать `idempotencyKey = IdempotencyKey(Uuid.random().toString())`.
2. `idempotencyKey` генерируется **ОДИН РАЗ** и сохраняется в Room вместе с записью.
3. Статус записи при создании: `SyncStatus.PENDING`.
4. `SyncDiaryWorker` (WorkManager) читает `PENDING`-записи и отправляет gRPC-запрос с метаданными `idempotency-key: <key>`.
5. `BackoffPolicy.EXPONENTIAL` с максимальным количеством попыток.
6. После успешной отправки статус меняется на `SyncStatus.SYNCED`.
7. **ЗАПРЕЩЕНО** генерировать новый `idempotencyKey` при retry — всегда использовать сохранённый.

### 3.6 Авторизация OAuth 2.1
- Протокол: Authorization Code + PKCE через библиотеку `AppAuth`.
- Токены хранить **исключительно** в Android Keystore / Tink.
- **ЗАПРЕЩЕНО** хранить токены в обычном `DataStore<Preferences>`, `SharedPreferences`, или `EncryptedSharedPreferences`.
- `AuthTokenProvider` — интерфейс из `:shared/domain`, платформенная реализация через Koin.

---

## 4. Backend (`Spring Boot 4.0.x`) — gRPC, Kafka, PostgreSQL

### 4.1 Транзакции и сетевые вызовы
- **ЗАПРЕЩЕНЫ** любые сетевые вызовы (gRPC, HTTP, Kafka `producer.send()`) внутри методов, аннотированных `@Transactional`.
- Правильный паттерн: транзакция сохраняет запись в `outbox_messages`, Outbox Processor читает и публикует в Kafka вне транзакции.

### 4.2 gRPC & Buf
- Все `.proto` файлы проходят проверку: `buf lint && buf breaking --against 'https://github.com/org/repo.git#branch=main'`.
- Генерация кода: `buf generate`.
- `GRPCRoute` — входная точка через Kubernetes Gateway API + Envoy Gateway.
- JWT валидация через `SecurityPolicy` в Envoy Gateway (не в приложении).
- Rate limiting — `BackendTrafficPolicy` с Redis-хранилищем счётчиков.

### 4.3 Kafka + Avro + Schema Registry
- Режим совместимости схем: `BACKWARD`.
- Паттерн Transactional Outbox:
  1. В одной транзакции: обновить бизнес-таблицу + вставить в `outbox_messages`.
  2. Outbox Processor (отдельный поток/job) читает `outbox_messages`, публикует в Kafka, помечает как `published`.
- **ЗАПРЕЩЕНО** публиковать в Kafka напрямую из `@Transactional` метода.

### 4.4 Идемпотентность
- Все мутирующие gRPC-методы принимают `Idempotency-Key` в метаданных.
- Сервис проверяет ключ в Redis/БД перед выполнением операции.
- Повторный запрос с тем же ключом возвращает закэшированный ответ без повторного выполнения.

### 4.5 Resilience4j
- Внешние вызовы (LLM API, внешние HTTP): `@CircuitBreaker` с `fallbackMethod`, возвращающим кэшированные данные из Redis.
- `@Retry` только для **идемпотентных** операций (GET, вызовы с `Idempotency-Key`).
- **ЗАПРЕЩЕНО** применять `@Retry` к не идемпотентным мутациям без `Idempotency-Key`.

### 4.6 PostgreSQL + Liquibase
- Миграции только через Liquibase (XML или YAML changesets, не SQL в виде raw-файлов без контрольных сумм).
- Обязательные индексы на часто фильтруемые поля (БЖУ, категории, цены).
- JPA-связи по умолчанию `LAZY`.
- N+1 решается через `@EntityGraph` или `JOIN FETCH` в конкретных Query-методах.
- **ЗАПРЕЩЕНО** использовать `FetchType.EAGER`.

### 4.7 Наблюдаемость (Observability)
- `/actuator/health/readiness` и `/actuator/health/liveness` — обязательны для всех сервисов.
- OpenTelemetry: `traceparent` прокидывается во все исходящие gRPC-вызовы и Kafka-сообщения.
- Формат логов: JSON с полями `traceId`, `spanId`, `userId`, `service`, `timestamp`.
- Стек: OpenTelemetry + Micrometer Tracing → Prometheus → Grafana + Loki + Tempo.

### 4.8 Секреты
- **ЗАПРЕЩЕНО** хранить секреты в `application.yml`, `application.properties`, переменных окружения в `docker-compose` без внешнего источника.
- Секреты поставляются исключительно через HashiCorp Vault или Kubernetes Secrets.

---

## 5. Testing — Golden Rules

### 5.1 KMP commonTest — NO MOCKS
- **ЗАПРЕЩЕНЫ** MockK, Mockito, любые mock-библиотеки в `commonTest`.
- **ОБЯЗАТЕЛЬНЫ** ручные `Fake`-реализации репозиториев:
  ```kotlin
  class FakeRecipeRepository : RecipeRepository {
      val recipes = mutableListOf<Recipe>()
      var shouldThrow: Exception? = null

      override suspend fun getById(id: RecipeId): Recipe {
          shouldThrow?.let { throw it }
          return recipes.first { it.id == id }
      }
  }
  ```
- `kotlinx-coroutines-test` (`runTest`, `UnconfinedTestDispatcher`, `StandardTestDispatcher`).
- `Turbine` для тестирования `Flow`.
- In-memory Room KMP с `BundledSQLiteDriver` для DAO-тестов.

### 5.2 Android instrumented tests
- Допустим MockK только для Android-специфичных компонентов (`Context`, `Resources`).
- Compose UI-тесты через `ComposeTestRule`.

### 5.3 Backend tests
- JUnit 5 (`@ExtendWith(SpringExtension::class)` или `@SpringBootTest`).
- `Testcontainers` для PostgreSQL, Redis, Kafka — никаких H2/in-memory.
- `WireMock` для внешних HTTP/LLM вызовов.
- k6 для нагрузочного тестирования gRPC-эндпоинтов.

### 5.4 Общие запреты в тестах
- **ЗАПРЕЩЕНО** обращаться к реальным внешним сервисам.
- **ЗАПРЕЩЕНО** использовать `Thread.sleep()` — использовать `runTest` + `advanceTimeBy()`.
- **ЗАПРЕЩЕНО** тестировать приватные методы напрямую.

---

## 6. Code Review Checklist (для `test-reviewer`)

При ревью любого PR проверить:

- [ ] Нет `!!` операторов.
- [ ] Нет `else` в `when` для sealed/enum.
- [ ] Все ID — value classes.
- [ ] Нет сырых типов как ID (`String`, `Int`, `Long`).
- [ ] `domain` не импортирует ничего кроме stdlib и coroutines.
- [ ] DTO/Entity не в `domain`.
- [ ] UseCase возвращает `Result<T>` через `operator fun invoke`.
- [ ] Нет `GlobalScope`.
- [ ] Диспетчеры инжектируются.
- [ ] Нет версий в `build.gradle.kts` (только `libs.*`).
- [ ] `ViewState` использует вложенный `sealed interface Content`.
- [ ] Нет логики/форматирования в `@Composable`.
- [ ] `idempotencyKey` генерируется один раз и сохраняется в Room.
- [ ] Нет сетевых вызовов внутри `@Transactional`.
- [ ] Kafka-публикация через Outbox, не напрямую.
- [ ] Связи JPA — `LAZY`.
- [ ] Нет `FetchType.EAGER`.
- [ ] Логи в JSON с `traceId`/`spanId`/`userId`.
- [ ] Секреты не в `application.yml`.
- [ ] Тесты KMP — только Fakes, без MockK.
- [ ] Тесты бэкенда — Testcontainers, без H2.

---

## 7. Session State & Checkpointing (`STATE.md`)

### 7.1 Правило чтения состояния
- **ПЕРЕД началом любой новой задачи** агент ОБЯЗАН прочитать корневой файл `STATE.md`.
- `STATE.md` является живым журналом прогресса — он заменяет память между сессиями.
- Если `STATE.md` отсутствует — создать его немедленно по шаблону из §7.3.

### 7.2 Правило обновления состояния
После завершения **каждого** из следующих событий агент ОБЯЗАН обновить `STATE.md`:
- Создана группа файлов (модуль, Skill, набор классов).
- Пройдена проверка (`./gradlew build`, `buf lint`, `ktlintCheck`).
- Обнаружена проблема или принято архитектурное решение.
- Завершён крупный этап (Шаг N из плана).

### 7.3 Формат `STATE.md`
```markdown
## Статус проекта: <краткое описание текущего состояния>
Обновлено: <ISO-дата>

### Выполнено
- [x] Шаг N: <описание>

### В работе
- [-] Шаг N+1: <описание> (текущая задача)

### Очередь
- [ ] Шаг N+2: <описание>
- [ ] Шаг N+3: <описание>

### Известные проблемы
- ⚠️ <описание проблемы и временного решения>

### Принятые решения
- 📌 <дата>: <решение и обоснование>
```

### 7.4 Привязка к Skills
- При начале задачи указать в `STATE.md` активированный Skill.
- При смене Skill — зафиксировать причину.

### 7.5 Защита от потери контекста
- Если контекст чата близок к лимиту, агент ОБЯЗАН:
  1. Записать текущий прогресс в `STATE.md`.
  2. Зафиксировать список незавершённых файлов.
  3. Указать следующую атомарную задачу для следующей сессии.
