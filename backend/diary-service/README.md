# Diary Service (`:backend:diary-service`)

> Серверное хранение и синхронизация записей дневника. Стоимость порции, КБЖУ и остатки рассчитывает мобильный доменный слой.

> **Статус:** целевая спецификация; Diary API, сущности и миграции ещё не реализованы. Примеры не являются действующим wire-контрактом.

---

## 1. База данных и схема (`diary_db`)

- **СУБД:** PostgreSQL 17 (порт `5432`, база `diary_db`).
- **Идемпотентность:** постоянная уникальность `(user_id, idempotency_key)` в БД; Redis может ускорять чтение, но не заменяет эту гарантию.
- **Инструмент миграций:** Liquibase (`src/main/resources/db/changelog/db.changelog-master.yaml`).

### Схема таблицы `diary_entries` (`001-diary-schema.xml`)

```sql
CREATE TABLE diary_entries (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,  -- защита от дублирования при retry
    food_item_id UUID NOT NULL,
    food_name VARCHAR(255) NOT NULL,
    quantity_grams NUMERIC(10, 3) NOT NULL,
    calories NUMERIC(7, 2) NOT NULL,
    proteins NUMERIC(6, 2) NOT NULL,
    fats NUMERIC(6, 2) NOT NULL,
    carbs NUMERIC(6, 2) NOT NULL,
    price NUMERIC(10, 2),                         -- стоимость порции в рублях
    unit_price_minor BIGINT,
    price_basis_milligrams BIGINT,
    meal_type VARCHAR(20) NOT NULL,               -- BREAKFAST, LUNCH, DINNER, SNACK
    recorded_at TIMESTAMPTZ NOT NULL,             -- дата и время приёма пищи
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_diary_request UNIQUE(user_id, idempotency_key),
    CONSTRAINT ck_diary_price CHECK (price IS NULL OR price >= 0),
    CONSTRAINT ck_diary_price_basis CHECK (
        (unit_price_minor IS NULL AND price_basis_milligrams IS NULL) OR
        (unit_price_minor IS NOT NULL AND price_basis_milligrams IS NOT NULL
         AND unit_price_minor >= 0 AND price_basis_milligrams > 0 AND price IS NOT NULL)
    )
);

CREATE INDEX idx_diary_entries_user_date ON diary_entries(user_id, recorded_at DESC);
```

### Схема таблицы `outbox_messages` (`002-outbox.xml`)

```sql
CREATE TABLE outbox_messages (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,          -- 'DiaryEntry'
    aggregate_id VARCHAR(64) NOT NULL,
    topic VARCHAR(100) NOT NULL,                  -- 'diary.entry.created'
    payload BYTEA NOT NULL,                       -- бинарная Avro-сериализация CaloriesConsumedEvent
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_pending ON outbox_messages(status, created_at);
```

---

## 2. JPA Сущности

- **`DiaryEntryEntity`**: запись в журнале приёма пищи (КБЖУ, граммовка, цена, идемпотентный ключ).
- **`OutboxMessageEntity`**: сущность Transactional Outbox для надёжной публикации в Kafka вне транзакции БД.

---

## 3. Protobuf Контракт

- **Файл контракта:** `proto/nutritionist/diary/v1/diary.proto`
- **Пакет:** `com.budgetntr.diary.v1` (Java/Kotlin: `com.budgetntr.proto.diary.v1`)

```protobuf
syntax = "proto3";

package com.budgetntr.diary.v1;

import "google/protobuf/timestamp.proto";

service DiaryService {
  // AddEntry создает новую запись в дневнике питания с проверкой idempotency-key
  rpc AddEntry(AddEntryRequest) returns (AddEntryResponse);

  // GetDailyEntries возвращает все приёмы пищи за конкретную дату и суммарный баланс
  rpc GetDailyEntries(GetDailyEntriesRequest) returns (GetDailyEntriesResponse);
}

message Money {
  int64 amount_minor = 1;
  string currency_code = 2;
}

message CostSnapshot {
  Money total = 1;
  Money unit_price = 2;
  optional int64 price_basis_milligrams = 3;
}

message NutritionSummary {
  int32 calories = 1;
  double proteins = 2;
  double fats = 3;
  double carbs = 4;
}

message AddEntryRequest {
  string food_item_id = 1;
  double quantity_grams = 2;
  google.protobuf.Timestamp recorded_at = 3;
  string meal_type = 4; // BREAKFAST, LUNCH, DINNER, SNACK
  string entry_id = 5;
  string food_name = 6;
  NutritionSummary nutrition = 7;
  CostSnapshot cost = 8;
}

message AddEntryResponse {
  DiaryEntryProto entry = 1;
}

message GetDailyEntriesRequest {
  string date = 1; // ISO-8601: "2026-11-11"
  string time_zone = 2;
}

message GetDailyEntriesResponse {
  repeated DiaryEntryProto entries = 1;
  NutritionSummary daily_total = 2;
}

message DiaryEntryProto {
  string id = 1;
  string food_item_id = 2;
  string food_name = 3;
  double quantity_grams = 4;
  NutritionSummary nutrition = 5;
  google.protobuf.Timestamp recorded_at = 6;
  string meal_type = 7;
  CostSnapshot cost = 8;
}
```

---

## 4. Снимок записи и синхронизация

- Клиент генерирует UUID `entry_id` и `idempotency-key` один раз, рассчитывает порцию и сохраняет весь payload в Room до сети. Запись принадлежит проверенному субъекту авторизации; тело запроса не выбирает другого пользователя.
- `cost` отсутствует, если стоимость неизвестна. Если стоимость известна, `total` обязателен. Нулевой total — известная нулевая стоимость. `unit_price` и положительный `price_basis_milligrams` присутствуют вместе, если сохранена исходная цена. Для ручной итоговой суммы они могут отсутствовать.
- MVP использует только RUB. API передаёт копейки через `int64`, сервер точно переводит их в `price NUMERIC(10,2)` делением на 100. Это смена представления, не пересчёт порции. В БД валюта не дублируется, пока иные валюты запрещены. Исходная цена и её база необязательны; количество уже хранится в записи.
- Сервер проверяет владельца, UUID, допустимые значения meal_type, дату/часовой пояс, конечность и диапазоны чисел, положительное количество с точностью до 0.001 г, неотрицательные КБЖУ и деньги, валюту и согласованность присутствия полей. Для `price NUMERIC(10,2)` максимум — 9 999 999 999 копеек. Проверка не обращается к текущим ценам и не пересчитывает клиентскую сумму.
- Это пользовательская оценка расходов на потреблённую порцию, а не подтверждённая оплата/чек. Серверное подтверждение означает сохранение, не подтверждение рыночной цены.
- Повтор `(subject, idempotency-key)` с тем же payload возвращает прежнюю запись; другой payload — `INVALID_ARGUMENT`. PK записи и ключ проверяются атомарно; конкурентный повтор не создаёт вторую запись или outbox-событие. При попытке повторно создать тот же `entry_id` с новым ключом возвращается конфликт, клиент не генерирует новый ID автоматически.
- Историческая стоимость не меняется при обновлении каталога. Для исправления пользователем потребуется отдельный будущий сценарий редактирования; данный AddEntry не является upsert.
- `GetDailyEntries` использует IANA time_zone для границ локального дня, возвращает сохранённые снимки и сумму сохранённых нутриентов. Клиент считает денежный остаток из объединения серверных записей и локальных PENDING по `entry_id`, без двойного учёта. Для недельного остатка загружаются все дни недели; неполный кэш явно помечается в UI.

## 5. Межсервисные события

Существующая [CaloriesConsumedEvent](../../shared/contracts/src/main/avro/CaloriesConsumedEvent.avsc) содержит `userId`, `mealId`, `calories`, `timestamp`, `mealType`, `notes`. Её целевое назначение — факт потребления, а не денежный расход. При будущей публикации `mealId = entry_id`, `timestamp = recorded_at`, `calories` берётся из сохранённой записи; доставка через outbox. Потребитель дедуплицирует создание по `(userId, mealId)`.

`diary.entry.created` не используется Recipe AI для ведения баланса. Поле стоимости в Avro не добавляется; новый обязательный потребитель не вводится. Дневник не потребляет `food.price.updated`: цены каталога не меняют исторические снимки.
