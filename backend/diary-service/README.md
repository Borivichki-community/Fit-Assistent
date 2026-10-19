# Diary Service

> Микросервис ведения дневника приёмов пищи и расчёта суточного баланса.

## Стек технологий
- **Framework:** Spring Boot 4.0.x / Kotlin 2.4.x
- **API:** gRPC (порт `9092`), HTTP Actuator (порт `8082`)
- **Database:** PostgreSQL 17 (`diary_db`), Liquibase
- **Cache & Idempotency:** Redis 7 (проверка `idempotency-key` в `IdempotencyServerInterceptor`)
- **Events:** Kafka 3.9 + Schema Registry 7.7 (Avro)
  - Produced: `diary.entry.created`
  - Consumed: `food.price.updated`
- **Pattern:** Transactional Outbox (`outbox_messages`)
- **Observability:** OpenTelemetry, Prometheus, JSON-логи

## Liquibase миграции
- `db/changelog/001-diary-schema.xml`: Таблица `diary_entries` (привязка к user_id, food_item_id, граммовка, дата)
- `db/changelog/002-outbox.xml`: Таблица `outbox_messages`
- `db/changelog/003-indexes.xml`: Индексы `idx_diary_user_date` по `user_id` и `recorded_at`
