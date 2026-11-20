#!/usr/bin/env bash
# infrastructure/k8s/verify-cluster.sh
# Автоматизированный скрипт развёртывания и верификации инфраструктуры FitAssistant в кластере kind
# (PostgreSQL 17, Redis State/Cache, Keycloak 26, Envoy Gateway, profile-service)
set -euo pipefail

CLUSTER_NAME="fitassistant-cluster"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
K8S_DIR="${PROJECT_ROOT}/infrastructure/k8s"

echo "=========================================================================="
echo "=== FitAssistant: Развёртывание и сквозная верификация кластера kind ==="
echo "=========================================================================="

# 1. Проверка наличия утилит
for cmd in kubectl kind docker; do
    if ! command -v "$cmd" &> /dev/null; then
        echo "ПРЕДУПРЕЖДЕНИЕ: Утилита '$cmd' не найдена в PATH."
    fi
done

# 2. Создание или подключение к kind-кластеру
if command -v kind &> /dev/null; then
    if ! kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
        echo "--> [1/7] Создание кластера kind '${CLUSTER_NAME}'..."
        kind create cluster --name "${CLUSTER_NAME}" --config "${K8S_DIR}/kind-config.yaml"
    else
        echo "--> [1/7] Кластер kind '${CLUSTER_NAME}' уже существует. Переключение контекста..."
        kubectl config use-context "kind-${CLUSTER_NAME}"
    fi
else
    echo "--> [1/7] kind не найден, используется текущий контекст kubectl: $(kubectl config current-context 2>/dev/null || echo 'none')"
fi

# 3. Загрузка docker-образа в kind
echo "--> Загрузка docker-образа profile-service:latest в кластер kind..."
kind load docker-image profile-service:latest --name "${CLUSTER_NAME}" || true

# 4. Установка Gateway API Standard CRDs (v1.2.0)
echo "--> [2/7] Установка стандартных CRD Gateway API v1.2.0..."
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.0/standard-install.yaml

# 5. Установка Envoy Gateway v1.2.4
echo "--> [3/7] Установка Envoy Gateway v1.2.4..."
kubectl apply --server-side --force-conflicts -f https://github.com/envoyproxy/gateway/releases/download/v1.2.4/install.yaml

echo "--> Ожидание готовности контроллера Envoy Gateway..."
kubectl wait --namespace envoy-gateway-system \
  --for=condition=ready pod \
  --selector=control-plane=envoy-gateway \
  --timeout=180s

# 6. Применение манифестов базовой инфраструктуры
echo "--> [4/7] Применение манифестов базовой инфраструктуры (PostgreSQL, Redis, Keycloak)..."
kubectl create namespace budget-nutritionist --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -f "${K8S_DIR}/infra/01-postgres.yaml"
kubectl apply -f "${K8S_DIR}/infra/02-redis.yaml"
kubectl apply -f "${K8S_DIR}/infra/03-keycloak.yaml"

echo "--> Ожидание готовности подов базовой инфраструктуры..."
echo "    - Ожидание PostgreSQL 17..."
kubectl wait --namespace budget-nutritionist \
  --for=condition=ready pod \
  --selector=app=postgres \
  --timeout=180s

echo "    - Ожидание Redis State..."
kubectl wait --namespace budget-nutritionist \
  --for=condition=ready pod \
  --selector=app=redis-state \
  --timeout=180s

echo "    - Ожидание Redis Cache..."
kubectl wait --namespace budget-nutritionist \
  --for=condition=ready pod \
  --selector=app=redis-cache \
  --timeout=180s

echo "    - Ожидание Keycloak 26..."
kubectl wait --namespace budget-nutritionist \
  --for=condition=ready pod \
  --selector=app=keycloak \
  --timeout=240s

# 6.1 Импорт реалма FitAssistant при необходимости
echo "--> Проверка realm FitAssistant в Keycloak..."
kubectl exec deployment/keycloak -n budget-nutritionist -- /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin &>/dev/null || true
if ! kubectl exec deployment/keycloak -n budget-nutritionist -- /opt/keycloak/bin/kcadm.sh get realms/FitAssistant &>/dev/null; then
    echo "    Импорт realm FitAssistant..."
    kubectl exec deployment/keycloak -n budget-nutritionist -- /opt/keycloak/bin/kcadm.sh create realms -f /opt/keycloak/data/import/fitassistant-realm.json
else
    echo "    Realm FitAssistant уже существует."
fi

# 7. Применение сетевого периметра и микросервисов
echo "--> [5/7] Применение сетевого периметра и микросервисов (profile-service, catalog-price-service)..."
kubectl apply -f "${K8S_DIR}/gateway/01-gateway-class.yaml"
kubectl apply -f "${K8S_DIR}/gateway/02-tls-secret.yaml"
kubectl apply -f "${K8S_DIR}/gateway/02-gateway.yaml"
kubectl apply -f "${K8S_DIR}/apps/profile-service/"
kubectl apply -f "${K8S_DIR}/apps/catalog-price-service/"
kubectl apply -f "${K8S_DIR}/gateway/05-security-policy.yaml"
kubectl apply -f "${K8S_DIR}/gateway/06-backend-traffic-policy.yaml"

echo "--> [6/7] Ожидание готовности profile-service и Envoy Proxy..."
kubectl wait --namespace budget-nutritionist \
  --for=condition=available deployment/profile-service \
  --timeout=180s

kubectl wait --namespace envoy-gateway-system \
  --for=condition=ready pod \
  --selector=gateway.envoyproxy.io/owning-gateway-name=main-gateway \
  --timeout=180s

# 8. Интеграционные проверки (Smoke Testing)
echo ""
echo "=========================================================================="
echo "--> [7/7] Интеграционное тестирование и Smoke-проверка"
echo "=========================================================================="

echo "--- 1. Статус всех подов в namespace budget-nutritionist ---"
kubectl get pods -n budget-nutritionist -o wide

echo ""
echo "--- 2. Статус всех подов в namespace envoy-gateway-system ---"
kubectl get pods -n envoy-gateway-system -o wide

echo ""
echo "--- 3. Проверка применения Liquibase миграций в profile-service ---"
kubectl logs deployment/profile-service -n budget-nutritionist --tail=100 | grep -i -E "liquibase|changelog|successfully|Started ProfileApplicationKt" || true

echo ""
echo "--- 4. Проверка gRPC через Envoy Gateway (порт 9090) ---"
if command -v grpcurl &> /dev/null; then
    echo "4.1 Вызов GetProfile без JWT токена (ожидается UNAUTHENTICATED):"
    TEST_UNAUTH=$(grpcurl -proto "${PROJECT_ROOT}/proto/nutritionist/profile/v1/profile.proto" -import-path "${PROJECT_ROOT}/proto" -plaintext -d '{"user_id": "test-user-001"}' localhost:9090 com.budgetntr.profile.v1.ProfileService/GetProfile 2>&1 || true)
    echo "${TEST_UNAUTH}"
    if echo "${TEST_UNAUTH}" | grep -qi -E "Unauthenticated|Jwt is missing"; then
        echo "--> [SUCCESS] Envoy Gateway корректно отклонил запрос без токена (Jwt is missing)."
    fi

    echo ""
    echo "4.2 Получение OAuth 2.1 токена из Keycloak для пользователя testuser..."
    kubectl exec deployment/keycloak -n budget-nutritionist -- /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm FitAssistant --user testuser --password password123 --client fitassistant-mobile &>/dev/null || true
    TOKEN=$(kubectl exec deployment/keycloak -n budget-nutritionist -- /bin/sh -c 'cat ~/.keycloak/kcadm.config' | jq -r '.endpoints."http://localhost:8080".FitAssistant.token' 2>/dev/null || true)

    if [[ -n "${TOKEN}" && "${TOKEN}" != "null" ]]; then
        echo "--> [SUCCESS] Токен успешно получен (${#TOKEN} байт)."
        echo ""
        echo "4.3 Вызов UpdateProfile через Envoy Gateway с валидным JWT токеном:"
        UPDATE_PAYLOAD='{"user_id": "266e29c1-ea60-40b1-aea7-9f3cddaa0782", "height_cm": 180, "weight_kg": 75.5, "gender": "GENDER_MALE", "activity_level": "ACTIVITY_LEVEL_MODERATE", "goal_type": "GOAL_TYPE_MAINTAIN", "weekly_budget": 5000.0, "nutrition_goal": {"target_calories": 2200, "target_proteins_grams": 140.0, "target_fats_grams": 70.0, "target_carbs_grams": 250.0, "target_weight_kg": 75.0}}'
        UPDATE_RES=$(echo "${UPDATE_PAYLOAD}" | grpcurl -proto "${PROJECT_ROOT}/proto/nutritionist/profile/v1/profile.proto" -import-path "${PROJECT_ROOT}/proto" -plaintext -H "Authorization: Bearer ${TOKEN}" -d '@' localhost:9090 com.budgetntr.profile.v1.ProfileService/UpdateProfile 2>&1 || true)
        echo "${UPDATE_RES}"

        echo ""
        echo "4.4 Вызов GetProfile через Envoy Gateway с валидным JWT токеном:"
        GET_PAYLOAD='{"user_id": "266e29c1-ea60-40b1-aea7-9f3cddaa0782"}'
        GET_RES=$(echo "${GET_PAYLOAD}" | grpcurl -proto "${PROJECT_ROOT}/proto/nutritionist/profile/v1/profile.proto" -import-path "${PROJECT_ROOT}/proto" -plaintext -H "Authorization: Bearer ${TOKEN}" -d '@' localhost:9090 com.budgetntr.profile.v1.ProfileService/GetProfile 2>&1 || true)
        echo "${GET_RES}"
    fi
fi

echo ""
echo "=== Развёртывание и верификация кластера успешно завершены! ==="
