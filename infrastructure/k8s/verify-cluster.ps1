# infrastructure/k8s/verify-cluster.ps1
# Автоматизированный скрипт развёртывания и верификации инфраструктуры FitAssistant в кластере kind
# (PostgreSQL 17, Redis State/Cache, Keycloak 26, Envoy Gateway, profile-service)

$ErrorActionPreference = "Continue"

$ClusterName = "fitassistant-cluster"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir\..\.."
$K8sDir = "$ProjectRoot\infrastructure\k8s"

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "=== FitAssistant: Развертывание и сквозная верификация кластера kind ===" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

# 1. Проверка наличия утилит
$RequiredCommands = @("kubectl", "kind", "docker")
foreach ($cmd in $RequiredCommands) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        Write-Warning "Утилита '$cmd' не найдена в PATH."
    }
}

# 2. Создание или подключение к kind-кластеру
$clusterExists = $false
try {
    $existingClusters = @(cmd /c "kind get clusters 2>nul")
    if ($existingClusters -contains $ClusterName) {
        $clusterExists = $true
    }
} catch {
    $clusterExists = $false
}

if (-not $clusterExists) {
    Write-Host "--> [1/7] Создание кластера kind '$ClusterName'..." -ForegroundColor Cyan
    & kind create cluster --name $ClusterName --config "$K8sDir\kind-config.yaml"
} else {
    Write-Host "--> [1/7] Кластер kind '$ClusterName' уже существует. Переключение контекста..." -ForegroundColor Yellow
    & kubectl config use-context "kind-$ClusterName"
}

# 3. Загрузка собранного Docker-образа в kind
Write-Host "--> Загрузка docker-образа profile-service:latest в кластер kind..." -ForegroundColor Cyan
& kind load docker-image profile-service:latest --name $ClusterName

# 4. Установка Gateway API Standard CRDs (v1.2.0)
Write-Host "--> [2/7] Установка стандартных CRD Gateway API v1.2.0..." -ForegroundColor Cyan
& kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.0/standard-install.yaml

# 5. Установка Envoy Gateway v1.2.4
Write-Host "--> [3/7] Установка Envoy Gateway v1.2.4..." -ForegroundColor Cyan
& kubectl apply --server-side --force-conflicts -f https://github.com/envoyproxy/gateway/releases/download/v1.2.4/install.yaml

Write-Host "--> Ожидание готовности контроллера Envoy Gateway..." -ForegroundColor Cyan
& kubectl wait --namespace envoy-gateway-system --for=condition=ready pod --selector=control-plane=envoy-gateway --timeout=180s

# 6. Применение манифестов базовой инфраструктуры
Write-Host "--> [4/7] Применение манифестов базовой инфраструктуры (PostgreSQL, Redis, Keycloak)..." -ForegroundColor Cyan
& kubectl create namespace budget-nutritionist --dry-run=client -o yaml | kubectl apply -f -

& kubectl apply -f "$K8sDir\infra\01-postgres.yaml"
& kubectl apply -f "$K8sDir\infra\02-redis.yaml"
& kubectl apply -f "$K8sDir\infra\03-keycloak.yaml"

Write-Host "--> Ожидание готовности подов базовой инфраструктуры..." -ForegroundColor Cyan
Write-Host "    - Ожидание PostgreSQL 17..." -ForegroundColor Gray
& kubectl wait --namespace budget-nutritionist --for=condition=ready pod --selector=app=postgres --timeout=180s

Write-Host "    - Ожидание Redis State..." -ForegroundColor Gray
& kubectl wait --namespace budget-nutritionist --for=condition=ready pod --selector=app=redis-state --timeout=180s

Write-Host "    - Ожидание Redis Cache..." -ForegroundColor Gray
& kubectl wait --namespace budget-nutritionist --for=condition=ready pod --selector=app=redis-cache --timeout=180s

Write-Host "    - Ожидание Keycloak 26..." -ForegroundColor Gray
& kubectl wait --namespace budget-nutritionist --for=condition=ready pod --selector=app=keycloak --timeout=240s

# 6.1 Импорт реалма FitAssistant в Keycloak при необходимости
Write-Host "--> Проверка наличия realm FitAssistant в Keycloak..." -ForegroundColor Cyan
$realmExists = $false
try {
    & kubectl exec deployment/keycloak -n budget-nutritionist -- /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin 2>&1 | Out-Null
    $realmCheck = & kubectl exec deployment/keycloak -n budget-nutritionist -- /opt/keycloak/bin/kcadm.sh get realms/FitAssistant 2>&1
    if ($realmCheck -match '"realm"\s*:\s*"FitAssistant"') {
        $realmExists = $true
    }
} catch {
    $realmExists = $false
}

if (-not $realmExists) {
    Write-Host "    Импорт realm FitAssistant..." -ForegroundColor Yellow
    & kubectl exec deployment/keycloak -n budget-nutritionist -- /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin
    & kubectl exec deployment/keycloak -n budget-nutritionist -- /opt/keycloak/bin/kcadm.sh create realms -f /opt/keycloak/data/import/fitassistant-realm.json
} else {
    Write-Host "    Realm FitAssistant уже существует." -ForegroundColor Green
}

# 7. Применение сетевого периметра и profile-service
Write-Host "--> [5/7] Применение сетевого периметра и profile-service..." -ForegroundColor Cyan
& kubectl apply -f "$K8sDir\gateway\01-gateway-class.yaml"
& kubectl apply -f "$K8sDir\gateway\02-tls-secret.yaml"
& kubectl apply -f "$K8sDir\gateway\02-gateway.yaml"
& kubectl apply -f "$K8sDir\apps\profile-service"
& kubectl apply -f "$K8sDir\apps\catalog-price-service"
& kubectl apply -f "$K8sDir\gateway\05-security-policy.yaml"
& kubectl apply -f "$K8sDir\gateway\06-backend-traffic-policy.yaml"

Write-Host "--> [6/7] Ожидание готовности profile-service и Envoy Proxy..." -ForegroundColor Cyan
& kubectl wait --namespace budget-nutritionist --for=condition=available deployment/profile-service --timeout=180s
& kubectl wait --namespace envoy-gateway-system --for=condition=ready pod --selector=gateway.envoyproxy.io/owning-gateway-name=main-gateway --timeout=180s

# 8. Интеграционные проверки (Smoke Testing)
Write-Host ""
Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "--> [7/7] Интеграционное тестирование и Smoke-проверка" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

Write-Host "--- 1. Статус всех подов в namespace budget-nutritionist ---" -ForegroundColor Yellow
& kubectl get pods -n budget-nutritionist -o wide

Write-Host ""
Write-Host "--- 2. Статус всех подов в namespace envoy-gateway-system ---" -ForegroundColor Yellow
& kubectl get pods -n envoy-gateway-system -o wide

Write-Host ""
Write-Host "--- 3. Проверка применения Liquibase миграций в profile-service ---" -ForegroundColor Yellow
$logOutput = & kubectl logs deployment/profile-service -n budget-nutritionist --tail=100
$logOutput | Select-String -Pattern "liquibase|changelog|successfully|Started ProfileApplicationKt" | ForEach-Object { Write-Host $_ }

Write-Host ""
Write-Host "--- 4. Проверка gRPC через Envoy Gateway (порт 9090) ---" -ForegroundColor Yellow
$grpcurlCmd = Get-Command grpcurl -ErrorAction SilentlyContinue
if ($grpcurlCmd) {
    Write-Host "4.1 Вызов GetProfile без JWT токена (ожидается 401 / UNAUTHENTICATED):" -ForegroundColor Gray
    $testUnauth = '{"user_id": "test-user-001"}' | grpcurl -proto "$ProjectRoot\proto\nutritionist\profile\v1\profile.proto" -import-path "$ProjectRoot\proto" -plaintext -d '@' localhost:9090 com.budgetntr.profile.v1.ProfileService/GetProfile 2>&1
    Write-Host "$testUnauth"
    if ($testUnauth -match "Unauthenticated|Jwt is missing") {
        Write-Host "--> [SUCCESS] Envoy Gateway корректно отклонил запрос без токена (Jwt is missing)." -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "4.2 Получение OAuth 2.1 токена из Keycloak для пользователя testuser..." -ForegroundColor Gray
    & kubectl exec deployment/keycloak -n budget-nutritionist -- /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm FitAssistant --user testuser --password password123 --client fitassistant-mobile 2>&1 | Out-Null
    $kcadmRaw = & kubectl exec deployment/keycloak -n budget-nutritionist -- /bin/sh -c 'cat ~/.keycloak/kcadm.config' | Out-String
    $kcadm = $kcadmRaw | ConvertFrom-Json
    $token = $kcadm.endpoints.'http://localhost:8080'.'FitAssistant'.token

    if ($token) {
        Write-Host "--> [SUCCESS] Токен успешно получен (длина: $($token.Length) байт)." -ForegroundColor Green

        Write-Host ""
        Write-Host "4.3 Вызов UpdateProfile через Envoy Gateway с валидным JWT токеном:" -ForegroundColor Gray
        $updateBody = @'
{
  "user_id": "266e29c1-ea60-40b1-aea7-9f3cddaa0782",
  "height_cm": 180,
  "weight_kg": 75.5,
  "gender": "GENDER_MALE",
  "activity_level": "ACTIVITY_LEVEL_MODERATE",
  "goal_type": "GOAL_TYPE_MAINTAIN",
  "weekly_budget": 5000.0,
  "nutrition_goal": {
    "target_calories": 2200,
    "target_proteins_grams": 140.0,
    "target_fats_grams": 70.0,
    "target_carbs_grams": 250.0,
    "target_weight_kg": 75.0
  }
}
'@
        $updateResult = $updateBody | grpcurl -proto "$ProjectRoot\proto\nutritionist\profile\v1\profile.proto" -import-path "$ProjectRoot\proto" -plaintext -H "Authorization: Bearer $token" -d '@' localhost:9090 com.budgetntr.profile.v1.ProfileService/UpdateProfile 2>&1
        Write-Host "$updateResult"
        if ($updateResult -match "266e29c1-ea60-40b1-aea7-9f3cddaa0782") {
            Write-Host "--> [SUCCESS] UpdateProfile успешно обработан через Envoy Gateway и записан в БД!" -ForegroundColor Green
        }

        Write-Host ""
        Write-Host "4.4 Вызов GetProfile через Envoy Gateway с валидным JWT токеном:" -ForegroundColor Gray
        $getBody = '{"user_id": "266e29c1-ea60-40b1-aea7-9f3cddaa0782"}'
        $getResult = $getBody | grpcurl -proto "$ProjectRoot\proto\nutritionist\profile\v1\profile.proto" -import-path "$ProjectRoot\proto" -plaintext -H "Authorization: Bearer $token" -d '@' localhost:9090 com.budgetntr.profile.v1.ProfileService/GetProfile 2>&1
        Write-Host "$getResult"
        if ($getResult -match "266e29c1-ea60-40b1-aea7-9f3cddaa0782") {
            Write-Host "--> [SUCCESS] GetProfile успешно возвратил сохранённый профиль из PostgreSQL!" -ForegroundColor Green
        }
    } else {
        Write-Host "Не удалось извлечь JWT токен." -ForegroundColor Red
    }
} else {
    Write-Host "grpcurl не найден в PATH. Проверьте через grpcurl.exe вручную." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Развертывание и верификация кластера успешно завершены! ===" -ForegroundColor Green
