# Catalog & Price Service

> Микросервис каталога продуктов питания, пищевой ценности и актуальных цен.

## Стек технологий
- **Framework:** Spring Boot 4.0.x / Kotlin 2.4.x
- **API:** gRPC (порт `9091`), HTTP Actuator (порт `8081`)
- **Database:** PostgreSQL 17 (`catalog_db`), Liquibase
- **Cache:** Redis 7 (кэширование цен и частых запросов)
- **Events:** Kafka 3.9 + Schema Registry 7.7 (Avro)
  - Produced: `food.price.updated`
  - Consumed: `user.profile.updated`
- **Pattern:** Transactional Outbox (`outbox_messages`)
- **Observability:** OpenTelemetry, Prometheus, JSON-логи

## Liquibase миграции
- `db/changelog/001-catalog-schema.xml`: Таблицы `categories`, `food_items`
- `db/changelog/002-prices-schema.xml`: Таблица `food_prices` (история и актуальные цены)
- `db/changelog/003-outbox.xml`: Таблица `outbox_messages`
- `db/changelog/004-indexes.xml`: Индексы по БЖУ (`idx_food_nutrition`) и ценам (`idx_food_price_category`)
