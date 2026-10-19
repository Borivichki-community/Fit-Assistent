# Profile Service

> Микросервис управления профилем пользователя и целями питания.

## Стек технологий
- **Framework:** Spring Boot 4.0.x / Kotlin 2.4.x
- **API:** gRPC (порт `9090`), HTTP Actuator (порт `8080`)
- **Database:** PostgreSQL 17 (`profile_db`), Liquibase
- **Events:** Kafka 3.9 + Schema Registry 7.7 (Avro)
  - Produced: `user.profile.updated`
- **Pattern:** Transactional Outbox (`outbox_messages`)
- **Auth:** OAuth 2.1 PKCE (JWT валидация через Envoy Gateway SecurityPolicy)
- **Observability:** OpenTelemetry (W3C traceparent), Prometheus metrics, JSON-логи с traceId/spanId/userId

## Liquibase миграции
- `db/changelog/001-profile-schema.xml`: Таблица `profiles` (цели калорий, БЖУ, ограничения)
- `db/changelog/002-outbox.xml`: Таблица `outbox_messages`
- `db/changelog/003-indexes.xml`: Индексы по `user_id`, `created_at`
