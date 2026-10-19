# STATE.md — Журнал прогресса «Бюджетный нутрициолог»

> Этот файл читается агентом **перед началом любой задачи**.
> Обновляется после каждого завершённого этапа.

Обновлено: 2026-10-19T23:55

---

## Статус проекта: Инфраструктура готова, переход к реализации первой фичи (Шаг 4)

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

---

### Активный Skill текущей сессии

`contracts-proto-and-avro` (завершение настройки контрактов и инфраструктуры).
