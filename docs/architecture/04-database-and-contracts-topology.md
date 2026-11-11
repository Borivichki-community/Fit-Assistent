# 04 — Топология Баз Данных, Контрактов и Межсервисного Взаимодействия

> **Проект:** «Бюджетный нутрициолог»  
> **Архитектурный стандарт:** Offline-First Client (Room KMP) · gRPC / Protobuf · Event-Driven Microservices (Spring Boot 4, PostgreSQL 17, Kafka 3.9 / Avro, Redis 7).

---

## 1. Общая архитектурная концепция

Система объединяет два ключевых контура:
1. **Клиентский оффлайн-контур (`:mobile`)**: мобильное устройство хранит данные локально в **Room KMP 2.8.x** (SQLite). Любые действия (запись приёма пищи, просмотр калоража за день) выполняются **мгновенно с нулевой сетевой задержкой**.
2. **Серверный распределённый контур (`:backend`)**: микросервисная архитектура с изолированными базами данных для каждого сервиса (Database-per-Service). Выступает как **System of Record (система долговременного хранения и бэкапа)**, движок **AI-генерации рецептов (`recipe-ai-service`)**, синхронизатор цен супермаркетов (`catalog-price-service`) и аналитическая платформа.

```
                          ┌──────────────────────────────────────┐
                          │     Kubernetes Envoy Gateway         │
                          │   JWT SecurityPolicy + Rate Limit    │
                          └──────────────┬───────────────────────┘
                                         │ gRPC (HTTP/2)
               ┌─────────────────────────┼──────────────────────────┐
               ▼                         ▼                          ▼
    ┌─────────────────────┐   ┌─────────────────────┐    ┌─────────────────────┐
    │   profile-service   │   │catalog-price-service│    │    diary-service    │
    │  PostgreSQL 17      │   │  PostgreSQL 17      │    │  PostgreSQL 17      │
    │  (profile_db)       │   │  (catalog_db)       │    │  (diary_db) + Redis │
    └──────────┬──────────┘   └──────────┬──────────┘    └──────────┬──────────┘
               │                         │                          │
               │ user.profile.updated    │ food.price.updated       │ diary.entry.created
               │                         │ user.favorite.changed    │
               └────────────┬────────────┴─────────────┬────────────┴────────────┘
                            │                          │
                            ▼                          ▼
               ┌───────────────────────────┐ ┌─────────────────────────┐
               │     recipe-ai-service     │ │     food-cv-service     │
               │   Gemini LLM + Redis      │ │  Vision AI + Redis      │
               └───────────────────────────┘ └─────────────────────────┘
```

---

## 2. Детальная карта баз данных и сущностей

### 2.1 Сервис `profile-service`
* **СУБД:** PostgreSQL 17 (`profile_db`).
* **Контракт:** [`profile.proto`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/proto/nutritionist/profile/v1/profile.proto) (`com.budgetntr.profile.v1.ProfileService`).
* **Таблица `profiles`:**
  * `id` (`UUID`, PK)
  * `user_id` (`VARCHAR(64)`, UNIQUE, индекс) — привязка к Keycloak `sub`.
  * `height_cm` (`INT`), `weight_kg` (`NUMERIC(5, 2)`).
  * `gender` (`VARCHAR(20)`) — `MALE`, `FEMALE`, `UNSPECIFIED`.
  * `activity_level` (`VARCHAR(30)`) — `SEDENTARY`, `LIGHT`, `MODERATE`, `VERY_ACTIVE`. (Коэффициент образа жизни для формулы метаболизма TDEE).
  * `goal_type` (`VARCHAR(30)`) — `LOSE_WEIGHT`, `MAINTAIN`, `GAIN_WEIGHT`.
  * `target_calories` (`INT`), `target_proteins_grams` (`NUMERIC(6, 2)`), `target_fats_grams` (`NUMERIC(6, 2)`), `target_carbs_grams` (`NUMERIC(6, 2)`), `target_weight_kg` (`NUMERIC(5, 2)`).
  * `weekly_budget` (`NUMERIC(10, 2)`) — недельный бюджет на питание в рублях.
  * `created_at`, `updated_at` (`TIMESTAMPTZ`).

---

### 2.2 Сервис `diary-service`
* **СУБД:** PostgreSQL 17 (`diary_db`) + Redis 7 (кэширование идемпотентности).
* **Контракт:** `proto/nutritionist/diary/v1/diary.proto` (`com.budgetntr.diary.v1.DiaryService`).
* **Таблица `diary_entries`:**
  * `id` (`UUID`, PK).
  * `user_id` (`VARCHAR(64)`), `idempotency_key` (`VARCHAR(64)`, UNIQUE, индекс).
  * `food_item_id` (`UUID`), `food_name` (`VARCHAR(255)`).
  * `quantity_grams` (`NUMERIC(7, 2)`).
  * `calories` (`NUMERIC(7, 2)`), `proteins` (`NUMERIC(6, 2)`), `fats` (`NUMERIC(6, 2)`), `carbs` (`NUMERIC(6, 2)`).
  * `price` (`NUMERIC(10, 2)`) — расчётная стоимость в рублях на момент приёма пищи.
  * `meal_type` (`VARCHAR(20)`) — `BREAKFAST`, `LUNCH`, `DINNER`, `SNACK`.
  * `recorded_at` (`TIMESTAMPTZ`, составной индекс с `user_id`).
* **Таблица `outbox_messages` (Transactional Outbox):**
  * `id` (`UUID`, PK), `aggregate_type` (`VARCHAR(50)`), `aggregate_id` (`VARCHAR(64)`).
  * `topic` (`VARCHAR(100)`), `payload` (`BYTEA` — Avro `CaloriesConsumedEvent`).
  * `status` (`VARCHAR(20)` — `PENDING`, `PUBLISHED`), `created_at` (`TIMESTAMPTZ`).

---

### 2.3 Сервис `catalog-price-service`
* **СУБД:** PostgreSQL 17 (`catalog_db`) + Redis 7 (кэширование цен и горячих продуктов).
* **Контракт:** `proto/nutritionist/catalog/v1/catalog.proto` (`com.budgetntr.catalog.v1.CatalogService`).
* **Таблицы:**
  * `food_categories` (`id`, `name`, `icon_url`).
  * `food_items` (`id`, `category_id`, `name`, `barcode`, `calories_per_100g`, `proteins_per_100g`, `fats_per_100g`, `carbs_per_100g`, `price_per_100g`).
  * `recipes` (`id`, `name`, `description`, `instructions`, `total_calories`, `total_price`, `prep_time_minutes`, `is_active`).
  * `recipe_ingredients` (`id`, `recipe_id`, `food_item_id`, `quantity_grams`).
  * `meal_plan_templates` (`id`, `name`, `target_calories`, `estimated_price`, `is_active`, `created_at`).
  * `meal_plan_items` (`id`, `meal_plan_id`, `recipe_id`, `meal_type` — `BREAKFAST`/`LUNCH`/`DINNER`/`SNACK`).
  * **Подсистема «Избранное» (User Favorites):**
    * `user_favorite_foods` (`id` UUID PK, `user_id` VARCHAR(64), `food_item_id` UUID FK, `added_at` TIMESTAMPTZ, UNIQUE(`user_id`, `food_item_id`)).
    * `user_favorite_recipes` (`id` UUID PK, `user_id` VARCHAR(64), `recipe_id` UUID FK, `added_at` TIMESTAMPTZ, UNIQUE(`user_id`, `recipe_id`)).
    * `user_favorite_meal_plans` (`id` UUID PK, `user_id` VARCHAR(64), `meal_plan_id` UUID FK, `added_at` TIMESTAMPTZ, UNIQUE(`user_id`, `meal_plan_id`)).
  * **Таблица `outbox_messages` (Transactional Outbox):**
    * `id` (`UUID`, PK), `aggregate_type` (`VARCHAR(50)`), `aggregate_id` (`VARCHAR(64)`).
    * `topic` (`VARCHAR(100)` — `user.favorite.changed`), `payload` (`BYTEA` — Avro `UserFavoriteChangedEvent`).
    * `status` (`VARCHAR(20)` — `PENDING`, `PUBLISHED`), `created_at` (`TIMESTAMPTZ`).

---

### 2.4 Сервис `recipe-ai-service`
* **Хранилище:** Redis 7.
* **Контракт:** `proto/nutritionist/recipe/v1/recipe_ai.proto`.
* **Назначение:** Кэш сгенерированных ИИ-рационов (TTL 24 часа), векторные индексы рецептов.
* **Логика:** 
  1. Вычитает факт съеденного из дневника (`diary.entry.created`) и отдаёт персонализированные рецепты строго под остаток калорий и остаток бюджета.
  2. Слушает события `user.favorite.changed` из Kafka: строит матрицу пользовательских вкусовых предпочтений (User Affinity / Preference Profile) в Redis. Любимые продукты, рецепты и готовые дневные меню получают повышенный вес при алгоритмической генерации суточного рациона.

---

### 2.5 Сервис `food-cv-service`
* **Хранилище:** Redis 7.
* **Контракт:** `proto/nutritionist/food_cv/v1/food_cv.proto`.
* **Назначение:** Кэш распознавания изображений по перцептивному хэшу `pHash`.

---

### 2.6 Мобильное приложение (`:mobile:shared`)
* **СУБД:** Room KMP 2.8.x SQLite (`AppDatabase` с `BundledSQLiteDriver`).
* **Таблицы:**
  * `profiles`: локальный кэш пользователя (PK `userId`, поля КБЖУ, рост/вес, бюджет).
  * `diary_entries`: журнал питания (PK `id`, `idempotencyKey`, нутриенты, `syncStatus` = `PENDING`/`SYNCED`/`FAILED`).
  * `food_items`: локальный кэш частых продуктов каталога.
  * `favorite_items`: локальный оффлайн-кэш избранных рецептов, продуктов и меню на день (`id`, `userId`, `targetType` = `FOOD`/`RECIPE`/`MEAL_PLAN`, `targetId`, `title`, `calories`, `price`, `syncStatus` = `PENDING`/`SYNCED`, `idempotencyKey`).
* **Оффлайн-синхронизация:**
  * Нажатие на «Сердечко» в UI мгновенно переключает статус в Room и отображается на экране (0 мс задержки).
  * `SyncFavoritesWorker` (WorkManager) с `BackoffPolicy.EXPONENTIAL` отправляет мутацию `AddFavorite` / `RemoveFavorite` по gRPC на сервер с метаданными `idempotency-key`. После подтверждения сервером запись переводится в `SYNCED`.

---

## 3. Сквозная матрица соответствия сущностей

| Концепция / Домен | `profile.proto` | PostgreSQL (`profile_db`) | Room SQLite (`:mobile:shared`) |
|---|---|---|---|
| ID пользователя | `string user_id` | `user_id VARCHAR(64)` | `val userId: String` (PK) |
| Биометрия | `height_cm`, `weight_kg` | `height_cm INT`, `weight_kg NUMERIC(5,2)` | `val heightCm: Int`, `weightKg: Double` |
| Коэффициент активности | `ActivityLevelProto` | `activity_level VARCHAR(30)` | `val activityLevel: String` |
| Цели по КБЖУ | `NutritionGoalProto` | `target_calories`, `target_proteins_grams`... | `val targetCalories: Int`, `targetProteinsGrams: Double`... |
| Бюджет | `double weekly_budget` | `weekly_budget NUMERIC(10, 2)` | `val weeklyBudget: Double` |

---

## 4. Анализ альтернативных вариантов и обоснование выбора

### Альтернатива 1: «Всё на мобильном устройстве (Local-Only SQLite, без сервера)»
* **Суть:** Приложение считает всё в Room и никуда ничего не отправляет.
* **Почему не подходит для «Бюджетного нутрициолога»:**
  1. **Потеря данных:** При удалении приложения, смене телефона или сбросе кэша пользователь навсегда теряет историю питания за недели и месяцы.
  2. **Невозможность работы ИИ-нутрициолога:** На смартфоне невозможно локально запускать тяжёлые LLM и держать парсер актуальных цен всех супермаркетов. Бэкенд обязан знать факт съеденного, чтобы нейросеть подобрала ужин под оставшиеся 500 ккал и 200 ₽.
  3. **Нет мультидевайса:** Невозможно переключиться между телефоном, планшетом или веб-версией.

### Альтернатива 2: «Монолитный бэкенд с единой базой данных»
* **Суть:** Все таблицы (`profiles`, `diary`, `catalog`, `recipes`) в одной схеме монолитного Spring Boot приложения.
* **Почему уступает:**
  1. **Взаимная блокировка ресурсов:** Тяжёлые вызовы генерации рецептов через Gemini LLM или обработка картинок Computer Vision вызывают нехватку потоков/соединений и «вешают» быстрые операции записи дневника и авторизации.
  2. **Размытие границ ответственности:** Разработчики начинают делать кросс-доменные прямые `JOIN` между каталогом цен и личным профилем, превращая код в спагетти.
  3. **Невозможность независимого масштабирования:** Сервис дневника требует быстрого масштабирования по I/O записи, а AI-сервис — по CPU/памяти и числу асинхронных вызовов к внешним API.

### Альтернатива 3: «Классический REST + JSON over HTTP/1.1»
* **Суть:** REST-эндпоинты вместо gRPC, JSON вместо Protobuf/Avro.
* **Почему уступает:**
  1. **Оверхед по трафику и парсингу:** Текстовый JSON в разы тяжелее компактного бинарного Protobuf. Для мобильного приложения на нестабильном мобильном интернете gRPC по HTTP/2 даёт колоссальный выигрыш в скорости и энергопотреблении.
  2. **Отсутствие строгого типизированного контракта:** В REST изменения структуры DTO легко ломают клиент в рантайме. В gRPC/Buf строгий контроль обратной совместимости (`buf breaking`) гарантирует целостность API ещё на этапе сборки в CI.

### Альтернатива 4: «Хранение избранного в profile-service и отправка напрямую с клиента через Kafka»
* **Суть:** Добавить таблицу `favorite_recipes` в `profile-service`, а мобильный клиент при добавлении в избранное отправляет сообщение напрямую в топик Kafka.
* **Почему это антипаттерн и категорически отвергнуто:**
  1. **Нарушение Bounded Contexts (DDD):** `profile-service` управляет биометрией (рост, вес, цели КБЖУ, бюджет). Рецепты, продукты и меню принадлежат домену `catalog-price-service`. Если положить избранное в профиль, `profile-service` не сможет обогатить список данными (название, КБЖУ, ингредиенты, актуальные цены), что приведёт к N+1 сетевым запросам и размытию доменных зон.
  2. **Нарушение безопасности периметра (Zero-Trust):** Мобильные клиенты **никогда не пишут напрямую в Kafka-брокер**. Брокер находится внутри защищённого контура Kubernetes. Открытие портов Kafka в интернет ломает аудит, JWT-валидацию на Envoy Gateway и открывает вектор DDoS-атак.
  3. **Отсутствие синхронного подтверждения (ACK/Error):** Прямая запись в Kafka не даёт клиенту валидации (существует ли рецепт, не удалён ли он).
  4. **Правильное решение:** Мобильный клиент вызывает gRPC-метод `AddFavorite` сервиса `catalog-price-service` через Envoy Gateway. Сервис атомарно сохраняет связь в `catalog_db` и пишет событие в локальный Transactional Outbox. Затем Outbox Processor асинхронно публикует `user.favorite.changed` в Kafka, откуда его забирает `recipe-ai-service` для персонализации рекомендаций.

### Почему наш выбор — лучший (Offline-First + Microservices + gRPC + Outbox):
1. **Максимальная отзывчивость для пользователя:** Ввод приёмов пищи происходит мгновенно (0 мс) благодаря Room KMP, вне зависимости от качества связи в супермаркете или на улице.
2. **Гарантированная надёжность доставки:** Паттерн `SyncDiaryWorker` + `IdempotencyKey` + Transactional Outbox исключает дублирование записей и потерю данных при сбоях сети.
3. **Безопасность и масштабируемость:** Envoy Gateway валидирует токены OAuth 2.1 PKCE на периметре сети, разгружая сервисы от криптографических проверок.
4. **Умный ИИ-ассистент:** Серверная агрегация данных даёт возможность реализовать полноценную ценность проекта — автоматическое составление меню под реальный остаток КБЖУ и рублей.
