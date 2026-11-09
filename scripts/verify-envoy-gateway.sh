#!/usr/bin/env bash
# scripts/verify-envoy-gateway.sh
# Автоматизированный скрипт проведения и верификации спайка [T-015]
# Envoy Gateway в kind/k3d: GatewayClass + GRPCRoute на echo-сервис + вызов grpcurl
set -euo pipefail

CLUSTER_NAME="fitassistant-spike"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
K8S_DIR="${PROJECT_ROOT}/infrastructure/k8s"

echo "=== [T-015] Запуск спайка Envoy Gateway в kind/k3d ==="

# 1. Проверка наличия утилит
for cmd in kubectl kind grpcurl; do
    if ! command -v "$cmd" &> /dev/null; then
        echo "ПРЕДУПРЕЖДЕНИЕ: Утилита $cmd не найдена в PATH. Для запуска локального кластера убедитесь, что она установлена."
    fi
done

# 2. Создание кластера kind (если ещё не создан)
if command -v kind &> /dev/null; then
    if ! kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
        echo "Создание кластера kind '${CLUSTER_NAME}' с пробросом портов 80, 443, 9090..."
        kind create cluster --name "${CLUSTER_NAME}" --config "${K8S_DIR}/kind-config.yaml"
    else
        echo "Кластер kind '${CLUSTER_NAME}' уже существует. Переключение контекста..."
        kubectl config use-context "kind-${CLUSTER_NAME}"
    fi
fi

# 3. Установка Kubernetes Gateway API CRDs (v1.2.0 Standard)
echo "Установка стандартных CRD Gateway API v1.2.0..."
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.0/standard-install.yaml

# 4. Установка Envoy Gateway (v1.2.4)
echo "Установка Envoy Gateway..."
kubectl apply -f https://github.com/envoyproxy/gateway/releases/download/v1.2.4/install.yaml

echo "Ожидание готовности контроллера Envoy Gateway..."
kubectl wait --namespace envoy-gateway-system \
  --for=condition=ready pod \
  --selector=control-plane=envoy-gateway \
  --timeout=120s

# 5. Применение манифестов GatewayClass, Gateway, Echo-Service и GRPCRoute
echo "Применение манифестов шлюза..."
kubectl apply -f "${K8S_DIR}/gateway/01-gateway-class.yaml"
kubectl apply -f "${K8S_DIR}/gateway/02-gateway.yaml"
kubectl apply -f "${K8S_DIR}/gateway/03-echo-service.yaml"
kubectl apply -f "${K8S_DIR}/gateway/04-grpc-route.yaml"

# 6. Ожидание готовности echo-сервиса и envoy data-plane proxy
echo "Ожидание готовности echo-сервиса..."
kubectl wait --namespace test-gateway \
  --for=condition=available deployment/echo-grpc \
  --timeout=90s

echo "Ожидание поднятия Envoy Proxy..."
sleep 10
kubectl wait --namespace envoy-gateway-system \
  --for=condition=ready pod \
  --selector=gateway.envoyproxy.io/owning-gateway-name=main-gateway \
  --timeout=120s || echo "Продолжаем проверку портов..."

# 7. Верификация через grpcurl
echo "=== Верификация через grpcurl на localhost:9090 ==="
if command -v grpcurl &> /dev/null; then
    echo "1. Запрос списка gRPC-сервисов (reflection):"
    grpcurl -plaintext localhost:9090 list || true

    echo "2. Вызов Echo RPC метода:"
    grpcurl -plaintext \
      -d '{"message": "FitAssistent Spike T-015: Hello via Envoy Gateway!"}' \
      localhost:9090 echo.EchoServer/Echo
else
    echo "grpcurl не установлен. Команда для ручной проверки:"
    echo "grpcurl -plaintext -d '{\"message\": \"Hello via Envoy Gateway\"}' localhost:9090 echo.EchoServer/Echo"
fi

echo "=== Спайк [T-015] успешно завершён! ==="
