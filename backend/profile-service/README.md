# Profile Service (`:backend:profile-service`)

> Микросервис управления биометрическим профилем пользователя, целями питания (КБЖУ) и недельным бюджетом.

> **Статус:** Profile Proto существует, серверные RPC пока являются каркасом. SQL/JPA ниже — целевая модель; master-changelog пуст.

---

## 1. База данных и схема (`profile_db`)

- **СУБД:** PostgreSQL 17 (порт `5432`, база `profile_db`).
- **Инструмент миграций:** Liquibase (`src/main/resources/db/changelog/db.changelog-master.yaml`).
- **Стратегия первичных ключей:** UUID v7 / Sequential UUID.

### Схема таблицы `profiles` (`001-init-profile-table.xml`)

```sql
CREATE TABLE profiles (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    height_cm INTEGER NOT NULL,
    weight_kg NUMERIC(5, 2) NOT NULL,
    gender VARCHAR(20) NOT NULL,            -- MALE, FEMALE, UNSPECIFIED
    activity_level VARCHAR(30) NOT NULL,    -- SEDENTARY, LIGHT, MODERATE, VERY_ACTIVE, UNSPECIFIED
    goal_type VARCHAR(30) NOT NULL,         -- LOSE_WEIGHT, MAINTAIN, GAIN_WEIGHT, UNSPECIFIED
    target_calories INTEGER NOT NULL,
    target_proteins_grams NUMERIC(6, 2) NOT NULL,
    target_fats_grams NUMERIC(6, 2) NOT NULL,
    target_carbs_grams NUMERIC(6, 2) NOT NULL,
    target_weight_kg NUMERIC(5, 2) NOT NULL,
    weekly_budget NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_profiles_user_id ON profiles(user_id);
```

---

## 2. JPA Сущности

- **`ProfileEntity`** (`dev.andre.nutritionist.profile.persistence.entity.ProfileEntity`):
  Корень агрегата профиля. Содержит биометрию, пол, коэффициент активности, бюджет, тип цели и встраиваемый объект `nutritionGoal`.
- **`NutritionGoalEmbeddable`** (`dev.andre.nutritionist.profile.persistence.entity.NutritionGoalEmbeddable`):
  Встраиваемый (`@Embeddable`) компонент нутрициологических целей (целевые калории, белки, жиры, углеводы, целевой вес). Соответствует `NutritionGoalProto`.
- **`Enums`**:
  - `Gender` (`MALE`, `FEMALE`, `UNSPECIFIED`)
  - `ActivityLevel` (`SEDENTARY`, `LIGHT`, `MODERATE`, `VERY_ACTIVE`, `UNSPECIFIED`) — коэффициент образа жизни для расчета метаболизма (TDEE).
  - `GoalType` (`LOSE_WEIGHT`, `MAINTAIN`, `GAIN_WEIGHT`, `UNSPECIFIED`)

---

## 3. Protobuf Контракт

- **Файл контракта:** `proto/nutritionist/profile/v1/profile.proto`
- **Пакет:** `com.budgetntr.profile.v1` (Java/Kotlin: `com.budgetntr.proto.profile.v1`)

```protobuf
syntax = "proto3";

package com.budgetntr.profile.v1;

import "google/protobuf/timestamp.proto";

service ProfileService {
  rpc GetProfile(GetProfileRequest) returns (ProfileProto);
  rpc UpdateProfile(UpdateProfileRequest) returns (ProfileProto);
}

enum GenderProto {
  GENDER_UNSPECIFIED = 0;
  GENDER_MALE = 1;
  GENDER_FEMALE = 2;
}

enum ActivityLevelProto {
  ACTIVITY_LEVEL_UNSPECIFIED = 0;
  ACTIVITY_LEVEL_SEDENTARY = 1;
  ACTIVITY_LEVEL_LIGHT = 2;
  ACTIVITY_LEVEL_MODERATE = 3;
  ACTIVITY_LEVEL_VERY_ACTIVE = 4;
}

enum GoalTypeProto {
  GOAL_TYPE_UNSPECIFIED = 0;
  GOAL_TYPE_LOSE_WEIGHT = 1;
  GOAL_TYPE_MAINTAIN = 2;
  GOAL_TYPE_GAIN_WEIGHT = 3;
}

message NutritionGoalProto {
  int32 target_calories = 1;
  double target_proteins_grams = 2;
  double target_fats_grams = 3;
  double target_carbs_grams = 4;
  double target_weight_kg = 5;
}

message ProfileProto {
  string id = 1;
  string user_id = 2;
  int32 height_cm = 3;
  double weight_kg = 4;
  GenderProto gender = 5;
  ActivityLevelProto activity_level = 6;
  GoalTypeProto goal_type = 7;
  NutritionGoalProto nutrition_goal = 8;
  google.protobuf.Timestamp updated_at = 9;
  double weekly_budget = 10;
}
```

---

## 4. Владение профилем и события регистрации

Profile хранит биометрию, цели питания и установленный пользователем недельный лимит бюджета. Мобильный клиент использует `GetProfile` / `UpdateProfile` и локальную копию в Room. Остаток лимита вычисляет мобильный доменный слой по снимкам дневника; это не отдельный серверный денежный счёт.

### Событие `UserRegisteredEvent` (T-025)
[UserRegisteredEvent](../../shared/contracts/src/main/avro/UserRegisteredEvent.avsc) описывает факт регистрации пользователя (`id`, `email`, `registrationTimestamp`, `source`).
- **Сценарий публикации:** При первом вызове `UpdateProfile` (создание нового профиля пользователя) сервис сохраняет профиль и атомарно записывает событие `UserRegisteredEvent` в таблицу `outbox_messages` в рамках одной ACID-транзакции PostgreSQL.
- **Идемпотентность:** Повторные вызовы `UpdateProfile` для существующего профиля обновляют биометрию, но НЕ генерируют повторные события регистрации.

---

## 5. Архитектура Kafka, партиционирование и надежность доставки (T-024)

### 5.1 Транзакционный Outbox (Transactional Outbox Pattern)

Для исключения распределённых транзакций (2PC) и исключения сетевых вызовов в Kafka внутри `@Transactional` применён паттерн **Transactional Outbox**:
1. **Таблица `outbox_messages`:**
   - `id UUID PRIMARY KEY`: уникальный идентификатор сообщения.
   - `aggregate_type VARCHAR(64)`: тип агрегата (`USER`).
   - `aggregate_id VARCHAR(64)`: идентификатор агрегата (`userId`), используемый как ключ партиционирования.
   - `topic VARCHAR(128)`: целевой топик Kafka (`user.registered`).
   - `payload BYTEA`: локально сериализованное бинарное Avro-сообщение (`SpecificDatumWriter`).
   - `status VARCHAR(20)`: `PENDING`, `PUBLISHED`, `FAILED`.
   - `attempts INT`: счётчик попыток публикации.
   - `created_at TIMESTAMPTZ`, `published_at TIMESTAMPTZ`.
   - Частичный индекс `idx_outbox_pending` по `created_at WHERE status = 'PENDING'`.
2. **Фоновый процессор `OutboxProcessor`:**
   - Опрашивает базу по расписанию (`fixedDelay = 500ms`) вне контекста бизнес-транзакции.
   - Выборка батча (50 записей) использует `FOR NO KEY UPDATE SKIP LOCKED` через `TransactionTemplate`, предотвращая конкуренцию при масштабировании инстансов сервиса.
   - Публикация в Kafka выполняется через `KafkaTemplate<String, Any>` **строго вне транзакции БД**.
   - Ключ партиции — строго `aggregate_id` (`userId`), гарантирующий FIFO-очерёдность событий одного пользователя внутри партиции.
   - При получении ACK от Kafka статус атомарно обновляется на `PUBLISHED`.
   - При ошибке инкрементируется `attempts`, при превышении лимита (5 попыток) выставляется статус `FAILED` с логированием в Dead Letter Queue / алертом.

### 5.2 Расчёт количества партиций (Jay Kreps / Confluent Formula)

Количество партиций топика `user.registered` на Фазе 1 выбрано равным **3**.

Обоснование по формуле Конфлюент / Джея Крепса (Jay Kreps):
$$P = \max\left(\left\lceil \frac{T_{target}}{T_{consumer}} \right\rceil,\ N_{consumers}\right)$$

Где:
- $T_{target}$ — целевая пропускная способность системы:
  - При 50 000 MAU и равномерном распределении: $\approx 0.84$ регистраций/сек.
  - При пиковых нагрузках (маркетинговые кампании, 1 000 000 MAU): $\approx 14$ регистраций/сек.
- $T_{consumer}$ — пропускная способность одного экземпляра обработчика (консьюмера): $\approx 100$ сообщений/сек (с учётом создания базовых справочников, отправки welcome-email и кэширования).
- $\left\lceil \frac{T_{target}}{T_{consumer}} \right\rceil = \left\lceil \frac{14}{100} \right\rceil = 1$.
- $N_{consumers}$ — целевое количество параллельных консьюмеров сервисов-подписчиков (например, notification-service, analytics-service) для обеспечения высокой доступности (HA) и нулевого простоя при rolling-update: $N_{consumers} = 3$.

Следовательно:
$$P = \max(1, 3) = 3$$

**Ресурсный баланс на VPS:**
- Топик с 3 партициями на 1 KRaft-брокере минимизирует количество открытых файловых дескрипторов и накладные расходы на буферы памяти (в отличие от овер-партиционирования 10+), позволяя укладываться в лимиты памяти скромного VPS.
- В то же время 3 партиции допускают горизонтальное масштабирование пула потребителей до 3 реплик без необходимости пересоздания топика или репартиционирования.

### 5.3 Архитектура KRaft (Kafka Raft Metadata Mode)

- **ZooKeeper-less:** В кластере используется встроенный кворум метаданных на базе алгоритма Raft (KRaft).
- **Экономия ресурсов VPS:** Устранение отдельного JVM-процесса Apache ZooKeeper экономит от 512 МБ до 1 ГБ оперативной памяти, что критично для VPS хостинга.
- **Быстрый консенсус и мгновенный failover:** Метаданные хранятся в специальном логе внутри брокеров; смена контроллера происходит за десятки миллисекунд без необходимости выгрузки/загрузки сотен z-нод.

### 5.4 Управление Offset'ами и защита от дубликатов

- **`enable.auto.commit: false`:** Автоматический коммит смещений отключён во всех консьюмерах. Автокоммит по таймеру несёт риск потери сообщений, если приложение упало после получения батча, но до его фактической обработки в БД.
- **`AckMode.MANUAL_IMMEDIATE`:** Смещение коммитится строго после успешной фиксации локальной транзакции подписчика.
- **`auto.offset.reset: earliest`:** При первом подключении консьюмерной группы или утере оффсетов обработка начинается с самого раннего доступного события, исключая потерю сообщений.
- **Идемпотентный продюсер:** В конфигурации продюсера включены `acks: all` и `enable.idempotence: true`. Брокер дедуплицирует повторные сетевые пакеты по `ProducerId` и `SequenceNumber`.

### 5.5 Защита от Stop-The-World Rebalance и контроль Lag

Rebalance-штормы возникают, когда обработка батча занимает больше времени, чем таймаут сессии консьюмера, в результате чего координатор считает узел отказавшим и инициирует дорогостоящее перераспределение партиций.

Для предотвращения деградации кластера настроены следующие параметры:
- **`max.poll.records = 50`:** Батч выборки из Kafka ограничен 50 записями, чтобы время его обработки было предсказуемым и гарантированно не превышало порог.
- **`max.poll.interval.ms = 300000` (5 минут):** Достаточно большой интервал между вызовами `poll()`, исключающий ложное исключение живого консьюмера из группы при кратковременных задержках ввода-вывода или GC-паузах.
- **Таймауты внешних вызовов:** Все сетевые и асинхронные операции в сервисе обёрнуты в таймауты (`send-timeout-seconds: 10`, `withTimeout(20_000)` в корутинах). Никакой сетевой вызов не может заблокировать цикл опроса или транзакцию базы данных.
- **Метрики Micrometer:**
  - `outbox.messages.published`: счётчик успешно опубликованных событий.
  - `outbox.pending.count`: текущий размер очереди неотправленных сообщений (лаг outbox).
