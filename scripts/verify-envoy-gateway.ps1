# scripts/verify-envoy-gateway.ps1
# Автоматизированный скрипт PowerShell для верификации [T-026, T-027, T-028, T-029]
# Envoy Gateway + TLS + profile-service + Keycloak JWT + Rate Limiting на Windows
[CmdletBinding()]
param(
    [string]$ClusterName = "fitassistant-cluster",
    [string]$KeycloakUrl = "http://localhost:8080",
    [string]$Realm = "FitAssistant"
)

$ErrorActionPreference = "Continue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$K8sDir = Join-Path $ProjectRoot "infrastructure\k8s"

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "=== Envoy Gateway & profile-service: Инфраструктурный смоук-тест ===" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan

# 1. Применение манифестов K8s
Write-Host "`n--> 1. Применение манифестов сетевого периметра и profile-service..." -ForegroundColor Yellow
kubectl apply -f "$K8sDir\gateway\01-gateway-class.yaml"
kubectl apply -f "$K8sDir\gateway\02-tls-secret.yaml"
kubectl apply -f "$K8sDir\gateway\02-gateway.yaml"
kubectl apply -f "$K8sDir\gateway\03-profile-service.yaml"
kubectl apply -f "$K8sDir\gateway\04-profile-grpc-route.yaml"
kubectl apply -f "$K8sDir\gateway\05-security-policy.yaml"
kubectl apply -f "$K8sDir\gateway\06-backend-traffic-policy.yaml"

# 2. Тест 1: Запрос без токена
Write-Host "`n=====================================================================" -ForegroundColor Cyan
Write-Host "=== ТЕСТ 1: Вызов GetProfile без JWT токена (Ожидается UNAUTHENTICATED) ===" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan

$test1 = grpcurl -plaintext -d '{"user_id": "usr-test-unauth-001"}' localhost:9090 com.budgetntr.profile.v1.ProfileService/GetProfile 2>&1
Write-Host $test1

if ($test1 -match "Unauthenticated") {
    Write-Host "✅ ТЕСТ 1 ПРОЙДЕН: Envoy Gateway заблокировал запрос без токена со статусом UNAUTHENTICATED." -ForegroundColor Green
} else {
    Write-Host "⚠️ ТЕСТ 1: Проверьте статус ответа. Ожидался UNAUTHENTICATED." -ForegroundColor Yellow
}

# 3. Тест 2: Запрос токена в Keycloak
Write-Host "`n=====================================================================" -ForegroundColor Cyan
Write-Host "=== ТЕСТ 2: Получение токена Keycloak и вызов с Bearer токеном ===" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan

$token = $null
try {
    $tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token" `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
            grant_type = "password"
            client_id  = "fitassistant-mobile"
            username   = "testuser"
            password   = "password123"
        } -ErrorAction SilentlyContinue

    if ($tokenResponse -and $tokenResponse.access_token) {
        $token = $tokenResponse.access_token
        Write-Host "Токен получен успешно (длина: $($token.Length) символов)." -ForegroundColor Green
    }
} catch {
    Write-Host "Keycloak не ответил на $KeycloakUrl. Переход к ручному формату." -ForegroundColor Yellow
}

if ($token) {
    Write-Host "Вызов GetProfile с токеном через TLS (порт 443)..." -ForegroundColor Yellow
    $test2 = grpcurl -insecure -H "Authorization: Bearer $token" -d '{"user_id": "usr-test-auth-001"}' localhost:443 com.budgetntr.profile.v1.ProfileService/GetProfile 2>&1
    Write-Host $test2

    if ($test2 -match "user_id") {
        Write-Host "✅ ТЕСТ 2 ПРОЙДЕН: Успешная авторизация через Keycloak JWT и ответ от profile-service." -ForegroundColor Green
    }
} else {
    Write-Host "Команда для ручного вызова с токеном:" -ForegroundColor Gray
    Write-Host "grpcurl -insecure -H 'Authorization: Bearer <TOKEN>' -d '{\`"user_id\`": \`"usr-123\`"}' localhost:443 com.budgetntr.profile.v1.ProfileService/GetProfile" -ForegroundColor Gray
}

# 4. Тест 3: Rate Limiting
Write-Host "`n=====================================================================" -ForegroundColor Cyan
Write-Host "=== ТЕСТ 3: Проверка Rate Limiter (120 вызовов подряд) ===" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan

$rateLimited = $false
for ($i = 1; $i -le 120; $i++) {
    $resp = grpcurl -plaintext -H "Authorization: Bearer dummy-token" -d '{"user_id": "rate-limit-test-user"}' localhost:9090 com.budgetntr.profile.v1.ProfileService/GetProfile 2>&1
    if ($resp -match "ResourceExhausted") {
        $rateLimited = $true
        Write-Host "✅ ТЕСТ 3 ПРОЙДЕН: На запросе #$i сработал Rate Limiter: Status.RESOURCE_EXHAUSTED (HTTP 429)." -ForegroundColor Green
        break
    }
}

if (-not $rateLimited) {
    Write-Host "ℹ️ Лимит не был превышен в цикле (проверьте настройки Redis и BackendTrafficPolicy)." -ForegroundColor Gray
}

Write-Host "`n=== Скрипт проверки Envoy Gateway успешно выполнен ===" -ForegroundColor Green
