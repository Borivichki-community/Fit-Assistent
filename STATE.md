# STATE.md — Журнал прогресса «Бюджетный нутрициолог»

> Этот файл читается агентом **перед началом любой задачи**.
> Обновляется после каждого завершённого этапа.

Обновлено: 2026-11-25

### Контракты Diary GetRange, Coach targetUserId, Offline Delete Tombstone, gRPC Auth Refresh, ConnectivityObserver и GetNutritionAnalyticsUseCase (2026-11-25)

- **Активные Skills:** `kmp-shared-feature`, `spring-grpc-service`, `testing-and-verification`. Роль: `implementer` + `test-reviewer`.
- **Ветка:** `feature/KAN-88-syncworker-favorite`.
- **Блок 1 — Дневник (`GetRange`, `targetUserId` для тренера и Offline Soft-Delete Tombstone):**
  - Proto/DTO/gRPC (`DiaryProto.kt`, `DiaryDtoMapper.kt`, `DiaryRemoteDataSource.kt`, `DiaryGrpcRemoteDataSource.kt`, `AndroidDiaryGrpcServiceClient.kt`): добавлены `GetRangeRequest` / `GetRangeResponse`, `targetUserId: String?` (proto field `10`) в `GetDailyEntriesRequest` и `GetRangeRequest`, round-trip маршаллеры `GetRangeRequestMarshaller` и `GetRangeResponseMarshaller`, `errorMapper` (`GrpcErrorMapper`) в `DiaryGrpcRemoteDataSource`.
  - Room KMP v5 (`AppDatabase.kt`, `DatabaseMigrations.kt`, `AndroidDatabaseBuilder.kt`, `schemas/.../5.json`): добавлен столбец `is_deleted INTEGER NOT NULL DEFAULT 0` в `MealRecordEntity`, статус `SyncStatus.DELETED_PENDING`, миграция `MIGRATION_4_5`.
  - DAO & Repository (`MealRecordDao.kt`, `DiaryRepository.kt`, `DiaryRepositoryImpl.kt`, `SyncDiaryWorker.kt`): `markAsDeleted`, `getPendingDeletedRecords`, `deletePermanently`, исключение `is_deleted = 1` из UI-запросов, `getRange` (режим тренера с `targetUserId` напрямую через gRPC без смешивания с локальной Room; свой диапазон с объединением `PENDING` и сокрытием `DELETED_PENDING`), tombstone-удаление и двусторонняя синхронизация `PENDING` + `DELETED_PENDING` в `syncPendingRecords()` и `SyncDiaryWorker`.
- **Блок 2 — Автоматический Refresh токена при `Status.UNAUTHENTICATED` (`UnifiedGrpcClientInterceptor.kt`):**
  - Внедрена обёртка `RetryingClientCall`: при `Status.UNAUTHENTICATED` запрашивает `AuthTokenProvider.refreshIfNeeded()` под `Mutex` (с double-check относительно неудавшегося токена, чтобы параллельные RPC делали только один refresh) и повторяет вызов ровно 1 раз с сохранением исходного `idempotency-key` и заголовков; при провале refresh или повторной ошибке прокидывает `UNAUTHENTICATED` (маппится в `DomainError.Unauthorized`).
- **Блок 3 — Реактивный `ConnectivityObserver`:**
  - Интерфейс `ConnectivityObserver` (`val isConnected: StateFlow<Boolean>`) в `commonMain`, реализация `AndroidConnectivityObserver` (`ConnectivityManager.NetworkCallback`) в `androidMain`, разрешение `ACCESS_NETWORK_STATE` в манифестах и биндинг в `AndroidDataModule.kt`.
- **Блок 4 — `GetNutritionAnalyticsUseCase` (`NutritionAnalytics.kt`, `GetNutritionAnalyticsUseCase.kt`, `Koin.kt`):**
  - Добавлены модели `PeriodAnalytics`, `DailyNutritionPoint` и UseCase расчёта КБЖУ, соблюдения норм, трат бюджета и дневной разбивки за период (включая `targetUserId` для тренера), зарегистрирован в `domainModule`.
- **Тестирование и верификация (без MockK, только Fakes):**
  - Новые тесты: `DiaryTombstoneSyncTest` (5 тестов), `GetNutritionAnalyticsUseCaseTest` (4 теста), `AuthTokenRefreshInterceptorTest` (4 теста), `DiaryGetRangeGrpcTest` (4 теста).
  - Обновлены и расширены: `FakeMealRecordDao`, `FakeDiaryRemoteDataSource`, `FakeDiaryRepository`, `SyncDiaryWorkerTest`, `MealRecordDaoTest`, `AppDatabaseMigrationTest` (`4 -> 5` и `3 -> 5`), `RecipeDatabaseMigrationTest`, `MainScreenTest`.
  - `:mobile:shared:testAndroidHostTest :mobile:shared:assemble` (234 passed, 0 failed) и `:mobile:androidApp:testDebugUnitTest :mobile:androidApp:assembleDebug` (80 passed, 0 failed) — **BUILD SUCCESSFUL**.

### Адаптивная иконка приложения и динамический Splash-экран (День / Ночь) (2026-11-25)

- **Активные Skills:** `android-compose-feature`, `testing-and-verification`. Роль: `implementer` + `test-reviewer`.
- **Ветка:** `feature/KAN-88-syncworker-favorite`.
- **Блок 1 — Новая адаптивная иконка приложения (Adaptive Icon):**
  - `mobile/androidApp/src/main/res/drawable/ic_launcher_background.xml`: векторный градиент в стиле Aether Emerald (#00F5A0 / #00C875 -> #005232 / #003921).
  - `mobile/androidApp/src/main/res/drawable/ic_launcher_foreground.xml` (и `drawable-v24/`): центральный векторный логотип — круговой циферблат с делениями нутриентов, внутренний белый диск, нижняя серповидная дуга телеметрии, росток с двумя листьями (контур `#102A1E`, заливки `#D5FCE6` и `#00C875`, блик `#FFFFFF`) и постамент, подогнанный под 72dp безопасную зону adaptive icon.
  - `mobile/androidApp/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` и `ic_launcher_round.xml`: обновлены ссылки на `@drawable/ic_launcher_background` и `@drawable/ic_launcher_foreground`.
  - Растровые ассеты: сгенерированы сглаженные `ic_launcher.png`, `ic_launcher_round.png` и `ic_launcher_foreground.png` для всех плотностей (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi), обновлен `ic_launcher-playstore.png` (512x512), удалены устаревшие `.webp` ассеты.
- **Блок 2 — Адаптивный Splash-экран (День / Ночь):**
  - Токены цветов: созданы `res/values/colors.xml` (светлая тема: `splash_bg` #FFFFFF, `splash_title` #0F172A, `splash_subtitle` #64748B, `splash_progress_track` #E2E8F0, `splash_progress_indicator` #00C875) и `res/values-night/colors.xml` (тёмная тема: `splash_bg` #0B0E14, `splash_title` #F4F7FA, `splash_subtitle` #8C96A8, `splash_progress_track` #1E293B, `splash_progress_indicator` #00F5A0).
  - Тема `Theme.FitAssistant.Splash` в `res/values/themes.xml` (на базе `Theme.Material3.DayNight.NoActionBar`), зарегистрирована в `AndroidManifest.xml`.
  - Векторный логотип `res/drawable/ic_splash_logo.xml` (180dp x 180dp) с орбитальными линиями ретикла и перекрестьями телеметрии.
  - Строковые ресурсы: добавлены `splash_title` ("Fit Assistant") и локализованные `splash_subtitle` ("Точный КБЖУ · Умный бюджет" / "Precise Nutrition · Smart Budget").
  - Вёрстка `res/layout/activity_splash.xml`: фон `@color/splash_bg`, логотип `ivSplashLogo`, заголовок `tvSplashTitle` (`sans-serif-black`, 32sp), подзаголовок `tvSplashSubtitle` (`sans-serif`, 16sp) и `LinearProgressIndicator` (180dp, высота 4dp, скругление 2dp, бесконечная плавная анимация).
  - Логика в `SplashActivity.kt`: плавная анимация появления (Alpha 0 -> 1 и Scale 0.95 -> 1.0 за 600 мс с `DecelerateInterpolator`), асинхронная проверка `authRepository.isAuthenticated()`, через 1.2 с бесшовный переход с fade-анимацией в `MainActivity` (`Dashboard` при авторизации, иначе `Auth/Login`) и `finish()`.
- **Тестирование и верификация:**
  - Обновлён `SplashActivityTest.kt` на базе Robolectric (проверка компонентов вёрстки, текстов и перехода в `MainActivity` с завершением `SplashActivity`).
  - Успешный запуск `:mobile:androidApp:assembleDebug` и `:mobile:androidApp:testDebugUnitTest` (79 passed, 0 failed).

- **Активные Skills:** `kmp-shared-feature`, `android-compose-feature`, `testing-and-verification`. Роль: `implementer` + `test-reviewer`.
- **Ветка:** `feature/KAN-16-Coach-premissions-recipebd`.
- **Блок 1 [T-070] — Фоновая синхронизация дневника (`SyncScheduler` + `SyncDiaryWorker`):**
  - `:mobile:shared`: создан интерфейс `SyncScheduler` (`dev.andre.nutritionist.domain.sync`); внедрён как опциональная зависимость в `DiaryRepositoryImpl` (при сбое сети и сохранении записи со статусом `PENDING` вызывается `syncScheduler?.scheduleDiarySync()`).
  - `:mobile:androidApp`: реализованы `SyncDiaryWorker` (`CoroutineWorker`, `KoinComponent`, `MAX_RETRIES = 5`, `BackoffPolicy.EXPONENTIAL` 10 сек, `NetworkType.CONNECTED`, перевод `PENDING -> SYNCED` или `FAILED` после исчерпания попыток с сохранением `idempotencyKey`) и `WorkManagerSyncScheduler` (`enqueueUniqueWork(..., ExistingWorkPolicy.KEEP)`), зарегистрированы в `workModule` (`AndroidModule.kt`).
- **Блок 2 [T-071] — Избранные продукты и сохранённые рецепты в Room KMP (миграция v3 → v4):**
  - *Примечание по версионированию:* схема v3 была занята ранее в `T-078` (`local_recipes`, `local_meal_plans`), поэтому `AppDatabase` переведён на `version = 4` с сохранением `3.json` и `MIGRATION_2_3`, добавлена явная миграция `MIGRATION_3_4` (`DatabaseMigrations.kt`, подключена в `AndroidDatabaseBuilder.kt` без `fallbackToDestructiveMigration()`), экспортирована схема `mobile/shared/schemas/dev.andre.nutritionist.data.local.AppDatabase/4.json`.
  - Созданы `FavoriteProductEntity`, `SavedRecipeEntity`, `FavoriteProductDao`, `SavedRecipeDao`, мапперы `FavoriteProductEntityMapper` и `SavedRecipeEntityMapper` (чистый KMP JSON сериализатор/парсер для `ingredients_json` без `java.*`).
  - Расширены `CatalogRepository`/`CatalogRepositoryImpl` (`toggleFavoriteProduct`, `observeFavoriteProducts`) и `RecipeRepository`/`RecipeRepositoryImpl` (`saveRecipe`, `deleteSavedRecipe`, `observeSavedRecipes`).
- **Блок 3 [T-072] — Презентационный MVI-слой KoTEA в `:mobile:shared` (без правок UI):**
  - `presentation/catalog/`: `CatalogSearchState`, `CatalogSearchEvent`, `CatalogSearchCommand`, `CatalogSearchNews`, `CatalogSearchUpdate`, `CatalogSearchCommandsFlowHandler` (`debounce(300ms)` + `distinctUntilChanged()` + пагинация и избранное на раздельном потоке), `CatalogSearchUiState`, `CatalogSearchUiStateMapper`, `CatalogSearchViewModel`.
  - `presentation/airecipe/`: `MenuState`, `MenuEvent`, `MenuCommand`, `MenuNews`, `MenuUpdate`, `MenuCommandsFlowHandler` (отмена предыдущего расчёта через `collectLatest` + `GenerateBudgetMenuUseCase`), `MenuUiState`, `MenuUiStateMapper`, `MenuViewModel`.
  - `presentation/dashboard/`: `DiaryState`, `DiaryEvent`, `DiaryCommand`, `DiaryNews`, `DiaryUpdate`, `DiaryCommandsFlowHandler` (реактивное наблюдение Room `observeDailyRecords` + пересчёт баланса КБЖУ через `DailyNutritionBalanceCalculator`), `DiaryUiState`, `DiaryUiStateMapper`, `DiaryViewModel`.
  - Все UseCase, DAO, репозитории и ViewModel зарегистрированы в `Koin.kt`.
- **Блок 4 [T-073] — Чистые KMP-форматтеры, UI-модели и мапперы:**
  - Созданы `MoneyFormatter`, `NutritionFormatter`, `DateFormatter` (на `kotlinx-datetime` и `kotlin.time.Instant`, без `java.*` и `String.format`).
  - Созданы UI-модели (`ProductUiModel`, `MealRecordUiModel`, `MealPlanUiModel`, `DailyNutritionUiModel`) и мапперы (`ProductUiMapper`, `MealRecordUiMapper`, `MealPlanUiMapper`, `DailyNutritionUiMapper`).
- **Тестирование и верификация (без MockK, только Fakes):**
  - `commonTest`: `FormattersTest`, `CatalogSearchUpdateTest`, `MenuUpdateTest`, `DiaryUpdateTest`, `CommandsFlowHandlerTest` (Turbine: debounce 300ms, `collectLatest`, реактивный дневник), `FavoritesAndSavedRecipesRepositoryTest`, расширен `DiaryRepositoryImplTest` (`FakeSyncScheduler`).
  - `androidHostTest`: `AppDatabaseMigrationTest` (проверка `3 -> 4` и `2 -> 4`, CRUD `FavoriteProductDao`/`SavedRecipeDao`).
  - `androidApp/test`: `SyncDiaryWorkerTest` (WorkRequest constraints/backoff, пустая очередь, успех `SYNCED`, retry < 5, `FAILED` >= 5).
  - `:mobile:shared:allTests` (215 passed, 0 failed, 0 skipped), `:mobile:androidApp:testDebugUnitTest` (79 passed, 0 failed, 0 skipped), `:mobile:androidApp:assembleDebug` — **BUILD SUCCESSFUL**.

### Fix CI — Устранение гонки в `LocalRecipeDaoTest` и дедупликация `observeMyRecipes` (2026-11-25)

- **Активные Skills:** `diagnose-build-failure`, `testing-and-verification`. Роль: `implementer`.
- **Ветка:** `feature/KAN-16-Coach-premissions-recipebd`.
- **Причина сбоя CI (`:mobile:shared:allTests`):**
  - В `LocalRecipeDaoTest.observeMyRecipes_filtersSystemAndDeletedAndEmitsReactively` последовательные `upsertRecipeWithIngredients` для `SYSTEM` и `USER` рецептов с асинхронным `InvalidationTracker` Room на `Dispatchers.IO` могли приводить к повторной эмиссии `[user-1]` в `Flow`, из-за чего `awaitItem()` после `markDeleted` получал устаревший непустой список.
- **Исправление:**
  - `mobile/shared/src/commonMain/kotlin/dev/andre/nutritionist/data/repository/RecipeCatalogRepositoryImpl.kt`: добавлен `.distinctUntilChanged()` в `observeMyRecipes()` для исключения дублирующих эмиссий Room при инвалидации `local_recipes` / `local_recipe_ingredients`.
  - `mobile/shared/src/androidHostTest/kotlin/dev/andre/nutritionist/data/local/LocalRecipeDaoTest.kt`: добавлен `.distinctUntilChanged()` в поток теста и убран обходной цикл `while (items.isEmpty())`.
- **Верификация:**
  - `:mobile:shared:allTests` (`:mobile:shared:testAndroidHostTest` — 187 passed, 0 skipped, 0 failed) — **BUILD SUCCESSFUL**.
  - `:mobile:androidApp:assembleDebug` — **BUILD SUCCESSFUL**.

### KAN-16 — Доступ тренера к дневнику ученика в :backend:diary-service (Fail-Closed + Redis Cache) (2026-11-24)

- **Активные Skills:** `contracts-proto-and-avro`, `spring-grpc-service`, `testing-and-verification`. Роль: `implementer` + `test-reviewer`.
- **Ветка:** `feature/KAN-16-Coach-premissions-recipebd`, коммит: `feat(diary): implement coach student diary access verification with fail-closed redis cache`.
- **Protobuf-контракт (`proto/nutritionist/diary/v1/diary.proto`):**
  - В `GetDailyEntriesRequest` и `GetRangeRequest` добавлено обратно совместимое поле `optional string target_user_id = 10;`.
  - Мутирующие запросы (`AddEntryRequest`, `DeleteMealRequest`) оставлены строго owner-only.
- **gRPC-клиент и Resilience4j (`:backend:diary-service`):**
  - `DiaryClientProperties` & `DiaryClientConfiguration`: конфигурация `diary.clients.recipe.*` (`localhost:9194`, K8s `recipe-ai-service.budget-nutritionist.svc.cluster.local:9194`, deadline `2s`), канал `recipeServiceChannel` и CircuitBreaker `diary-recipe-access`.
  - `RecipeAccessClient`: вызов `CheckCoachAccess` с прокидыванием `x-user-id`, `x-user-roles`, `x-dev-bypass` и W3C `traceparent`; строгая политика Fail-Closed (`Status.UNAVAILABLE` при сбое сети / таймауте / открытом CircuitBreaker).
- **Кэширование прав доступа и безопасность (`CoachAccessCacheService`, `DiaryAuthContext`, `DiaryGrpcService`):**
  - `CoachAccessCacheService`: Redis-ключ `diary:coach_access:{coachId}:{studentId}` (`"1"` / `"0"`, TTL `60s`), прозрачная деградация на gRPC-клиент при недоступности Redis и проброс `Status.UNAVAILABLE` без записи в кэш при сбое `recipe-ai-service`.
  - `DiaryRequestContextInterceptor` & `DiaryAuthContext`: разбор `x-user-roles`, сохранение `DiaryAuthContext` в gRPC `Context`, обход проверки только для `ROLE_ADMIN` / `ROLE_SERVICE`.
  - `DiaryGrpcService`: проверка прав тренера выполняется в gRPC-слое (`withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(...))`) строго ДО входа в `@Transactional(readOnly = true)` методы `DiaryService`.
- **Интеграционное тестирование и верификация:**
  - `CoachStudentDiaryAccessIntegrationTest` & `CoachAccessOutageIntegrationTest` (Testcontainers PostgreSQL 17 + Redis 7, локальный Fake `RecipeAiService` без MockK): чтение своего дневника без вызова gRPC, отказ `PERMISSION_DENIED` без связи и при `can_view_diary = false`, успешное чтение тренером с кэшированием в Redis (`TTL <= 60s`), Fail-Closed `Status.UNAVAILABLE` при отказе `recipe-ai-service` и переходе CircuitBreaker в `OPEN`, прозрачный fallback на gRPC при падении Redis, owner-only изоляция мутаций.
  - `:backend:diary-service:test` (73 теста) и `:backend:diary-service:ktlintCheck` — **BUILD SUCCESSFUL**.
  - `:backend:recipe-ai-service:compileKotlin` и `:mobile:androidApp:compileDebugKotlin` — **BUILD SUCCESSFUL**.

### KAN-16 / T-078 — Мобильный Data-слой: Room KMP v3, gRPC клиенты и offline-first репозитории в :mobile:shared (2026-11-24)

- **Активные Skills:** `kmp-shared-feature`, `testing-and-verification`. Роль: `implementer` + `test-reviewer`.
- **Ветка:** `feature/KAN-16-Coach-premissions-recipebd`, коммит: `feat(shared): T-078 implement Room v3 schema, gRPC remote clients and offline-first recipe repositories`.
- **Room KMP v3 и явная миграция (`commonMain` & `androidMain`):**
  - `AppDatabase.kt`: версия увеличена до `3`, зарегистрированы сущности `LocalRecipeEntity`, `LocalRecipeIngredientEntity`, `LocalMealPlanEntity`, `LocalMealPlanItemEntity` и DAO `localRecipeDao()`, `localMealPlanDao()`.
  - `DatabaseMigrations.kt`: реализована явная миграция `MIGRATION_2_3` (создание таблиц `local_recipes`, `local_recipe_ingredients`, `local_meal_plans`, `local_meal_plan_items` с внешними ключами `ON DELETE CASCADE` и индексами), подключена в `AndroidDatabaseBuilder.kt` без `fallbackToDestructiveMigration()`.
  - Экспортирована схема Room `mobile/shared/schemas/dev.andre.nutritionist.data.local.AppDatabase/3.json`.
- **gRPC клиенты, бинарные маршаллеры и мапперы:**
  - `RecipeProto.kt` (`commonMain`), `RecipeCatalogMarshallers.kt` (`androidMain`), `AndroidRecipeGrpcServiceClient.kt` и `RecipeGrpcRemoteDataSource.kt`: поддержаны все 18 RPC-методов каталога рецептов, меню и тренерского доступа с прокидыванием `idempotency-key`.
  - `UnifiedGrpcClientInterceptor.kt`: расширен список мутирующих методов (`Fork*`, `Publish*`, `Invite*`, `Respond*`, `Grant*`, `Revoke*`) для автоматической подстановки `idempotency-key`.
  - `RecipeCatalogDtoMapper.kt`, `RecipeEntityMapper.kt`, `MealPlanEntityMapper.kt`: двусторонний маппинг DTO ↔ Domain ↔ Room Entity.
- **Offline-first репозитории и Koin DI:**
  - `RecipeCatalogRepositoryImpl`: реактивное чтение `observeMyRecipes()`, offline-first `saveRecipe` (`PENDING` → `SYNCED`, переиспользование `idempotencyKey`, замена временного локального ID на серверный, `createRecipe` vs `updateRecipe`), защита локальных `PENDING`-правок от перезаписи при поиске и чтении по ID, `forkRecipe`, `publishRecipe`, `deleteRecipe`.
  - `MealPlanRepositoryImpl`: удалённая работа с планами питания, кэширование в `LocalMealPlanDao` и offline fallback в `getMealPlanById`.
  - `CoachingRepositoryImpl`: управление связями тренер-ученик и выдача/отзыв прав доступа.
  - `Koin.kt` и `AndroidDataModule.kt`: зарегистрированы новые DAO, gRPC-клиенты/DataSource (с `GrpcErrorMapper.map`) и репозитории; `mobile/androidApp` UI не изменялся.
- **Тестирование и верификация:**
  - `commonTest` (без MockK): `FakeLocalRecipeDao`, `FakeRecipeRemoteDataSource`, `RecipeCatalogRepositoryImplTest`.
  - `androidHostTest`: `LocalRecipeDaoTest`, `RecipeDatabaseMigrationTest` (валидация `MIGRATION_2_3`), `AndroidRecipeGrpcSerializationTest` (wire-level round-trip маршаллеров), изоляция `testUserId` в `RemoteDataSourcesIntegrationTest`.
  - `:mobile:shared:allTests` — **BUILD SUCCESSFUL** (187 тестов пройдено, 0 ошибок, 0 пропущено).
  - `:mobile:shared:assemble` — **BUILD SUCCESSFUL**.
  - `:mobile:androidApp:assembleDebug` — **BUILD SUCCESSFUL**.

### KAN-16 / T-077 — KMP Domain-слой в :mobile:shared (commonMain, без UI): каталог рецептов, планы питания и тренерский доступ (2026-11-24)

- **Активные Skills:** `kmp-shared-feature`, `testing-and-verification`. Роль: `implementer` + `test-reviewer`.
- **Ветка:** `feature/KAN-16-Coach-premissions-recipebd`, коммит: `feat(shared): T-077 implement recipe catalog, meal plans and coaching domain models and use cases`.
- **Доменные модели и Value Classes (`dev.andre.nutritionist.domain.model`):**
  - `ids.kt`: добавлены `@JvmInline value class` `RecipeIngredientId`, `MealPlanTemplateId`, `MealPlanItemId`, `CoachLinkId`, `AccessGrantId`, а также генератор `MealRecordId.generate()`.
  - `Recipe.kt`: добавлены `RecipeAuthorType`, `ContentVisibility`, расширены `RecipeIngredient` (`id`, `nutritionPer100g`) и `Recipe` (`authorType`, `authorId`, `originRecipeId`, `description`, `instructions`, `visibility`, `servingsCount`, `totalWeightGrams`, `version`, `createdAt`, `updatedAt`) с обратной совместимостью.
  - `MealPlanTemplate.kt`: добавлены `AccessMode`, `PlanItemType`, `MealPlanItem`, `MealPlanDay`, `MealPlanTemplate`.
  - `CoachingModels.kt`: добавлены `CoachLinkStatus`, `CoachStudentLink`, `GrantSource`, `GrantTargetType`, `AccessGrant`.
  - `SearchAndPagination.kt`: добавлены `RecipeSearchCriteria`, `MealPlanSearchCriteria`, `PagedResult<T>`.
- **Интерфейсы репозиториев (`dev.andre.nutritionist.domain.repository`):**
  - `RecipeCatalogRepository` (`observeMyRecipes`, `searchPublicRecipes`, `getRecipeById`, `saveRecipe`, `deleteRecipe`, `forkRecipe`, `publishRecipe`).
  - `MealPlanRepository` (`searchMealPlans`, `getMealPlanById`, `saveMealPlan`, `deleteMealPlan`).
  - `CoachingRepository` (`getCoachLinks`, `inviteStudent`, `respondCoachInvite`, `grantAccess`, `revokeAccess`).
- **Калькулятор и UseCases (`commonMain` + Koin DI):**
  - `RecipeNutritionCalculator`: чистый клиентский расчёт `RecipeNutritionSummary` (общий вес, итоговое КБЖУ, КБЖУ на 100 г с защитой от деления на ноль), расчёт порции и пропорциональной стоимости (возврат `null` при неизвестной стоимости).
  - `LogRecipeToDiaryUseCase`: расчёт КБЖУ и стоимости порции рецепта и сохранение `MealRecord` в `DiaryRepository` (`sourceType = EntrySourceType.RECIPE`, `sourceId = recipe.id.raw`, `syncStatus = SyncStatus.PENDING`).
  - `ApplyMealPlanDayToDiaryUseCase`: пакетное добавление всех блюд `MealPlanDay` (или выбранного дня `MealPlanTemplate`) в `DiaryRepository` с исчерпывающим маппингом `PlanItemType`.
  - `Koin.kt`: регистрация `LogRecipeToDiaryUseCase` и `ApplyMealPlanDayToDiaryUseCase` в `domainModule`.
- **Тестирование и верификация (`commonTest`, без MockK):**
  - Созданы `FakeDiaryRepository`, `RecipeNutritionCalculatorTest`, `LogRecipeToDiaryUseCaseTest`, `ApplyMealPlanDayToDiaryUseCaseTest`.
  - `:mobile:shared:allTests` — **BUILD SUCCESSFUL** (171 тест пройден, 0 пропущено, 0 ошибок).
  - `:mobile:androidApp:compileDebugKotlin` — **BUILD SUCCESSFUL**.

### KAN-16 / T-076 — Бизнес-логика каталога рецептов, форков, меню тренера, Entitlements и gRPC-эндпоинтов в :backend:recipe-ai-service (2026-11-24)

- **Активные Skills:** `spring-grpc-service`, `testing-and-verification`. Роль: `implementer`.
- **Ветка:** `feature/KAN-16-Coach-premissions-recipebd`, коммит: `feat(recipe): T-076 implement recipe catalog, fork, meal plan and entitlements access services`.
- **Реализованные сервисы и репозитории:**
  - `KeysetPagination`, `RecipeSearchRepository`, `MealPlanSearchRepository`:
    - Высокопроизводительная Keyset-пагинация без `OFFSET` по составному ключу `(title, id)` с декодированием/кодированием Base64 курсора.
    - Фильтрация с учетом видимости и прав доступа: публичные сущности + авторские сущности текущего пользователя + выданные доступы через `AccessGrant`.
  - `RecipeCatalogService`:
    - Keyset-поиск, получение, создание, обновление, удаление рецептов.
    - Серверный расчет пищевой ценности на 100 г при создании и обновлении с каскадным пересчетом ингредиентов (`BigDecimal.HALF_UP`, нулевая безопасность).
    - `forkRecipe`: глубокое копирование ингредиентов, привязка `origin_recipe_id`, выставление `author_type = USER / COACH`, `author_id = currentUserId`, `visibility = PRIVATE`.
    - Проверка прав на запись (только автор или `ROLE_ADMIN`).
    - Публикация рецепта (`publishRecipe`) с обновлением видимости.
  - `MealPlanService`:
    - Keyset-поиск и сохранение планов питания с расчетом суточных средних КБЖУ.
    - Проверка прав (Entitlements): список блюд (`items`) отдается, если план `FREE && PUBLIC`, пользователь является автором плана, либо имеет активный (не истекший) `AccessGrant`. Иначе отдается превью без блюд (`canViewItems = false`).
    - Расчет региональной стоимости блюд через `CatalogClient.getAveragePrices` строго вне `@Transactional`.
  - `CoachingAccessService`:
    - Приглашение учеников (`inviteStudent`), подтверждение инвайтов (`respondCoachInvite`), получение списков связей тренера и подопечного (`listCoachLinks`).
    - Выдача и отзыв доступов (`grantAccess`, `revokeAccess`), проверка прав тренера на просмотр дневника (`checkCoachAccess`).
  - `AuthContext` & `RecipeIdentityInterceptor`:
    - Извлечение `userId` и ролей из gRPC Metadata `x-user-id` и `x-roles`, проброс контекста в корутины.
  - `RedisMenuStore`:
    - Кэширование публичных рецептов (`readCachedRecipe`, `writeCachedRecipe`, `evictCachedRecipe`) через Base64-сериализацию Protobuf без проблем совместимости.
  - `RecipeGrpcService`:
    - Реализация всех 18 RPC-методов `RecipeAiServiceCoroutineImplBase` с выполнением в `withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging))`.
    - Инвалидация кэша в Redis при обновлении, удалении и публикации рецептов.
    - Маппинг исключений в стандартные gRPC Status (`PERMISSION_DENIED`, `NOT_FOUND`, `INVALID_ARGUMENT`, `INTERNAL`).
- **Интеграционное тестирование и верификация:**
  - `RecipeCatalogGrpcIntegrationTest` (PostgreSQL 17 + Redis 7 Testcontainers):
    - Поиск рецептов с keyset-пагинацией.
    - Создание рецепта и автоматический расчет пищевой ценности на 100 г.
    - Форк системного рецепта с изоляцией от оригинала и возможностью независимой модификации.
    - Публикация рецепта владельцем и проверка доступности для других пользователей.
  - `CoachingAccessIntegrationTest` (PostgreSQL 17 + Redis 7 Testcontainers):
    - Полный цикл связи тренер-ученик (инвайт, принятие, проверка прав на просмотр дневника).
    - Защита платного плана питания: отказ в доступе к блюдам до выдачи гранта, открытие полного доступа после выдачи `AccessGrant`.
  - Результаты:
    - `:backend:recipe-ai-service:check` — **BUILD SUCCESSFUL** (все 22 теста пройдены, 0 сбоев).
    - `ktlintCheck` — **BUILD SUCCESSFUL** (0 ошибок стиля кода).

### KAN-16 / T-075 — Расширение Protobuf-контракта recipe.proto каталогом рецептов, меню и правами тренера (2026-11-24)

- **Активные Skills:** `contracts-proto-and-avro`, `testing-and-verification`. Роль: `implementer`.
- **Ветка:** `feature/KAN-16-Coach-premissions-recipebd`, коммит: `0ccc923` (`feat(proto): T-075 expand recipe.proto with recipe catalog, meal plans and coaching access RPCs`).
- **Контракт proto/nutritionist/recipe/v1/recipe.proto:**
  - 100% обратная совместимость: номера и типы всех существующих полей в `GenerateWeekMenu*` и `GenerateMealPlan*` сохранены без изменений.
  - Добавлены перечисления (Enums):
    - `AuthorType` (`AUTHOR_TYPE_UNSPECIFIED = 0`, `AUTHOR_TYPE_SYSTEM = 1`, `AUTHOR_TYPE_USER = 2`, `AUTHOR_TYPE_COACH = 3`, `AUTHOR_TYPE_AI = 4`)
    - `ContentVisibility` (`CONTENT_VISIBILITY_UNSPECIFIED = 0`, `CONTENT_VISIBILITY_PRIVATE = 1`, `CONTENT_VISIBILITY_LINK_ONLY = 2`, `CONTENT_VISIBILITY_PUBLIC = 3`)
    - `AccessMode` (`ACCESS_MODE_UNSPECIFIED = 0`, `ACCESS_MODE_FREE = 1`, `ACCESS_MODE_COACH_ASSIGNED = 2`, `ACCESS_MODE_PAID = 3`)
    - `GrantSource` (`GRANT_SOURCE_UNSPECIFIED = 0`, `GRANT_SOURCE_COACH_GRANT = 1`, `GRANT_SOURCE_PURCHASE = 2`, `GRANT_SOURCE_SUBSCRIPTION = 3`)
    - `CoachLinkStatus` (`COACH_LINK_STATUS_UNSPECIFIED = 0`, `COACH_LINK_STATUS_PENDING = 1`, `COACH_LINK_STATUS_ACTIVE = 2`, `COACH_LINK_STATUS_REVOKED = 3`)
    - `PlanItemType` (`PLAN_ITEM_TYPE_UNSPECIFIED = 0`, `PLAN_ITEM_TYPE_RECIPE = 1`, `PLAN_ITEM_TYPE_PRODUCT = 2`)
  - Добавлены сущности и DTO-сообщения (Messages):
    - `RecipeIngredientProto`, `RecipeProto`, `MealPlanItemProto`, `MealPlanTemplateProto`, `CoachStudentLinkProto`, `AccessGrantProto`.
  - Добавлены RPC-методы в `service RecipeAiService`:
    - Управление рецептами: `SearchRecipes`, `GetRecipe`, `CreateRecipe`, `UpdateRecipe`, `DeleteRecipe`, `ForkRecipe`, `PublishRecipe`.
    - Управление планами питания: `SearchMealPlans`, `GetMealPlan`, `CreateMealPlan`, `UpdateMealPlan`, `DeleteMealPlan`.
    - Тренерский доступ и Entitlements: `InviteStudent`, `RespondCoachInvite`, `ListCoachLinks`, `GrantAccess`, `RevokeAccess`, `CheckCoachAccess`.
  - Для каждого RPC добавлены строго типизированные `*Request` и `*Response` сообщения с комментариями по конвенциям Protobuf Style Guide.
- **Верификация и кодогенерация:**
  - `:backend:recipe-ai-service:generateProto` — успешно сгенерированы Java/Kotlin stubs.
  - `:backend:recipe-ai-service:compileKotlin` — успешно (старый сервис `RecipeGrpcService` совместим с расширенным `RecipeAiServiceCoroutineImplBase` благодаря default-имплементациям в gRPC).
  - `:mobile:shared:assemble` — успешно.
  - `:backend:recipe-ai-service:test` — 16 тестов (100% GREEN, включая `E2EFourServiceChainTest`, `RecipeFlowTest`, `RecipePersistenceIntegrationTest`).


### KAN-16 / T-074 — Подключение PostgreSQL recipe_db, Liquibase миграции и JPA персистентность в :backend:recipe-ai-service (2026-11-24)

- **Активные Skills:** `spring-grpc-service`, `testing-and-verification`. Роль: `implementer`.
- **Ветка:** `feature/KAN-16-Coach-premissions-recipebd`, коммит: `70f5490` (`feat(recipe): T-074 add PostgreSQL recipe_db, Liquibase migrations, system recipes seed and JPA persistence`).
- **Инфраструктура и конфигурация базы данных:**
  - `infrastructure/init-db.sql`: добавлено создание базы `CREATE DATABASE recipe_db;`.
  - `infrastructure/k8s/infra/01-postgres.yaml`: добавлено создание `recipe_db` и выдача прав в `01-init.sql`.
  - `infrastructure/k8s/apps/recipe-ai-service/configmap.yaml`: добавлены переменные `RECIPE_DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
  - `backend/recipe-ai-service/build.gradle.kts`: подключены плагины `kotlinJpa`, `allOpen` для сущностей и зависимости `libs.spring.starter.jpa`, `libs.postgresql.jdbc`, `libs.spring.starter.liquibase`, `libs.testcontainers.postgresql`.
  - `backend/recipe-ai-service/src/main/resources/application.yml`: настроен `datasource` (Hikari `RecipeAiServiceHikariPool`, max-pool-size: 10), `jpa` (`open-in-view: false`, `ddl-auto: "none"`), `liquibase` (`db.changelog-master.xml`), `management.endpoint.health.group.readiness.include` обновлен (`readinessState,redis,db`).
- **Liquibase миграции:**
  - `001-init-recipe-schema.xml`: расширение `pg_trgm`, таблицы `recipes`, `recipe_ingredients`, `meal_plans`, `meal_plan_items`, `coach_student_links`, `access_grants`, триграммный GIN-индекс на `recipes(title)`, внешние ключи с каскадным удалением и составные индексы.
  - `002-seed-system-recipes.xml`: сидирование 15 системных рецептов (`author_type='SYSTEM'`, `visibility='PUBLIC'`) с реальными ID продуктов из `initial_products.csv`.
- **JPA сущности и Spring Data JPA репозитории:**
  - Сущности: `RecipeEntity` (LAZY, `@Version`, каскадное управление ингредиентами), `RecipeIngredientEntity`, `MealPlanEntity` (LAZY, `@Version`), `MealPlanItemEntity`, `CoachStudentLinkEntity`, `AccessGrantEntity`. Все связи строго `FetchType.LAZY`, equals/hashCode по `id`.
  - Репозитории: `RecipeRepository` (`findWithIngredientsById`, `findByAuthorId`, `findByVisibility`, `findByAuthorType`), `RecipeIngredientRepository`, `MealPlanRepository` (`findWithItemsById`, `findByAuthorId`), `MealPlanItemRepository`, `CoachStudentLinkRepository`, `AccessGrantRepository`.
- **Верификация и тестирование:**
  - Создан базовый класс `AbstractPostgresIntegrationTest` с запуском PostgreSQL 17 alpine и Redis 7 Testcontainers.
  - Написан полный интеграционный тест `RecipePersistenceIntegrationTest` (проверка сидирования 15 рецептов, каскадного удаления ингредиентов, инкремента `@Version` при оптимистической блокировке, связей coach-student и прав доступа access_grants).
  - Успешно пройдены проверки:
    - `:backend:recipe-ai-service:test` — 16 тестов (100% GREEN, 0 skipped, 0 failed).
    - `:backend:recipe-ai-service:ktlintCheck` — успешно без замечаний.


### KAN-84 — Реализация KMP Domain и Data слоёв в :mobile:shared [T-066 — T-069] + Live E2E (2026-11-24)

- **Активные Skills:** `kmp-shared-feature`, `spring-grpc-service`, `contracts-proto-and-avro`, `testing-and-verification`. Роль: `implementer`.
- **[T-066] Rich Domain Models & Nutrition Calculator (`commonMain`):**
  - Созданы строгие доменные сущности с `@JvmInline value class` для всех идентификаторов: `MealRecordId`, `IdempotencyKey`, `MealPlanId`, `ProductId`, `StoreId`, `RecipeId`, `DiaryEntryId`, `FoodItemId`, `UserId`, `ProfileId`.
  - Реализованы чистые Value Objects: `NutritionFacts`, `Money` (с валидацией неотрицательности и точностью до копеек), `MealRecord`, `MealPlan`, `GeneratedMealItem`, `Product`, `Price`, `MealType`, `EntrySourceType`, `SyncStatus`, `TargetPeriod`.
  - Реализован чистый калькулятор питания `NutritionCalculator` (формула Миффлина — Сан-Жеора для BMR, TDEE с коэффициентом физической активности, дефицит/профицит калорий под цель, макронутриенты БЖУ) и `DailyNutritionBalanceCalculator`.
  - 100% покрытие unit-тестами в `commonTest` (Fakes, без MockK).
- **[T-067] UseCase генерации бюджетного меню (`commonMain`):**
  - Реализован `GenerateBudgetMenuUseCase` (`operator fun invoke(params): Flow<GenerateBudgetMenuState>`).
  - Fail-fast валидация входных параметров: калории > 0, бюджет > 0, корректный период (`DomainError.ValidationError`).
  - Реактивный поток состояний: `Validating` → `GeneratingPlan` (вызов `RecipeRepository.generateMealPlan`) → `CheckingPrices` (проверка цен в реальном времени через `CatalogRepository.searchProducts`) → `Ready` / `PriceWarning` / `Failed`.
  - Полный набор юнит-тестов с Turbine и Fake-репозиториями в `commonTest`.
- **[T-068] gRPC Клиенты & Data Sources (`androidMain`):**
  - Реализованы gRPC клиенты и Remote Data Sources для всех внешних сервисов:
    - `CatalogGrpcRemoteDataSource` (`AndroidCatalogGrpcServiceClient`) для `catalog-price-service:9192`.
    - `DiaryGrpcRemoteDataSource` (`AndroidDiaryGrpcServiceClient`) для `diary-service:9193`.
    - `RecipeGrpcRemoteDataSource` (`AndroidRecipeGrpcServiceClient`) для `recipe-ai-service:9194`.
  - Реализован `UnifiedGrpcClientInterceptor`:
    - Автоматическая инжекция `Authorization: Bearer <token>` через `AuthTokenProvider`.
    - Инжекция OpenTelemetry `traceparent: 00-<traceId>-<spanId>-01`.
    - Идемпотентность: проброс `idempotency-key` из `CallOptions` / `Context` / генерация UUID для мутирующих методов.
    - Поддержка `x-user-id` и `x-dev-bypass` для прямого межсервисного тестирования.
  - Реализован `GrpcErrorMapper` с маппингом gRPC кодов `Status.Code` на чистые доменные ошибки `DomainError` (`NetworkError`, `AuthenticationError`, `ValidationError`, `ConflictError`, `ServerError` и др.).
  - Реализованы Protobuf marshallers для Android среды без runtime кодогенерации протобафа.
- **[T-069] Offline-first Room KMP 2.8.x & Diary Repository:**
  - Создана таблица `meal_records` в `AppDatabase` с полями: `id`, `meal_type`, `recorded_at`, `source_type`, `source_id`, `name_snapshot`, `weight_grams`, `calories`, `proteins`, `fats`, `carbs`, `cost_kopecks`, `idempotency_key`, `sync_status`.
  - Экспортирована схема Room v2 (`mobile/shared/schemas/dev.andre.nutritionist.data.local.AppDatabase/2.json`).
  - Реализован `MealRecordDao` (Flow, CRUD, batch sync queries).
  - Реализован `MealRecordEntityMapper` (двусторонний чистый маппер Entity ↔ Domain).
  - Реализован `DiaryRepositoryImpl`:
    - Offline-first паттерн: локальная запись сохраняется со статусом `SyncStatus.PENDING`.
    - Немедленная попытка синхронизации в `diary-service`. При успехе статус обновляется на `SyncStatus.SYNCED`.
    - При сбое сети статус остаётся `SyncStatus.PENDING` для отложенной синхронизации через `SyncDiaryWorker` с сохранением исходного `IdempotencyKey`.
- **[Live Microservices E2E Verification]:**
  - Запущены все 4 локальных микросервиса: `catalog-price-service` (9192), `diary-service` (9193), `recipe-ai-service` (9194), `profile-service` (9090) с PostgreSQL (5433), Redis (6379), Kafka (9092) и Schema Registry (8081).
  - Автоматизированный сквозной тест `LiveMicroservicesE2ETest` подтвердил 100% работоспособность цепочки:
    1. Поиск продуктов в каталоге (`catalog-price-service:9192`): найдены 4 позиции куриного мяса из PostgreSQL.
    2. Генерация сбалансированного блюда (`recipe-ai-service:9194`): получен `PlanId` с блюдом «Сбалансированное блюдо: Авокадо Хасс спелое» (625.6 ккал, 258.53 RUB).
    3. Offline-first сохранение в Room DB (статус `PENDING`) → удалённая отправка в `diary-service:9193` → обновление в Room до `SYNCED`.
    4. Transactional Outbox в `diary-service` опубликовал событие `diary.entry.created` в Kafka с Avro-схемой.
    5. Проверка идемпотентности: повторный gRPC-запрос с тем же `IdempotencyKey` вернул кэшированную запись через `IdempotencyService` (`Idempotency response replayed`) без дублирования.
    6. Запрос суточных записей `GetDailyEntries` подтвердил наличие сохранённой записи и корректный подсчёт суточных нутриентов.
- **Тестовый статус:** 158 из 158 тестов успешно пройдены (100% GREEN) в `:mobile:shared:allTests`.

### KAN-76 — Генерация рецептов и планов питания (Блоки 1-4 завершены) (2026-11-24)

- Активны `contracts-proto-and-avro`, `spring-grpc-service`, `testing-and-verification`. Роль: `implementer`.
- **БЛОК 1 (Контракт):**
  - Обновлён `proto/nutritionist/recipe/v1/recipe.proto`: добавлен RPC `GenerateMealPlan(GenerateMealPlanRequest) returns (GenerateMealPlanResponse)`.
  - Добавлены enum `TargetPeriod` (DAY, BREAKFAST, LUNCH, DINNER, SNACK), типы `TargetNutrition`, `GenerateMealPlanRequest`, `GenerateMealPlanResponse`, `GeneratedMeal`.
  - В enum `MealType` добавлен `MEAL_TYPE_SNACK = 4`.
  - Проверена генерация gRPC stubs в `:shared:contracts` и `:backend:recipe-ai-service`.
- **БЛОК 2 (Чистая архитектура :backend:recipe-ai-service):**
  - Произведён переход от плоской структуры пакета `dev.andre.nutritionist.recipe.*` к чистой архитектуре:
    - `config/` (`RecipeProperties`, `RecipeConfiguration`, `GrpcBootstrapConfiguration`, `RecipeWarmup`)
    - `domain/model/` (`TargetPeriod`, `GeneratedRecipe`, `MenuRequest`, `UserId`, `ProductId`, `RequestContext`)
    - `domain/service/` (`RecipeMenuService`, `ResilientMenuGenerator`)
    - `domain/validator/` (`MenuValidator`)
    - `infrastructure/grpc/` (`RecipeGrpcService`, `RecipeIdentityInterceptor`)
    - `infrastructure/grpc/client/` (`CatalogClient`)
    - `infrastructure/grpc/mapper/` (`RecipeDtoMapper`)
    - `infrastructure/llm/` (`LlmClient`, `GeminiLlmClient`, `MenuExceptions`)
    - `infrastructure/llm/prompt/` (`MealPlanPromptBuilder`)
    - `infrastructure/redis/` (`RedisMenuStore`)
  - Гибкий генератор промпта `MealPlanPromptBuilder` поддерживает как 7-дневное меню, так и произвольный период/приём пищи (день, завтрак, обед, ужин, перекус) с учётом `user_note`, бюджета и предпочтений.
  - В `RedisMenuStore` добавлена сериализация `GeneratedMealPlan` через `JsonMapper`.
  - Все 10 тестов `backend:recipe-ai-service:test` (включая стриминг, WireMock LLM, Resilience4j Circuit Breaker/Retry, Redis квоты и fallback) успешно пройдены.
- **БЛОК 3 (K8s манифесты и Helmfile):**
  - Созданы полные манифесты для `recipe-ai-service` в `infrastructure/k8s/apps/recipe-ai-service/`: `configmap.yaml` (порты 9194/8084, `catalog-price-service:9192`), `secret.yaml` (`RECIPE_LLM_API_KEY`), `service.yaml`, `deployment.yaml` (non-root UID 10001, readiness/liveness probes), `grpc-route.yaml` (`com.budgetntr.recipe.v1.RecipeAiService`).
  - Созданы полные манифесты для `diary-service` в `infrastructure/k8s/apps/diary-service/`: `configmap.yaml` (порты 9193/8083, `diary_db`, Kafka, Redis), `secret.yaml` (`DB_USERNAME`, `DB_PASSWORD`), `service.yaml`, `deployment.yaml` (non-root UID 10001), `grpc-route.yaml` (`com.budgetntr.diary.v1.DiaryService`).
  - Проверено подключение в `infrastructure/k8s/charts/helmfile.yaml` (разделы 7 `diary-service` и 8 `recipe-ai-service`).
- **БЛОК 4 (Сквозная цепочка E2E из 4 сервисов):**
  - Подтверждена локальная инфраструктура (PostgreSQL 5433 с базами `profile_db`, `catalog_db` с 110 продуктами, `diary_db`, Redis 6379, Kafka 9092, Schema Registry 8081).
  - Собраны `bootJar` для всех 4 сервисов: `profile-service.jar`, `catalog-price-service.jar`, `diary-service.jar`, `recipe-ai-service.jar`.
  - Реализован и успешно выполнен автоматизированный сквозной тест `E2EFourServiceChainTest` в `:backend:recipe-ai-service`, проверивший выполнение полной цепочки:
    - **Шаг 1: `UpdateProfile` (ProfileService, 9090):** создан профиль `usr_test_flow` (ккал=2200, бюджет=4500.0 RUB).
    - **Шаг 2: `SearchProducts` (CatalogService, 9192):** найден продукт «Куриное филе грудки охл» (`20000000-0000-0000-0000-000000000057`, 113 ккал/100г, 23.6г белка).
    - **Шаг 3: `GenerateMealPlan` (RecipeAiService, 9194):** сгенерирован план ужина `TARGET_PERIOD_DINNER` с блюдом «Куриное филе на пару с отварным рисом» (626.5 ккал, 66г белка, 10.25 руб).
    - **Шаг 4: `AddEntry` + `GetDailyEntries` (DiaryService, 9193):** блюдо сохранено в дневник питания с `ENTRY_SOURCE_TYPE_RECIPE` и `source_id = plan_id`; вызов `GetDailyEntries` подтвердил наличие записи с точным расчётом суточных итогов (626 ккал, 66.0г белка).

### T-056 — итоги GetDay по приёмам пищи

- Активны spring-grpc-service, contracts-proto-and-avro, testing-and-verification; роль implementer.
- Реализованы чистый агрегатор сохранённых нутриентов, четыре группы BREAKFAST/LUNCH/DINNER/SNACK и совместимое поле meal_totals в GetDailyEntries. Одна выборка дня, без повторного расчёта порций и запросов каталога.
- Созданы MealNutritionSummary, DiaryNutritionAggregator и три целевых тестовых класса. Обновлены DailyEntries, DiaryService, gRPC-маппер/обработчик, diary.proto и README. БЖУ суммируются сотыми долями грамма; переполнение итогов возвращает RESOURCE_EXHAUSTED.

### T-055 — consumer регистрации пользователей

- Реализован Kafka-consumer регистрации пользователей UserRegisteredEvent с инициализацией дневника и защитой от повторов.
- Подключена миграция 003-diary-registration-consumer.yaml и интеграционные тесты.

### T-053 — оформление коммитов после восстановления файла

- По поручению пользователя изменения разделены на четыре коммита: общий outbox; интеграция Diary и тесты; Docker; документация.
- Первый коммит Avro сохранён: 4aa9ff8. Потерянный backend/outbox/build.gradle.kts восстановлен пользователем из 38189db после конфликта инструмента IDE. Остальные изменения T-053 сохранились.
- PASS после восстановления: :backend:diary-service:compileKotlin и :backend:outbox:ktlintCheck (offline); состав индекса и git diff --cached --check проверены перед каждым коммитом.
- Созданы 91dcf11 (общий outbox), da45026 (Diary и тесты), a28325d (Docker). README, архитектурное описание и этот журнал включены в заключительный коммит документации. Сообщения на русском без служебных префиксов. Push не выполнялся.

### T-053 — реализация завершена: автоматическая доставка и порядок

- Активны spring-grpc-service и testing-and-verification; роль implementer. Поручено завершить всю T-053.
- План: общий JDBC store/processor, session advisory lock одного отправителя, sequence и сериализация append одного пользователя, канонический UUID key, ACK/PUBLISHED и retry/backoff/FAILED, интеграционные проверки, Docker и документация.
- Существующие принятые изменения сохраняются; shared/contracts/build.gradle.kts не переписывается. Проверки используют временные контейнеры.
- Созданы общий JDBC store/processor/settings, Diary scheduler и миграция 005; append использует transaction advisory lock пользователя и канонический UUID key. PASS: compileKotlin и расширенный набор Diary с Kafka/Schema Registry outage, ACK/replay, конкурентными appends и обработчиками.
- Усилен случай потери DB-соединения: стабильный transactional.id с suffix cache=1, reset producer под advisory lock и Kafka fencing; потребители read_committed. По независимому ревью добавлен verifyOwnership callback внутри Kafka-транзакции после init/begin до send: исходное соединение и точный advisory lock проверяются через pg_locks. Тест задержки старого владельца до Kafka init и takeover PASS; замечание P1 закрыто ревьюером.
- PASS: все 24 Diary теста (17 publishing + 6 T-052 + 1 JPA), 0 failures/errors/skipped; bootJar. Проверены миграция существующего backlog 004→005, FIFO, canonical key, traceparent, scheduler, реальные Kafka/Schema Registry outages, ACK gate, конкуренция обработчиков/appends, backoff/FAILED/manual retry, повтор после ACK/DB failure и takeover/fencing. Финальное форматирование нового кода завершено: общий outbox ktlint PASS, в изменённых Diary-файлах нарушений нет. Общий Diary lint остаётся FAIL на прежних ID/JPA/generated файлах.
- BLOCKED (проверка среды): полная Docker-сборка из исходников дошла до Gradle, но Maven Central и Google Maven не ответили при загрузке зависимостей (repo.maven.apache.org:443 и dl.google.com:443 failed to respond). Зависимости/репозитории не менялись ради обхода. COPY backend/shared/contracts/proto и передача контекста выполнены успешно; сборка исходников внутри Docker требует доступных репозиториев.
- PASS: runtime-образ fitassistant/diary-service:t053-runtime-check собран из локального проверенного bootJar без Gradle/network. Временный Dockerfile находится в игнорируемом backend/diary-service/build/t053-runtime/. Проверен запуск образа с отдельной временной PostgreSQL 17: Started DiaryApplicationKt, gRPC 9093, UID 10001, миграция 005 подтверждена запросом databasechangelog. Эта проверка не подменяет полную Docker-сборку из исходников; Kafka-доставка отдельно подтверждена 24 интеграционными тестами.
- Временные runtime-контейнеры приложения/PostgreSQL, их анонимные volumes и отдельная сеть удалены. Рабочие данные не затрагивались. Независимое ревью после исправления гонки владения — без блокирующих замечаний. Коммитов/push нет.
- T-053 реализована целиком; незавершённых исходников по задаче нет. Эксплуатационные условия: общий transaction-id-prefix всем репликам одной очереди и отдельный для независимых очередей; потребители read_committed; неизменное число партиций; при первом обновлении остановить старые writers на время миграции. Гарантия at least once, дедупликация потребителем по (userId, mealId).

### T-053 — этап 1 завершён: общий контракт и Kafka-публикатор

- Skills: spring-grpc-service, testing-and-verification; diagnose-build-failure для сбоев проверок. Роль implementer, независимое read-only ревью test-reviewer завершено без оставшихся блокирующих замечаний.
- Реализованы пункты 1–4 раздела «Логика и смысл»: общий JVM-модуль :backend:outbox с OutboxMessage/OutboxMessageId и OutboxPublisher, Diary-публикатор и явная конфигурация producer. Сохранение T-052 и Avro-схема не изменены.
- Публикатор восстанавливает CaloriesConsumedEvent из сохранённого бинарного Avro; KafkaAvroSerializer добавляет Confluent-обрамление/schema ID вне транзакции БД. Topic diary.entry.created, key — сохранённый partition_key, traceparent — сохранённый входящий заголовок. В producer включены acks=all и enable.idempotence=true.
- Вызов внутри DB-транзакции, неправильный topic/user key, обрезанный payload и лишние байты отклоняются до отправки. UUID сравниваются по значению: uppercase UUID допустим, исходный partition_key при отправке сохраняется.
- Публикация вызывается явно; даже после успешного future строка остаётся PENDING, статусом будет управлять следующий этап. Автоматического запуска нет. T-053 целиком не закрыта.
- PASS: compileKotlin; финальный полный :backend:diary-service:test — 12/12, 0 failures/errors/skipped (5 новых Kafka-тестов, 6 T-052 и 1 JPA). Проверены реальный RPC, идемпотентный повтор, чтение specific Avro из Kafka, user key/traceparent, отсутствие заголовка, сохранённый снимок после удаления meal, uppercase UUID и отказы до отправки.
- PASS: :backend:outbox:ktlintCheck, форматирование новых Diary-файлов (нет замечаний в свежих отчётах ktlint), Android Studio Gradle Sync, IDE inspections без ошибок, git diff --check.
- FAIL: общий :backend:diary-service:ktlintCheck — прежние четыре ID-модели, порядок импортов DiaryEntryPersistenceTest и generated protobuf. Линтеры и старые файлы не менялись.
- Диагностика: Testcontainers отклонил tag@digest при проверке совместимости образа; исправлено на digest-only того же образа из Compose. Отдельный запуск Diary lint также падал в :shared:contracts:processResources с ConcurrentModificationException при разрешении SourceDirectorySet; полный запуск test затем lint проходит этот шаг. Пользовательский shared/contracts/build.gradle.kts не менялся. Финальная команда: ./gradlew -PbackendOnly=true :backend:diary-service:test :backend:outbox:ktlintCheck :backend:diary-service:ktlintCheck --continue --offline --console=plain --no-daemon --max-workers=1; exit 1 только из-за прежних lint-нарушений.
- Проверки выполнялись на временных PostgreSQL/Kafka/Schema Registry; рабочие данные и внешние сервисы не затрагивались. Коммитов нет.
- Следующая атомарная задача (этап 2): общий JDBC store и процессор очереди, фиксация PUBLISHED после Kafka ACK, повторы/backoff, порядок событий пользователя и координация обработчиков. Учесть каноническое представление user key при проектировании строгого порядка; затем сквозная проверка рестартов/ошибок, поставка Docker и закрытие T-053.

### T-052 — атомарное сохранение записи и outbox (2026-11-15)

- Skills: spring-grpc-service, testing-and-verification; роль implementer. Работа ведётся в исходной ветке T-052, consumer T-055 не переносится.
- В DiaryService.addEntry добавлено сохранение CaloriesConsumedEvent в outbox_messages в транзакции IdempotencyService: запись, событие и ответ фиксируются вместе. Проходной UseCase не создаётся; сетевых клиентов в операции нет.
- Добавлены DiaryOutboxRepository и миграция 004-diary-outbox.yaml, подключён :shared:contracts. Avro-схема не меняется. Событие содержит сохранённые mealId/userId/calories/recordedAt/mealType; partition_key=userId, traceparent сохраняется из metadata. Статус PENDING; публикация относится к T-053.
- PASS: ./gradlew :backend:diary-service:test --offline --console=plain — BUILD SUCCESSFUL, 7/7 тестов (6 новых gRPC/Spring/PostgreSQL + существующий JPA), 0 failures/errors/skipped. Проверены чтение Avro из BYTEA, traceparent, PENDING/partition_key, повтор, гонка 8 вызовов, конфликт payload/ID, откат при ошибке outbox и после обеих вставок, повтор после отката и валидация.
- FAIL: общий ktlintCheck — прежние ID-модели, generated protobuf и порядок импортов DiaryEntryPersistenceTest; форматирование нового теста исправлено точечно. Проверки выполнялись только на временной PostgreSQL; рабочие данные, Kafka и Schema Registry не затрагивались. git diff --check — без ошибок. Коммиты и переключения веток не выполнялись.

### T-055 / KAN-73 — входящий consumer регистрации (2026-11-15)

- Активные Skills: `spring-grpc-service`, `contracts-proto-and-avro`, `testing-and-verification`; `twg`/`twg-jira` использованы только для чтения Jira. Роль: `implementer`.
- Прочитаны KAN-70/71/72/73/75; T-055 требует UserRegisteredEvent → инициализация дневника, с processed_events, retry и DLT по шаблону T-054. Исходящий CaloriesConsumedEvent относится к отдельным T-052/T-053.
- Kafka Profile изучена в develop c86dfe0: user.registered, Avro UserRegisteredEvent; текущая T-052 остаётся на abcfe1e, слияние и коммиты не выполняются. Схема Avro не меняется.
- Реализованы consumer/конфигурация и транзакционная инициализация: diary_users хранит userId/registered_at/initialized_at без email и фиктивных приёмов пищи. processed_events и diary_users записываются вместе; ACK выполняется после коммита.
- В схеме регистрации нет отдельного eventId: постоянный семантический ключ UserRegisteredEvent:userId гарантирует одну инициализацию при повторной доставке на новом offset. Записи дневника не зависят от скорости доставки регистрации и не удаляются при повторе.
- Добавлены миграция 003-diary-registration-consumer.yaml, зависимость от :shared:contracts, ограниченные retry и user.registered.DLT. DLT поддерживает исходные байты ошибки десериализации и различные SpecificRecord через RecordNameStrategy; неуспешная отправка в DLT не подтверждает исходный offset. Существующий gRPC integration test сохранён, Kafka в нём явно отключена.
- PASS: offline Gradle test — все 26 тестов diary-service, 0 failures/errors/skipped (9 новых Kafka + 9 gRPC + 7 идемпотентности + 1 JPA). Использованы временные PostgreSQL 17, Kafka 4.2.2 и Schema Registry 8.2.4. Проверены wire Avro, ACK после коммита, повтор на новом offset, гонка 8 доставок, rollback/3 попытки, DLT валидации/неверного Avro/другого типа Avro, продолжение партиции, сохранение прежних записей и gRPC health SERVING.
- FAIL: общий ktlintCheck — прежнее форматирование четырёх ID-моделей, DiaryEntryPersistenceTest и generated protobuf Kotlin. В новых файлах ошибок нет; линтер и generated-код не менялись. git diff --check — без ошибок.
- При диагностике тестового стенда исправлены JDBC-передача Instant через Timestamp и подключение Schema Registry к отдельной сети Testcontainers: имя registrationNetwork исключает затенение свойства GenericContainer.network. diagnose-build-failure использован для диагностики и точечного исправления форматирования.
- Рабочие данные и Compose-стенд не менялись; Profile producer отдельно не запускался, совместимость подтверждена чтением develop и отправкой реального Avro-контракта в тестовый брокер. T-052/T-053, общий исходящий outbox, изменения Jira, слияние веток и коммиты не выполнялись.

### Упрощение типов и подключение Diary RPC (2026-11-15)

- Активные Skills: `spring-grpc-service`, `testing-and-verification`; `diagnose-build-failure` использован для исправления тестового lifecycle и форматирования. Роль: `implementer`.
- Scope/StoredResponse вложены в IdempotencyStore; конфликт вложен в IdempotencyService. Ссылки обновлены; четыре отдельных файла удалены, EnableScheduling перенесён в GrpcBootstrapConfiguration.
- Реализованы DiaryGrpcService/DiaryService, репозиторий и JPA-хранилище, мапперы/валидация, внедряемые диспетчеры и DiaryRequestContextInterceptor.
- RPC AddEntry/DeleteMeal используют execute с одной JDBC/JPA-транзакцией. Чтение GetDailyEntries/GetRange ограничено текущим пользователем, даты включительны в указанном часовом поясе, запрос БД использует полуоткрытый интервал.
- Доверенная identity берётся только из x-user-id, выставленного Envoy после JWT-проверки согласно архитектуре; backend должен быть изолирован от прямых внешних запросов. JWT внутри приложения не декодируется и не проверяется.
- PASS: compileKotlin/compileTestKotlin offline; все 17 тестов diary-service (9 сетевых gRPC/Spring/PostgreSQL, 7 идемпотентности, 1 JPA/Liquibase), 0 failures/errors. Проверены регистрация глобальных interceptor, четыре RPC, повтор/конфликт, гонка 8 запросов, rollback, ownership, снимки стоимости, границы DST и запуск зарегистрированной задачи очистки. Отклонённый тест логирующего interceptor не восстанавливался.
- Исправлены только тестовые ошибки: ручное закрытие Spring-контекста мешало afterAll listeners; проверка ScheduledMethodRunnable не учитывала OutcomeTrackingRunnable Spring 7. Контекст теперь закрывает Spring, задача проверяется через зарегистрированный Runnable и удаление просроченного ответа.
- FAIL: общий ktlintCheck — прежнее форматирование четырёх ID-моделей и DiaryEntryPersistenceTest, generated protobuf Kotlin. В изменённых участках ошибок нет; настройки линтера и generated-код не менялись. git diff --check — без ошибок.
- Цепочка внутри diary-service подключена; рабочая БД не изменялась. Envoy/JWT-периметр и клиентская мобильная синхронизация не проверялись; Kafka/outbox не реализовывались в этой задаче.

### Второй этап T-051 — PostgreSQL-идемпотентность (2026-11-15)

- Активные Skills: `spring-grpc-service`, `testing-and-verification`; роль: `implementer`.
- Реализованы IdempotencyService/Store и PostgreSQL-адаптер, таблица ключей и очистка по TTL. Отклонённый пользователем тест логирующего interceptor не восстановлен.
- Ключ ограничен пользователем и RPC; хэш SHA-256 рассчитывается по переданным байтам запроса. Исполнитель передаёт синхронное действие, возвращающее сериализованный ответ; всё действие, ключ и ответ фиксируются одной новой READ COMMITTED транзакцией на том же DataSource. Действие должно содержать только локальные операции БД и формирование ответа, без переключения потока и внешних вызовов.
- Store использует INSERT ON CONFLICT DO NOTHING и SELECT FOR UPDATE; конкурентная очистка обрабатывается повторной попыткой захвата. TTL по умолчанию 24 часа с начала транзакции; до фактической очистки ответ продолжает возвращаться. Очистка раз в час, порциями до 1000, настройки через diary.idempotency.*.
- Очистка удаляет только результаты из idempotency_keys; постоянная уникальность (user_id, idempotency_key) в diary_entries сохраняется. После очистки старый ответ не восстанавливается автоматически: обработку такой ситуации предстоит подключить на уровне операции дневника.
- DiaryGrpcService и DiaryService остаются пустыми: gRPC-проверка обязательного ключа, авторизация, сериализация, отображение IdempotencyConflictException в gRPC-статус и вызов execute из AddEntry/DeleteMeal ещё не подключены. Само добавление/удаление дневника не входит в этот этап; T-051 целиком пока не завершена.
- PASS: offline Gradle compileKotlin/compileTestKotlin; 7 новых интеграционных тестов и существующий DiaryEntryPersistenceTest — 8/8, 0 failures/errors, PostgreSQL 17 Testcontainers. Проверены миграции, повтор, конфликт тела, гонка 8 запросов, откат, разделение пользователя/RPC, TTL и пустой успешный ответ.
- FAIL: общий ktlintCheck — ранее существовавшие исходники и generated protobuf; в новых файлах замечаний нет. Линтер и зависимости не менялись. git diff --check — без ошибок.
- Рабочая PostgreSQL не изменялась. Полный запуск Spring, регистрация расписания в работающем приложении и сквозной gRPC-вызов не проверялись.

### Первый этап T-051 — перехват и логирование (2026-11-15)

- Активные Skills: `spring-grpc-service`, `testing-and-verification`; `diagnose-build-failure` подключён для разбора ktlint. Роль: `implementer`.
- Создан `DiaryMutationLoggingInterceptor`: глобальная регистрация Spring gRPC, выбор только AddEntry/DeleteMeal по сгенерированным дескрипторам, логирование начала/наличия ключа и итогового статуса. Тела и значения ключей не логируются.
- Это только логирование: вызовы проходят дальше без проверки обязательности ключа и без защиты от повторов. Store, таблица ключей, хэш, транзакционная координация и TTL пока не реализованы; DiaryGrpcService/DiaryService остаются без изменений.
- Добавлены четыре теста in-process gRPC: AddEntry с metadata, DeleteMeal без ключа, чтение без логов мутаций, сохранение статуса ошибки.
- PASS: compileKotlin, compileTestKotlin и четыре теста DiaryMutationLoggingInterceptorTest (0 failures/errors) через offline Gradle.
- FAIL: общий ktlintCheck — существующие заглушки/форматирование, порядок импортов DiaryEntryPersistenceTest и generated protobuf Kotlin. Ошибка цепочки вызовов в новом тесте исправлена; существующие исходники и настройки линтера не изменены.
- Полный запуск Spring, рабочая БД и механизмы повторов/TTL не проверялись.

### Дополнение контракта Diary — T-050 (2026-11-15)

- Активные Skills: `contracts-proto-and-avro`, `testing-and-verification` (проверка генерации и компиляции); роль: `implementer`.
- Добавлены RPC `DeleteMeal` и `GetRange` с отдельными request/response; существующие RPC, сообщения и номера полей сохранены.
- `DeleteMealRequest.entry_id` задаёт удаляемую запись; успешный ответ пустой. Ключ удаления передаётся через metadata `idempotency-key`, пользователь определяется авторизацией.
- `GetRangeRequest`: `start_date` и `end_date` в формате YYYY-MM-DD, обе даты включительно в `time_zone`; ответ содержит список `DiaryEntryProto`. Эти правила должен соблюдать будущий серверный обработчик.
- PASS: `./gradlew :backend:diary-service:generateProto :backend:diary-service:compileKotlin :backend:diary-service:compileJava --offline --console=plain` — BUILD SUCCESSFUL; `git diff --check` — без ошибок. Diff контракта содержит только добавления.
- Buf отсутствует в PATH: `buf lint` и `buf breaking` не выполнены. Серверные обработчики и runtime не проверялись и не изменялись.

### Доменные модели Diary (2026-11-14)

- Активные Skills: `spring-grpc-service`, `testing-and-verification`; роль: `implementer`.
- Готово: добавлены `DiaryEntry`, `NutritionSummary`, `Money`, `CostSnapshot`, `AddEntryCommand`, `DailyEntries` и enum `Currency.RUB` в domain/model.
- Новые модели используют существующие typed IDs, деньги в целых копейках, `kotlin.time.Instant`, nullable-снимок неизвестной стоимости; без новых зависимостей, JPA/Protobuf-аннотаций и java-импортов.
- PASS: `./gradlew :backend:diary-service:compileKotlin :backend:diary-service:test --tests dev.andre.nutritionist.diary.persistence.DiaryEntryPersistenceTest --offline --console=plain` — BUILD SUCCESSFUL. Новые модели скомпилированы, существующий тест хранения прошёл на временной PostgreSQL.
- Добавлены только модели данных; валидация, расчёт итогов, мапперы и подключение моделей к сервису пока не реализованы.

### Начальная миграция Diary (2026-11-14)

- Активные Skills: `spring-grpc-service`, `testing-and-verification`; роль: `implementer`.
- Готово: пустой XML удалён; создан `001-init-diary-table.yaml` с таблицей `diary_entries` по README, уникальным ключом повтора, CHECK стоимости, индексом пользователя/времени и rollback удаления таблицы.
- Миграция подключена через master; `application.yml` уже настроен корректно. Outbox и обработчики не входят в эту задачу.
- PASS: временный JUnit-тест через Gradle init-script и Testcontainers PostgreSQL 17 — BUILD SUCCESSFUL. Проверены Liquibase validate/update, повторный update без потери трёх записей (один changeset в DATABASECHANGELOG), уникальность, неизвестная/нулевая стоимость, CHECK отрицательной суммы и неполной/нулевой базы цены, индекс recorded_at DESC.
- Исправлен временный тест: после ожидаемых SQL-ошибок включён autocommit. В проект постоянные тесты/зависимости не добавлялись; проверочные файлы находятся в `/private/tmp/fitassistant-diary-liquibase-check*`, отчёт — `backend/diary-service/build/reports/tests/test/`.
- Рабочая БД и контейнеры проекта не изменялись; полный старт сервиса не проверялся. PostgreSQL использует named volume `postgres_data` по текущему Compose.

### Gradle-генерация Diary (2026-11-14)

- Активные Skills: `contracts-proto-and-avro`, `testing-and-verification`; роль: `implementer`.
- Готово: подключены Protobuf-плагин, protoc, Java/Kotlin messages, gRPC Java/Kotlin и runtime-зависимости через каталог версий. Android Studio Gradle Sync — успешно.
- Источник: `proto/nutritionist/diary/v1/**`; вывод: `backend/diary-service/build/generated/sources/proto/`.
- PASS: `./gradlew :backend:diary-service:generateProto :backend:diary-service:compileKotlin :backend:diary-service:compileJava :backend:diary-service:compileTestKotlin --offline --console=plain` — BUILD SUCCESSFUL. Kotlin/Java скомпилированы; тестовых исходников нет (`NO-SOURCE`).
- Тесты и запуск сервиса не выполнялись; Buf по-прежнему отсутствует.

### Контракт дневника (2026-11-14)

- Активные Skills: `contracts-proto-and-avro`, `testing-and-verification`; роль: `implementer`.
- Готово: `proto/nutritionist/diary/v1/diary.proto` заполнен по `backend/diary-service/README.md`.
- Добавлены unary RPC `AddEntry`, `GetDailyEntries`, сообщения записи, КБЖУ и стоимости; исправлен outer class на `DiaryOuterClass`.
- Сохранены поля и номера из проектной спецификации. Пользователь определяется авторизацией, ключ повтора передаётся в metadata `idempotency-key`; служебная outbox-таблица не публикуется в API.
- PASS: локальный `protoc` проверил Diary и Profile совместно, создал descriptor set и Java/Kotlin-исходники во временных JAR `/private/tmp/fitassistant-diary-check*` (exit code 0). Это проверка генерации сообщений, не компиляция JVM-кода и не проверка gRPC-stubs.
- `buf lint` / `buf breaking` не выполнены: Buf отсутствует в PATH. Пустой исходный Diary-файл не был рабочим wire-контрактом; совместимость автоматическим инструментом не подтверждена.
- Сущности, миграции, обработчики и настройка генерации diary-service не реализовывались; runtime и БД не проверялись.

### Генерация Profile и исправление зависимости (2026-11-13)

- Активированы Skills: `diagnose-build-failure`, `contracts-proto-and-avro`.
- Проверка с зависимостью `profile-service` от `:mobile:shared` остановилась на несовместимых Android-вариантах. По разрешению пользователя строка зависимости закомментирована.
- Скачаны необходимые инструменты генерации через Gradle. Buf не найден в PATH; для Gradle-генерации он не требуется.
- `./gradlew :backend:profile-service:generateProto :backend:profile-service:compileKotlin :backend:profile-service:compileTestKotlin --console=plain` — BUILD SUCCESSFUL.
- Синхронизация Android Studio — `Sync finished successfully`.
- Protobuf и gRPC классы сгенерированы в `backend/profile-service/build/generated/sources/proto/`. Основной и тестовый код скомпилированы; тесты и запуск сервиса не выполнялись.


---

## Статус проекта: [T-043] Реализован скелет catalog-price-service, Liquibase-миграции, сущности JPA и интеграционные тесты
## Статус проекта: [T-016 / T-017 / T-018] Завершены: Скелет profile-service, Liquibase и реализация gRPC-сервиса (ProfileGrpcService + ProfileService + ProfileDtoMapper)
## Статус проекта: [T-043]—[T-049] Микросервис catalog-price-service полностью разработан, протестирован и упакован в K8s

Ветка: `feature/KAN-61-catalog-skelet`
Ветка: `feature/KAN-36-liquibase` (Ключ задачи: KAN-36, T-016, T-017, T-018)
Роли: `implementer` + `test-reviewer`

---

### Выполнено

- [x] **[T-049] Манифесты Kubernetes и интеграция с Helmfile:**
  - Созданы декларативные K8s-манифесты `infrastructure/k8s/apps/catalog-price-service/`:
    - `deployment.yaml`: non-root securityContext, probes `/actuator/health/liveness` и `/actuator/health/readiness`, limits/requests, envFrom configMap и secret.
    - `service.yaml`: порты gRPC (9192) и Actuator (8086).
    - `configmap.yaml` и `secret.yaml`.
    - `grpc-route.yaml` и `infrastructure/k8s/gateway/04-catalog-grpc-route.yaml`: GRPCRoute для Envoy Gateway маршрутизации `nutritionist.catalog.v1.CatalogService` на порт 9192.
  - Подключен релиз `catalog-price-service` в `infrastructure/k8s/charts/helmfile.yaml`.
  - Коммит: `8140d4d chore(deploy): add catalog-price-service to k8s manifests and helmfile`.

- [x] **[T-048] Полный набор E2E тестов и сценарий нагрузочного тестирования:**
  - Разработан `CatalogEndToEndIntegrationTest.kt` на Testcontainers (PostgreSQL 17 + Redis 7):
    - Сквозной сценарий жизненного цикла: поиск продуктов с критериями -> выбор карточки -> чтение с кэшированием в Redis -> инвалидация кэша администратором при `upsertPrice` -> верификация обновленной цены и пересчитанной средней цены.
    - Граничные условия: `NOT_FOUND` для несуществующих UUID, `INVALID_ARGUMENT` для некорректных форматов и отрицательных цен, пустые списки.
    - Нагрузочный смоук-тест на 50 параллельных корутинах (`async(Dispatchers.IO)`): одновременные `SearchProducts` и `GetAveragePrices` без сбоев пула HikariCP и Redis.
  - Коммит: `7f22523 test(catalog): T-048 add full integration test suite and load test scenario`.

- [x] **[T-047] Отказоустойчивый Redis cache-aside с Circuit Breaker и метриками:**
  - Настроен `RedisConfig` и `CatalogCacheService` с сериализацией DTO (`CachedProductDto`, `CachedAveragePriceDto`) в JSON через Jackson.
  - Интегрирован Resilience4j Circuit Breaker `catalog-redis-cache`: при падении или таймаутах Redis вызовы прозрачно деградируют в PostgreSQL без выброса ошибок наружу.
  - Micrometer счетчики метрик: `cache_hits_total`, `cache_misses_total` с тегом `cache`.
  - Инвалидация кэша в `CatalogService.upsertRegionalPrice`.
  - Интеграционный тест `CatalogCacheResilienceIntegrationTest` (проверка кэш-хитов, промахов, инвалидации и эмуляция сетевого сбоя Redis).
  - Коммит: `07b694e feat(catalog): T-047 integrate resilient Redis cache-aside with metrics`.

- [x] **[T-046] Начальные данные каталога и региональных цен (Seed Data):**
  - Созданы CSV-файлы: `initial_regions.csv` (6 регионов РФ), `initial_products.csv` (110 продуктов с КБЖУ), `initial_prices.csv` (660 региональных цен).
  - Liquibase changelog `002-seed-initial-catalog.xml` с `<loadData>` и безопасными преобразованиями типов.
  - Интеграционный тест `CatalogSeedDataIntegrationTest`: проверка >= 100 продуктов, >= 6 регионов, корректности расчета средней цены.
  - Коммит: `76194b5 feat(catalog): T-046 add initial seed data for products and regional prices`.

- [x] **[T-046] Remediation & Hardening :backend:profile-service (AGENTS.md & spring-grpc-service):**
  - **Thread-safe транзакции и gRPC Context:** Устранён thread hopping в `ProfileGrpcService.kt` — удалён `withContext(dispatchers.io)`, ломавший `@Transactional` / `ThreadLocal` Spring Data JPA; контекст вызова и аутентификации стабилизирован через `GrpcContextElement.current() + MDCContext(mapOf("userId" to request.userId))`.
  - **AuthContext & Coroutines:** В `AuthContext.kt` реализованы extension-свойства `CoroutineContext.authContext` и `currentCoroutineAuthContext()` с поддержкой `GrpcContextElement` для бесшовной передачи identity пользователя сквозь корутины.
  - **Native Outbox Locking:** Заменён хрупкий Hibernate-хинт `-2` в `OutboxJpaRepository.kt` на нативный SQL `SELECT * FROM outbox_messages WHERE status = :status ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED` с поддержкой динамического лимита пачки.
  - **Outbox Retention & Cleanup:** Реализован шедулер `OutboxCleanupJob` (`0 0 3 * * ?`) для удаления `PUBLISHED` сообщений старше 7 дней через нативный запрос `deletePublishedBefore`. Метод `fetchPendingBatch()` в `OutboxProcessor.kt` инкапсулирован как `private`.
  - **Resilience4j Circuit Breaker & Graceful Degradation:** Настроен `@CircuitBreaker(name = "redis-idempotency", fallbackMethod = ...)` и программная защита через `CircuitBreakerRegistry` в `RedisIdempotencyStore.kt` для методов `tryAcquire`, `complete`, `release`, `get`. При отказе Redis сервис переходит в graceful degradation mode (`IdempotencyResult.Acquired`), не блокируя пользователей.
  - **Структурированное JSON-логирование:** Настроен `logback-spring.xml` на базе `net.logstash.logback.encoder.LogstashEncoder` с пробросом `traceId`, `spanId`, `userId`, `outboxId` и тегом сервиса `profile-service`. Удалена имитация JSON строковыми паттернами из `application.yml`.
  - **JPA Best Practices & Immutability:** В `ProfileEntity.kt` и `OutboxMessageEntity.kt` добавлены обязательные комментарии `// mutable by design: JPA managed entity` для изменяемых полей и переопределены `equals`/`hashCode` по бизнес-ключу `id`.
  - **Ktlint & Build:** Изолирован плагин `ktlint` от кодогенерации protobuf (`/build/` и `/generated/`), устранены все замечания стиля.
  - **Верификация тестами:** Разработаны `ProfileTransactionThreadSafetyTest`, `OutboxCleanupJobTest`, `RedisIdempotencyCircuitBreakerTest`. Все 83+ теста модуля успешно проходят (`BUILD SUCCESSFUL`), `ktlintCheck` — 0 violations.
  - Реализован `ProductSearchCriteria` и `KeysetCursor` с Base64-кодированием курсора `(lastValue, lastId)`.
  - Разработан `ProductSearchRepository` и `ProductSearchRepositoryImpl` на JPA Criteria API без использования `OFFSET`, с динамической фильтрацией по query (триграммный индекс `idx_products_name_trgm`), category (`idx_products_category`), диапазонам калорий и белков (`idx_products_calories_proteins`), сортировками по имени, калориям и белкам.
  - Настроен `ProductEntity` с `@BatchSize(size = 50)` для предотвращения N+1 при выборке связанных сущностей цен.
  - Метод `searchProducts` в `CatalogGrpcService` и `CatalogService` полностью интегрирован с keyset-пагинацией.
  - Разработан интеграционный бенчмарк-тест `ProductSearchBenchmarkIntegrationTest` на 10 000 записей (наполнение batch insert'ом, верификация последовательных непересекающихся страниц, стабильность курсора при параллельных вставках перед курсором, `EXPLAIN ANALYZE` подтверждающий выборку по индексам `idx_products_name` и `idx_products_category`).
  - Все 21 тест модуля зеленые, `ktlintCheck` пройден без замечаний.
  - Коммит: `feat(catalog): T-045 implement keyset pagination and indexed search with benchmark`.

- [x] **[T-044] Protobuf-контракты и базовая реализация gRPC-сервиса:**
  - Создан gRPC-контракт `proto/nutritionist/catalog/v1/catalog.proto` (`SearchProducts`, `GetProduct`, `GetAveragePrices`, `UpsertPrice`, сообщения `NutritionFacts`, `ProductItem`, `RegionalPriceItem`, `AveragePriceItem`, пагинация `page_size`/`page_token`).
  - Обновлен `proto/buf.gen.yaml` с правилом для `CatalogOuterClass`.
  - В `backend/catalog-price-service/build.gradle.kts` настроен плагин `com.google.protobuf` (генерация Java + Kotlin coroutines stubs из `proto/`), корректно изолирован `ktlint` от сгенерированных protobuf файлов.
  - Разработан `CatalogDtoMapper` для конвертации JPA-сущностей в Protobuf-сообщения.
  - Реализован `CatalogGrpcService` на Kotlin coroutines gRPC (`CatalogServiceCoroutineImplBase`): методы `searchProducts`, `getProduct`, `getAveragePrices`, `upsertPrice` с проверкой прав `ROLE_ADMIN` / `ROLE_SERVICE` через `AuthContext`.
  - Тестирование: `CatalogGrpcServiceSecurityTest` на Testcontainers PostgreSQL (6 интеграционных тестов: запрет неавторизованным на `upsertPrice`, запрет обычному пользователю, успешный апсерт от ADMIN/SERVICE, анонимное чтение `getProduct` и `getAveragePrices`). Все 16 тестов зеленые, `ktlintCheck` чист.
  - Коммит: `feat(catalog): T-044 define catalog.proto contract and implement gRPC service stub`.

- [x] **[T-043] Скелет catalog-price-service + Liquibase-миграции:**
  - Создана Liquibase-схема (`db/changelog/v1/001-init-catalog-schema.xml`, `db.changelog-master.xml`) для таблиц `regions`, `products`, `regional_prices` с версионированием `version BIGINT` и безопасным подключением расширения `pg_trgm` и GIN-индекса.
  - Разработаны JPA-сущности `RegionEntity`, `ProductEntity` с `@Version` и `@NamedEntityGraph("Product.withPricesAndRegions")`, `RegionalPriceEntity` со связями `LAZY`.
  - Разработаны репозитории `ProductJpaRepository`, `RegionJpaRepository`, `RegionalPriceJpaRepository` и сервис `CatalogService` с методом `upsertRegionalPrice`.
  - Инфраструктура: `CoroutineDispatchers`, `AuthContext`, `SecurityGrpcInterceptor`, `GrpcExceptionInterceptor`.
  - Тестирование: `CatalogPersistenceIntegrationTest` на Testcontainers PostgreSQL 17 (10 тестов: сохранение, `@Version` инкремент, уникальность штрихкода, уникальность региональных цен, уникальность названий регионов, выборка по нутриентам, выборка по категориям, отсутствие N+1 через `@NamedEntityGraph`). Все проверки `ktlintCheck` пройдены.
  - Коммит: `24b4850 feat(catalog): T-043 setup catalog schema, entities and indexes with Liquibase`.

- [x] **Базовая инфраструктура в Kubernetes (`infrastructure/k8s/infra/`) и связка с Envoy Gateway:**
  - **PostgreSQL 17 (`infrastructure/k8s/infra/01-postgres.yaml`):**
    - Создан StatefulSet и сервис `postgres` (порт 5432) в неймспейсе `budget-nutritionist`.
    - Секрет `postgres-secret` (`POSTGRES_USER=postgres`, `POSTGRES_PASSWORD=postgres`, `POSTGRES_DB=profile_db`).
    - ConfigMap `postgres-initdb-config` со скриптом инициализации баз данных `catalog_db`, `diary_db`, `keycloak_db`.
    - Безопасность: `runAsNonRoot: true`, `runAsUser: 70` (UID пользователя postgres в Alpine), `allowPrivilegeEscalation: false`, сброс capabilities ALL.
    - Health probes: `livenessProbe` и `readinessProbe` через `pg_isready`.
    - Ресурсы: requests 100m/128Mi, limits 500m/512Mi.
  - **Redis State & Redis Cache (`infrastructure/k8s/infra/02-redis.yaml`):**
    - Архитектурное разделение согласно `docs/architecture/05-highload-kafka-redis-sizing.md`:
      - `redis-state`: для идемпотентности gRPC (`IdempotencyServerInterceptor`) и Global Rate Limiting (`BackendTrafficPolicy`), аргументы `--maxmemory 128mb --maxmemory-policy volatile-ttl --appendonly yes`.
      - `redis-cache`: для L2 кэширования профилей/каталога/рецептов, аргументы `--maxmemory 256mb --maxmemory-policy allkeys-lru`.
    - Сервисы `redis-state` и `redis-cache` (порт 6379) в неймспейсе `budget-nutritionist`.
    - Безопасность: `runAsNonRoot: true`, `runAsUser: 999` (UID redis), `allowPrivilegeEscalation: false`.
    - Health probes: `livenessProbe` и `readinessProbe` через `redis-cli ping`.
    - Ресурсы: requests 50m/64Mi, limits 250m/192Mi (state) и 250m/384Mi (cache).
  - **Keycloak 26 (`infrastructure/k8s/infra/03-keycloak.yaml`):**
    - Деплоймент и сервис `keycloak` (порт 8080 для HTTP, 9000 для management) в неймспейсе `budget-nutritionist`.
    - Образ `quay.io/keycloak/keycloak:26.0` в режиме `start-dev --import-realm`.
    - ConfigMap `keycloak-realm-config` с монтированием `fitassistant-realm.json` в `/opt/keycloak/data/import/`.
    - Подключение к БД: `KC_DB=postgres`, `KC_DB_URL=jdbc:postgresql://postgres.budget-nutritionist.svc.cluster.local:5432/keycloak_db`.
    - Секрет `keycloak-secret` для учетных данных администратора.
    - Безопасность: `runAsNonRoot: true`, `runAsUser: 1000`, `allowPrivilegeEscalation: false`.
    - Health probes: `livenessProbe` (`/realms/master`) и `readinessProbe` (`/realms/FitAssistant`) на порту 8080.
  - **Обновление Helmfile и политик сетевого периметра:**
    - `infrastructure/k8s/charts/helmfile.yaml`: перенаправлен Envoy Gateway Rate Limiter на `redis-state.budget-nutritionist.svc.cluster.local:6379`, добавлены декларативные описания инфры.
    - `infrastructure/k8s/gateway/06-backend-traffic-policy.yaml`: EnvoyProxy redis url обновлен на `redis-state.budget-nutritionist.svc.cluster.local:6379`.
    - `infrastructure/k8s/gateway/05-security-policy.yaml`: `remoteJWKS.uri` обновлен на межнеймспейсный FQDN `http://keycloak.budget-nutritionist.svc.cluster.local:8080/realms/FitAssistant/protocol/openid-connect/certs`.
  - **Связка и конфигурация `profile-service` (`infrastructure/k8s/apps/profile-service/`):**
    - `PROFILE_DB_URL`: `jdbc:postgresql://postgres.budget-nutritionist.svc.cluster.local:5432/profile_db`
    - `DB_USERNAME` и `DB_PASSWORD`: маппинг из `postgres-secret`.
    - `SPRING_DATA_REDIS_HOST`: `redis-state.budget-nutritionist.svc.cluster.local` (порт `6379`).
    - `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI`: `http://keycloak.budget-nutritionist.svc.cluster.local:8080/realms/FitAssistant/protocol/openid-connect/certs`.
    - `SPRING_GRPC_SERVER_ADDRESS`: `0.0.0.0` (разрешено прослушивание gRPC со всех интерфейсов).
    - `SERVER_PORT` и `MANAGEMENT_PORT`: `8080` (соответствие `containerPort` и HTTP health probes).
  - **Скрипты автоматического запуска и проверки:**
    - Разработан `infrastructure/k8s/verify-cluster.sh` (Bash).
    - Разработан `infrastructure/k8s/verify-cluster.ps1` (PowerShell).
    - Обновлен `scripts/verify-envoy-gateway.sh` с учетом применения манифестов `infrastructure/k8s/infra/`.

- [x] **Единый KMP-валидатор профиля пользователя и онбординга (`ProfileValidator` в `:mobile:shared`):**
  - **Доменный слой (`:mobile:shared/src/commonMain`):**
    - Создан чистый sealed-интерфейс ошибок `FieldValidationError` (`Empty`, `NotANumber`, `NegativeOrZero`, `OutOfRange`, `TooShort`) в пакете `dev.andre.nutritionist.domain.validation`.
    - Реализован `ProfileValidator` с чистыми функциями валидации (`validateName`, `validateHeight`, `validateWeight`, `validateTargetWeight`, `validateAge`, `validateWeeklyBudget`, `validateDailyCalories`).
    - Границы синхронизированы с серверным `ProfileRequestValidator`:
      - Рост: `50 .. 300` см
      - Вес: `20.0 .. 500.0` кг (поддержка запятой и точки как десятичных разделителей)
      - Возраст: `14 .. 120` лет
      - Недельный бюджет: `1.0 .. 1_000_000.0`
      - Длина имени: `>= 2` символов
    - Реализована комплексная валидация онбординга `ProfileValidator.validateOnboarding(OnboardingData)`.
    - Чистота KMP: ни одной платформенной зависимости (`android.*`, `androidx.*`, `java.*`, `R.string`), запрет `!!`.
  - **UI-слой (`:mobile:androidApp`):**
    - Создан маппер ошибок `FieldValidationError.toMessageRes(): Int` с исчерпывающим `when` без `else`.
    - Удалён устаревший платформенный класс `ProfileFieldsValidation.kt`.
    - Очищен `RegistrationWizardScreen.kt`: заменены разрозненные inline-проверки на доменный `ProfileValidator`. Ошибки хранятся как типизированные `FieldValidationError?` и разрешаются в `@Composable` через `toMessageRes()`.
    - Диалоги редактирования `EditWeightDialog.kt` и `EditBudgetDialog.kt` переведены на вызовы `ProfileValidator`.
    - Обновлён `ProfileScreenUnitTest.kt` на тестирование `ProfileValidator` и `toMessageRes()`.
    - Добавлены тесты в `RegistrationWizardScreenTest.kt` на блокировку перехода при некорректном росте и бюджете.
  - **Тестирование и верификация:**
    - Разработан исчерпывающий набор юнит-тестов `ProfileValidatorTest.kt` в `commonTest` (17 тестов, граничные значения, некорректные типы, пустые строки, комплексный онбординг).
    - `:mobile:shared:allTests` (`testAndroidHostTest`): 111 passed, 0 failed.
    - `:mobile:androidApp:testDebugUnitTest`: 72 passed, 0 failed.
    - `:mobile:androidApp:assembleDebug` и `:mobile:shared:assemble`: BUILD SUCCESSFUL.

- [x] **Устранение дублирования бизнес-логики и переход на SSOT на бэкенде (profile-service) для норм КБЖУ:**
  - **Ликвидация клиентской формулы Миффлина — Сан-Жеора (`:mobile:shared`):**
    - Удалён дублирующий калькулятор `NutritionGoalCalculator.kt` и его устаревший тест `NutritionGoalCalculatorTest.kt` из `:mobile:shared`. Единственным источником истины (SSOT) для расчёта норм является `profile-service`.
    - Добавлены значения по умолчанию и константа `NutritionGoal.EMPTY` (`targetCalories = 0, targetProteinsGrams = 0.0, targetFatsGrams = 0.0, targetCarbsGrams = 0.0, targetWeightKg = 0.0`).
    - Упрощён `OnboardingDataMapper.kt`: `OnboardingData.toProfile()` передаёт `NutritionGoal.EMPTY`, а бэкенд вычисляет нормы, возвращает их в ответе `UpdateProfile` и они атомарно кэшируются в локальный Room.
    - Обновлён `OnboardingDataMapperTest.kt` на проверку генерации `NutritionGoal.EMPTY`.
  - **Доменный калькулятор дневного баланса и остатка (`DailyNutritionBalanceCalculator` в `:mobile:shared`):**
    - Создана иммутабельная модель съеденного `ConsumedNutrition(calories, proteinsGrams, fatsGrams, carbsGrams)`.
    - Созданы результирующие модели баланса `DailyMacroBalance` и `DailyNutritionBalance` с вычислением остатков, процента выполнения цели и флагов превышения нормы.
    - Реализован чистый офлайн-калькулятор `DailyNutritionBalanceCalculator.calculate(goal, consumed)` с защитой от деления на 0 и округлением до 1 знака.
    - Реализована функция распределения бюджета приёма пищи `getRemainingBudgetForMeal(balance, mealType)` (исчерпывающий `when` по `MealType`: завтрак 25%, обед 35%, ужин 25%, перекус 15%).
    - Модель `MealType` вынесена в `dev.andre.nutritionist.domain.model` (`:mobile:shared`), в `:mobile:androidApp` сделан `typealias`.
    - Разработан набор unit-тестов `DailyNutritionBalanceCalculatorTest` (6 тестов: базовый день, перебор калорий, защита от деления на 0, граничные макросы, распределение по приёмам пищи, нулевой бюджет при переборе).
  - **Интеграция с Presentation / UI (`:mobile:androidApp`):**
    - В `DashboardUiStateMapper` интегрирован `DailyNutritionBalanceCalculator` для расчета сводки `NutritionSummaryUiModel` и макросов `MacroNutrientUiModel`.
    - В UI передаются вычисленные остатки («Осталось 450 ккал», «Перебор 250 ккал», остатки макросов).
  - **Верификация:**
    - 82/82 тестов в `:mobile:shared:allTests` (`testAndroidHostTest`) GREEN.
    - 70/70 тестов в `:mobile:androidApp:testDebugUnitTest` GREEN.
    - `:mobile:androidApp:assembleDebug` BUILD SUCCESSFUL.

- [x] **Редизайн главного экрана приложения (Dashboard / Дневник питания КБЖУ в стиле Antigravity):**
  - **Маппинг данных и архитектура представления (`:mobile:androidApp`):**
    - Создана модель `MealType` (`BREAKFAST`, `LUNCH`, `DINNER`, `SNACK`) с исчерпывающим `when` без `else`.
    - Разработаны UI-модели состояния: `MacroNutrientUiModel`, `NutritionSummaryUiModel`, `MealEntryUiModel`, `MealSectionUiModel`, `DayOfWeekUiModel`, `DashboardUiState`.
    - Реализован чистый маппер `DashboardUiStateMapper`, агрегирующий употреблённые калории и макросы (Б/Ж/У) и сопоставляющий их с целевыми показателями `Profile.nutritionGoal` с бэкенда (`targetCalories`, `targetProteinsGrams`, `targetFatsGrams`, `targetCarbsGrams`).
  - **UI-компоненты Antigravity Design System (`:mobile:androidApp`):**
    - `DashboardWeekStreakBar.kt`: недельная полоса («Пн»–«Вс») с круглыми индикаторами, чекмарком выполненного дня, бейджем серии (стрика) с иконкой пламени и подписью дней серии.
    - `DashboardNutritionKpiBar.kt`: горизонтальная сводная плашка с 5 колонками (Жиры | Углев | Белк | Цель % | Калории) с крупным шрифтом значений и целевыми значениями из профиля.
    - `MealItemRow.kt`: строка добавленного блюда с иконкой, названием, порцией, раскладкой БЖУ и калориями.
    - `MealSectionCard.kt`: карточка приёма пищи со скруглением `Spacing.cardRadius`, тематическими иконками (`FitIcons.Sun`, `FitIcons.Noon`, `FitIcons.Sunset`, `FitIcons.Moon`), бейджем калоража, кнопкой добавления `+` (`testTag("add_meal_<TYPE>")`), раскрывающимся списком блюд и быстрыми действиями («Копировать», «+ Сохранить Еду», «+ Камера»).
    - Кнопка дополнительной активности: «+ Добавить Упражнение/Сон» (`FitIcons.Exercise`).
    - `FitIcons.kt`: добавлены векторные иконки `Sun`, `Noon`, `Sunset`, `Moon`, `Plus`, `Flame`, `Check`, `ChevronDown`, `ChevronUp`, `ChevronRight`, `Bowl`, `Exercise`, `Info`, `Camera`, `Copy`.
  - **Разделение Route / Screen (`:mobile:androidApp`):**
    - `MainScreen.kt`: чисто декларативный stateless-компонент, принимающий `DashboardUiState` и события (`onAddMealClick`, `onMealItemClick`, `onToggleMealSection`, `onCompleteProfileClick`, `onNavigateToProfile`, `onDestinationSelected`, `onAddExerciseSleepClick`).
    - `DashboardRoute.kt`: stateful-компонент, наблюдающий за `ProfileViewModel` и `authRepository`, связывающий данные через `DashboardUiStateMapper` и управляющий навигацией и состоянием раскрытия карточек.
  - **Локализация (`strings.xml` и `values-ru/strings.xml`):**
    - Добавлены двуязычные строковые ресурсы для всех приёмов пищи, КБЖУ, стрика, быстрых действий и дополнительной активности.
  - **Тестирование и верификация (Robolectric):**
    - В `MainScreenTest.kt` разработаны тесты на:
      1. Отображение заголовка `FitTopBar` ("FitApp").
      2. Отображение баннера незаполненного профиля и клик.
      3. Скрытие баннера при заполненном профиле.
      4. Динамическое отображение целей КБЖУ из `Profile.nutritionGoal` и употреблённых макросов.
      5. Отображение всех 4 приёмов пищи по порядку («Завтрак», «Обед», «Ужин», «Перекус»).
      6. Раскрытие секции «Ужин», отображение блюда с калориями и быстрых действий.
      7. Клики по кнопкам добавления `+` с передачей соответствующего `MealType`.
      8. Клик по строке блюда с передачей идентификатора.
      9. Отображение даты и стрика.
    - 9/9 тестов в `MainScreenTest` GREEN.
    - 70/70 тестов в `:mobile:androidApp:testDebugUnitTest` GREEN.
    - `:mobile:androidApp:assembleDebug` BUILD SUCCESSFUL.

- [x] **Точечное модальное редактирование параметров профиля (Вес, Активность, Цель, Бюджет):**
  - **MVI/KoTEA контракт (`:mobile:shared`):**
    - В `ProfileUiModel` добавлены сырые типизированные свойства `rawWeightKg: Double`, `rawActivityLevel: ActivityLevel`, `rawGoalType: GoalType`, `rawWeeklyBudget: Double` для предзаполнения полей ввода и радиокнопок диалогов.
    - В `ProfileUiStateMapper` реализован проброс этих полей из сущности `Profile`.
    - В `ProfileEvent.Ui` добавлены точечные события: `ChangeWeight(val weightKg: Double)`, `ChangeActivity(val activityLevel: ActivityLevel)`, `ChangeGoal(val goalType: GoalType)`, `ChangeBudget(val weeklyBudget: Double)`.
    - В редьюсере `ProfileUpdate` (чистый `DslUpdate`) добавлена обработка новых событий: создание мутированного `updatedProfile`, выставление `isSaving = true` и отправка команды `ProfileCommand.UpdateProfile(updatedProfile)`. При получении `ProfileLoaded` сохраняются `displayName` и `age`, сбрасывается `isSaving = false` и эмитится `ProfileNews.ProfileSaved`.
  - **Компоненты диалогов и кликабельность (`:mobile:androidApp`):**
    - `MetricCard.kt`: добавлен параметр `onClick: (() -> Unit)? = null` с поддержкой Material 3 клика и визуальным индикатором редактирования `FitIcons.Edit`.
    - `FitIcons.kt`: добавлена векторная иконка `FitIcons.Edit`.
    - Созданы компактные диалоги по правилу Single Responsibility File Rule в `dev.andre.nutritionist.android.ui.dialogs`:
      - `EditWeightDialog.kt` (ввод веса с валидацией 20..300 кг).
      - `EditActivityDialog.kt` (радиокнопки 4 уровней активности `ActivityLevel`).
      - `EditGoalDialog.kt` (выбор цели питания `GoalType`).
      - `EditBudgetDialog.kt` (ввод недельного бюджета с валидацией > 0).
    - `ProfileSuccessContent.kt`: добавлено локальное состояние `activeDialog: ProfileEditField?`, клики по карточкам открывают соответствующие модальные диалоги.
    - В меню «Настройки» убран принудительный редирект на полный визард онбординга.
    - Добавлены строковые ресурсы в `res/values/strings.xml` и `res/values-ru/strings.xml`.
  - **Тестирование и верификация:**
    - `ProfileUiStateMapperTest.kt`: тест маппинга сырых полей для диалогов.
    - `ProfileUpdateTest.kt`: 4 новых теста на обработку `ChangeWeight`, `ChangeActivity`, `ChangeGoal`, `ChangeBudget` и генерацию `ProfileCommand.UpdateProfile`.
    - `ProfileScreenUnitTest.kt`: 5 новых Compose UI тестов на Robolectric (тесты 14-18): проверка открытия диалогов при клике на карточки, ввод/выбор значений, вызовы `ProfileEvent.Ui.*` и валидация.
    - 79/79 тестов в `:mobile:shared:allTests` GREEN.
    - 66/66 тестов в `:mobile:androidApp:testDebugUnitTest` GREEN.
    - `:mobile:androidApp:assembleDebug` BUILD SUCCESSFUL.

- [x] **Устранение дублирования ввода имени и фамилии при регистрации через Keycloak:**
  - **Архитектурное решение:** Keycloak является центральным Identity Provider (OIDC / OAuth 2.1 PKCE) и уже собирает имя, фамилию, email и пароль при регистрации пользователя. Дублирование устранено в мобильном клиенте (`mobile:androidApp`): убран избыточный `OnboardingNameStep` из мастера регистрации.
  - `RegistrationWizardScreen.kt`:
    - Количество страниц визарда уменьшено с 5 до 4:
      - Шаг 0: Физические параметры (`OnboardingBodyStep` — рост, вес, возраст, пол).
      - Шаг 1: Физическая активность (`OnboardingActivityStep` — уровень активности).
      - Шаг 2: Бюджет питания (`OnboardingBudgetStep` — недельный бюджет).
      - Шаг 3: Цель питания (`OnboardingGoalStep` — выбор цели и завершение).
    - Убран неиспользуемый `firstNameError` и валидация имени на стороне клиента.
    - `DotsIndicator`: переведен на `totalDots = 4`.
    - `isFinalStep` настроен на индекс 3.
  - `RegistrationWizardRoute.kt`:
    - Добавлен параметр `initialData: OnboardingData = OnboardingData()` для проброса предзаполненного имени.
  - `AppNavHost.kt`:
    - Имя пользователя извлекается из токена Keycloak через `authRepository.getUserDisplayName()` и передается в визард как `initialData = OnboardingData(firstName = userDisplayName ?: "")`.
    - Все шаги визарда персонализируются именем пользователя из Keycloak (например, «Aleksey, расскажи о себе»).
  - `RegistrationWizardScreenTest.kt`:
    - Обновлены тесты на Robolectric под 4-шаговый визард (проверка персонализированного заголовка на шаге 0, валидация роста/веса/возраста, переход к шагу активности, завершение всех 4 шагов с сохранением собранных параметров).
    - 3/3 теста в `RegistrationWizardScreenTest` GREEN, 61/61 тестов в `:mobile:androidApp:testDebugUnitTest` GREEN, `:mobile:androidApp:assembleDebug` BUILD SUCCESSFUL.

- [x] **Реорганизация UX онбординга, ликвидация `runBlocking` в AuthRepository и стабилизация Transactional Outbox в `profile-service`:**
  - **Ликвидация `runBlocking` (`mobile:androidApp`):**
    - В `AuthStorage` и `EncryptedPrefsAuthStorage` добавлен потокобезопасный синхронный метод чтения кэшированного состояния `getAuthStateJsonSync(): String?`.
    - В `AuthRepositoryImpl` из блока `init` полностью убран `runBlocking(dispatchers.io)`, инициализация восстановлена через синхронный вызов `authStorage.getAuthStateJsonSync()`, полностью удалён импорт `kotlinx.coroutines.runBlocking`.
    - В `AuthRepositoryImplTest` добавлен тест `initialization restores cached session synchronously from authStorage without runBlocking`.
  - **UX онбординга и баннер «Заполнить профиль» (`mobile:androidApp` & `mobile:shared`):**
    - В `ProfileUiModel` добавлено свойство `val isComplete: Boolean = true`, а в `ProfileUiStateMapper` реализован расчет `isComplete = heightCm > 0 && weightKg > 0.0`.
    - Создан компонент `CompleteProfileBanner.kt` (Single Responsibility File Rule) в `dev.andre.nutritionist.android.ui.components` с кнопкой перехода в мастер онбординга.
    - В `MainScreen.kt` интегрировано отображение `CompleteProfileBanner` в верхней части экрана под `FitTopBar`.
    - Создан stateful `DashboardRoute.kt`, наблюдающий за профилем через `ProfileViewModel`: при ошибке загрузки (404/ошибка) или незаполненном профиле (`isComplete == false`) выставляется флаг `showCompleteProfileBanner = true`.
    - В `AppNavHost.kt` маршрут `Dashboard` переведён на `DashboardRoute`. При входе пользователя без профиля отображается баннер с кнопкой «Заполнить профиль», ведущий на `AppDestination.Registration.route`.
    - В `ProfileScreen.kt` и `ProfileSuccessContent.kt` добавлена кнопка и пункт меню «Заполнить анкету заново» (`R.string.profile_fill_survey_again`).
    - В `res/values/strings.xml` и `res/values-ru/strings.xml` добавлены локализованные строки для баннера и повторного заполнения анкеты.
    - Тесты: добавлены тесты в `MainScreenTest` и `ProfileScreenUnitTest` (61/61 тестов в `:mobile:androidApp:testDebugUnitTest` GREEN).
  - **Диагностика и исправление Outbox (`backend:profile-service`):**
    - В `ProfileService.kt` уточнено условие `isNewProfile`: если профиль был пустышкой (`heightCm <= 0` или `weightKg <= 0`), генерация `UserRegisteredEvent` надёжно срабатывает при первом заполнении реальных данных онбординга. Добавлено логирование `logger.info("Scheduling UserRegisteredEvent to outbox for userId={}", entity.userId)` и `logger.debug("Skipping UserRegisteredEvent: profile already exists for userId={}", command.userId.raw)`.
    - В `OutboxJpaRepository.kt` добавлен метод `resetFailedToPending()` для восстановления сообщений со статусом `FAILED` обратно в `PENDING` с обнулением счётчика попыток.
    - В `OutboxProcessor.kt` добавлена задержка `retryBackoffMs` в `application.yml` (`outbox.retry-backoff-ms: 2000`), автоматический сброс зависших `FAILED` сообщений при старте сервиса (`@PostConstruct`) и периодический шедулер `retryFailedMessagesScheduled()`.
    - В тестах `AbstractPostgresIntegrationTest`, `ProfileServiceApplicationTests`, `ProfileOutboxIntegrationTest` настроен динамический порт gRPC `spring.grpc.server.port: 0`, исключающий коллизии портов с работающим Envoy Gateway (порт 9090).
    - Разработаны интеграционные тесты `stub profile with zero parameters triggers outbox event upon first real parameters submission` и `resetFailedToPending recovers failed outbox messages for successful publishing` (82/82 тестов в `:backend:profile-service:test` GREEN).


- [x] **Начальный скелет микросервиса `:backend:catalog-price-service` и Liquibase-миграции каталога продуктов, магазинов и цен:**
  - **Liquibase-миграции (`catalog_db`):** Создан мастер-лог `src/main/resources/db/changelog/db.changelog-master.xml` и ченджсет `v1/001-init-catalog-schema.xml`. Описаны таблицы `stores` (UUID PK, `name`, `chain_name`, `address`, `is_active`, `created_at`), `products` (UUID PK, `name`, `barcode`, `calories_per_100g`, `proteins_per_100g`, `fats_per_100g`, `carbs_per_100g`, `version` BIGINT, `created_at`) и `prices` (UUID PK, FK на `products(id)` ON DELETE CASCADE, FK на `stores(id)` ON DELETE CASCADE, `price`, `currency`, `unit`, `updated_at`, уникальное ограничение `uq_product_store`). Добавлены индексы `idx_products_nutrition`, `idx_products_barcode`, `idx_products_name`, `idx_prices_store_product`, `idx_prices_updated_at`.
  - **JPA-сущности и репозитории (`dev.andre.nutritionist.catalog.persistence`):**
    - Реализованы JPA-сущности `StoreEntity`, `ProductEntity` и `PriceEntity`.
    - Все связи `@ManyToOne` и `@OneToMany` строго `FetchType.LAZY`. Категорический запрет `FetchType.EAGER` соблюдён.
    - Настроена оптимистичная блокировка через `@Version var version: Long = 0L` на `ProductEntity`.
    - На `ProductEntity` объявлен граф `@NamedEntityGraph(name = "Product.withPricesAndStores", attributeNodes = [NamedAttributeNode("prices", subgraph = "prices-subgraph")], subgraphs = [NamedSubgraph(name = "prices-subgraph", attributeNodes = [NamedAttributeNode("store")])])` для полного решения проблемы N+1.
    - В `ProductJpaRepository` добавлен метод `findWithPricesAndStoresById` с `@EntityGraph` (`EntityGraphType.LOAD`), а также фильтрация по диапазону калорий и минимальному белку `findByCaloriesPer100gBetweenAndProteinsPer100gGreaterThanEqual`.
    - Созданы `StoreJpaRepository` и `PriceJpaRepository` (с запросом `JOIN FETCH` цен с магазинами).
  - **Архитектурный каркас (по эталону `profile-service`):**
    - `CoroutineDispatchers` и `DefaultCoroutineDispatchers` (`dev.andre.nutritionist.catalog.coroutines`).
    - `GrpcExceptionInterceptor` (`dev.andre.nutritionist.catalog.grpc.interceptor`) с маппингом доменных и персистентных исключений в gRPC статусы (`NOT_FOUND`, `INVALID_ARGUMENT`, `ALREADY_EXISTS`, `ABORTED`, `INTERNAL`).
    - `SecurityGrpcInterceptor` и `AuthContext` (`dev.andre.nutritionist.catalog.grpc.security`) для проброса пользователя и ролей из Envoy Gateway и MDC-логирования `userId`.
    - Актуализирован `application.yml`: Hikari pool (10 соединений, таймаут 3000), порт gRPC 9192, web/actuator порт 8086, отключен `open-in-view: false`, `hibernate.ddl-auto: "none"`, JSON pattern логирования.
  - **Сервисный слой (`CatalogService`):**
    - Реализованы `@Transactional(readOnly = true)` методы поиска и получения каталога, а также `@Transactional` метод `upsertPrice` без единого сетевого вызова внутри транзакции.
  - **Тестирование и верификация (Testcontainers PostgreSQL 17):**
    - Настроен `AbstractPostgresIntegrationTest` с `PostgreSQLContainer("postgres:17-alpine")`.
    - Разработан `CatalogPersistenceIntegrationTest`: проверен прокат Liquibase-миграций, сохранение сущностей, работа `@NamedEntityGraph` без `LazyInitializationException` вне транзакции, фильтрация по КБЖУ, срабатывание уникального ограничения `uq_product_store` с выбросом `DataIntegrityViolationException`, а также идемпотентный `upsertPrice`.
    - Добавлен `CatalogPriceApplicationTests` на поднятие Spring-контекста.
    - Все 6/6 тестов `:backend:catalog-price-service:test` и `ktlintCheck` успешно пройдены (GREEN).

- [x] **Устранение разрыва данных между онбордингом и профилем, интеграция gRPC/Room сохранения OnboardingData и удаление хардкода "John"/"28":**
  - **Причина разрыва и падения бэкенда с 404 (Profile not found):** В `AppNavHost.kt` лямбда `onFinish` в `RegistrationWizardRoute` не принимала `OnboardingData` и не инициировала сохранение профиля. Из-за этого при переходе на экран профиля `GetProfile(userId)` на бэкенде падал с `NoSuchElementException("Profile not found: ...")`. В UI отображался захардкоженный "John" (`R.string.profile_default_user_name`) и возраст "28".
  - **Причина отсутствия сетевых вызовов/логов в `profile-service` и перезаписи метрик веса/роста/активности:** Класс [`AndroidProfileGrpcServiceClient.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/androidMain/kotlin/dev/andre/nutritionist/data/remote/grpc/AndroidProfileGrpcServiceClient.kt) был заглушкой (`createDefaultProfileProto`), которая не обращалась к переданному `ManagedChannel` и возвращала захардкоженный дефолтный профиль (175 см, 70 кг, Male, Light activity). При открытии экрана профиля `refreshProfile()` вызывал этот мок и перезаписывал Room-кэш, а сетевой gRPC-вызов на бэкенд не отправлялся (в логах `profile-service` была тишина).
  - **Реализация реального gRPC-клиента в `:mobile:shared`:**
    - В [`AndroidProfileGrpcServiceClient.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/androidMain/kotlin/dev/andre/nutritionist/data/remote/grpc/AndroidProfileGrpcServiceClient.kt) реализованы `MethodDescriptor` для `GetProfile` и `UpdateProfile` сервиса `com.budgetntr.profile.v1.ProfileService`.
    - Написаны бинарные Protobuf маршаллеры `GetProfileRequestMarshaller`, `UpdateProfileRequestMarshaller` и `ProfileProtoMarshaller` на базе `CodedOutputStream`/`CodedInputStream`, полностью согласованные со схемой `proto/nutritionist/profile/v1/profile.proto`.
    - Асинхронный вызов `channel.unaryCall` построен на базе корутин (`suspendCancellableCoroutine`) и `io.grpc.stub.ClientCalls.asyncUnaryCall` с поддержкой отмены корутин (`call.cancel`).
    - В [`ProfileRepositoryImpl.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/commonMain/kotlin/dev/andre/nutritionist/data/repository/ProfileRepositoryImpl.kt) реализовано немедленное сохранение в Room перед сетевым запросом и `recover`, гарантирующий offline-first работу без потери введённых данных.
    - В [`ProfileSuccessContent.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/kotlin/dev/andre/nutritionist/android/ui/screens/ProfileSuccessContent.kt) добавлена карточка «Рост» (`heightLabel`), теперь все метрики (Вес, Рост, Активность, Цель, Бюджет) динамически отображаются на экране.
    - Написаны тесты бинарного маршаллинга в [`AndroidProfileGrpcServiceClientTest.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/androidHostTest/kotlin/dev/andre/nutritionist/data/remote/grpc/AndroidProfileGrpcServiceClientTest.kt).
    - Успешно пройдены все тесты: `:mobile:shared:allTests` (74 passed), `:mobile:androidApp:testDebugUnitTest` (56 passed) и сборка `:mobile:androidApp:assembleDebug`.
  - **Доменные вычисления и маппинг (`:mobile:shared`):** 
    - `OnboardingData` перенесён в доменный слой `:mobile:shared` (`dev.andre.nutritionist.domain.model.OnboardingData`).
    - Реализован чистый KMP-калькулятор `NutritionGoalCalculator` с детерминированным расчётом базового обмена веществ (BMR) по формуле Миффлина — Сан-Жеора, коэффициентами активности (PAL), фактором цели и расчётом КБЖУ.
    - Создан `OnboardingDataMapper` (`toProfile(...)`) для преобразования данных онбординга в доменный `Profile` с типизированными идентификаторами `UserId` и `ProfileId`.
  - **MVI KoTEA Presentation (`ProfileCommand`, `ProfileEvent`, `ProfileUpdate`, `ProfileCommandsFlowHandler`, `ProfileUiStateMapper`):**
    - В `ProfileCommand` добавлена команда `UpdateProfile(val profile: Profile)`.
    - В `ProfileEvent.Ui` добавлены события `SaveOnboardingProfile(data, userId)` и `UpdateProfile(profile)`.
    - `ProfileCommandsFlowHandler` обрабатывает сохранение: вызывает `profileRepository.updateProfile(command.profile)` (сохранение через gRPC в `profile-service` PostgreSQL и кэширование в Room `ProfileDao`) и при успехе эмитит `ProfileEvent.Internal.ProfileLoaded(updatedProfile)`.
    - В `ProfileUpdate` добавлен флаг `isSaving`, при успешном сохранении профиля эмитится одноразовое уведомление `ProfileNews.ProfileSaved`.
    - В `ProfileUiModel` добавлено поле `displayName: String`. `ProfileUiStateMapper` заполняет имя пользователя с фоллбэком на `userId.raw`.
  - **Интеграция AppAuth & UI (`AuthRepository`, `AppNavHost.kt`, `ProfileSuccessContent.kt`, `ProfileScreen.kt`):**
    - В `AuthRepository` / `AuthRepositoryImpl` добавлен метод `getUserDisplayName(): String?`, извлекающий `given_name` -> `name` -> `preferred_username` из ID Token.
    - В `AppNavHost.kt` маршрут `AppDestination.Registration` подключен к `ProfileViewModel`: отображает `FullScreenLoading` во время сохранения и переходит на `Dashboard` только после эмиссии `ProfileNews.ProfileSaved`.
    - В `ProfileSuccessContent.kt` убран хардкод `R.string.profile_default_user_name` ("John"), отображается динамический `profile.displayName.ifBlank { profile.userId.raw }`.
    - В `ProfileScreen.kt` убран хардкод "John" и "28", значения берутся из актуального состояния `ProfileState.Content.Success`.
  - **Тестирование и верификация:**
    - `NutritionGoalCalculatorTest`: проверка расчёта формулы Миффлина — Сан-Жеора для всех полов и уровней активности.
    - `OnboardingDataMapperTest`: проверка корректности маппинга данных онбординга в доменный профиль.
    - `ProfileUiStateMapperTest`: проверка проброса `displayName` и корректного фоллбэка на `userId.raw`.
    - `ProfileCommandsFlowHandlerTest`: проверка вызова `updateProfile` -> эмит `ProfileLoaded(profile)`.
    - `ProfileUpdateTest`: проверка `SaveOnboardingProfile` -> команда `UpdateProfile` и эмит `ProfileSaved`.
    - `AuthRepositoryImplTest`: проверка извлечения `given_name` и `preferred_username` из JWT токенов.
    - `ProfileScreenUnitTest` (Robolectric): 11 UI-тестов, проверка отображения реального имени и метрик без "John" и "28".
    - Все 71 тест `:mobile:shared:allTests` — GREEN.
    - Все 56 тестов `:mobile:androidApp:testDebugUnitTest` — GREEN.
    - Сборка `:mobile:androidApp:assembleDebug` — BUILD SUCCESSFUL.

- [x] **Исправление зависания («Connecting to Keycloak…») и ошибки «ERR_CONNECTION_REFUSED / localhost:8080» при авторизации и регистрации:**
  - **Причина зависания:** В `AppNavHost.kt` на уровне корня висел `LaunchedEffect(authViewModel) { authViewModel.news.collect { ... } }`. Так как `StoreImpl.news` построен на одноканальной очереди `Channel<News>` (`receiveAsFlow()`), `AppNavHost` перехватывал событие `AuthNews.LaunchAuthTab(intent)`, и дочерний `LoginRoute` его не получал, оставаясь в бесконечной загрузке.
  - **Причина ошибки ERR_CONNECTION_REFUSED:** В `infrastructure/docker-compose.local.yml` было захардкожено `KC_HOSTNAME: http://localhost:8080`. В результате Keycloak генерировал HTML-формы логина и ссылки регистрации со строгим экшеном `http://localhost:8080/...`. На Android-эмуляторе `localhost` указывает на сам эмулятор (127.0.0.1 ОС Android), где порт 8080 не слушается, что приводило к ошибке `This site can't be reached / localhost refused to connect / ERR_CONNECTION_REFUSED`.
  - **Решение:**
    1. Из `infrastructure/docker-compose.local.yml` удалён фиксированный `KC_HOSTNAME: http://localhost:8080`, включен динамический резолв хоста `KC_HOSTNAME_STRICT: "false"`. Теперь при запросе с эмулятора (`Host: 10.0.2.2:8080`) Keycloak динамически отдаёт все ссылки и формы на `10.0.2.2:8080`, а при запросе с хоста — на `localhost:8080`.
    2. В `mobile/androidApp`: в `AuthConfig.kt` эндпоинты по умолчанию настроены на `10.0.2.2:8080` (стандартный хост-мост Android Emulator).
    3. В `AppNavHost.kt` удален перехватывающий `LaunchedEffect(authViewModel)`. Слушатель `authViewModel.news` перенесён локально в `ProfileRoute.kt` для обработки `NavigateToAuth` только на экране профиля.
    4. В `AuthContract.kt` и `AuthUpdate.kt` добавлен `AuthEvent.Ui.AuthCancelled` (`AuthState.Content.Idle`).
    5. В `AuthCommandsFlowHandler.kt` перехвачена ошибка `AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW`, которая теперь плавно переводит экран в `Idle` без пугающего сообщения об ошибке.
    6. Добавлен юнит-тест в `AuthUpdateTest.kt`. Все 50/50 unit-тестов `:mobile:androidApp:testDebugUnitTest` GREEN.
    7. Проверено в живом Android эмуляторе: и вход через Keycloak, и регистрация нового аккаунта открываются мгновенно, страницы рендерятся без ошибок связи, а при закрытии вкладки приложение возвращается в исходное состояние.

- [x] **Шаг 6 (T-042): Демо среза №1 — сквозная цепочка Авторизация (Keycloak OAuth 2.1 PKCE) → Gateway → profile-service → PostgreSQL:**
  - **Ревизия экранов входа (`LoginScreen.kt`, `SignUpScreen.kt`):** Устранены бутафорские поля паролей и mock 4-значных SMS/email кодов. Реализован лаконичный и современный экран входа: «Войти через Keycloak / Аккаунт» (AppAuth PKCE), «Войти через Google» (`kc_idp_hint=google`), быстрая регистрация («Создать аккаунт» с `prompt=create`), чистый экран `SignUpScreen.kt` без mock-полей.
  - **MVI Presentation & AppAuth (`AuthViewModel`, `AuthContract`, `AuthCommandsFlowHandler`):** Расширены события и команды авторизации (`idpHint`, `prompt`). В `AuthRepository` добавлен метод `getUserId(): UserId?` с безопасным извлечением `sub` claim из токена. В `AuthRepositoryImpl` токены надёжно хранятся в Android Keystore (`EncryptedPrefsAuthStorage`), а `AuthCallCredentials` автоматически прикрепляет токен к gRPC-запросам без блокировки потоков.
  - **Сквозная навигация и перехват Intent (`MainActivity.kt`, `AppNavHost.kt`, `LoginRoute.kt`):** `MainActivity` перехватывает deep link `com.budgetntr.fitassistant://oauth2redirect`, передаёт Intent в `AuthViewModel.processAuthResponse()`, переводит авторизованного пользователя на `Dashboard` и динамически передаёт `userId = authRepository.getUserId()` в `ProfileRoute`.
  - **Envoy Gateway & gRPC (`05-security-policy.yaml`, `AndroidDataModule.kt`):** Запросы направляются на `10.0.2.2:9090` (Envoy Gateway), шлюз валидирует JWT по JWKS Keycloak realm `FitAssistant`, пробрасывает заголовок `x-user-id: <subKeycloak>`, `SecurityGrpcInterceptor` валидирует права доступа к профилю, `ProfileService` читает/пишет данные в PostgreSQL `profile_db`, а `ProfileScreen` отображает реальные данные КБЖУ, веса и бюджета.
  - **Тестирование и верификация (AGENTS.md §6):**
    - Unit-тесты `:mobile:androidApp:testDebugUnitTest`: 42/42 GREEN (`LoginScreenTest`, `SignUpScreenTest`, `AuthUpdateTest`, `AuthCommandsFlowHandlerTest`, `AuthRepositoryImplTest`).
    - Unit-тесты `:mobile:shared:allTests`: 59/59 GREEN.
    - Сборка `:mobile:androidApp:assembleDebug`: BUILD SUCCESSFUL.
    - Устранено падение AppAuth `IllegalArgumentException: only https connections are permitted`: реализован `AppAuthConnectionBuilder`, в `AppAuthConfiguration` включен `skipIssuerHttpsCheck(true)`, в `res/xml/network_security_config.xml` разрешен cleartext HTTP для `10.0.2.2`, а в `AuthConfig` добавлены прямые эндпоинты `authEndpoint` и `tokenEndpoint` (`http://10.0.2.2:8080/...`) для стабильной работы в Android эмуляторе. Проверено в живом эмуляторе — Custom Tabs успешно открывает страницу Keycloak.
  - **Локальный запуск и верификация сервисов (Docker & profile-service bootRun):**
    - В `infrastructure/docker-compose.local.yml` внешний порт PostgreSQL переведён на `5433` во избежание конфликта с локальной службой Windows, в `.env` актуализированы `PROFILE_DB_URL`, `CATALOG_DB_URL`, `DIARY_DB_URL`. Все 5 контейнеров (Postgres, Keycloak, Redis, Kafka, Schema Registry) запущены и находятся в статусе `Up (healthy)`.
    - В `application.yml` сервиса `profile-service` исправлен парсинг Logback JSON pattern (устранено некорректное экранирование `\"`), веб-порт Tomcat переведён на `8085` (`server.port: 8085` и `management.server.port: 8085`) во избежание конфликта с Keycloak (порт `8080`).
    - Успешно проверен запуск `./gradlew :backend:profile-service:bootRun`: Liquibase применил миграции к `profile_db`, HikariCP подключен, Tomcat поднят на порту `8085`, gRPC Server слушает порт `9090`, Actuator health (`liveness` и `readiness`) возвращает `UP`.
    - Все 80/80 unit и integration тестов `:backend:profile-service:test` пройдены успешно (GREEN).

- [x] **Идемпотентность в profile-service (Redis State Machine & Concurrency Protection):**
  - **Конечный автомат состояний в Redis:** Добавлен sealed interface [`IdempotencyResult`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/src/main/kotlin/dev/andre/nutritionist/profile/grpc/idempotency/IdempotencyStore.kt) (`Acquired`, `InProgress`, `Cached`, `PayloadMismatch`).
  - **Атомарный захват и валидация:** В [`RedisIdempotencyStore`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/src/main/kotlin/dev/andre/nutritionist/profile/grpc/idempotency/RedisIdempotencyStore.kt) реализован атомарный `SETNX` со статусом `PROCESSING` и lock TTL 60с, хэширование тела запроса (SHA-256) и сохранение в компактном бинарном формате. При параллельных запросах конкуренты получают `Status.ABORTED`, при несовпадении хэша — `Status.INVALID_ARGUMENT`, при повторе — мгновенный возврат кэшированного ответа из Redis без обращения к БД.
  - **Освобождение при сбоях:** При ошибках валидации или исключениях в сервисе перехватчик [`IdempotencyServerInterceptor`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/src/main/kotlin/dev/andre/nutritionist/profile/grpc/idempotency/IdempotencyServerInterceptor.kt) автоматически удаляет ключ из Redis (`release`), что позволяет клиенту выполнить retry без ожидания истечения lock TTL.
  - **Тестирование и верификация:** 
    - Добавлены тесты в [`ProfileGrpcServiceIntegrationTest`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/src/test/kotlin/dev/andre/nutritionist/profile/grpc/ProfileGrpcServiceIntegrationTest.kt): проверка обнаружения параллельных in-flight запросов (`ABORTED`), защита от повторного использования ключа с другим payload (`INVALID_ARGUMENT`), очистка блокировки при ошибке валидации с последующим успешным retry.
    - Все 80/80 тестов в `:backend:profile-service:test` пройдены успешно (GREEN).
    - `ktlintCheck` и `detekt` пройдены без единого замечания.

- [x] **Шаг 5 (T-024 / T-025): Transactional Outbox в profile-service, событие UserRegisteredEvent и E2E интеграция:**
  - **[T-024] Схема и сущности Outbox:** Создан Liquibase changeset `003-create-outbox-table.xml` (таблица `outbox_messages` с `id UUID PK`, `aggregate_type`, `aggregate_id`, `topic`, `payload BYTEA`, `status`, `attempts`, `created_at`, `published_at`, и частичным индексом `idx_outbox_pending` по `created_at WHERE status = 'PENDING'`). Созданы JPA Entity [`OutboxMessageEntity`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/src/main/kotlin/dev/andre/nutritionist/profile/outbox/entity/OutboxMessageEntity.kt) и репозиторий [`OutboxJpaRepository`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/src/main/kotlin/dev/andre/nutritionist/profile/outbox/repository/OutboxJpaRepository.kt) с пессимистической блокировкой `FOR NO KEY UPDATE SKIP LOCKED` (`@Lock(LockModeType.PESSIMISTIC_WRITE)` и `jakarta.persistence.lock.timeout = -2`).
  - **[T-024] Надежная публикация вне транзакции:** Реализован локальный in-memory конвертер Avro [`OutboxPayloadConverter`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/src/main/kotlin/dev/andre/nutritionist/profile/outbox/service/OutboxPayloadConverter.kt) без сетевого I/O внутри БД-транзакции. Создан [`OutboxService`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/src/main/kotlin/dev/andre/nutritionist/profile/outbox/service/OutboxService.kt) (`@Transactional(propagation = Propagation.MANDATORY)`). Создан фоновый процессор [`OutboxProcessor`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/src/main/kotlin/dev/andre/nutritionist/profile/outbox/processor/OutboxProcessor.kt) (`@Scheduled` каждые 500мс, атомарная выборка батча через `TransactionTemplate`, отправка в Kafka через `KafkaTemplate` строго вне БД-транзакции, ключ партиции строго `aggregateId` (`userId`) для гарантии FIFO, фиксация статуса `PUBLISHED` при ACK, инкремент попыток и статус `FAILED` при исчерпании лимита, структурированные логи с MDC `userId`/`outboxId`, метрики Micrometer `outbox.messages.published` и `outbox.pending.count`).
  - **[T-025] Интеграция с регистрацией пользователя:** В [`ProfileService`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/src/main/kotlin/dev/andre/nutritionist/profile/service/ProfileService.kt) метод `upsertProfile` сохраняет событие `UserRegisteredEvent` через `OutboxService` строго при создании нового профиля (`isNewProfile = existing == null`). Повторные обновления профиля не вызывают генерацию событий регистрации.
  - **Конфигурация Kafka Producer:** Создан [`KafkaProducerConfig`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/src/main/kotlin/dev/andre/nutritionist/profile/config/KafkaProducerConfig.kt) с гарантией надёжности: `acks = all`, `enable.idempotence = true`, `KafkaAvroSerializer`.
  - **Документация архитектуры:** В [`backend/profile-service/README.md`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/backend/profile-service/README.md) добавлен раздел 5 «Архитектура Kafka, партиционирование и надежность доставки» с расчетом числа партиций по формуле Джея Крепса ($P = \max(\lceil T_{target} / T_{consumer} \rceil, N_{consumers}) = 3$), описанием преимуществ KRaft на VPS, ручным управлением оффсетами (`AckMode.MANUAL_IMMEDIATE`, `enable.auto.commit = false`) и защитой от Stop-The-World Rebalance.
  - **Тестирование и верификация:** 
    - `ProfilePersistenceTests`: проверка Liquibase, pessimistic lock `SKIP LOCKED`, обновление статусов и retry-механизм.
    - `ProfileOutboxIntegrationTest`: сквозной интеграционный тест с реальными Testcontainers (PostgreSQL 17 + Redis 7 + Confluent Kafka 7.7 + Schema Registry 7.7) — регистрация -> сохранение в Outbox -> отправка воркером -> получение реальным Kafka Consumer с валидацией партиционирования по `userId`, полей Avro (`email`, `source`, `id`, `registrationTimestamp`) и отсутствия дубликатов при повторных обновлениях.
    - Полная проверка: 77/77 тестов GREEN (`:backend:profile-service:test`), `ktlintCheck` и `detekt` без ошибок.

- [x] **Шаг 6 (T-026 / T-027 / T-028 / T-029): Миграция инфраструктуры Envoy Gateway на боевой profile-service, TLS, Keycloak JWT и Rate Limiting:**
  - **[T-026] Helmfile & TLS:** Обновлен `infrastructure/k8s/charts/helmfile.yaml` (Envoy Gateway v1.2.4 с Redis бэкендом). Создан секрет `02-tls-secret.yaml` с самоподписанным сертификатом (CN: `api.budget-nutritionist.app`, SAN: `localhost`, `127.0.0.1`, `envoy-gateway`). В `02-gateway.yaml` настроен HTTPS listener (порт 443, `tls.mode: Terminate`) и сохранён insecure HTTP listener (порт 9090).
  - **[T-027] Profile Service & GRPCRoute:** Создан манифест `03-profile-service.yaml` (Deployment: non-root UID 10001, лимиты 512M/500m, Actuator liveness/readiness пробы на 8080; Service: gRPC 9090 и Actuator 8080). Создан `04-profile-grpc-route.yaml` с привязкой сервиса `com.budgetntr.profile.v1.ProfileService` к `main-gateway`. Актуализирован `infrastructure/docker/Dockerfile.service` (многоэтапная сборка JDK 21 / JRE 21, запуск от UID 10001, обязательное копирование `proto/` и `shared/`).
  - **[T-028] SecurityPolicy с Keycloak JWT:** В `05-security-policy.yaml` настроена валидация JWT Keycloak (`issuer`, `remoteJWKS`) и проброс клеймов `sub` -> `x-user-id` и `realm_access.roles` -> `x-user-roles`. Запросы без токена блокируются шлюзом со статусом `UNAUTHENTICATED` (HTTP 401).
  - **[T-029] Двухуровневый Rate Limiter:** В `06-backend-traffic-policy.yaml` настроен Local Rate Limit (in-memory Token Bucket 20 req/s, burst 50) и Global Rate Limit (100 req/min на пользователя по заголовку `x-user-id` Distinct с хранением счётчиков в Redis). Обновлены смоук-скрипты `scripts/verify-envoy-gateway.sh` и `scripts/verify-envoy-gateway.ps1` для сквозного тестирования 3 сценариев (unauth 401, keycloak jwt success, burst 120 rate limit 429).

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
- [x] **Шаг 5 (T-016 / KAN-35): Скелет profile-service (Spring Boot 4.x + gRPC + Actuator):**
  - Подключение `com.google.protobuf` кодогенерации для `profile-service` с `protoc-gen-grpc-java` и `protoc-gen-grpc-kotlin` (корутинные стабы `ProfileServiceCoroutineImplBase`);
  - Реализация `ProfileGrpcService.kt` (`@GrpcService`, структурированное логирование через MDC `userId`, строгая валидация аргументов, соблюдение AGENTS.md без `!!`);
  - Конфигурация стендов и профилей: `application.yml` (база, Actuator пробы, Hikari, Kafka, JSON-логирование), `application-dev.yml` (gRPC Reflection=ON, DEBUG-логирование), `application-prod.yml` (gRPC Reflection=OFF, graceful shutdown 30s, WARN-логирование);
  - Интеграционные тесты жизнеспособности `ProfileServiceApplicationTests.kt` (успешный подъем контекста, liveness `/actuator/health/liveness` со статусом UP, readiness `/actuator/health/readiness` со статусом UP);
  - Архитектурная изоляция кодогенерации для Android через выделенный модуль `:mobile:proto-android` (чистый KMP `commonMain` сохранён без JVM-зависимостей, `androidMain` имеет автоматический доступ к сгенерированным stubs);
  - Полная верификация: `:backend:profile-service:check`, `:mobile:shared:allTests` (53 теста), `:mobile:androidApp:testDebugUnitTest` — ВСЕ GREEN.

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

- [x] **Шаг 5 (T-016 / KAN-35 / KAN-37 / KAN-38): profile-service ревью и устранение замечаний (Clean Architecture, @Version, Idempotency, Testcontainers):**
  - **Clean Architecture & Domain Model (Замечания 8 и 9):**
    - Создана чистая доменная модель `NutritionGoal` в `domain.model`;
    - `NutritionGoalCalculator` отвязан от JPA-сущностей, переведен на чистые доменные типы, оформлен как stateless `object` с подробным KDoc;
  - **Декомпозиция сервисного слоя (Замечания 1 и 2):**
    - Создана команда `UpsertProfileCommand`, исключающая протекание Protobuf DTO в сервисный слой;
    - Обновлен `ProfileDtoMapper` для двустороннего маппинга Domain <-> Proto <-> Embeddable;
    - Выделен метод `resolveNutritionGoal` в `ProfileService`;
  - **Оптимистические блокировки @Version (Замечание 3):**
    - Добавлено поле `@Version @Column(name = "version") var version: Long = 0` в `ProfileEntity`;
    - Подготовлена Liquibase-миграция `002-add-version-column.xml` и включена в `db.changelog-master.xml`;
    - В `GrpcExceptionInterceptor` добавлена трансляция `OptimisticLockException` и `OptimisticLockingFailureException` в gRPC статус `Status.ABORTED`;
  - **Предотвращение утечки информации (Замечание 7):**
    - В `ProfileGrpcService` заменено информативное сообщение об ошибке на нейтральное `"Profile not found"`;
  - **Серверный интерцептор идемпотентности и RedisIdempotencyStore (Замечание 6):**
    - Реализован боевой интерфейс `IdempotencyStore` и `RedisIdempotencyStore` с TTL 24 часа через `RedisTemplate<String, ByteArray>`;
    - Удалена временная in-memory реализация `InMemoryIdempotencyStore`;
    - Подключена зависимость `spring-boot-starter-data-redis` и настроен `RedisConfig`;
    - `AbstractPostgresIntegrationTest` расширен контейнером Redis 7 (Testcontainers singleton);
    - Создан `IdempotencyServerInterceptor`, корректно отправляющий `sendHeaders(Metadata())` перед повторной отправкой закешированного ответа;
    - Написан интеграционный тест идемпотентных запросов с заголовком `idempotency-key` в `ProfileGrpcServiceIntegrationTest` с проверкой кэша в Redis;
  - **Typed Value Class UserId (AGENTS.md §1.3):**
    - Создан `@JvmInline value class UserId(val raw: String)`;
    - `UpsertProfileCommand`, `ProfileService.getProfileByUserId`, `ProfileService.upsertProfile`, `ProfileGrpcService` и `ProfileDtoMapper` переведены на строгий `UserId` вместо сырого `String`;
  - **Удаление мертвого кода в ProfileDtoMapper и ProfileJpaRepository:**
    - Удалены неиспользуемые методы `UpdateProfileRequest.toNewEntity()`, `ProfileEntity.applyUpdates()`, `NutritionGoalEmbeddable.toProto()`, `NutritionGoalProto.toEmbeddable()`;
    - Удален неиспользуемый метод `existsByUserId` из `ProfileJpaRepository`;
  - **Паттерн Singleton Container для Testcontainers (Замечание 10):**
    - В базовом абстрактном классе `AbstractPostgresIntegrationTest` настроен совместный запуск singleton PostgreSQL 17 и Redis 7 контейнеров;
  - **Полная верификация качества кода:**
    - `ktlintCheck`: 0 ошибок;
    - `detekt`: 0 ошибок;
    - `:backend:profile-service:check`: 68 passed, 0 skipped, 0 failed.

- [x] **Шаг 4 (T-041 / KAN-59): Миграция экранов Авторизации, Регистрации (1.png), Онбординг-визарда (2–6.png) и удаление тестовой кнопки Add Meal:**
  - Удалена тестовая кнопка «Add meal» (`btn_add_meal_test`) и сопутствующий счётчик из `MainScreen.kt`, а тесты `MainScreenTest.kt` переведены на проверку реальных блоков экрана (`FitApp` top bar, «Мои планы», «Завтрак»).
  - Реализован экран авторизации [`LoginScreen.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/kotlin/dev/andre/nutritionist/android/ui/screens/auth/LoginScreen.kt) и [`LoginRoute.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/kotlin/dev/andre/nutritionist/android/ui/screens/auth/LoginRoute.kt) в точности по `screenshots/экран авторизации.jpg`:
    - Белый круглый аватар пользователя [`AuthAvatarHeader.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/kotlin/dev/andre/nutritionist/android/ui/components/AuthAvatarHeader.kt) с чёрным силуэтом;
    - Поля ввода Email (с иконкой почты) и пароля (с иконкой ключа и скрытием символов) через [`AuthInputField.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/kotlin/dev/andre/nutritionist/android/ui/components/AuthInputField.kt);
    - Кнопка «Авторизация» (белая заливка, чёрный текст, 52dp) через [`AuthActionButton.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/kotlin/dev/andre/nutritionist/android/ui/components/AuthActionButton.kt);
    - Разделитель «— Нет аккаунта? —», кнопка «Создать аккаунт», кнопки входа «Через Telegram» и «Через Google».
  - Реализован экран регистрации [`SignUpScreen.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/kotlin/dev/andre/nutritionist/android/ui/screens/auth/SignUpScreen.kt) и [`SignUpRoute.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/kotlin/dev/andre/nutritionist/android/ui/screens/auth/SignUpRoute.kt) в точности по `screenshots/экран регистрации 1.jpg`:
    - Ввод email и пароля;
    - Шаг подтверждения кода из письма («Код из письма» 4–6 цифр, повторная отправка);
    - Быстрая регистрация «Через Google»;
    - Кнопка «Назад» для возврата к экрану входа.
  - Настроена сквозная навигация в [`AppNavHost.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/kotlin/dev/andre/nutritionist/android/navigation/AppNavHost.kt) и [`MainActivity.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/kotlin/dev/andre/nutritionist/android/MainActivity.kt):
    - `startDestination = AppDestination.Auth.route` — приложение всегда начинается с экрана авторизации;
    - Вход (email/пароль, Google, Telegram) -> переход на `Dashboard` с очисткой back stack;
    - «Создать аккаунт» -> переход на `SignUpScreen` (1.png);
    - Подтверждение кода / Google регистрация -> переход к 5-шаговому мастеру онбординга (`RegistrationWizardScreen`, 2–6.png: имя/фамилия, параметры тела, активность, бюджет, цель);
    - Завершение онбординга -> переход на `Dashboard` с очисткой back stack.
  - Написаны изолированные Robolectric Compose UI тесты: `LoginScreenTest.kt` и `SignUpScreenTest.kt`.
  - Реализован легковесный и высокопроизводительный сплэш-экран [`SplashActivity.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/kotlin/dev/andre/nutritionist/android/SplashActivity.kt) и разметка [`activity_splash.xml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/res/layout/activity_splash.xml) без сторонних библиотек:
    - Чёрный фон (`#000000`), сверху неоновый логотип `fit-assistent`, в центре 3D-яблоко 200dp ([`splash_logo.png`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/main/res/drawable/splash_logo.png)), снизу текст «Здравствуйте!»;
    - Анимация 1.5 секунды: верхний и нижний текст за 1200мс проявляются и плавно встают на места с `DecelerateInterpolator`, центральное яблоко за 1500мс уменьшается с 1.1f до 0.85f (эффект антигравитационного ухода вглубь);
    - По завершении 1500мс — переход на `MainActivity` с плавной сменой экранов `overridePendingTransition(fade_in, fade_out)` и `finish()`;
    - Написан Robolectric unit-тест [`SplashActivityTest.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/androidApp/src/test/kotlin/dev/andre/nutritionist/android/SplashActivityTest.kt);
    - Проверена и подтверждена корректность применения иконки приложения: `ic_launcher_background.xml` подключен в adaptive icon `mipmap-anydpi-v26/ic_launcher.xml` и `ic_launcher_round.xml`.
  - Верификация сборки и тестов: `./gradlew :mobile:shared:allTests :mobile:androidApp:testDebugUnitTest :mobile:androidApp:assembleDebug` — ВСЕ ПРОЙДЕНЫ УСПЕШНО (GREEN, BUILD SUCCESSFUL).


---

### В работе

- [-] **Шаг 4:** Реализация фичи дневника (diary: AddDiaryEntry + Room KMP + SyncWorker).

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
- 📌 2026-11-11: **Архитектура подсистемы «Избранное» (Favorites)** — отвергнуто хранение избранного в `profile-service` (нарушение Bounded Contexts DDD) и прямая запись с мобильного клиента в Kafka (нарушение безопасности периметра). Избранное (продукты, рецепты, меню на день) локализовано в `catalog-price-service`. Синхронизация с мобильного клиента: Room KMP (0 мс) $\to$ `SyncFavoritesWorker` (gRPC + `idempotency-key`) $\to$ Transactional Outbox в `catalog_db` $\to$ Kafka топик `user.favorite.changed` $\to$ `recipe-ai-service` (обновление матрицы предпочтений пользователя в Redis для персонализации генерации рациона).

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

## Checkpoint 2026-11-07 — [T-040] Токены: безопасное хранение (Keystore), refreshIfNeeded, AuthInterceptor
- Активные Skills: `android-compose-feature`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- **Реализация:**
  1. `AuthClientInterceptor.kt`: автоматический вызов `tokenProvider.refreshIfNeeded()` перед выполнением gRPC-вызова для гарантии свежести токена с fallback на `getAccessToken()`.
  2. `AuthRepositoryImpl.kt`: полнофункциональный `refreshIfNeeded()` с вызовом `currentAuthState.performActionWithFreshTokens(authService)` и перезаписью зашифрованного хранилища `EncryptedPrefsAuthStorage` (Android Keystore `MasterKey`).
  3. Koin DI: `AuthRepositoryImpl` зарегистрирован в `androidAppModule` с привязкой к `AuthTokenProvider::class` и автоматически внедряется в `AuthClientInterceptor`.
  4. Тесты:
     - `AuthClientInterceptorTest`: добавлен тест `interceptCall invokes refreshIfNeeded and attaches refreshed token` (4/4 GREEN);
     - `AuthRepositoryImplTest`: добавлены тесты `refreshIfNeeded returns failure when user is not authorized` и `refreshIfNeeded returns current access token when token is fresh` (5/5 GREEN).
- **Верификация сборки и тестов:**
  - `./gradlew :mobile:shared:allTests :mobile:androidApp:testDebugUnitTest :mobile:androidApp:assembleDebug` — ВСЕ ПРОЙДЕНЫ УСПЕШНО (BUILD SUCCESSFUL, 54/54 shared tests GREEN, 18/18 androidApp tests GREEN).

## Checkpoint 2026-11-07 — [T-040 / Option A] gRPC Non-blocking CallCredentials (AuthCallCredentials) & Ликвидация runBlocking
- Активные Skills: `kmp-shared-feature`, `android-compose-feature`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- **Реализация "Способа А" (gRPC CallCredentials):**
  1. **Ликвидация `runBlocking`:** Из gRPC клиента полностью удалён `runBlocking`, устранив риск блокировки Main/UI потока или потоков диспетчера корутин при выполнении gRPC-запросов.
  2. **Создан `AuthCallCredentials`** (`dev.andre.nutritionist.data.remote.grpc.auth.AuthCallCredentials`):
     - Наследует официальный абстрактный класс gRPC `io.grpc.CallCredentials`.
     - Реализует `applyRequestMetadata(requestInfo, appExecutor, applier)`: запускает корутину на `dispatchers.io`, асинхронно обновляет токен через `tokenProvider.refreshIfNeeded()`, прикрепляет метаданные `authorization: Bearer <token>` и вызывает `appExecutor.execute { applier.apply(headers) }` (в точном согласии с контрактом gRPC).
     - При критических ошибках передаёт статус `Status.UNAUTHENTICATED` через `applier.fail(status)`.
  3. **Рефакторинг `AuthClientInterceptor`** (`dev.andre.nutritionist.data.remote.grpc.interceptor.AuthClientInterceptor`):
     - Преобразован в неблокирующий интерцептор, прикрепляющий `CallCredentials` к `callOptions` через `callOptions.withCallCredentials(credentials)`.
     - Предоставлен вторичный конструктор `AuthClientInterceptor(tokenProvider, dispatchers)`.
  4. **Koin DI (`AndroidDataModule.kt`):**
     - Модуль `androidRemoteDataModule` регистрирует `AuthCallCredentials(tokenProvider = get(), dispatchers = get())`.
     - `AuthClientInterceptor` регистрируется с внедрением `credentials = get<AuthCallCredentials>()`.
     - Все gRPC вызовы канала `ManagedChannel` автоматически получают неблокирующую авторизацию.
  5. **Тестирование (без MockK, чистые Fakes в `androidHostTest`):**
     - `AuthCallCredentialsTest`: 6 тестов (прикрепление токена, автоматический refresh, обработка null/blank токенов, обработка ошибок через `applier.fail(Status.UNAUTHENTICATED)`, верификация неблокирующего выполнения с `StandardTestDispatcher`);
     - `AuthClientInterceptorTest`: 3 теста (делегирование `CallCredentials` в `callOptions`, проверка secondary constructor, проверка неблокирующего поведения).
- **Верификация сборки и тестов:**
  - `./gradlew :mobile:shared:allTests :mobile:androidApp:testDebugUnitTest :mobile:androidApp:assembleDebug` — ВСЕ ПРОЙДЕНЫ УСПЕШНО (BUILD SUCCESSFUL, 59/59 shared tests GREEN, 18/18 androidApp tests GREEN).

## Checkpoint 2026-11-08 — [T-041 / KAN-59] ProfileRoute и ProfileScreen (MVI-связка, форма профиля/регистрации, валидация полей, Previews, UI-тесты)
- Активные Skills: `legacy-migration`, `android-compose-feature`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- Ветка: `feature/KAN-59-profile-screen-migration`.
- **Реализация:**
  1. **Миграция UI/UX с Flutter (`FitAssistentAndroid` & `@screenshots`):**
     - На основе экранов регистрации (`экран регистрации 1..6.jpg`) и профиля перенесены ключевые поля: имя пользователя, возраст, целевой вес, дневной калораж.
     - Виджеты `Form` / `TextFormField` / `Column` транслированы в M3 `OutlinedTextField`, `Column` со скроллом `verticalScroll`, токены `Spacing` и палитру `MaterialTheme.colorScheme`.
  2. **Чистая валидация полей (`ProfileFieldsValidation.kt`):**
     - Пакет: `dev.andre.nutritionist.android.presentation.profile.validation`.
     - Sealed-интерфейс ошибок `ProfileValidationError` (`Empty`, `NotANumber`, `NegativeOrZero`, `OutOfRange`, `TooShort`).
     - Чистые функции `validateName`, `validateAge`, `validateTargetWeight`, `validateDailyCalories` без побочных эффектов.
     - Маппер в строковые ресурсы `toErrorMessageResId` с исчерпывающим `when` без `else`.
  3. **Презентация и MVI-связка (KoTEA в `:mobile:shared` и `:mobile:androidApp`):**
     - В `ProfileState.Content` и `ProfileUiState.Content` добавлены состояния `Form` и `ValidationError`.
     - В `ProfileEvent.Ui` добавлены события `SaveProfile`, `NameChanged`, `AgeChanged`, `TargetWeightChanged`, `DailyCaloriesChanged`.
     - Редьюсер `ProfileUpdate` и маппер `ProfileUiStateMapper` обновлены с исчерпывающим `when` без `else`.
     - `ProfileViewModel` предоставляет `profileState: StateFlow<ProfileState>` для прямой подписки UI.
  4. **Compose UI (`ProfileRoute.kt`, `ProfileScreen.kt`):**
     - `ProfileRoute` (stateful): сбор `profileState` через `collectAsStateWithLifecycle()`, прослушивание `news` (Snackbar, навигация), отправка `LoadProfile`.
     - `ProfileScreen` (stateless): поддержка состояний `Loading`, `Form`, `ValidationError`, `Success`, `Error`, баннера `OfflineBanner()`, отображение `isError` и `supportingText` для каждого поля ввода.
     - Полный набор `@Preview` функций для тем Light и Dark для состояний Loading, Content/Form, ValidationError, Error.
  5. **Локализация:**
     - Все строки вынесены в `res/values/strings.xml` и `res/values-ru/strings.xml`. Полное отсутствие хардкода строк в Composable-функциях.
  6. **UI-тестирование (NO MockK):**
     - `ProfileScreenTest.kt` в `androidTest` и `ProfileScreenUnitTest.kt` в `test` (Robolectric):
       - Тест-кейс 1: проверка отображения текста ошибки валидации на фиксированном `ProfileState` через `assertIsDisplayed()`.
       - Тест-кейс 2: клик по кнопке сохранения со скроллом `performScrollTo().performClick()` и верификация диспатча события `ProfileEvent.Ui.SaveProfile`.
       - Тест чистых функций валидации по всем граничным условиям.
- **Верификация сборки и тестов:**
  - `./gradlew :mobile:shared:allTests` — 53/53 GREEN.
  - `./gradlew :mobile:androidApp:testDebugUnitTest` — 28/28 GREEN (включая RegistrationWizardScreenTest).
  - `./gradlew :mobile:androidApp:assembleDebug` — BUILD SUCCESSFUL.

## Checkpoint 2026-11-08 — [T-041 / KAN-59] 5-шаговый визард онбординга регистрации (HorizontalPager + DotsIndicator + WizardNavButton)
- Активные Skills: `legacy-migration`, `android-compose-feature`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- **Реализация:**
  1. **Очистка структуры исходников:**
     - Все созданные файлы перенесены из `src/main/java/` и `src/androidTest/java/` в канонические каталоги `src/main/kotlin/` и `src/androidTest/kotlin/`. Папки `java` полностью удалены.
  2. **Реализация компонентов визарда онбординга (по скриншотам 1-6):**
     - `DotsIndicator.kt`: анимированный 5-точечный индикатор шагов с плавной сменой размеров (8dp -> 10dp) и акцентным цветом `#00D09E`.
     - `WizardNavButton.kt`: минималистичная круглая кнопка (56dp) с рипплом, поддержкой направлений `BACK` («←»), `FORWARD` («→») и `FINISH` («✓»).
     - `OnboardingData.kt`: модель данных сессии регистрации (`firstName`, `lastName`, `heightCm`, `weightKg`, `age`, `gender`, `activityLevel`, `weeklyBudget`, `goalType`).
     - `OnboardingNameStep.kt` (Шаг 1): «Давай знакомиться! Как тебя зовут?» (Имя, Фамилия).
     - `OnboardingBodyStep.kt` (Шаг 2): «{Имя}, расскажи о себе» (Рост см, Вес кг, Возраст, Пол М/Ж в виде анимированных плашек `GenderPill`).
     - `OnboardingActivityStep.kt` (Шаг 3): «{Имя}, какой у тебя уровень активности?» (слайдер с 4 уровнями, бейджем статуса и кнопками «Меньше» / «Больше»).
     - `OnboardingBudgetStep.kt` (Шаг 4): «{Имя}, расскажи о себе — Ваш бюджет» (поле ввода + карточка рекомендации 2000-2500 руб/нед).
     - `OnboardingGoalStep.kt` (Шаг 5): «Какова ваша цель?» (интерактивные карточки: Набор массы, Похудение, Поддержание веса с изумрудной рамкой активного элемента).
     - `RegistrationWizardScreen.kt`: хост-экран на базе `HorizontalPager(userScrollEnabled = false)` с плавной анимацией перехода `animateScrollToPage()`, валидацией перед переходом между шагами и нижней панелью.
     - `RegistrationWizardRoute.kt`: интеграция в граф навигации `AppDestination.Registration` (`AppNavHost.kt`).
  3. **Локализация:**
     - Добавлено 28 новых ключей в `res/values/strings.xml` и `res/values-ru/strings.xml`.
  4. **Тестирование:**
     - `RegistrationWizardScreenTest.kt`: 3 теста (проверка первого шага и ошибки валидации, переход на шаг 1 при валидном имени, сквозное прохождение всех 5 шагов до финиша с получением `OnboardingData`).
- **Верификация сборки и тестов:**
  - `./gradlew :mobile:androidApp:testDebugUnitTest` — 28/28 GREEN.
  - `./gradlew :mobile:androidApp:assembleDebug` — BUILD SUCCESSFUL.

## Checkpoint 2026-11-08 — [T-010] Protobuf / Buf breaking против main в CI с защитой от намеренной поломки
- Активные Skills: `contracts-proto-and-avro`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- **Реализация:**
  1. `proto/buf.yaml`: расширены правила breaking change с базового `FILE` до `FILE` + `WIRE_JSON` (контроль удаления полей, номеров тегов, типов и RPC).
  2. `scripts/verify-buf-breaking.sh`: переносимый скрипт верификации с динамическим определением базовой ветки (`main`/`master`), симуляцией намеренной поломки контракта (удаление `id = 1` из `profile.proto`), проверкой ненулевого exit code `buf breaking` и чистым прогоном актуальных контрактов.
  3. `.github/workflows/build.yml`: шаг `proto-verify` обновлен для обязательного запуска проверки совместимости против `main`/`master` с гарантией падения пайплайна при breaking change.

## Checkpoint 2026-11-08 — [T-012 / KAN-31] Avro-схемы, BACKWARD-совместимость и CI-проверка Schema Registry
- Активные Skills: `contracts-proto-and-avro`, `testing-and-verification`, `diagnose-build-failure`.
- Роли: `implementer` + `test-reviewer`.
- Ветка: `feature/KAN-31` (`feature/KAN-12-avro-events-compatibility`).
- **Реализация:**
  1. **Avro-схемы (.avsc):**
     - `UserRegisteredEvent.avsc` (`shared/contracts/src/main/avro/UserRegisteredEvent.avsc`): namespace `dev.andre.nutritionist.contracts.avro`, логические типы `uuid` для `id`, `timestamp-millis` для `registrationTimestamp`, опциональное поле `source` с `"default": null` для безопасной эволюции.
     - `CaloriesConsumedEvent.avsc` (`shared/contracts/src/main/avro/CaloriesConsumedEvent.avsc`): namespace `dev.andre.nutritionist.contracts.avro`, логические типы `uuid` для `userId` и `mealId`, `timestamp-millis` для `timestamp`, калории `int`, опциональные поля `mealType` и `notes` с обязательным `"default": null` для гарантии BACKWARD-совместимости.
  2. **Gradle модуль общих контрактов `:shared:contracts`:**
     - Подключен в `settings.gradle.kts` (`:shared:contracts`).
     - `build.gradle.kts`: плагин Avro (`com.github.davidmc24.gradle.plugin.avro:1.9.1` в `libs.versions.toml`), таска кодогенерации `generateAvroJava`, зависимость `schema-registry-client` (`io.confluent:kafka-schema-registry-client:8.2.4`), таска `testSchemaCompatibility` (совместима с Gradle 9.6 Configuration Cache, проверяет локальный синтаксис/наличие default-значений для union-типов и сетевую совместимость с Schema Registry в режиме BACKWARD по адресу из `SCHEMA_REGISTRY_URL`).
  3. **Domain-модели и изолирующий слой маппинга:**
     - Domain сущности: `UserId`, `MealId` (Typed IDs на базе `@JvmInline value class`), `User`, `Meal` с использованием `kotlinx.datetime.Instant` и строгой изоляцией от Avro DTO (§2.2).
     - Мапперы `UserRegisteredEventMapper` и `CaloriesConsumedEventMapper`: полное отсутствие оператора `!!`, безопасная валидация `requireNotNull`, маппинг в обе стороны.
  4. **CI-конвейер GitHub Actions (`.github/workflows/avro-compatibility-check.yml`):**
     - Запуск при PR в `main`/`master`/`develop` при изменениях в контрактах.
     - Поднятие сервисов Kafka и Schema Registry (режим `BACKWARD`), установка глобального уровня совместимости в реестре.
     - Выполнение `./gradlew :shared:contracts:generateAvroJava :shared:contracts:testSchemaCompatibility` и `./gradlew :shared:contracts:test`.
  5. **Тестирование (без MockK, чистые Fakes/Unit-тесты в `EventMappingTest.kt`):**
     - Проверка прямого и обратного маппинга с сохранением типизированных ID.
     - Проверка обработки отсутствующих опциональных полей и подстановки дефолтов из схемы.
     - Проверка бинарной сериализации/десериализации Avro payload.
     - Проверка совместимости через `MockSchemaRegistryClient` (подтверждение совместимости V1 $\leftrightarrow$ V2 с default и отказ при breaking changes без default).
- **Верификация сборки и тестов:**
  - `./gradlew :shared:contracts:generateAvroJava :shared:contracts:testSchemaCompatibility` — BUILD SUCCESSFUL.
  - `./gradlew :shared:contracts:test` — 6/6 tests GREEN, BUILD SUCCESSFUL.
  - `./gradlew :shared:contracts:ktlintCheck` — BUILD SUCCESSFUL (0 нарушений).

## Checkpoint 2026-11-08 — [T-013 / KAN-13] Спайк интеграции Resilience4j в Spring Boot 4.1 + Kotlin Coroutines (ADR-002)
- Активные Skills: `spring-grpc-service`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- Ветка: `spike/KAN-13-resilience4j-spring-boot4`.
- **Проведено исследование (Spike) и сравнительный анализ трёх подходов:**
  1. Стартер `resilience4j-spring-boot3/4` (AOP аннотации `@CircuitBreaker`, `@Retry`);
  2. Чистое ядро + `resilience4j-kotlin` (функциональные неблокирующие расширения для корутин);
  3. Абстракция Spring Cloud CircuitBreaker (`ReactiveCircuitBreakerFactory` + Reactor bridge).
- **Результаты:**
  - Оформлен и принят архитектурный документ [`docs/architecture/adr/adr-002-resilience4j-integration.md`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/docs/architecture/adr/adr-002-resilience4j-integration.md).
  - Подготовлены демонстрационные прототипы для каждого из 3 подходов в `docs/architecture/adr/prototypes/`:
    - [`Approach1_Starter.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/docs/architecture/adr/prototypes/Approach1_Starter.kt)
    - [`Approach2_CoreKotlin.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/docs/architecture/adr/prototypes/Approach2_CoreKotlin.kt)
    - [`Approach3_SpringCloud.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/docs/architecture/adr/prototypes/Approach3_SpringCloud.kt)
  - Выбран **Вариант 2 (Чистое ядро + `resilience4j-kotlin`)**: нулевой риск блокировки пулов потоков (`Thread Starvation`), гарантированная защита от ложного срабатывания при отмене корутин (`CancellationException`), строгая изоляция Clean Architecture (домен не знает о resilience4j), минимальный оверхед в рантайме.
## Checkpoint 2026-11-08 — [T-014] gRPC-клиент для KMP: Unary + Server-Streaming на Android (grpc-kotlin) за интерфейсом RemoteDataSource, стратегия iOS (ADR-003)
- Активные Skills: `kmp-shared-feature`, `spring-grpc-service`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- **Проведён аудит реализации и сформирован архитектурный документ:**
  - Оформлен и зафиксирован [`docs/architecture/adr/adr-003-grpc-kmp-client.md`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/docs/architecture/adr/adr-003-grpc-kmp-client.md).
  - **Архитектурный выбор:** Clean Architecture изоляция сетевого слоя интерфейсом `*RemoteDataSource` в `commonMain` без прямых зависимостей от `io.grpc.*` и `protobuf`.
  - **Unary RPC:** `suspend fun invoke(): Result<DomainModel>`, реализация в `ProfileGrpcRemoteDataSource.kt` с защитой от подавления `CancellationException` и маппингом через `ProfileDtoMapper.kt` (без `!!`).
  - **Server-Streaming RPC:** `fun stream(): Flow<DomainModel>`, сквозной `Flow` из `grpc-kotlin` с `map` в domain-модели и автоматической отменой HTTP/2 `RST_STREAM` при отмене сбора flow.
  - **Безопасная неблокирующая авторизация:** `AuthCallCredentials` наследует `io.grpc.CallCredentials`, обновляет токен в `dispatchers.io` через `refreshIfNeeded()` без `runBlocking` (опция А из T-040).
  - **Стратегия готовности к iOS:** `commonMain` полностью платформонезависим; при подключении `iosArm64`/`iosSimulatorArm64` реализация `IosProfileRemoteDataSource` биндится через Koin с использованием `grpc-swift` и iOS Keychain.
  - Верифицировано тестами в `commonTest` и `androidHostTest` без использования MockK (чистые Fakes).

## Checkpoint 2026-11-08 — [T-015] Envoy Gateway в kind/k3d: GatewayClass + GRPCRoute на echo-сервис, grpcurl через gateway (ADR-004)
- Активные Skills: `spring-grpc-service`, `contracts-proto-and-avro`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- **Проведён спайк и сформированы инфраструктурные манифесты:**
  - Оформлен и принят архитектурный документ [`docs/architecture/adr/adr-004-envoy-gateway.md`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/docs/architecture/adr/adr-004-envoy-gateway.md).
  - Создана конфигурация кластера kind с пробросом портов (80, 443, 9090): [`infrastructure/k8s/kind-config.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/kind-config.yaml).
  - Сформированы манифесты Kubernetes Gateway API v1:
    - [`01-gateway-class.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/01-gateway-class.yaml) (Envoy Gateway controller);
    - [`02-gateway.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/02-gateway.yaml) (порт 9090 gRPC, порт 80 HTTP);
    - [`03-echo-service.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/03-echo-service.yaml) (gRPC echo-сервер `rancher/grpc-echo:v1.0.0` на порту 9090);
    - [`04-grpc-route.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/04-grpc-route.yaml) (маршрутизация `echo.EchoServer` на `echo-grpc-service`);
    - [`05-security-policy.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/05-security-policy.yaml) (SecurityPolicy для валидации JWT токенов Keycloak);
    - [`06-backend-traffic-policy.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/06-backend-traffic-policy.yaml) (Rate Limiter);
    - [`helmfile.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/charts/helmfile.yaml) (декларативное развёртывание Envoy Gateway v1.2.4).
  - Написаны переносимые скрипты автоматического развертывания и тестирования спайка через `grpcurl`:
    - Bash: [`scripts/verify-envoy-gateway.sh`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/scripts/verify-envoy-gateway.sh);
    - PowerShell: [`scripts/verify-envoy-gateway.ps1`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/scripts/verify-envoy-gateway.ps1).

## Checkpoint 2026-11-11 — Исправление архитектурного аудита 83f9352

- Ветка: `feature/KAN-36-liquibase`; исходный HEAD: `83f935266f4d7f4228fb34d19c57747a4578139e`; рабочее дерево перед началом чистое.
- Активные Skills: `contracts-proto-and-avro`, `testing-and-verification`; роли: `implementer`, затем `test-reviewer`.
- В работе: согласование избранного, устойчивой очереди операций, worker, событий и клиентского расчёта стоимости (пункты 2–5 и 7 аудита).
- Решение: стоимость и остатки рассчитывает мобильный доменный слой; сервер хранит снимок записи. Recipe AI получает ограничения запроса, без событийного денежного баланса.
- Границы: новые функциональные модули не реализуются; действующие Proto/Avro сохраняются, целевые контракты уточняются в документации. Без commit/push.

## Checkpoint 2026-11-11 — [T-017 / KAN-36] Выравнивание схемы profile-service под profile.proto и документация топологии БД
- Активные Skills: `legacy-migration`, `spring-grpc-service`, `contracts-proto-and-avro`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- **Реализация:**
  1. **Ликвидация галлюцинированных сущностей и выравнивание под контракт:**
     - Удалены сущности `AccumulatorEntity.kt`, `AccumulatorJpaRepository.kt`, `accumulator-changelog.xml` (шаги, вода, активные минуты — отсутствуют в `profile.proto`).
     - Удалены `TargetEntity.kt`, `TargetJpaRepository.kt`, `V1-init-target-table.xml`.
     - Создан `@Embeddable NutritionGoalEmbeddable` (`targetCalories`, `targetProteinsGrams`, `targetFatsGrams`, `targetCarbsGrams`, `targetWeightKg`).
     - `ProfileEntity.kt` выровнен строго 1:1 под `profile.proto` и мобильную модель Room `ProfileEntity.kt`.
     - `001-init-profile-table.xml`: единая таблица `profiles` со всеми полями КБЖУ, роста/веса, `goal_type` и `weekly_budget`.
     - Исправлен `application.yml` (`management.endpoint.health.group.readiness.include` настроен на существующие contributors: `readinessState,db`).
  2. **Документирование архитектуры баз данных и контрактов:**
     - Детализированы `README.md` для всех 5 бэкенд-микросервисов (`profile-service`, `diary-service`, `catalog-price-service`, `recipe-ai-service`, `food-cv-service`) и мобильного приложения (`mobile/README.md`).
     - Создан единый топологический документ [`docs/architecture/04-database-and-contracts-topology.md`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/docs/architecture/04-database-and-contracts-topology.md) с описанием всех БД, межсервисных взаимодействий, альтернатив и преимуществ выбранного решения.
  3. **Верификация:**
     - `./gradlew :backend:profile-service:test` — 6/6 tests GREEN (Testcontainers PostgreSQL 17 + Liquibase).
     - `./gradlew :backend:profile-service:ktlintCheck` — BUILD SUCCESSFUL.
     - `./gradlew :mobile:shared:allTests` — 59/59 tests GREEN.

## Checkpoint 2026-11-11 — Архитектурный анализ подсистемы «Избранное» (продукты, рецепты, меню на день) и обновление документации
- Активные Skills: `contracts-proto-and-avro`, `spring-grpc-service`, `kmp-shared-feature`, `android-compose-feature`.
- Роли: `implementer` + `test-reviewer`.
- **Архитектурный анализ предложения пользователя:**
  1. **Отказ от размещения избранного в `profile-service`:** `profile-service` отвечает за биометрию (рост, вес, цели КБЖУ, бюджет). Каталог продуктов, рецептов и дневных рационов — это зона ответственности `catalog-price-service`. Размещение избранного в профиле привело бы к антипаттерну God Service и N+1 распределенным запросам при рендеринге списков.
  2. **Отказ от прямой записи мобильного клиента в Kafka:** мобильные клиенты не имеют прямого доступа к брокеру Kafka по соображениям безопасности (Zero-Trust, Envoy Gateway JWT-авторизация, отсутствие синхронного ACK/валидации).
  3. **Утвержденная схема работы:**
     - **Клиент (Offline-First):** мгновенное добавление в Room KMP (`favorite_items`) $\to$ `SyncFavoritesWorker` (WorkManager + `BackoffPolicy.EXPONENTIAL`) отправляет gRPC `AddFavorite`/`RemoveFavorite` с метаданными `idempotency-key`.
     - **Бэкенд:** `catalog-price-service` атомарно сохраняет связь в PostgreSQL (`catalog_db`: таблицы `user_favorite_foods`, `user_favorite_recipes`, `user_favorite_meal_plans`) и пишет событие в локальный `outbox_messages`.
     - **Событийная шина (Kafka):** Outbox Processor публикует Avro-событие `UserFavoriteChanged` в топик `user.favorite.changed`.
     - **Потребитель:** `recipe-ai-service` слушает этот топик и обновляет матрицу предпочтений пользователя (User Affinity) в Redis, отдавая больший вес любимым продуктам и рецептам при генерации суточного меню.
- **Обновлена документация:**
  - `docs/architecture/04-database-and-contracts-topology.md`: обновлена схема топологии, таблицы `catalog_db` и Room, добавлен разбор «Альтернатива 4».
  - `docs/architecture/02-backend-stack.md`: добавлен топик `user.favorite.changed` и Avro-схема `UserFavoriteChanged`.
  - `docs/architecture/03-mobile-stack.md`: добавлен `SyncFavoritesWorker` и структура фичи `favorites`.

## Checkpoint 2026-11-12 — [T-016 / T-017 / T-018 / KAN-36] Полная реализация profile-service (gRPC, JPA, ProfileDtoMapper, Testcontainers)
- Активные Skills: `spring-grpc-service`, `contracts-proto-and-avro`, `testing-and-verification`.
- Роли: `implementer` + `test-reviewer`.
- Ветка: `feature/KAN-36-liquibase` (задачи T-016, T-017, T-018).
- **Реализация:**
  1. **[T-016] Скелет и конфигурация профилей profile-service:**
     - Подтверждена работоспособность Spring Boot 4.1 + `spring-boot-starter-grpc-server`.
     - Actuator endpoints: `/actuator/health/liveness` и `/actuator/health/readiness` (с проверкой db), изоляция портов (Management port 8080, gRPC 9090).
     - Конфигурации окружений: `application.yml` (базовая конфигурация), `application-dev.yml` (gRPC Reflection включен, show-sql=true), `application-prod.yml` (Reflection выключен по принципу минимизации привилегий, graceful shutdown 30s).
  2. **[T-017] Liquibase миграция и топология базы данных:**
     - Подтверждено применение `001-init-profile-table.xml`: единая таблица `profiles` с первичным ключом UUID, уникальным ограничением `uq_profiles_user_id` на `user_id`, КБЖУ целями и бюджетом.
     - Ликвидированы легаси-галлюцинации (аккумуляторы шагов, воды и отдельная таблица TargetEntity).
  3. **[T-018] Бизнес-логика, JPA сущности, DTO-маппинг и gRPC-сервис:**
     - `ProfileEntity` с `@Embedded NutritionGoalEmbeddable` и `ProfileJpaRepository`.
     - `ProfileDtoMapper.kt` и `ProfileEnumMapper.kt`:
       - Исчерпывающий `when` для enum'ов (`Gender`, `ActivityLevel`, `GoalType`) без ветки `else`;
       - Конвертация `BigDecimal` $\leftrightarrow$ `Double` без потери точности (`setScale(2, RoundingMode.HALF_UP)`);
       - Защита от `null` без оператора `!!`.
     - Транзакционный слой `ProfileService.kt`: `getProfileByUserId` (`@Transactional(readOnly = true)`) и `upsertProfile` (`@Transactional`), соблюдение запрета сетевых вызовов в `@Transactional`.
     - `CoroutineDispatchers.kt` (`DefaultCoroutineDispatchers`, `TestCoroutineDispatchers`): единый интерфейс диспетчеров корутин для бэкенда в точном соответствии с Android/KMP (§1.5).
     - Выделенный слой валидации `grpc.validation` (`Violation.kt`, `ViolationsGrpcMapper.kt`, `ProfileRequestValidator.kt`):
       - Защита от `NaN` и `Infinity` через первоочередную проверку `!value.isFinite()`;
       - Реалистичные верхние и нижние границы параметров тела, бюджета и КБЖУ;
       - Сбор всех нарушений одновременно в единый список (детерминированный порядок без `HashSet`);
       - Преобразование нарушений в стандартный `StatusException` с `com.google.rpc.BadRequest` (`FieldViolation`) через `StatusProto` (gRPC Rich Error Model);
       - Использование `kotlinx-coroutines-slf4j` (`MDCContext`) для безопасной передачи `userId` в логирование при переключении потоков корутин на `dispatchers.io`.
     - `ProfileGrpcService.kt`:
       - Реализация `getProfile(request)`: вызов валидатора, переключение на `MDCContext + dispatchers.io`, возврат `ProfileProto`, при отсутствии — `Status.NOT_FOUND`;
       - Реализация `updateProfile(request)`: вызов валидатора, сохранение в PostgreSQL (UPSERT по `userId`), возврат `ProfileProto`;
       - Тонкий контроллер без бойлерплейта ручной валидации и без потери MDC.
  4. **Тестирование и верификация (Testcontainers PostgreSQL 17):**
     - `ProfileGrpcServiceIntegrationTest.kt` (Testcontainers PostgreSQL 17 + gRPC in-process channel):
       - Тест-кейс 1: `getProfile` для несуществующего пользователя возвращает `Status.NOT_FOUND`;
       - Тест-кейс 2: `updateProfile` успешно создаёт профиль, сохраняет в PostgreSQL и возвращает `ProfileProto` со всеми полями КБЖУ;
       - Тест-кейс 3: повторный вызов `updateProfile` обновляет существующий профиль (UPSERT по `userId`), не создавая дубликат;
       - Тест-кейс 4: валидация граничных условий (пустой `userId`, неположительный рост/вес, отрицательный бюджет).
     - `ProfileRequestValidatorTest.kt`: 15 параметризованных и изолированных тестов на все типы нарушений (`REQUIRED`, `OUT_OF_RANGE`, `NOT_FINITE`, включая `NaN`, `Infinity`, превышение максимума и одновременный сбор всех ошибок).
     - `ProfileDtoMapperTest.kt`: 10 unit-тестов на прямое и обратное преобразование enum'ов, целей КБЖУ, профилей и обновления сущностей.
     - `ProfilePersistenceTests.kt`: валидация Liquibase миграций и уникального ограничения `user_id`.
     - `ProfileServiceApplicationTests.kt`: валидация запуска контекста и Actuator liveness/readiness проб.
- **Верификация сборки и линтеров:**
  - `./gradlew :backend:profile-service:test` — 39/39 tests GREEN (BUILD SUCCESSFUL).
  - `./gradlew :backend:profile-service:ktlintCheck` — BUILD SUCCESSFUL (0 нарушений).
  - `./gradlew :backend:profile-service:detekt` — BUILD SUCCESSFUL (0 нарушений).
  - `./gradlew :backend:profile-service:check` — BUILD SUCCESSFUL.

## Checkpoint 2026-11-13 — [T-019 / T-020 / KAN-38] Доменная логика расчёта целей питания (NutritionGoalCalculator) и серверный интерцептор gRPC (GrpcExceptionInterceptor)
- Активные Skills: `spring-grpc-service`, `testing-and-verification`, `diagnose-build-failure`.
- Роли: `implementer` + `test-reviewer`.
- Ветка: `feature/KAN-38-logic-add-target` (задачи T-019, T-020, KAN-38).
- **Реализация:**
  1. **[T-019] Доменный калькулятор целей питания NutritionGoalCalculator:**
     - Пакет: `dev.andre.nutritionist.profile.domain.calculator.NutritionGoalCalculator`.
     - Чистые функции без сайд-эффектов и без зависимостей от Spring/JPA/Android.
     - Формула BMR Миффлина — Сан-Жеора для мужчин (`10*w + 6.25*h - 5*a + 5`), женщин (`10*w + 6.25*h - 5*a - 161`) и unknown (`10*w + 6.25*h - 5*a - 78`).
     - Коэффициенты активности PAL (`SEDENTARY`: 1.2, `LIGHT`: 1.375, `MODERATE`: 1.55, `VERY_ACTIVE`: 1.725).
     - Корректировка калоража под цель (`LOSE_WEIGHT`: дефицит 18% * 0.82, `MAINTAIN`: * 1.0, `GAIN_WEIGHT`: профицит 12% * 1.12).
     - Макронутриенты: белки (2.0 г/кг или 1.5 г/кг при поддержании), жиры (не менее 1.0 г/кг и не менее 25% калоража), углеводы (остаток суточного калоража).
     - Защита от деления на ноль, отрицательных значений и округление `BigDecimal` с `RoundingMode.HALF_UP`.
     - Интеграция в `ProfileService.upsertProfile`: если в `UpdateProfileRequest` отсутствует `nutrition_goal` или `target_calories == 0`, цели автоматически вычисляются через калькулятор и сохраняются в PostgreSQL.
  2. **[T-020] Глобальный серверный интерцептор GrpcExceptionInterceptor:**
     - Пакет: `dev.andre.nutritionist.profile.grpc.interceptor.GrpcExceptionInterceptor`.
     - Аннотации `@Component`, `@GlobalServerInterceptor`, `@Order(Ordered.HIGHEST_PRECEDENCE)`.
     - Реализация `ForwardingServerCallListener` и `ForwardingServerCall` для надёжного перехвата синхронных и асинхронных корутинных исключений gRPC Kotlin.
     - Трансляция статус-кодов:
       * `NoSuchElementException` / `EntityNotFoundException` $\to$ `Status.NOT_FOUND`
       * `IllegalArgumentException` / `IllegalStateException` $\to$ `Status.INVALID_ARGUMENT`
       * `DataIntegrityViolationException` $\to$ `Status.ALREADY_EXISTS`
       * `StatusException` / `StatusRuntimeException` $\to$ пропуск без искажений с сохранением rich error `StatusProto` (`BadRequest`)
       * Прочие непредвиденные `Throwable` $\to$ `Status.INTERNAL` с сокрытием внутренних деталей SQL/стектрейса и логированием ошибки с `userId` в MDC.
  3. **Тестирование и верификация:**
     - `NutritionGoalCalculatorTest.kt`: 8 unit-тестов (формулы для мужчин/женщин, проверка баланса калорий $(белки \times 4) + (жиры \times 9) + (углеводы \times 4) \approx калории$ для всех уровней активности и целей питания, целевой вес, граничные значения).
     - `GrpcExceptionInterceptorTest.kt`: 8 unit-тестов (проверка трансляции всех типов исключений, сохранение `StatusProto`, сокрытие SQL в `Status.INTERNAL`, разворачивание вложенных исключений).
     - `ProfileGrpcServiceIntegrationTest.kt`: интеграционные тесты с реальным Testcontainers PostgreSQL 17 (проверка авторасчёта КБЖУ при отсутствии `nutrition_goal` и при `target_calories == 0`, трансляция `NoSuchElementException` $\to$ `Status.NOT_FOUND`).
- **Верификация сборки и линтеров:**
  - `./gradlew :backend:profile-service:test` — 67/67 tests GREEN (BUILD SUCCESSFUL).
  - `./gradlew :backend:profile-service:ktlintCheck` — BUILD SUCCESSFUL (0 нарушений).
  - `./gradlew :backend:profile-service:detekt` — BUILD SUCCESSFUL (0 нарушений).
  - `./gradlew :backend:profile-service:check` — BUILD SUCCESSFUL.
  - `./gradlew :mobile:shared:allTests` — 59/59 tests GREEN.

## Checkpoint 2026-11-13 — Комплексное архитектурное ревью profile-service
- Активные Skills: `spring-grpc-service`, `testing-and-verification`.
- Роль: `test-reviewer`.
- Создан подробный отчёт: `backend/profile-service/profile-service-review.md`.
- **Ключевые выводы ревью:**
  1. Выявлены и локализованы 2 критических бага:
     - `IdempotencyServerInterceptor`: `IllegalStateException: sendHeaders has not been called` при отдаче закешированного ответа.
     - `AbstractPostgresIntegrationTest`: конфликт кеширования `ApplicationContext` Spring и закрытия контейнера `@Testcontainers` (требуется Singleton pattern).
  2. Выявлены архитектурные несоответствия:
     - Нарушение AGENTS.md §1.3 (отсутствие `UserId` value class, использование сырых `String`).
     - В миграции `002-add-version-column.xml` колонка `version` добавлена, но поле `@Version` отсутствует в `ProfileEntity` (оптимистическая блокировка не работает).
     - Дублирование регистрации перехватчиков в `@GrpcService(interceptors = [...])` при наличии `@GlobalServerInterceptor`.
  3. Выявлен мёртвый код:
     - `ProfileDtoMapper`: неиспользуемые методы `toNewEntity`, `applyUpdates`, `toProto`, `toEmbeddable`.
     - `ProfileJpaRepository`: неиспользуемый метод `existsByUserId`.
     - Избыточные зависимости `spring-kafka`, `avro-runtime` и фиктивный тест `AvroCompatibilityTest.kt`.

## Checkpoint 2026-11-13 — [T-021 / T-022 / T-023 / KAN-39] Безопасность gRPC (SecurityInterceptor), регистрация пользователей (KeycloakClient / UserRegistrationService) и Testcontainers интеграционные тесты
- Активные Skills: `spring-grpc-service`, `testing-and-verification`, `diagnose-build-failure`.
- Роли: `implementer` + `test-reviewer`.
- Ветка: `feature/KAN-38-logic-add-target` (задачи T-021, T-022, T-023).
- **Реализация:**
  1. **[T-021] Безопасность и разграничение доступа (Auth & Access Control):**
     - `AuthContext.kt`: модель контекста безопасности с `userId`, `roles`, `isAuthenticated`, `isAdmin` / `isService` проверками, `AUTH_CONTEXT_KEY` для gRPC Context и CoroutineContext extension.
     - `SecurityGrpcInterceptor.kt`: глобальный перехватчик `@Component @GlobalServerInterceptor @Order(100)`. Извлечение метаданных `x-user-id`, `x-user-roles` (CSV / JSON array) с fallback на парсинг Bearer JWT (`sub`, `realm_access.roles`). Передача `AuthContext` в gRPC `Contexts.interceptCall` и автоматическая привязка `userId` в MDC.
     - `ProfileGrpcService.kt`: проверка прав доступа `checkAccess(targetUserId)`:
       * Неавторизованный запрос $\to$ `Status.UNAUTHENTICATED`;
       * Обычный пользователь (`ROLE_USER`) с несовпадающим `targetUserId` $\to$ `Status.PERMISSION_DENIED`;
       * Владелец (`userId == targetUserId`) или администратор (`ROLE_ADMIN` / `ROLE_SERVICE`) $\to$ успешный доступ к операциям `getProfile` и `updateProfile`.
  2. **[T-023] Регистрация пользователя (Keycloak Admin / Синхронизация профиля):**
     - `KeycloakClient.kt` (`KeycloakClientImpl` на Spring `RestClient`): создание пользователя в Keycloak Admin REST API (`POST /admin/realms/{realm}/users`).
     - Сетевой вызов Keycloak выполняется **строго вне `@Transactional`** (`AGENTS.md §4.1`). Конфликт (409 Conflict) транслируется в `Status.ALREADY_EXISTS` без создания осиротевших записей в `profile_db`.
     - `UserRegistrationService.kt`: оркестрация создания пользователя в Keycloak, расчёт стартовых целей КБЖУ через `NutritionGoalCalculator` и атомарный UPSERT в PostgreSQL.
     - Поддержка идемпотентности с кешированием в `IdempotencyStore`.
     - Исправлен `IdempotencyServerInterceptor`: добавлен обязательный вызов `call.sendHeaders(Metadata())` перед отправкой кешированного gRPC-ответа.
  3. **[T-022] Тестирование и верификация (Testcontainers PostgreSQL 17):**
     - `AbstractPostgresIntegrationTest.kt`: контейнер PostgreSQL 17 переведён на singleton pattern (`.apply { start() }`) для предотвращения остановки между тест-классами.
     - `FakeKeycloakClient.kt` и `TestKeycloakConfig.kt`: чистый in-memory фейк (`AGENTS.md §5.1`) без использования MockK.
     - `ProfileGrpcServiceIntegrationTest.kt` расширен до 19 интеграционных тестов:
       * Проверка `Status.UNAUTHENTICATED` при отсутствии заголовков авторизации;
       * Проверка `Status.PERMISSION_DENIED` при попытке доступа к чужому профилю с ролью `ROLE_USER`;
       * Проверка успешного доступа владельца профиля (`authenticatedUserId == targetUserId`);
       * Проверка обхода ограничений владельца для `ROLE_ADMIN` и `ROLE_SERVICE`;
       * Проверка аутентификации через Bearer JWT токен с корректным извлечением roles и sub;
       * Проверка идемпотентности и авторасчёта КБЖУ.
- **Верификация сборки, тестов и линтеров:**
  - `./gradlew :backend:profile-service:test` — 82/82 tests GREEN (100% успех на Testcontainers PostgreSQL 17).
  - `./gradlew :backend:profile-service:ktlintCheck` — BUILD SUCCESSFUL (0 нарушений).
  - `./gradlew :backend:profile-service:detekt` — BUILD SUCCESSFUL (0 нарушений).
  - `./gradlew :backend:profile-service:check` — BUILD SUCCESSFUL.













