# Profile Service (`:backend:profile-service`)

> Микросервис управления биометрическим профилем пользователя, целями питания (КБЖУ) и недельным бюджетом.

> **Статус:** реализованы GetProfile/UpdateProfile и T-054: consumer CaloriesConsumedEvent, журнал processed_events, дневной остаток и DLT через общий Outbox. Миграции подключены в master-changelog.

---

## 1. База данных и схема (`profile_db`)

- **СУБД:** PostgreSQL 17 (порт `5432`, база `profile_db`).
- **Инструмент миграций:** Liquibase (`src/main/resources/db/changelog/db.changelog-master.xml`).
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
  rpc GetDailyCaloriesBalance(GetDailyCaloriesBalanceRequest) returns (DailyCaloriesBalanceResponse);
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

## 4. Дневной остаток калорий — T-054

Consumer группы `profile-calories-v1` читает `diary.entry.created` с `isolation.level=read_committed` и без auto-commit. Ключ должен соответствовать UUID пользователя. Avro-схема и общий маппер не менялись. Нулевые калории допустимы, отрицательные отклоняются.

`processed_events` хранит факты потребления и SHA-256 канонических данных: calories, timestamp в миллисекундах, mealType, notes. Уникальность — `(consumer_name, user_id, meal_id)`. Повтор не увеличивает сумму; изменённое содержимое с теми же ID считается конфликтом. INSERT ON CONFLICT и проверка выполняются одной DB-транзакцией; offset подтверждается после её завершения. После commit БД и сбоя offset повтор безопасен. Журнал не очищается по TTL.

`GetDailyCaloriesBalance` принимает `user_id` (UUID), ISO `date` и IANA `time_zone`. Возвращает date, time_zone, target_calories, consumed_calories, remaining_calories. Требует доверенный `x-user-id` от Envoy; запрос другого пользователя запрещён. Этот RPC не использует Redis-кэш идемпотентных ответов. Прямой внешний доступ к backend должен быть закрыт: JWT проверяется на Gateway, клиентский x-user-id заменяется доверенной identity.

Расчёт: текущая цель профиля минус SUM фактов за полуоткрытый интервал от начала дня до начала следующего дня в указанной зоне. Учитываются DST, поздние события и изменение цели. Перерасход возвращается отрицательным значением. Профиль и сумма читаются в одной REPEATABLE READ транзакции; если профиля нет — NOT_FOUND. События можно принять до создания профиля.

Это асинхронная проекция доставленных фактов добавления, а не гарантия полной истории: consumer lag, события в DLT и истёкший Kafka retention могут давать неполные данные. `DeleteMeal` не компенсируется: действующая схема не содержит удаления/корректировки. Для остатка актуального дневника после удалений нужен отдельный контракт компенсации. Мобильный расчёт по Room, включая PENDING, остаётся источником UI и контекста Recipe AI.

### Retry и dead-letter topic

`profile.calories.max-attempts=4` — всего четыре обработки временной ошибки. Backoff: 1000, 2000, 4000 мс, предел 10000 мс; параметры initial-backoff-ms/max-backoff-ms. Ошибки валидации не повторяются. Таймауты Schema Registry ограничены 2 секундами; внутренние HTTP retries отключены, повтором управляет listener.

После исчерпания попыток recoverer сохраняет исходные bytes, nullable key/value, все заголовки и координаты источника в `outbox_messages`. Уникальность по координатам исходной записи предотвращает повторную постановку. Если DB-запись не удалась, offset не подтверждается. Отдельный процессор общего `:backend:outbox` публикует в `diary.entry.created.profile.dlt` вне DB-транзакции; PUBLISHED выставляется только после ACK. Для повреждённого Avro не требуется успешная десериализация или Schema Registry при отправке в DLT.

В DLT value — исходный Confluent Avro payload или исходные повреждённые bytes/tombstone; key и заголовки сохраняются. Добавляются profile-original-topic, profile-original-partition, profile-original-offset и profile-failure-type. DLT — транспорт для диагностики, не новая Avro-схема. При повторной доставке DLT возможны дубликаты.

Outbox повторяет публикацию до `profile.calories.dlt-outbox.max-attempts=10`, с backoff от 1 секунды до 5 минут. После лимита строка остаётся FAILED. После устранения причины повторить конкретную запись через `JdbcOutboxStore.retryFailed(OutboxMessageId)`; не удалять payload. Для восстановления бизнес-обработки исправить причину и переотправить исходный key/value в основной topic. Уже принятые mealId повторно не учитываются.

Listener и DLT scheduler включены по умолчанию; `PROFILE_CALORIES_ENABLED=false` отключает их. DLT создаётся KafkaAdmin, по умолчанию 1 partition/1 replica для локальной среды; в production задать dlt-partitions/dlt-replicas и права на создание либо заранее создать topic. Исходный topic должен быть подготовлен инфраструктурой/Diary; consumer не меняет его число партиций. При первом старте группа читает earliest из доступной истории.

### Проверка

Целевой интеграционный класс: `CaloriesConsumerIntegrationTest`. Использует отдельные PostgreSQL/Kafka/Schema Registry без рабочих данных. Команда:

```bash
./gradlew -PbackendOnly=true :backend:profile-service:test --tests '*CaloriesConsumerIntegrationTest' --offline --console=plain --no-daemon --max-workers=1
```

## 5. Владение профилем и события

Profile хранит биометрию, цели питания и установленный пользователем недельный лимит бюджета. Мобильный клиент использует существующие `GetProfile` / `UpdateProfile` и локальную копию в Room. Остаток лимита вычисляет мобильный доменный слой по снимкам дневника; это не отдельный серверный денежный счёт.

[UserRegisteredEvent](../../shared/contracts/src/main/avro/UserRegisteredEvent.avsc) описывает регистрацию (`id`, `email`, `registrationTimestamp`, `source`). Это не событие изменения профиля; при `UpdateProfile` оно не публикуется в `user.profile.updated`. Производитель регистрации и доставка — отдельная будущая интеграция.

Отдельное событие обновления профиля для текущего сценария не нужно: мобильный клиент передаёт ограничения в запрос Recipe AI. Новая схема `ProfileUpdated` и подписки каталога/AI не добавляются. Действующая Avro-схема регистрации сохраняется.
