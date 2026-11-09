# ADR-004: Использование Kubernetes Gateway API и Envoy Gateway в роли Ingress/API Gateway для gRPC микросервисов

* **Статус**: Accepted (Принято)
* **Дата**: 2026-11-08
* **Контекст задачи**: [T-015] Спайк / Архитектура Envoy Gateway в kind/k3d (GatewayClass + GRPCRoute на echo-сервис + grpcurl)
* **Авторы**: `implementer` (архитектура инфраструктуры), `test-reviewer` (аудит безопасности и протоколов)

---

## 1. Контекст и проблематика (Context)

В проекте «Бюджетный нутрициолог» (FitAssistent2) все межсервисные и клиент-серверные взаимодействия микросервисов (`profile-service`, `catalog-price-service`, `diary-service`, `recipe-ai-service`, `food-cv-service`) базируются на протоколе **gRPC** (бинарный Protobuf, HTTP/2, мультиплексирование, Unary и Server-Streaming).

### Ограничения устаревшего Kubernetes Ingress (Ingress NGINX / Traefik):
Исторический стандарт `networking.k8s.io/v1 Ingress` разрабатывался для HTTP/1.1 REST веб-приложений. При попытке использования Ingress для gRPC возникают критические проблемы:
1. **Отсутствие нативного ресурса GRPCRoute:** Маршрутизация настраивается через костыли с аннотациями (`nginx.ingress.kubernetes.io/backend-protocol: "GRPC"`) и регулярными выражениями по URL-путям (`/com.budgetntr.profile.v1.ProfileService/GetProfile`);
2. **Проблемы с HTTP/2 трейлерами и стримингом:** Ingress-контроллеры часто сбрасывают или буферизуют HTTP/2 trailers (`grpc-status`, `grpc-message`), что ломает обработку ошибок в gRPC-клиентах;
3. **Сложность валидации JWT на границе (Edge Authentication):** Проверка токенов OAuth 2.1 PKCE в Ingress NGINX требует интеграции с громоздкими внешними `oauth2-proxy` или сторонними Lua-скриптами, что увеличивает задержки;
4. **Монолитность манифеста:** Один объект `Ingress` смешивает сетевые настройки хоста (TLS, порты) с бизнес-маршрутами конкретных сервисов, нарушая принцип разделения ответственности между DevOps и продуктовыми командами.

---

## 2. Факторы принятия решений (Decision Drivers)

1. **Первоклассная поддержка gRPC (First-Class Citizen):** нативная спецификация `GRPCRoute` с сопоставлением по именам сервисов и RPC-методов;
2. **Аутентификация на границе сети (Edge Security):** валидация JWT-токенов Keycloak на шлюзе и автоматический проброс извлеченных заголовков (`x-user-id`, `x-user-email`) в бэкенд-сервисы без нагрузки на Spring-приложения;
3. **Глобальный Rate Limiting:** защита от DDoS и злоупотреблений бесплатным тарифом (например, ограничение запросов к AI-генерации рецептов) через Redis до того, как запрос дойдет до Spring Boot;
4. **Разделение ролей (Role-Oriented Architecture):**
   - Инфраструктурная команда управляет `GatewayClass` и `Gateway` (порты, TLS-сертификаты, глобальные политики);
   - Команды микросервисов управляют своими независимыми `GRPCRoute` в собственных пространствах имён;
5. **Локальная воспроизводимость:** возможность быстрого запуска в легковесных кластерах `kind` или `k3d` для локальной разработки и автотестов в CI/CD.

---

## 3. Рассмотренные варианты (Considered Options)

### Вариант 1: Kubernetes Ingress (Ingress-NGINX)
* **Плюсы**: Повсеместная распространенность, простота начального развертывания.
* **Минусы**:
  * ❌ Нет нативной поддержки gRPC routing (только костыли с аннотациями);
  * ❌ Нестабильная работа с Server-Streaming gRPC (буферизация стримов);
  * ❌ Отсутствие декларативных политик rate-limit и jwt-auth без Lua.

### Вариант 2: Istio Service Mesh (Istio Ingress Gateway)
* **Плюсы**: Мощный функционал mTLS, развитая маршрутизация.
* **Минусы**:
  * ❌ Избыточная сложность: на данном этапе проекту не нужен Service Mesh с sidecar-инъекциями в каждый под;
  * ❌ Высокое потребление памяти и CPU контроллером `istiod` и sidecar-прокси.

### Вариант 3: Kubernetes Gateway API + Envoy Gateway (Выбранный вариант)
* **Плюсы**:
  * ✅ Официальная спецификация Kubernetes Gateway API v1 (Standard Channel);
  * ✅ Официальная реализация от команды Envoy Proxy;
  * ✅ Нативный ресурс `GRPCRoute` для маршрутизации gRPC методов;
  * ✅ Декларативные политики `SecurityPolicy` (OAuth 2.1 JWT) и `BackendTrafficPolicy` (Rate Limiter);
  * ✅ Легковесность: запускается как обычный контроллер, динамически конфигурирующий Envoy Proxy.

---

## 4. Сравнительная матрица вариантов

| Критерий | Ingress NGINX | Istio Ingress | Envoy Gateway (Gateway API) |
| :--- | :--- | :--- | :--- |
| **Спецификация** | Устаревший Ingress v1 | Кастомные CRD Istio | **Стандарт K8s Gateway API v1** |
| **gRPC Маршрутизация** | ⚠️ Через HTTP/1 paths | 🟢 VirtualService gRPC | 🟢 **Нативный GRPCRoute** |
| **HTTP/2 Streaming** | ⚠️ Риск буферизации | 🟢 Полная поддержка | 🟢 **Нативная поддержка Envoy** |
| **JWT Validaton на Edge** | ❌ Требует oauth2-proxy/Lua | 🟢 RequestAuthentication | 🟢 **SecurityPolicy CRD** |
| **Global Rate Limiting** | ❌ Необходимы плагины | 🟢 EnvoyFilter / Redis | 🟢 **BackendTrafficPolicy (Redis)** |
| **Оверхед на ресурсы** | 🟢 Низкий | ❌ Высокий (Mesh) | 🟢 **Низкий (только Gateway)** |
| **Разделение ролей** | ❌ Монолитный Ingress | 🟢 Gateway + VirtualService| 🟢 **Gateway + GRPCRoute (RBAC)**|

---

## 5. Решение и результаты Спайка [T-015] (Decision Outcome)

Принять **Envoy Gateway** поверх **Kubernetes Gateway API v1** в качестве единственного API Gateway проекта «Бюджетный нутрициолог».

### 5.1 Архитектура и структура конфигураций
Создана инфраструктурная база в каталоге [`infrastructure/k8s/`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/):
1. **Конфигурация кластера kind**: [`kind-config.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/kind-config.yaml) — проброс портов 80 (HTTP), 443 (HTTPS) и 9090 (gRPC Gateway) на хост-машину;
2. **GatewayClass**: [`01-gateway-class.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/01-gateway-class.yaml) — контроллер `gateway.envoyproxy.io/gatewayclass-controller`;
3. **Gateway**: [`02-gateway.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/02-gateway.yaml) — слушатели gRPC (порт 9090) и HTTP (порт 80);
4. **Тестовый Echo-сервис**: [`03-echo-service.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/03-echo-service.yaml) — Deployment и Service `echo-grpc-service:9090` (`appProtocol: grpc`, образ `rancher/grpc-echo:v1.0.0`);
5. **GRPCRoute**: [`04-grpc-route.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/04-grpc-route.yaml) — маршрутизация вызовов `echo.EchoServer` на `echo-grpc-service`;
6. **SecurityPolicy**: [`05-security-policy.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/05-security-policy.yaml) — валидация JWT Keycloak;
7. **BackendTrafficPolicy**: [`06-backend-traffic-policy.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/gateway/06-backend-traffic-policy.yaml) — Rate Limiter 100 req/min на пользователя;
8. **Helmfile**: [`helmfile.yaml`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/infrastructure/k8s/charts/helmfile.yaml) — декларативная установка Envoy Gateway v1.2.4.

### 5.2 Автоматизация и верификация (grpcurl)
Разработаны скрипты автоматического развертывания и тестирования спайка:
- Bash для Linux / CI: [`scripts/verify-envoy-gateway.sh`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/scripts/verify-envoy-gateway.sh);
- PowerShell для Windows: [`scripts/verify-envoy-gateway.ps1`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/scripts/verify-envoy-gateway.ps1).

**Сценарий проверки:**
1. Развертывание локального кластера kind с открытым портом 9090;
2. Установка Gateway API Standard CRDs v1.2.0 и Envoy Gateway v1.2.4;
3. Применение манифестов GatewayClass, Gateway, Echo Deployment, GRPCRoute;
4. Тестовый запрос через `grpcurl`:
   ```bash
   grpcurl -plaintext \
     -d '{"message": "FitAssistent Spike T-015: Hello via Envoy Gateway!"}' \
     localhost:9090 echo.EchoServer/Echo
   ```
5. **Ожидаемый ответ от echo-сервиса через Envoy Gateway:**
   ```json
   {
     "message": "FitAssistent Spike T-015: Hello via Envoy Gateway!"
   }
   ```

---

## 6. Последствия (Consequences)

### Положительные:
- Единая точка входа для всех мобильных клиентов и микросервисов;
- Полная разгрузка Spring Boot микросервисов от ручной валидации JWT и отслеживания счетчиков Rate Limiting;
- Готовность CI/CD к интеграционным тестам с реальным Gateway в локальном кластере kind;
- Соответствие целевой архитектуре бэкенда (`docs/architecture/02-backend-stack.md §2`).

### Риски и их минимизация:
- **Риск:** Разработчики могут забыть проставить `appProtocol: grpc` в Kubernetes Service.
  - *Минимизация:* Закрепить правило в `AGENTS.md` и добавить проверку в линтер манифестов Helm/K8s.
- **Риск:** Холодный старт Envoy Proxy при динамическом создании шлюзов.
  - *Минимизация:* Зафиксировать статический пул подов Gateway через `minReplicas: 2` и `PodDisruptionBudget`.
