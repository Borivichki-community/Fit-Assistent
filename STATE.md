# STATE.md — Журнал прогресса «Бюджетный нутрициолог»

> Этот файл читается агентом **перед началом любой задачи**.
> Обновляется после каждого завершённого этапа.

Обновлено: 2026-11-07T18:15

---

## Статус проекта: T-036 (SSOT ProfileRepositoryImpl) завершена, тесты починены (53/53 GREEN), лицензия Apache 2.0 для KoTEA оформлена

---

### Выполнено

- [x] **Шаг 1:** Создание `AGENTS.md` (корневой rulebook) — 361 строка, все правила Kotlin/KMP/Mobile/Backend/Testing.
- [x] **Шаг 1:** Создание `docs/architecture/01-overview-kmp-infra.md` — структура `:shared`, Koin, Room KMP, gRPC, Docker Compose, CI/CD.
- [x] **Шаг 1:** Создание `docs/architecture/02-backend-stack.md` — Envoy Gateway, Buf, Kafka/Avro, Outbox, Resilience4j, OpenTelemetry.
- [x] **Шаг 1:** Создание `docs/architecture/03-mobile-stack.md` — MVI-эталоны, Offline-first, AppAuth/Tink, таблица миграции, iOS-чеклист.
- [x] **Шаг 1:** Создание `.agents/agents/implementer.md` — роль разработчика.
- [x] **Шаг 1:** Создание `.agents/agents/test-reviewer.md` — роль ревьюера + шаблоны тестов.
- [x] **Шаг 2 (А):** Добавление раздела `## 7. Session State & Checkpointing` в `AGENTS.md`.
- [x] **Шаг 2 (А):** Создание `STATE.md` (этот файл).
- [x] **Шаг 2 (Б):** Создание `.agents/skills/kmp-shared-feature/SKILL.md` + `examples/OfflineSyncPattern.kt`.
- [x] **Шаг 2 (Б):** Создание `.agents/skills/android-compose-feature/SKILL.md` + `examples/MviRouteScreenTemplate.kt`.
- [x] **Шаг 2 (Б):** Создание `.agents/skills/spring-grpc-service/SKILL.md` + `examples/OutboxAndResilience.kt`.
- [x] **Шаг 2 (Б):** Создание `.agents/skills/contracts-proto-and-avro/SKILL.md`.
- [x] **Шаг 2 (Б):** Создание `.agents/skills/testing-and-verification/SKILL.md`.
- [x] **Шаг 2 (Б):** Создание `.agents/skills/legacy-migration/SKILL.md`.
- [x] **Шаг 2 (Б):** Создание `.agents/skills/diagnose-build-failure/SKILL.md`.
- [x] **Шаг 3:** Перестройка модулей в Clean Architecture структуру и создание инфраструктуры:
  - Папка `backend/` с микросервисами (`profile-service`, `catalog-price-service`, `diary-service`, `recipe-ai-service`, `food-cv-service`);
  - Мобильные модули в `mobile/` (`androidApp`, `shared`);
  - Корневая папка `proto/` с `buf.yaml`, `buf.gen.yaml` и подпапками контрактов (`nutritionist/profile/v1`, `nutritionist/catalog/v1`, `nutritionist/diary/v1`, `nutritionist/recipe/v1`);
  - `infrastructure/docker-compose.local.yml` (PostgreSQL 17 с БД `profile_db`, `catalog_db`, `diary_db`, `keycloak_db`, Redis 7, Kafka 3.9, Schema Registry 7.7, Keycloak 26.0) и `infrastructure/init-db.sql`;
  - Структура папок `infrastructure/keycloak/`, `infrastructure/docker/` (`Dockerfile.service`, `Dockerfile.gateway`), `infrastructure/k8s/charts/` (`api-gateway`, `helmfile.yaml`);
  - Папка `.github/workflows/` (`build.yml`, `deploy.yml`).
- [x] **Локальная верификация и запуск:**
  - `infrastructure/docker-compose.local.yml`: удалён устаревший атрибут `version: "3.9"`, все 5 контейнеров успешно запущены и функционируют (`postgres` [healthy], `redis` [healthy], `kafka`, `schema-registry`, `keycloak`);
  - `mobile/androidApp`: добавлены необходимые Compose-зависимости (`compose.runtime`, `compose.foundation`, `compose.material3`, `compose.ui`), синхронизирован Gradle;
  - Сборка и тесты: `:mobile:shared:assemble`, `:mobile:androidApp:assembleDebug`, `:mobile:shared:allTests`, `:mobile:androidApp:testDebugUnitTest` — ВСЕ ПРОЙДЕНЫ УСПЕШНО (GREEN);
  - Добавлена интерактивная мини-фича на `MainScreen` (кнопка добавления приёма пищи со счётчиком) для проверки полного пайплайна CI/CD.
- [x] **Шаг 4 (Profile Remote):** Реализация gRPC-клиента Profile в KMP shared:
  - Контракт `proto/nutritionist/profile/v1/profile.proto`;
  - Domain слой в `commonMain`: `Profile`, `Gender`, `ActivityLevel`, `GoalType`, `NutritionGoal`, `AuthTokenProvider`, `TraceContextHolder`, `ProfileRemoteDataSource`;
  - Interceptors в `androidMain`: `AuthClientInterceptor` (OAuth 2.1 токен), `TraceparentClientInterceptor` (W3C traceparent);
  - Фабрика канала и клиент: `GrpcClientFactory`, `ProfileGrpcRemoteDataSource`, `ProfileGrpcServiceClient`;
  - Mapper DTO -> Domain: `ProfileDtoMapper` (без `!!` и с исчерпывающим `when`);
  - Koin DI модуль: `androidRemoteDataModule`;
  - Добавлены зависимости gRPC в `libs.versions.toml` и `mobile/shared/build.gradle.kts`;
  - Buf lint верификация контрактов `proto/`: исправлены doc-комментарии, удален `option kotlin_package`, обновлен `proto/buf.yaml` (STANDARD, исключения настроены под структуру `proto/nutritionist`), `buf lint proto` проходит чисто (exit code 0).
  - Покрыто 15 Unit-тестами без моков (`commonTest` + `androidHostTest`: Fakes, coroutines `runTest`, DTO mapper, interceptors);
  - Сборка и тесты: `:mobile:shared:allTests` (15/15 GREEN), `:mobile:androidApp:testDebugUnitTest` (3/3 GREEN).

- [x] **Шаг 4 (KoTEA Integration & Mobile Architecture Update):** Интеграция The Elm Architecture (KoTEA) в KMP `:mobile:shared`:
  - Создана чистая KMP-реализация ядра KoTEA в `dev.andre.nutritionist.core.kotea.*` (`Store`, `Next`, `Update`, `DslUpdate`, `CommandsFlowHandler`, `UiStateMapper`, `StoreImpl`, `KoteaStoreFactory`) на чистом Kotlin + `kotlinx.coroutines` под лицензией Apache 2.0;
  - Покрыта unit-тестами `StoreTest.kt` (State update, Commands dispatching to internal events, News emission via Turbine);
  - Обновлён `AGENTS.md` (§2.3 Anti-Boilerplate UseCase rule, §3 Mobile UI / KoTEA architecture, §6 Code review checklist);
  - Обновлён `.agents/skills/android-compose-feature/SKILL.md` и шаблон `MviRouteScreenTemplate.kt`;
  - Все тесты `:mobile:shared:allTests` (20/20 GREEN) и `:mobile:androidApp:testDebugUnitTest` (3/3 GREEN) пройдены успешно.

- [x] **Шаг 4 (Profile Feature on KoTEA):** Полная реализация фичи Profile на базе KoTEA:
  - Domain & Data: `ProfileRepository` и `ProfileRepositoryImpl` (делегирование в `ProfileRemoteDataSource`, `getProfile` и `updateProfile`);
  - Presentation (KoTEA): `ProfileState`, `ProfileEvent` (`Ui`, `Internal`), `ProfileCommand`, `ProfileNews`, `ProfileUpdate` (`DslUpdate`), `ProfileCommandsFlowHandler`, `ProfileUiStateMapper` (`ProfileUiState`, `ProfileUiModel`), `ProfileViewModel`;
  - Koin DI: регистрация компонентов в `Koin.kt` (`dataModule`, `presentationModule`);
  - Unit-тесты: `ProfileRepositoryImplTest`, `ProfileUpdateTest`, `ProfileCommandsFlowHandlerTest` (Turbine), `ProfileUiStateMapperTest`;
  - Верификация сборки: `:mobile:shared:allTests` (34/34 GREEN), `:mobile:androidApp:testDebugUnitTest` (3/3 GREEN), `:mobile:shared:assemble`, `:mobile:androidApp:assembleDebug` — ВСЕ ПРОЙДЕНЫ УСПЕШНО (BUILD SUCCESSFUL).
- [x] **Шаг 4 (Docker Images Update):** Обновлены и подтянуты все актуальные Docker-образы инфраструктуры и сборщика:
  - `postgres:17-alpine`, `redis:7-alpine`, `apache/kafka:4.2.2`, `confluentinc/cp-schema-registry:8.2.4`, `quay.io/keycloak/keycloak:26.0` (все с sha256);
  - Базовые образы JDK: `azul/zulu-openjdk:21`, `azul/zulu-openjdk:21-jre`.
- [x] **Шаг 4 (T-038 / KAN-56): Android UI M3-тема (Antigravity Dark), декомпозиция файлов 1-файл-1-функция/класс, полная локализация RU/EN без хардкода:**
  - Проведен полный аудит и устранение любого хардкода строк в Kotlin-коде: все кнопки, заголовки, секции, лейблы, fallback-значения и contentDescription вынесены в `res/values/strings.xml` (английский язык) и `res/values-ru/strings.xml` (русский язык);
  - Проведена строгая декомпозиция: каждый Kotlin-файл содержит строго **одну функцию или один класс/объект**;
  - Вся переиспользуемая логика вынесена в `ui/components/`:
    - `FitTopBar.kt`: переиспользуемая верхняя плашка «FitApp» + бургер-меню (локализовано);
    - `MetricCard.kt`: переиспользуемая карточка метрик параметров (вес, активность, цель, бюджет);
    - `FitBottomNavigationBar.kt` и `BottomNavItem.kt`: переиспользуемая плавающая нижняя панель с 4 вкладками (все подписи и contentDescription локализованы);
    - `Spacing.kt`: объект токенов отступов и скруглений (24.dp, 32.dp);
    - `FitIcons.kt`: независимый набор векторной графики;
  - Декомпозированы компоненты состояния: `LoadingContent.kt`, `ErrorContent.kt`, `OfflineBanner.kt`, `FullScreenLoading.kt`;
  - Декомпозированы экраны: `ProfileRoute.kt`, `ProfileScreen.kt`, `ProfileSuccessContent.kt`, `SettingsActionRow.kt`, `MainScreen.kt`, `CalendarCard.kt`, `CircleArrowButton.kt`, `DateCircleItem.kt`, `PlanChip.kt`, `AddPlanButton.kt`;
  - Тесты (`CommonComponentsTest`, `MainScreenTest`) переведены на получение строк из ресурсов через `ApplicationProvider`, что гарантирует прохождение в любой системной локали;
  - Обновлён `.agents/skills/android-compose-feature/SKILL.md` (Шаги 12 и 13, чеклист);
  - Верификация сборки и тестов: `./gradlew :mobile:androidApp:assembleDebug :mobile:androidApp:testDebugUnitTest :mobile:shared:allTests` (59/59 GREEN, BUILD SUCCESSFUL).
- [x] **Шаг 4 (T-036 / KAN-54): ProfileRepositoryImpl — Room Single Source of Truth (SSOT), KoTEA Flow, Apache 2.0:**
  - Реализован паттерн SSOT в `ProfileRepository` (`observeProfile`, `getProfile`, `refreshProfile`, `updateProfile`);
  - `ProfileRepositoryImpl` (`dao`, `remoteDataSource`, `dispatchers`): подписка UI только на локальную БД Room через `observeProfile`, неблокирующий фоновый `refreshProfile`, синхронизация кэша при мутации `updateProfile`;
  - Адаптация `ProfileCommandsFlowHandler`: реактивный `merge(observeFlow, refreshFlow)` на `dispatchers.io`, неблокирующий фоновый сетевой refresh без мерцания UI, безопасная обработка отмены `CancellationException` без ошибочной эмиссии `ProfileLoadFailed`;
  - Исправление и актуализация тестов: `ProfileCommandsFlowHandlerTest`, `ProfileViewModelTest` (синхронизация с контрактом `ProfileRepository`, отсутствие падений на `CancellationException`), `ProfileRepositoryImplTest` (Fakes + Turbine + in-memory state);
  - Юридическое оформление лицензии Apache License 2.0: созданы `LICENSE` и `NOTICE` в корне проекта с атрибуцией Tinkoff Bank (KoTEA: Kotlin The Elm Architecture);
  - Верификация сборки и тестов: `./gradlew :mobile:shared:allTests` (53/53 GREEN).

---

### В работе

- [-] **Шаг 4:** Реализация Compose UI экрана профиля (`ProfileRoute`, `ProfileScreen`) и фичи дневника (diary: AddDiaryEntry + Room KMP + SyncWorker).

---

### Очередь

- [ ] **Шаг 5:** Написание тестов (commonTest Fakes, Testcontainers backend).
- [ ] **Шаг 6:** Сквозная интеграция и верификация.

---

### Известные проблемы

- ⚠️ `libs.versions.toml` уже имеет 49 строк — проверять существующие версии перед добавлением новых, чтобы избежать дублирования.

---

### Принятые решения

- 📌 2026-10-19: **Архитектура Skills** — 7 специализированных навыков покрывают все слои: KMP shared, Android Compose MVI, Spring gRPC, Contracts (proto/avro), Testing, Legacy migration, Диагностика сборки.
- 📌 2026-10-19: **Checkpointing** — `STATE.md` является единственным механизмом памяти между сессиями; агент обязан читать и обновлять его при каждой сессии.
- 📌 2026-10-19: **Инфраструктура и контракты (Шаг 3)** — создана корневая папка `proto/` с `buf.yaml`, `buf.gen.yaml` и пакетами сервисов; настроен `infrastructure/docker-compose.local.yml` (PostgreSQL 17, Redis 7, Kafka 3.9 KRaft, Schema Registry 7.7, Keycloak 26.0), заготовки Dockerfile, Helmfile и CI workflows.
- 📌 2026-10-20: **Верификация сборки** — локальная инфраструктура Docker запущена без предупреждений; в `mobile/androidApp` добавлены прямые зависимости Compose Multiplatform (`runtime`, `foundation`, `material3`, `ui`), сборка и unit-тесты KMP shared и AndroidApp успешно верифицированы.
- 📌 2026-10-22: **Project Boundary & Онлайн-исследование Git** — в `AGENTS.md`, `implementer.md`, `test-reviewer.md` зафиксирован строгий запрет на выход за пределы директории проекта без явного согласия пользователя. В `legacy-migration/SKILL.md` закреплено правило: исследование репозиториев (VKEducation, FitAssistentAndroid, ai-calorie-tracker-backend) производится **только онлайн** через GitHub без клонирования, скачивания и сохранения файлов на диск.

---

### Активный Skill текущей сессии

`legacy-migration` (обновление правил миграции и протокола безопасности).
`diagnose-build-failure` / `testing-and-verification` (верификация сборки и инфраструктуры).


## Checkpoint 2026-10-23 — T-005 / KAN-24, обновление Kafka-зависимостей
Текущая ветка feature/KAN-24-gradle-backend. Этот checkpoint уточняет старый статус выше; не подтверждает готовность всех интеграций.
- Активный Skill: diagnose-build-failure; цель — согласовать Kafka client/Confluent/Avro без Docker и без изменения пользовательских мобильных настроек.
- Confluent serializer 7.7.0 → 8.2.4; Avro 1.11.3 → 1.12.1 в Version Catalog.
- В Profile/Catalog/Diary исключён kafka-clients только из транзитивного дерева Confluent serializer. Spring Kafka получает Apache client 4.2.2 из Boot BOM; dependencyInsight и содержимое всех трёх Boot JAR это подтвердили.
- Добавлен AvroCompatibilityTest в Profile: реальная сериализация/десериализация с MockSchemaRegistryClient без сети; 1 test PASS, failures/errors/skips=0.
- PASS: ./gradlew build :backend:profile-service:detektMain :backend:profile-service:detektTest --stacktrace (189 tasks:14 executed,175 up-to-date). iOS targets ранее отключены пользователем; iOS не проверена.
- PASS: git diff --check. Docker/Compose, Kafka broker и Registry не менялись; commit/push не выполнялись.
- Ограничения: live Kafka/Registry/DB/Redis не проверены; четыре специальных правила detekt не настроены и не доказаны; detekt сейчас применяется к Profile, не ко всем backend-модулям; удалённый CI не запускался. Полную T-005 не объявлять завершённой только по успешному build.
- По запросу пользователя добавлены поясняющие комментарии у версий Confluent/Avro, трёх исключений Kafka client и offline-теста; поведение не изменялось. Пользователь повторил build + detektMain/detektTest, отдельно AvroCompatibilityTest и dependencyInsight — предоставленный лог содержит BUILD SUCCESSFUL для этих команд.

## Checkpoint 2026-10-25 — Общие конфигурации запуска IDEA
- Активный Skill: testing-and-verification; роль implementer.
- В `.run/` перенесены шесть конфигураций из локального RunManager IDEA: Catalog Price, Diary Local, Profile Local, RecipeAI Local, FitAssistant — Full Docker, FitAssistant — Infrastructure.
- Android, iOS, Gradle check и шаблоны не перенесены. Исходный workspace.xml родительского проекта не изменён.
- Общие конфигурации рассчитаны на открытие в IDEA корня репозитория Fit-Assistent: `.env` задаётся через `$PROJECT_DIR$/.env`, Compose-файлы — через `infrastructure/`, рабочая папка Application — `$PROJECT_DIR$`.
- Сохранено имя Docker-подключения FitAssistant Docker; на другой машине при необходимости нужно выбрать своё подключение. Содержимое `.env` в XML не переносилось.
- PASS: синтаксис шести XML, набор конфигураций, пути, соответствие main-классов и Gradle-модулей, отсутствие личных абсолютных путей, доступность файлов для Git, `docker compose --env-file .env -f infrastructure/docker-compose.local.yml -f infrastructure/docker-compose.backend.yml config --quiet`, `git diff --check`.
- Сборка, запуск приложений и проверка отображения конфигураций в интерфейсе IDEA не выполнялись. Commit/push и смена ветки не выполнялись.

## Checkpoint 2026-11-02 — [T-035] data/local: Room KMP (Entity, DAO, DatabaseBuilder, Mapper, DI, Tests)
- Активные Skills: `kmp-shared-feature`, `legacy-migration`, `testing-and-verification`.
- Реализованы компоненты локальной БД для профиля пользователя (`Profile`):
  - `ProfileEntity` (`dev.andre.nutritionist.data.local.entity.ProfileEntity`): таблица `profiles`, primary key `userId`, расплющенный `NutritionGoal`, строковые enum'ы;
  - `ProfileDao` (`dev.andre.nutritionist.data.local.dao.ProfileDao`): `getProfile`, реактивный `observeProfile` (`Flow`), `insertOrReplace` (`OnConflictStrategy.REPLACE`), `clear`;
  - `AppDatabase` (`dev.andre.nutritionist.data.local.AppDatabase`): `@Database(version = 1, exportSchema = true)` и `@ConstructedBy(AppDatabaseConstructor::class)`;
  - Schema export: KSP успешно сгенерировал схему `mobile/shared/schemas/dev.andre.nutritionist.data.local.AppDatabase/1.json`;
  - `DatabaseBuilder<T>` (`dev.andre.nutritionist.data.local.DatabaseBuilder`): общий интерфейс в `commonMain`;
  - `AndroidDatabaseBuilder` (`dev.andre.nutritionist.data.local.AndroidDatabaseBuilder`): фабрика для Android в `androidMain` через `Room.databaseBuilder` с `BundledSQLiteDriver` и `Dispatchers.IO`;
  - `ProfileEntityMapper` (`dev.andre.nutritionist.data.mapper.ProfileEntityMapper`): маппер Domain $\leftrightarrow$ Entity без `else` в `when` для enum-ов, поддержка `Instant` и value classes;
  - Koin DI: модуль `localDataModule` зарегистрирован в `sharedModule` в `di/Koin.kt`, `androidLocalDataModule` добавлен в `androidMain` и включён в `androidAppModule`;
  - Тесты: 
    - `ProfileEntityMapperTest` в `commonTest`: 3 unit-теста на маппинг в обе стороны и fallback для некорректных значений;
    - `ProfileDaoTest` в `androidHostTest` с Robolectric: 4 теста in-memory Room SQLite (`insertAndGetProfile`, `getProfile_returnsNull_whenNotFound`, `insertOrReplace_updatesExistingProfile`, `observeProfile_emitsUpdatesTurbine` с библиотекой Turbine).
- PASS: `./gradlew :mobile:shared:allTests :mobile:androidApp:testDebugUnitTest` (53/53 GREEN).

## Checkpoint 2026-11-07 — [T-036 / KAN-54] ProfileRepositoryImpl (SSOT, Room Flow, KoTEA Handler, Apache 2.0 License)
- Активные Skills: `kmp-shared-feature`, `android-compose-feature`, `diagnose-build-failure`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- Ветка: `feature/KAN-54` (коммит `b559947` и последующие исправления).
- **Анализ сбоя тестов:**
  - В коммите `b559947` был расширен контракт `ProfileRepository` для реализации Single Source of Truth (`observeProfile`, `refreshProfile`, `updateProfile: Result<Profile>`).
  - При запуске `./gradlew :mobile:shared:allTests` возникли ошибки компиляции `compileAndroidHostTest`: `ProfileCommandsFlowHandlerTest.kt` и `ProfileViewModelTest.kt` содержали устаревшие классы `FakeProfileRepository`, не реализующие новые методы интерфейса.
  - В `ProfileCommandsFlowHandler` реализация через `channelFlow` приводила к потере сигналов отмены и скрывала нормальное поведение корутин.
- **Выполненные исправления и оптимизации:**
  1. `ProfileCommandsFlowHandler.kt`: переведён на реактивный `merge(observeFlow, refreshFlow)`. 
     - `observeFlow`: непрерывно передаёт в Store актуальное состояние из Room через `observeProfile(userId)`.
     - `refreshFlow`: в фоне запускает `refreshProfile(userId)` на `dispatchers.io`. В случае сетевой ошибки и отсутствия кэша эмитит `ProfileLoadFailed`, а при наличии локального кэша не блокирует показ данных. При `CancellationException` поток безопасно отменяется без ложного отображения ошибки пользователю.
  2. `ProfileCommandsFlowHandlerTest.kt`:
     - Обновлён `FakeProfileRepository` с поддержкой всех методов `ProfileRepository`;
     - Добавлены/актуализированы тесты на успешную загрузку, обработку сетевых ошибок, отмену (`CancellationException`) и принудительный refresh (`RefreshProfile`).
  3. `ProfileViewModelTest.kt`:
     - `FakeProfileRepository` актуализирован под контракт SSOT;
     - Все сценарии обновления UI-состояний (`Loading` $\to$ `Success` / `Error` + `News`) успешно выполняются.
  4. **Оформление лицензии Apache License 2.0 (KoTEA):**
     - В корень проекта добавлен файл `LICENSE` с полным текстом Apache License 2.0;
     - В корень проекта добавлен файл `NOTICE` с указанием авторских прав Tinkoff Bank (KoTEA);
     - Проверено сохранение оригинальных заголовков во всех 8 файлах пакета `dev.andre.nutritionist.core.kotea.*`.
- **Верификация и результаты:**
  - `./gradlew :mobile:shared:allTests` — 53 теста выполнены, 0 сбоев, 0 ошибок (BUILD SUCCESSFUL).
  - Проверено соблюдение всех правил `AGENTS.md`: запрет оператора `!!`, исчерпывающий `when` без `else` для sealed интерфейсов, строгая изоляция domain-слоя, отсутствие `MockK` в `commonTest` (только чистые Fakes и Turbine).

## Checkpoint 2026-11-07 — [T-039 / KAN-57] Android: OAuth 2.0 PKCE через AppAuth & Keycloak
- Активные Skills: `android-compose-feature`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- Ветка: `feature/KAN-57-android-appauth-keycloak`.
- **Реализация:**
  1. Добавлены зависимости `appauth:0.11.1` и `androidx-security-crypto:1.1.0-alpha06` в `gradle/libs.versions.toml` и `mobile/androidApp/build.gradle.kts` с `manifestPlaceholders["appAuthRedirectScheme"]`.
  2. Добавлены строго типизированные value classes `@JvmInline value class AccessToken`, `RefreshToken`, `IdToken` в `ids.kt`.
  3. `AuthConfig.kt`: конфигурация параметров Keycloak (discovery endpoint, clientId `fitassistant-mobile`, redirect URI `com.budgetntr.fitassistant://oauth2redirect`).
  4. `AuthStorage.kt` и `EncryptedPrefsAuthStorage.kt`: безопасное хранилище `AuthState` на базе `EncryptedSharedPreferences` (AES256_SIV + AES256_GCM) и Android Keystore `MasterKey`, операции строго на `dispatchers.io`.
  5. `TokenExchanger.kt` и `AuthIntentProvider.kt`: абстракции обмена кодов и подготовки интента для Custom Tabs (PKCE Code Verifier + S256 Challenge).
  6. `AuthRepository.kt` и `AuthRepositoryImpl.kt`: реактивный статус `observeAuthState()`, безопасный парсинг `processAuthResponse(intent)` с валидацией `AuthorizationException` и `AuthorizationResponse`, `logout()`, реализация контракта `AuthTokenProvider` для `AuthClientInterceptor`.
  7. KoTEA Presentation (`AuthContract.kt`, `AuthUpdate.kt`, `AuthCommandsFlowHandler.kt`, `AuthViewModel.kt`): исчерпывающий `when` без `else`, чистый стейт с вложенным `sealed interface Content`, асинхронный поток команд.
  8. `AndroidManifest.xml`: конфигурация `RedirectUriReceiverActivity` со схемой `com.budgetntr.fitassistant` и хостом `oauth2redirect`, разрешение `INTERNET`.
  9. Koin DI: модуль `authModule` в `AndroidModule.kt` с регистрацией всех компонентов и привязкой к `AuthTokenProvider`.
  10. Тесты (без MockK, чистые Fakes `FakeAuthStorage`, `FakeTokenExchanger`, `FakeAuthRepository`, `FakeAuthIntentProvider` + `Turbine` + Robolectric):
      - `AuthRepositoryImplTest`: успешная авторизация с сохранением токенов, обработка отмены пользователем / ошибки, logout.
      - `AuthUpdateTest`: исчерпывающее тестирование переходов состояний редьюсера.
      - `AuthCommandsFlowHandlerTest`: тестирование подготовки интента, обмена токенов, обработки ошибок и выхода.
- **Верификация сборки и тестов:**
  - `./gradlew :mobile:androidApp:testDebugUnitTest :mobile:androidApp:assembleDebug` — ВСЕ ПРОЙДЕНЫ УСПЕШНО (BUILD SUCCESSFUL, 16/16 тестов GREEN).
  - `./gradlew :mobile:shared:allTests` — 53/53 GREEN.



