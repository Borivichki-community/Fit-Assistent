# Recipe AI Service

> Микросервис генерации бюджетных рецептов с помощью AI (Gemini / LLM) и оптимизации стоимости питания.

## Стек технологий
- **Framework:** Spring Boot 4.0.x / Kotlin 2.4.x
- **API:** gRPC (порт `9093`), HTTP Actuator (порт `8083`)
- **Cache:** Redis 7 (кэширование сгенерированных и популярных рецептов)
- **Resilience:** Resilience4j (`@CircuitBreaker` с fallback на кэш Redis, `@TimeLimiter(10s)`, `@Retry` для идемпотентных GET)
- **Events:** Kafka 3.9 + Schema Registry 7.7 (Avro)
  - Produced: `recipe.generated`
  - Consumed: `diary.entry.created`
- **Observability:** OpenTelemetry, Prometheus, JSON-логи
