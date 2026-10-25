# STATE.md — Журнал прогресса «Бюджетный нутрициолог»

> Этот файл читается агентом **перед началом любой задачи**.
> Обновляется после каждого завершённого этапа.

Обновлено: 2026-10-20T21:23

---

## Статус проекта: Инфраструктура запущена и проверена, мобильные модули компилируются и протестированы, готовность к пушу и реализации Шага 4

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

---

### В работе

- [-] **Шаг 4:** Реализация первой фичи (diary: AddDiaryEntry UseCase + Room Entity + SyncWorker).

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
