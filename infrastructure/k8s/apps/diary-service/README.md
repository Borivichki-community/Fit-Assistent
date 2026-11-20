# diary-service Kubernetes Manifests

Каталог для манифестов микросервиса дневника питания (`diary-service`).

## Стандартная структура файлов

Для соблюдения единого стандарта платформы FitAssistant добавьте сюда следующие файлы:

1. `configmap.yaml` — ConfigMap с переменными окружения:
   - `SPRING_PROFILES_ACTIVE: "prod"`
   - `SPRING_GRPC_SERVER_ADDRESS: "0.0.0.0"`
   - `SPRING_GRPC_SERVER_PORT: "9193"`
   - `SERVER_PORT: "8087"`
   - `MANAGEMENT_SERVER_PORT: "8087"`
   - `SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres.budget-nutritionist.svc.cluster.local:5432/diary_db"`
   - `SPRING_DATA_REDIS_HOST: "redis-cache.budget-nutritionist.svc.cluster.local"`
   - `JWK_SET_URI: "http://keycloak.budget-nutritionist.svc.cluster.local:8080/realms/FitAssistant/protocol/openid-connect/certs"`
2. `secret.yaml` — Secret для чувствительных данных (`DB_USERNAME`, `DB_PASSWORD`).
3. `deployment.yaml` — Deployment с probes (`/actuator/health/liveness`, `/actuator/health/readiness`), `securityContext` (non-root, UID 10001) и ресурсами.
4. `service.yaml` — Service (`grpc: 9193`, `actuator: 8087`).
5. `grpc-route.yaml` — GRPCRoute для Envoy Gateway на `com.budgetntr.diary.v1.DiaryService`.
