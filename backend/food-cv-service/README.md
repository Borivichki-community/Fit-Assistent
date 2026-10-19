# Food CV Service

> Микросервис распознавания блюд и продуктов по фотографиям (Computer Vision / Multimodal AI).

## Стек технологий
- **Framework:** Spring Boot 4.0.x / Kotlin 2.4.x
- **API:** gRPC (порт `9094`), HTTP Actuator (порт `8084`)
- **Cache:** Redis 7 (кэширование результатов распознавания по хэшу изображения)
- **Resilience:** Resilience4j (`@CircuitBreaker` на внешние вызовы нейросетей)
- **Events:** Kafka 3.9 + Schema Registry 7.7 (Avro)
  - Produced: `food.detected`
- **Observability:** OpenTelemetry, Prometheus, JSON-логи
