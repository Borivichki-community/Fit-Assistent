# Diary Service (`:backend:diary-service`)

> Микросервис учёта приёмов пищи (дневник питания), расчёта фактического суточного баланса калорий/БЖУ и публикации событий о факте потребления.

---

## 1. База данных и схема (`diary_db`)

- **СУБД:** PostgreSQL 17 (порт `5432`, база `diary_db`).
- **Кэш и идемпотентность:** Redis 7 (проверка заголовка `idempotency-key` в `IdempotencyServerInterceptor`).
- **Инструмент миграций:** Liquibase (`src/main/resources/db/changelog/db.changelog-master.xml`).

### Схема таблицы `diary_entries` (`001-diary-schema.xml`)

```sql
CREATE TABLE diary_entries (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,  -- защита от дублирования при retry
    food_item_id UUID NOT NULL,
    food_name VARCHAR(255) NOT NULL,
    quantity_grams NUMERIC(7, 2) NOT NULL,
    calories NUMERIC(7, 2) NOT NULL,
    proteins NUMERIC(6, 2) NOT NULL,
    fats NUMERIC(6, 2) NOT NULL,
    carbs NUMERIC(6, 2) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,                -- стоимость порции в рублях
    meal_type VARCHAR(20) NOT NULL,               -- BREAKFAST, LUNCH, DINNER, SNACK
    recorded_at TIMESTAMPTZ NOT NULL,             -- дата и время приёма пищи
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
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

message AddEntryRequest {
  string food_item_id = 1;
  double quantity_grams = 2;
  google.protobuf.Timestamp recorded_at = 3;
  string meal_type = 4; // BREAKFAST, LUNCH, DINNER, SNACK
}

message AddEntryResponse {
  string entry_id = 1;
  NutritionSummary nutrition = 2;
}

message NutritionSummary {
  double calories = 1;
  double proteins = 2;
  double fats = 3;
  double carbs = 4;
  string price_label = 5; // например: "185 ₽"
}

message GetDailyEntriesRequest {
  string date = 1; // ISO-8601: "2026-11-11"
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
}
```

---

## 4. Межсервисные события (Kafka + Avro)

- **Produced Events:**
  - Топик: `diary.entry.created`
  - Схема Avro: `shared/contracts/src/main/avro/CaloriesConsumedEvent.avsc`
  - Потребители: `recipe-ai-service` (коррекция остатка бюджета и калорий для генерации меню).
- **Consumed Events:**
  - Топик: `food.price.updated` (актуализация расчётных цен в журнале при изменении цен продуктов).
