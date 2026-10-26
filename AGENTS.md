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

### 0.1 Границы проекта и безопасность файловой системы (Project Boundary Rule)
- **СТРОГИЙ ЗАПРЕТ НА ВЫХОД ИЗ ПРОЕКТА:** Агенту **ЗАПРЕЩЕНО** выходить за пределы корневой папки текущего проекта (`C:\Users\andre\AndroidStudioProjects\FitAssistent2`), читать, искать, сканировать файлы или директории в родительских папках ОС (`C:\Users\andre\...`, `Desktop`, `IdeaProjects`, `Downloads` и т.д.) без явной просьбы пользователя.
- **ОБЯЗАТЕЛЬНОЕ ПОЛУЧЕНИЕ РАЗРЕШЕНИЯ:** Если для выполнения задачи агенту требуется доступ к файлам вне проекта, внешним директориям или внешним сетевым ресурсам / репозиториям, агент **ОБЯЗАН явно запросить разрешение у пользователя** и дождаться подтверждения перед выполнением любого чтения, сканирования или выполнения команд вне проекта.
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

### 2.3 UseCase — контракт и целесообразность (Anti-Boilerplate Rule)
```kotlin
// :shared/domain/usecase/recipe/CalculateDietPlanUseCase.kt
class CalculateDietPlanUseCase(
    private val recipeRepository: RecipeRepository,
    private val profileRepository: ProfileRepository,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend operator fun invoke(userId: UserId): Result<DietPlan> =
        withContext(dispatchers.io) {
            runCatching {
                // Реальная доменная логика: оркестрация нескольких источников и расчёт
                val profile = profileRepository.getProfile(userId)
                val recipes = recipeRepository.getRecommended(profile.nutritionGoal)
                DietPlan(profile = profile, recipes = recipes)
            }
        }
}
```
- Возвращаемый тип всегда `Result<T>`.
- Принцип Fail Fast: `runCatching` оборачивает весь блок.
- **ЗАПРЕЩЕНО** возвращать `null` или использовать кастомные sealed-результаты вместо `Result<T>` без явного обоснования.
- **Критерий необходимости UseCase (Anti-Boilerplate):**
  - UseCase создаётся **ТОЛЬКО** тогда, когда он содержит реальную доменную логику: координацию нескольких репозиториев, валидацию доменных правил, вычисления или применение доменных политик.
  - **ЗАПРЕЩЕНО** создавать однострочные сквозные UseCase-прокси (anemic / pass-through), которые просто перенаправляют вызов в один метод репозитория без дополнительной логики. В таких случаях вызывающий слой (например, `CommandsFlowHandler` в KoTEA) обязан вызывать интерфейс `Repository` напрямую, не плодя бесполезный бойлерплейт.

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

## 3. Mobile UI (`androidApp`) & Presentation (`:shared`) — KoTEA (The Elm Architecture) & Compose

### 3.1 Архитектура KoTEA в `:mobile:shared`
Взаимодействие UI и бизнес-логики строится по паттерну The Elm Architecture с использованием ядра KoTEA (`dev.andre.nutritionist.core.kotea.*`):
- **`Store<State, UiEvent, News>`**: единая точка входа. Предоставляет `state: StateFlow<State>`, `news: Flow<News>` и метод `dispatch(event: UiEvent)`.
- **`State`**: неделимый снимок состояния фичи. Обязателен паттерн со вложенным `sealed interface Content` (`Loading`, `Success`, `Error`). Запрещены флаги `isLoading: Boolean` и `error: String?` на верхнем уровне.
- **`Event`**: все события фичи.
  - `sealed interface Ui : Event` — намерения пользователя (`LoadProfile`, `Retry`, `EditClicked`).
  - `sealed interface Internal : Event` — результаты выполнения асинхронных команд (`ProfileLoaded`, `ProfileLoadFailed`).
- **`Command`**: асинхронные сайд-эффекты (загрузка данных, запись в БД).
- **`News`**: разовые уведомления для UI (навигация, snackbar).
- **`DslUpdate`**: чистый синхронный редьюсер. Внутри `update(event)` **СТРОГО ЗАПРЕЩЕНЫ** корутины, сетевые вызовы и любые сайд-эффекты. Допустимы только функции `state { ... }`, `commands(...)`, `news(...)`. `when(event)` обязан быть исчерпывающим без ветки `else`.
- **`CommandsFlowHandler`**: асинхронный обработчик команд. Слушает поток `commands: Flow<Command>` и возвращает `Flow<Event>`. При отсутствии сложной доменной логики обращается к `Repository` напрямую (без проходных UseCase).
- **`UiStateMapper`**: чистый маппер `fun map(state: State): UiState`. Формирует готовые для отображения строки (`heightLabel = "$heightCm см"`, `priceLabel = "$price ₽"`).

### 3.2 Тонкий Compose UI — обязательное правило
- В `@Composable` функциях **ЗАПРЕЩЕНЫ**:
  - Вызовы UseCase, DAO, репозиториев или сети.
  - Бизнес-логика, расчёты, преобразования типов.
  - Любое форматирование цен, дат, единиц (`"350 ₽"`, `"1 200 ккал"`).
- Все строки для отображения формируются через `UiStateMapper` в `:shared`.

### 3.3 Разделение Route / Screen
- **`*Route`** (stateful): подключается к Koin ViewModel / Store, собирает состояние через `collectAsStateWithLifecycle()`, слушает `news` и управляет навигацией.
- **`*Screen`** (stateless): принимает только `state: UiState` и лямбду `onEvent: (UiEvent) -> Unit`. Тестируется изолированно.

  ```kotlin
  @Composable
  fun RecipeRoute(
      navController: NavController,
      viewModel: RecipeViewModel = koinViewModel(),
  ) {
      val state by viewModel.state.collectAsStateWithLifecycle()

      LaunchedEffect(Unit) {
          viewModel.news.collect { news ->
              when (news) {
                  is RecipeNews.NavigateToDetail ->
                      navController.navigate(RecipeDetailRoute(news.recipeId))
              }
          }
      }

      RecipeScreen(state = state, onEvent = viewModel::dispatch)
  }

  @Composable
  fun RecipeScreen(
      state: RecipeUiState,
      onEvent: (RecipeEvent.Ui) -> Unit,
  ) {
      // Чистый декларативный UI без логики
  }
  ```

### 3.4 State & News в Store
- Состояние: `StateFlow<State>` → `collectAsStateWithLifecycle()`.
- Разовые эффекты: `Channel<News>(Channel.BUFFERED)` + `receiveAsFlow()`.
- **ЗАПРЕЩЕНО** использовать `SharedFlow` для news (риск пропуска события при отсутствии подписчика).

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
- [ ] UseCase возвращает `Result<T>` через `operator fun invoke` (при наличии реальной логики; нет сквозных anemic UseCase).
- [ ] Нет `GlobalScope`.
- [ ] Диспетчеры инжектируются.
- [ ] Нет версий в `build.gradle.kts` (только `libs.*`).
- [ ] `State` / `UiState` использует вложенный `sealed interface Content` (нет флагов isLoading/error).
- [ ] Редьюсер `DslUpdate` чистый и синхронный (сайд-эффекты только в `CommandsFlowHandler`).
- [ ] Строки форматируются через `UiStateMapper` в `:shared`, нет логики/форматирования в `@Composable`.
- [ ] `idempotencyKey` генерируется один раз и сохраняется в Room.
- [ ] Нет сетевых вызовов внутри `@Transactional`.
- [ ] Kafka-публикация через Outbox, не напрямую.
- [ ] Связи JPA — `LAZY`.
- [ ] Нет `FetchType.EAGER`.
- [ ] Логи в JSON с `traceId`/`spanId`/`userId`.
- [ ] Секреты не в `application.yml`.
- [ ] Тесты KMP — только Fakes, без MockK.
- [ ] Тесты бэкенда — Testcontainers, без H2.
- [ ] Соблюдены границы проекта (нет обращений за пределы проекта без явного запроса пользователя).

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
