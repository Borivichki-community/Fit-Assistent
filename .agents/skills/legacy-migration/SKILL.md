---
name: legacy-migration
description: >
  Перенос наработок из существующих проектов. Используй этот Skill при миграции кода из
  VKEducation (замена Retrofit на gRPC, Room на Room KMP 2.8.x, LiveData на StateFlow,
  MockK на Fakes, удаление Context из ViewModel, перевод в MVI), из ai-calorie-tracker-backend
  (перенос в Spring Boot 4 микросервисы, переход на gRPC и OAuth 2.1 PKCE), а также при
  использовании FitAssistentAndroid исключительно как UX-референса без копирования кода.
---

# Skill: Legacy Migration

## Назначение

Этот Skill управляет пошаговым переносом кода из трёх официальных Git-источников:
1. **VKEducation** — Android-проект (XML Views, Retrofit, LiveData, MockK):
   - Git: `https://github.com/Andrsn5/VKEducation`
2. **ai-calorie-tracker-backend** — Spring Boot backend (REST, H2, токены в SharedPreferences):
   - Git: `https://github.com/Win122333/ai-calorie-tracker-backend`
3. **FitAssistentAndroid** — UX-референс (только визуальные паттерны, код не копировать):
   - Git: `https://github.com/Andrsn5/FitAssistentAndroid`

---

## 0. Регламент миграции: только онлайн-исследование (без клонирования)

### 0.1 Соблюдение границ проекта (Project Boundary Rule)
- **СТРОГИЙ ЗАПРЕТ:** Агенту запрещено производить произвольный поиск, сканирование или чтение файлов на диске пользователя вне текущего проекта `FitAssistent2`.

### 0.2 Исследование исходников только онлайн (БЕЗ клонирования на диск)
1. **ЗАПРЕТ НА СКАЧИВАНИЕ И КЛОНИРОВАНИЕ:**
   - **ЗАПРЕЩЕНО** выполнять `git clone`, скачивать zip-архивы или подгружать сторонние репозитории на локальный диск пользователя.
   - Никаких временных папок со сторонним кодом на диске создаваться не должно. Дисковое пространство не засоряется.
2. **ОНЛАЙН-ПРОСМОТР ЧЕРЕЗ GITHUB:**
   - Исследование legacy-кода выполняется **исключительно онлайн** через GitHub URL / GitHub API / `raw.githubusercontent.com`.
   - Агент читает только те конкретные файлы, которые необходимы для задачи (например, `ApiService.kt`, DTO или мапперы), переходя по онлайн-ссылкам репозитория.
   - Пользователь может указать прямую ссылку на файл на GitHub (или ветку), либо агент находит путь к файлу через онлайн-структуру репозитория.
3. **Чистая реализация с нуля:**
   - Код не копируется «как есть». Изучив сигнатуры и поля онлайн, агент сразу пишет чистый, идиоматичный код под целевую архитектуру (KMP `:shared`, Compose MVI, Spring Boot 4 gRPC) непосредственно в модулях `FitAssistent2`.

---

## Workflow

### Шаг 1. Прочитать STATE.md и таблицу миграции

```
1. Прочитать STATE.md — понять, что уже перенесено.
2. Прочитать docs/architecture/03-mobile-stack.md §5 — таблица миграции.
3. Определить: что мигрирует (компонент) и куда (целевое место).
```

### Шаг 2. Миграция из VKEducation

#### 2.1 Карта миграции Android → KMP/Compose

| Источник (VKEducation) | Целевое место | Ключевые изменения |
|---|---|---|
| `RecipeListFragment.kt` | `androidApp/feature/recipe/RecipeScreen.kt` | XML → Composable, RecyclerView → LazyColumn |
| `RecipeAdapter.kt` | Удалить | Заменить на items { } в LazyColumn |
| `RecipeViewModel.kt` (LiveData) | `androidApp/feature/recipe/RecipeViewModel.kt` | LiveData → StateFlow, добавить sealed interface Content |
| `RecipeRepository.kt` (Retrofit) | `:shared/data/repository/RecipeRepositoryImpl.kt` | Retrofit → gRPC DataSource + Room offline |
| `SessionManager.kt` (SharedPreferences) | `androidApp/auth/AuthStateManager.kt` | SharedPreferences → Tink + Android Keystore |
| `MockK-тесты` | `commonTest/fake/Fake*.kt` | MockK → ручные Fake-реализации |
| `Event<T>` (SingleLiveEvent) | `Channel<Effect>(Channel.BUFFERED)` | LiveData Event → Channel + receiveAsFlow() |

#### 2.2 Чеклист миграции ViewModel

```
Исходный код (VKEducation):
    class RecipeViewModel : ViewModel() {
        val recipes = MutableLiveData<List<Recipe>>()
        val isLoading = MutableLiveData<Boolean>()
        val error = MutableLiveData<String?>()
        fun loadRecipes() { ... }
    }

Целевой код:
    class RecipeViewModel(
        private val searchUseCase: SearchRecipesUseCase,  // ← инжектируется, не создаётся
        private val dispatchers: CoroutineDispatchers,    // ← инжектируется
    ) : ViewModel() {
        private val _state = MutableStateFlow(RecipeViewState())
        val state: StateFlow<RecipeViewState> = _state.asStateFlow()
        private val _effects = Channel<RecipeEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()
        // sealed interface Content вместо isLoading/error
    }
```

Шаги:
- [ ] Убрать `Context` из ViewModel (Context → Koin androidContext() в AndroidModule)
- [ ] Заменить `LiveData` на `StateFlow`
- [ ] Убрать флаги `isLoading`/`error` → добавить `sealed interface Content`
- [ ] Заменить `Event<T>` / `SingleLiveEvent` на `Channel(BUFFERED)`
- [ ] Убрать прямые вызовы Retrofit → использовать UseCase из :shared
- [ ] Убрать `viewModelScope.launch { ... }` без диспетчера → `launch(dispatchers.default)`

#### 2.3 Чеклист миграции Fragment → Compose

```
[ ] Fragment удалить
[ ] Layout XML удалить (res/layout/*.xml для этого экрана)
[ ] Создать *Screen.kt (stateless Composable)
[ ] Создать *Route.kt (stateful Composable с koinViewModel())
[ ] RecyclerView + Adapter → LazyColumn с items {}
[ ] ClickListener → onIntent callback
[ ] ItemDecoration → пропуски через Modifier.padding()
[ ] ProgressBar → CircularProgressIndicator в when (state.content) { Loading }
[ ] TextView error → ErrorContent Composable в when (state.content) { Error }
[ ] Picasso/Glide → Coil 3 (coil-compose: AsyncImage)
```

#### 2.4 Чеклист миграции Retrofit → gRPC

```
[ ] Retrofit interface → proto/service.proto (RPC-метод)
[ ] Retrofit DTO → proto Message type
[ ] RetrofitDataSource → GrpcDataSource (suspend + coroutines)
[ ] OkHttpClient → ManagedChannel (OkHttpGrpcChannelFactory в androidMain)
[ ] Перехватчик токена → AuthInterceptor (ClientInterceptor)
[ ] Вызов репозитория с .execute() → suspend fun + runCatching
```

#### 2.5 Миграция Room → Room KMP 2.8.x

```
[ ] Зависимость: room-runtime → Room KMP 2.8.x (через libs.versions.toml)
[ ] @Database перенести в commonMain (нет android.*)
[ ] Добавить exportSchema = true
[ ] Добавить schemaDirectory в build.gradle.kts: room { schemaDirectory(...) }
[ ] DAO: suspend fun совместимы, Flow<T> совместим
[ ] DatabaseBuilder перенести в androidMain через AndroidDatabaseBuilder
[ ] Тесты: in-memory Driver → BundledSQLiteDriver (работает в commonTest)
[ ] KSP: add("kspAndroid", ...) + add("kspIosArm64", ...) в build.gradle.kts
```

#### 2.6 Миграция тестов MockK → Fakes

```
Исходный тест (VKEducation):
    val mockRepo = mockk<RecipeRepository>()
    every { mockRepo.getById(any()) } returns recipe
    coEvery { mockRepo.search(any()) } returns listOf(recipe)

Целевой тест:
    val fakeRepo = FakeRecipeRepository()
    fakeRepo.givenRecipes(recipe)
    // Тест выглядит как реальный сценарий, не как mock-спецификация
```

Шаги:
- [ ] Удалить зависимость `mockk` из `commonTest` блока в `build.gradle.kts`
- [ ] Создать `FakeRecipeRepository.kt` в `commonTest/fake/`
- [ ] Создать `FakeDiaryRepository.kt`
- [ ] Создать `FakeFoodCatalogRepository.kt`
- [ ] Переписать тесты: `coEvery { }` → `fakeRepo.givenRecipes()`
- [ ] Проверить, что `runTest` используется вместо `runBlocking`

---

### Шаг 3. Миграция из ai-calorie-tracker-backend

#### 3.1 Карта миграции Backend → Spring Boot 4 микросервисы

| Источник (ai-calorie-tracker) | Целевой сервис | Ключевые изменения |
|---|---|---|
| `UserEntity.kt` (JPA) | `profile-service` | Liquibase миграция + LAZY |
| `UserController.kt` (REST) | `profile-service` gRPC impl | REST → gRPC proto, убрать JWT из кода |
| `CalorieService.kt` | `catalog-price-service` | Добавить Outbox, убрать прямой Kafka |
| `FoodController.kt` | `food-cv-service` | REST → gRPC, Resilience4j |
| `RecipeController.kt` | `recipe-ai-service` | CircuitBreaker, Outbox events |
| `Тесты с H2` | Testcontainers PostgreSQL | Удалить H2, добавить @Container |
| `SharedPreferences токены` | Vault / K8s Secrets | application.yml → внешний источник |

#### 3.2 Чеклист миграции REST → gRPC

```
[ ] Создать .proto файл с RPC-методами (см. contracts-proto-and-avro)
[ ] Запустить buf generate proto/
[ ] Создать @GrpcService impl
[ ] Удалить @RestController
[ ] JWT валидация → убрать из кода → SecurityPolicy в Envoy Gateway
[ ] Rate limiting → убрать из кода → BackendTrafficPolicy в Envoy Gateway
[ ] Проверить idempotency-key через IdempotencyServerInterceptor
```

#### 3.3 Чеклист безопасного переноса JPA Entity

```
[ ] Перенести Entity в целевой сервис
[ ] Проверить FetchType — все связи LAZY (нет EAGER)
[ ] Создать Liquibase changeset для таблицы (не полагаться на ddl-auto=create)
[ ] Добавить индексы через отдельный changeset
[ ] Заменить em.flush() / em.clear() на явные @Transactional границы
[ ] Добавить @EntityGraph или JOIN FETCH там, где нужна связь
```

#### 3.4 Чеклист миграции тестов H2 → Testcontainers

```
[ ] Удалить h2-зависимость из build.gradle.kts
[ ] Удалить spring.datasource.url=jdbc:h2:mem из test application.yml
[ ] Добавить testcontainers BOM в libs.versions.toml
[ ] Создать companion object с @Container PostgreSQLContainer
[ ] Добавить @DynamicPropertySource для spring.datasource.url
[ ] Проверить, что flyway/liquibase применяется к тестовой БД
[ ] Запустить ./gradlew :server:test с запущенным Docker
```

---

### Шаг 4. FitAssistentAndroid — только UX-референс

**ВАЖНО:** Из `FitAssistentAndroid` НЕ копируется код. Только визуальные паттерны.

#### 4.1 Что брать из FitAssistentAndroid

```
✅ Разрешено (UX-паттерны):
  - Структура навигации (какие экраны, как они связаны)
  - Визуальные компоненты (карточки рецептов, шкалы БЖУ, колёсико калорий)
  - Цветовая схема и типографика
  - Анимации и переходы между экранами

❌ Запрещено (технический код):
  - Прямое копирование классов
  - Копирование логики форматирования
  - Копирование тестов (особенно с MockK)
  - Копирование build.gradle.kts без адаптации под libs.versions.toml
```

---

### Шаг 5. Порядок миграции (рекомендуемый)

1. **Сначала контракты** (proto файлы) — см. `contracts-proto-and-avro`.
2. **Затем domain** (модели, репозитории, UseCases в :shared) — см. `kmp-shared-feature`.
3. **Затем data** (Room Entity, DAO, gRPC DataSource, mapper) — см. `kmp-shared-feature`.
4. **Затем backend** (Spring сервисы, Liquibase, Outbox) — см. `spring-grpc-service`.
5. **Затем UI** (ViewModel, Compose экраны) — см. `android-compose-feature`.
6. **Наконец тесты** — см. `testing-and-verification`.

---

### Шаг 6. Обновить STATE.md

После каждого перенесённого компонента обновить STATE.md:
- Отметить перенесённые файлы из таблицы миграции.
- Записать оставшиеся компоненты.
- Отметить известные несовместимости.

---

## Чеклист финальной проверки миграции

```
[ ] Нет импортов из VKEducation / ai-calorie-tracker пакетов
[ ] Нет LiveData в androidApp (только StateFlow)
[ ] Нет isLoading/error флагов в ViewState
[ ] Нет Context в ViewModel
[ ] Нет MockK/Mockito в commonTest
[ ] Нет H2 в backend тестах
[ ] Нет токенов в application.yml
[ ] Нет FetchType.EAGER в JPA
[ ] Нет прямого Kafka.send() в @Transactional
[ ] Нет String.format() в commonMain
[ ] Все Retrofit DTO заменены на proto Messages
[ ] Все SharedPreferences токены заменены на Tink + Keystore
[ ] ./gradlew :app:shared:allTests — GREEN
[ ] ./gradlew :server:test — GREEN
[ ] ./gradlew :app:androidApp:assembleDebug — GREEN
```
