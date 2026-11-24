# recipe-ai-service Kubernetes Manifests

Каталог для манифестов микросервиса генерации рецептов и рекомендаций (`recipe-ai-service`).

## Стандартная структура файлов

Для соблюдения единого стандарта платформы FitAssistant добавьте сюда следующие файлы:

1. `configmap.yaml` — ConfigMap с переменными окружения:
   - `SPRING_PROFILES_ACTIVE: "prod"`
   - `SPRING_GRPC_SERVER_ADDRESS: "0.0.0.0"`
   - `SPRING_GRPC_SERVER_PORT: "9194"`
   - `SERVER_PORT: "8088"`
   - `MANAGEMENT_SERVER_PORT: "8088"`
   - `SPRING_DATA_REDIS_HOST: "redis-cache.budget-nutritionist.svc.cluster.local"`
   - `JWK_SET_URI: "http://keycloak.budget-nutritionist.svc.cluster.local:8080/realms/FitAssistant/protocol/openid-connect/certs"`
2. `secret.yaml` — Secret для API-ключей LLM/OpenAI/Anthropic и прочих чувствительных данных.
3. `deployment.yaml` — Deployment с probes (`/actuator/health/liveness`, `/actuator/health/readiness`), `securityContext` (non-root, UID 10001) и ресурсами.
4. `service.yaml` — Service (`grpc: 9194`, `actuator: 8088`).
5. `grpc-route.yaml` — GRPCRoute для Envoy Gateway на `com.budgetntr.recipe.v1.RecipeService`.
