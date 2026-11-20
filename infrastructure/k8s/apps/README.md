# FitAssistant Application Kubernetes Manifests

Данный каталог содержит декларативные Kubernetes-манифесты для всех микросервисов платформы «Бюджетный нутрициолог».

## Структура каталогов

Каждый микросервис располагается в собственной изолированной директории со стандартизированным набором из 5 файлов:

```
apps/
├── catalog-price-service/   # Каталог продуктов и региональные цены (порт gRPC: 9192, HTTP: 8086)
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── grpc-route.yaml
├── profile-service/         # Профили пользователей и антропометрия (порт gRPC: 9090, HTTP: 8080)
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── grpc-route.yaml
├── diary-service/           # Дневник питания и КБЖУ (порт gRPC: 9193, HTTP: 8087)
│   └── README.md
├── recipe-ai-service/       # ИИ-генерация рецептов (порт gRPC: 9194, HTTP: 8088)
│   └── README.md
└── food-cv-service/         # Распознавание еды по фото (порт gRPC: 9195, HTTP: 8089)
    └── README.md
```

## Стандарт манифестов приложения

1. **`configmap.yaml`**: Переменные конфигурации Spring Boot (`SPRING_PROFILES_ACTIVE`, порты, URL БД, Redis, Keycloak JWKS).
2. **`secret.yaml`**: Чувствительные данные (`DB_USERNAME`, `DB_PASSWORD`, API ключи) в `Opaque` Secret.
3. **`deployment.yaml`**:
   - `securityContext`: `runAsNonRoot: true`, `runAsUser: 10001`, `runAsGroup: 10001`, `fsGroup: 10001`, `drop: [ALL]`, `allowPrivilegeEscalation: false`.
   - Пробы жизнеспособности Spring Actuator: `startupProbe`, `livenessProbe`, `readinessProbe`.
   - Ресурсы: `requests` (CPU 200m, RAM 384Mi), `limits` (CPU 1000m, RAM 768Mi).
4. **`service.yaml`**: `ClusterIP` с именованными портами:
   - `grpc`: `appProtocol: grpc`
   - `actuator`: `appProtocol: http`
5. **`grpc-route.yaml`**: `GRPCRoute` (Gateway API v1) с привязкой к `main-gateway` (`envoy-gateway-system`) для маршрутизации внешнего трафика.

## Развёртывание

Применение манифестов конкретного сервиса:
```bash
kubectl apply -f infrastructure/k8s/apps/<service-name>/
```
