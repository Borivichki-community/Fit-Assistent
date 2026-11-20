# food-cv-service Kubernetes Manifests

Каталог для манифестов микросервиса компьютерного зрения и распознавания блюд по фото (`food-cv-service`).

## Стандартная структура файлов

Для соблюдения единого стандарта платформы FitAssistant добавьте сюда следующие файлы:

1. `configmap.yaml` — ConfigMap с переменными окружения:
   - `SPRING_PROFILES_ACTIVE: "prod"`
   - `SPRING_GRPC_SERVER_ADDRESS: "0.0.0.0"`
   - `SPRING_GRPC_SERVER_PORT: "9195"`
   - `SERVER_PORT: "8089"`
   - `MANAGEMENT_SERVER_PORT: "8089"`
   - `JWK_SET_URI: "http://keycloak.budget-nutritionist.svc.cluster.local:8080/realms/FitAssistant/protocol/openid-connect/certs"`
2. `secret.yaml` — Secret для чувствительных данных и ключей доступа к Vision API.
3. `deployment.yaml` — Deployment с probes (`/actuator/health/liveness`, `/actuator/health/readiness`), `securityContext` (non-root, UID 10001) и ресурсами.
4. `service.yaml` — Service (`grpc: 9195`, `actuator: 8089`).
5. `grpc-route.yaml` — GRPCRoute для Envoy Gateway на `com.budgetntr.foodcv.v1.FoodCvService`.
