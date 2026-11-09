# scripts/verify-envoy-gateway.ps1
# PowerShell-скрипт запуска и проверки спайка [T-015] под Windows
param(
    [string]$ClusterName = "fitassistant-spike"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$K8sDir = Join-Path $ProjectRoot "infrastructure\k8s"

Write-Host "=== [T-015] Запуск спайка Envoy Gateway в kind/k3d (PowerShell) ===" -ForegroundColor Cyan

# 1. Проверка kind
if (Get-Command kind -ErrorAction SilentlyContinue) {
    $existing = kind get clusters
    if ($existing -notcontains $ClusterName) {
        Write-Host "Создание кластера kind '$ClusterName'..." -ForegroundColor Green
        kind create cluster --name $ClusterName --config "$K8sDir\kind-config.yaml"
    } else {
        Write-Host "Кластер '$ClusterName' уже запущен." -ForegroundColor Yellow
        kubectl config use-context "kind-$ClusterName"
    }
} else {
    Write-Host "kind не найден в PATH. Проверьте подключение к k3d/k8s кластеру." -ForegroundColor Yellow
}

# 2. Установка Gateway API CRDs v1.2.0
Write-Host "Применение стандартных Gateway API CRDs v1.2.0..." -ForegroundColor Cyan
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.0/standard-install.yaml

# 3. Установка Envoy Gateway v1.2.4
Write-Host "Установка Envoy Gateway v1.2.4..." -ForegroundColor Cyan
kubectl apply -f https://github.com/envoyproxy/gateway/releases/download/v1.2.4/install.yaml

Write-Host "Ожидание готовности Envoy Gateway..." -ForegroundColor Cyan
kubectl wait --namespace envoy-gateway-system --for=condition=ready pod --selector=control-plane=envoy-gateway --timeout=120s

# 4. Применение манифестов
Write-Host "Применение манифестов GatewayClass, Gateway, Echo-Service, GRPCRoute..." -ForegroundColor Cyan
kubectl apply -f "$K8sDir\gateway\01-gateway-class.yaml"
kubectl apply -f "$K8sDir\gateway\02-gateway.yaml"
kubectl apply -f "$K8sDir\gateway\03-echo-service.yaml"
kubectl apply -f "$K8sDir\gateway\04-grpc-route.yaml"

# 5. Ожидание готовности
Write-Host "Ожидание готовности тестового echo-сервиса..." -ForegroundColor Cyan
kubectl wait --namespace test-gateway --for=condition=available deployment/echo-grpc --timeout=90s

# 6. Тестовый вызов grpcurl
Write-Host "=== Вызов тестового метода через grpcurl на localhost:9090 ===" -ForegroundColor Green
if (Get-Command grpcurl -ErrorAction SilentlyContinue) {
    grpcurl -plaintext -d '{\"message\": \"FitAssistent Spike T-015: Hello via Envoy Gateway!\"}' localhost:9090 echo.EchoServer/Echo
} else {
    Write-Host "grpcurl не установлен локально. Команда для вызова:" -ForegroundColor Yellow
    Write-Host 'grpcurl -plaintext -d "{\"message\": \"Hello via Envoy Gateway\"}" localhost:9090 echo.EchoServer/Echo' -ForegroundColor White
}

Write-Host "=== Спайк [T-015] успешно завершён! ===" -ForegroundColor Green
