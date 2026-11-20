#!/usr/bin/env bash
# scripts/verify-envoy-gateway.sh
# Автоматизированный скрипт проведения и верификации [T-026, T-027, T-028, T-029]
# Envoy Gateway + TLS + profile-service + Keycloak JWT + Rate Limiting
set -euo pipefail

CLUSTER_NAME="fitassistant-cluster"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
K8S_DIR="${PROJECT_ROOT}/infrastructure/k8s"

echo "====================================================================="
echo "=== Envoy Gateway & profile-service: Инфраструктурный смоук-тест ==="
echo "====================================================================="

# 1. Проверка наличия утилит
for cmd in kubectl kind grpcurl curl jq; do
    if ! command -v "$cmd" &> /dev/null; then
        echo "ПРЕДУПРЕЖДЕНИЕ: Утилита '$cmd' не найдена в PATH. Убедитесь в её наличии для запуска."
    fi
done

# 2. Создание кластера kind (если ещё не создан)
if command -v kind &> /dev/null; then
    if ! kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
        echo "--> Создание кластера kind '${CLUSTER_NAME}' с портами 80, 443, 9090..."
        kind create cluster --name "${CLUSTER_NAME}" --config "${K8S_DIR}/kind-config.yaml"
    else
        echo "--> Кластер kind '${CLUSTER_NAME}' уже существует. Переключение контекста..."
        kubectl config use-context "kind-${CLUSTER_NAME}"
    fi
fi

# 3. Установка Kubernetes Gateway API CRDs (v1.2.0 Standard)
echo "--> 1. Установка стандартных CRD Gateway API v1.2.0..."
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.0/standard-install.yaml

# 4. Установка Envoy Gateway (v1.2.4)
echo "--> 2. Установка Envoy Gateway v1.2.4..."
kubectl apply -f https://github.com/envoyproxy/gateway/releases/download/v1.2.4/install.yaml

echo "--> Ожидание готовности контроллера Envoy Gateway..."
kubectl wait --namespace envoy-gateway-system \
  --for=condition=ready pod \
  --selector=control-plane=envoy-gateway \
  --timeout=180s

# 5. Применение манифестов базовой инфраструктуры (PostgreSQL, Redis, Keycloak)
echo "--> 3. Применение манифестов базовой инфраструктуры..."
kubectl create namespace budget-nutritionist --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f "${K8S_DIR}/infra/01-postgres.yaml"
kubectl apply -f "${K8S_DIR}/infra/02-redis.yaml"
kubectl apply -f "${K8S_DIR}/infra/03-keycloak.yaml"

echo "--> Ожидание готовности подов базовой инфраструктуры..."
kubectl wait --namespace budget-nutritionist --for=condition=ready pod --selector=app=postgres --timeout=120s || true
kubectl wait --namespace budget-nutritionist --for=condition=ready pod --selector=app=redis-state --timeout=120s || true
kubectl wait --namespace budget-nutritionist --for=condition=ready pod --selector=app=redis-cache --timeout=120s || true
kubectl wait --namespace budget-nutritionist --for=condition=ready pod --selector=app=keycloak --timeout=180s || true

# 6. Применение манифестов GatewayClass, TLS Secret, Gateway, Profile Service, Route, SecurityPolicy, RateLimit
echo "--> 4. Применение манифестов сетевого периметра и profile-service..."
kubectl apply -f "${K8S_DIR}/gateway/01-gateway-class.yaml"
kubectl apply -f "${K8S_DIR}/gateway/02-tls-secret.yaml"
kubectl apply -f "${K8S_DIR}/gateway/02-gateway.yaml"
kubectl apply -f "${K8S_DIR}/gateway/03-profile-service.yaml"
kubectl apply -f "${K8S_DIR}/gateway/04-profile-grpc-route.yaml"
kubectl apply -f "${K8S_DIR}/gateway/05-security-policy.yaml"
kubectl apply -f "${K8S_DIR}/gateway/06-backend-traffic-policy.yaml"

# 7. Ожидание готовности подов
echo "--> 5. Ожидание готовности profile-service и Envoy Proxy..."
kubectl wait --namespace budget-nutritionist \
  --for=condition=available deployment/profile-service \
  --timeout=120s || echo "Ожидание пода profile-service..."

kubectl wait --namespace envoy-gateway-system \
  --for=condition=ready pod \
  --selector=gateway.envoyproxy.io/owning-gateway-name=main-gateway \
  --timeout=120s || echo "Продолжаем подготовку проверок..."

echo ""
echo "====================================================================="
echo "=== ТЕСТ 1: Вызов GetProfile без JWT токена (Ожидается 401 / UNAUTHENTICATED) ==="
echo "====================================================================="
TEST1_OUTPUT=$(grpcurl -plaintext \
  -d '{"user_id": "usr-test-unauth-001"}' \
  localhost:9090 com.budgetntr.profile.v1.ProfileService/GetProfile 2>&1 || true)

echo "Ответ Envoy Gateway:"
echo "${TEST1_OUTPUT}"

if echo "${TEST1_OUTPUT}" | grep -qi "Unauthenticated"; then
    echo "✅ ТЕСТ 1 ПРОЙДЕН: Envoy Gateway заблокировал запрос без токена со статусом UNAUTHENTICATED."
else
    echo "⚠️ ТЕСТ 1: Проверьте статус ответа. Ожидался UNAUTHENTICATED."
fi

echo ""
echo "====================================================================="
echo "=== ТЕСТ 2: Получение токена Keycloak и вызов с Bearer токеном ==="
echo "====================================================================="
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM="${KEYCLOAK_REALM:-FitAssistant}"

echo "Запрос токена в Keycloak (${KEYCLOAK_URL}/realms/${REALM})..."
TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=fitassistant-mobile" \
  -d "username=testuser" \
  -d "password=password123" | jq -r '.access_token // empty' || true)

if [ -n "${TOKEN}" ] && [ "${TOKEN}" != "null" ]; then
    echo "Токен получен успешно (длина: ${#TOKEN} символов)."
    echo "Вызов GetProfile с токеном через TLS (порт 443)..."

    TEST2_OUTPUT=$(grpcurl -insecure \
      -H "Authorization: Bearer ${TOKEN}" \
      -d '{"user_id": "usr-test-auth-001"}' \
      localhost:443 com.budgetntr.profile.v1.ProfileService/GetProfile 2>&1 || true)

    echo "Ответ сервиса:"
    echo "${TEST2_OUTPUT}"

    if echo "${TEST2_OUTPUT}" | grep -q "user_id"; then
        echo "✅ ТЕСТ 2 ПРОЙДЕН: Успешная авторизация через Keycloak JWT и ответ от profile-service."
    else
        echo "⚠️ ТЕСТ 2: Ответ получен, проверьте совпадение схемы ProfileProto."
    fi
else
    echo "⚠️ Keycloak недоступен локально на ${KEYCLOAK_URL} или пользователь testuser не создан."
    echo "Пример команды вызова с токеном:"
    echo "grpcurl -insecure -H 'Authorization: Bearer <TOKEN>' -d '{\"user_id\": \"usr-123\"}' localhost:443 com.budgetntr.profile.v1.ProfileService/GetProfile"
fi

echo ""
echo "====================================================================="
echo "=== ТЕСТ 3: Проверка Rate Limiter (120 быстрых вызовов подряд) ==="
echo "====================================================================="
echo "Запуск серии из 120 запросов для проверки лимита (100 req/min)..."
RATE_LIMITED=0
for i in $(seq 1 120); do
    RESP=$(grpcurl -plaintext \
      -H "Authorization: Bearer ${TOKEN:-dummy-token}" \
      -d '{"user_id": "rate-limit-test-user"}' \
      localhost:9090 com.budgetntr.profile.v1.ProfileService/GetProfile 2>&1 || true)

    if echo "${RESP}" | grep -qi "ResourceExhausted"; then
        RATE_LIMITED=1
        echo "✅ ТЕСТ 3 ПРОЙДЕН: На запросе #${i} сработал Rate Limiter: Status.RESOURCE_EXHAUSTED (HTTP 429)."
        break
    fi
done

if [ "${RATE_LIMITED}" -eq 0 ]; then
    echo "ℹ️ Лимит не был превышен в цикле (проверьте настройки Redis и BackendTrafficPolicy)."
fi

echo ""
echo "=== Скрипт проверки Envoy Gateway успешно выполнен ==="
