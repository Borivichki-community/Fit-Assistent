---
name: contracts-proto-and-avro
description: >
  Изменение .proto контрактов и Avro-схем Kafka. Используй этот Skill при добавлении новых
  RPC-методов, изменении существующих message-типов, добавлении полей в Avro-схемы,
  обновлении манифестов GRPCRoute / SecurityPolicy / BackendTrafficPolicy для Envoy Gateway,
  запуске buf lint / buf breaking / buf generate. Обязателен при любой работе в proto/ или
  при изменении Kafka-топиков и Schema Registry.
---

# Skill: Contracts — Protobuf & Avro

## Назначение

Этот Skill управляет всеми изменениями публичных контрактов системы:
gRPC (`.proto`) и Kafka (`.avsc`). Контракты — это API-граница между сервисами;
любое breaking change может сломать клиентов.

---

## Workflow

### Шаг 1. Прочитать STATE.md и архитектурный документ

```
1. Прочитать STATE.md — понять текущий статус.
2. Прочитать docs/architecture/02-backend-stack.md §3 (Buf) и §4 (Kafka/Avro).
3. Прочитать docs/architecture/01-overview-kmp-infra.md §5 (gRPC в KMP).
```

### Шаг 2. Изменение .proto файлов

#### 2.1 Структура proto/
```
proto/
├── buf.yaml
├── buf.gen.yaml
├── buf.lock
└── com/budgetntr/
    ├── diary/v1/
    │   └── diary.proto
    ├── recipe/v1/
    │   └── recipe.proto
    ├── profile/v1/
    │   └── profile.proto
    ├── catalog/v1/
    │   └── catalog.proto
    └── common/v1/
        └── common.proto      # общие типы: Money, NutritionFacts, PageInfo
```

#### 2.2 Правила именования (buf lint DEFAULT)
- Пакет: `com.budgetntr.<service>.v1`
- Message: `PascalCase`
- Field: `snake_case`
- Enum value: `UPPER_SNAKE_CASE`, нулевое значение: `<ENUM_NAME>_UNSPECIFIED`
- Service: `<Name>Service`
- RPC: `PascalCase` (глагол + существительное)

#### 2.3 Правила backward compatibility

**РАЗРЕШЕНО (не breaking):**
- Добавить новое поле в message (с новым field number)
- Добавить новый RPC в service
- Добавить новое значение в enum (ЕСЛИ не последнее по алфавиту, лучше — в конец)
- Добавить опциональное поле

**ЗАПРЕЩЕНО (breaking):**
- Удалить поле или изменить его тип
- Изменить field number
- Переименовать поле (изменяет JSON name)
- Удалить RPC
- Изменить тип request/response в RPC
- Изменить пакет или имя message

#### 2.4 Пример корректного добавления поля
```protobuf
message AddEntryRequest {
  string food_item_id = 1;
  double quantity_grams = 2;
  google.protobuf.Timestamp recorded_at = 3;
  // Новое поле добавляется в конец с новым номером:
  string notes = 4;   // ✅ OK: добавление нового поля
  // ЗАПРЕЩЕНО: изменить тип или номер существующего поля
}
```

#### 2.5 Обязательные doc-комментарии
```protobuf
// DiaryService manages user food diary operations.
service DiaryService {
  // AddEntry creates a new diary entry.
  // The client MUST provide idempotency-key in gRPC metadata.
  // Idempotent: same key returns cached response.
  rpc AddEntry(AddEntryRequest) returns (AddEntryResponse);
}

// AddEntryRequest contains data to create a diary entry.
message AddEntryRequest {
  // food_item_id is the unique identifier of the food item.
  string food_item_id = 1;
  // quantity_grams is the amount consumed in grams. Must be > 0.
  double quantity_grams = 2;
}
```

### Шаг 3. Запуск Buf проверок (ОБЯЗАТЕЛЬНО перед коммитом)

```bash
# 1. Lint — стиль и конвенции
buf lint proto/

# 2. Breaking change detection — нет ли поломок API
buf breaking proto/ --against '.git#branch=main'

# 3. При чистых проверках — генерация кода
buf generate proto/

# 4. Push в Buf Schema Registry (если подключён BSR)
buf push proto/ --label main
```

**Если `buf breaking` упал:**
- Нельзя игнорировать!
- Если breaking change необходим → создать новую версию пакета (`v2`).
- Оба пакета (`v1` и `v2`) поддерживать параллельно до полной миграции клиентов.

### Шаг 4. Обновить манифесты Kubernetes Gateway

#### 4.1 При добавлении нового RPC в существующий сервис
- Проверить `infrastructure/k8s/gateway/grpcroute-<service>.yaml`.
- Обычно дополнительных изменений не требуется — GRPCRoute матчит по service package.

#### 4.2 При добавлении нового сервиса
```yaml
# infrastructure/k8s/gateway/grpcroute-<newservice>.yaml
apiVersion: gateway.networking.k8s.io/v1
kind: GRPCRoute
metadata:
  name: <newservice>-grpc-route
  namespace: budget-nutritionist
spec:
  parentRefs:
    - name: main-gateway
      namespace: envoy-gateway-system
  hostnames: ["api.budget-nutritionist.app"]
  rules:
    - matches:
        - method:
            service: com.budgetntr.<newservice>.v1.<NewService>Service
      backendRefs:
        - name: <newservice>
          port: 909X   # следующий свободный порт
```

#### 4.3 SecurityPolicy — JWT для нового маршрута
```yaml
apiVersion: gateway.envoyproxy.io/v1alpha1
kind: SecurityPolicy
metadata:
  name: jwt-auth-<newservice>
  namespace: budget-nutritionist
spec:
  targetRef:
    group: gateway.networking.k8s.io
    kind: GRPCRoute
    name: <newservice>-grpc-route
  jwt:
    providers:
      - name: keycloak
        issuer: "https://auth.budget-nutritionist.app/realms/budget-nutritionist"
        audiences: ["budget-nutritionist-api"]
        remoteJWKS:
          uri: "https://auth.budget-nutritionist.app/realms/budget-nutritionist/protocol/openid-connect/certs"
```

### Шаг 5. Изменение Avro-схем

#### 5.1 Правила совместимости BACKWARD
- Режим Schema Registry: `BACKWARD` (новая схема совместима с **предыдущей**).
- **РАЗРЕШЕНО (BACKWARD совместимо):**
  - Добавить поле с `"default"` значением.
  - Удалить поле, у которого было `"default"`.
- **ЗАПРЕЩЕНО:**
  - Добавить поле без `"default"` (старые консьюмеры не смогут десериализовать).
  - Изменить тип поля.
  - Переименовать поле (без `"aliases"`).

#### 5.2 Пример добавления поля в Avro-схему
```json
{
  "namespace": "com.budgetntr.events.diary.v1",
  "type": "record",
  "name": "DiaryEntryCreated",
  "fields": [
    { "name": "eventId",       "type": "string" },
    { "name": "entryId",       "type": "string" },
    { "name": "userId",        "type": "string" },
    { "name": "foodItemId",    "type": "string" },
    { "name": "quantityGrams", "type": "double" },
    { "name": "calories",      "type": "double" },
    { "name": "recordedAtMs",  "type": "long", "logicalType": "timestamp-millis" },
    { "name": "idempotencyKey","type": "string" },
    // Новое поле ОБЯЗАНО иметь default:
    { "name": "notes",         "type": ["null", "string"], "default": null }
  ]
}
```

#### 5.3 Проверка совместимости перед публикацией
```bash
# Зарегистрировать новую версию схемы с проверкой совместимости
curl -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data '{"schema": "<AVRO_SCHEMA_JSON_ESCAPED>"}' \
  http://localhost:8081/compatibility/subjects/diary.entry.created-value/versions/latest

# Ожидаемый ответ: {"is_compatible": true}
```

### Шаг 6. Обновить generated код

После `buf generate`:
- Проверить, что generated файлы в `server/src/main/java/` обновились.
- Убедиться, что KMP-shared стабы (если есть) также обновлены.
- Запустить тесты: `./gradlew :server:test`.

### Шаг 7. Обновить STATE.md

---

## Чеклист перед коммитом

```
[ ] buf lint proto/ — чисто (нет ошибок)
[ ] buf breaking proto/ --against '.git#branch=main' — нет breaking changes
[ ] buf generate proto/ — код сгенерирован
[ ] Все новые message и RPC имеют doc-комментарии
[ ] Новые поля в proto добавлены в конец с новым field number
[ ] Новые поля в Avro имеют default значение
[ ] Avro Schema Registry: is_compatible = true
[ ] GRPCRoute обновлён (если добавлен новый сервис)
[ ] SecurityPolicy создана (если добавлен новый сервис)
[ ] ./gradlew :server:compileKotlin — GREEN (сгенерированный код компилируется)
```

---

## Таблица: Breaking vs Non-breaking changes

| Изменение | Proto | Avro |
|---|---|---|
| Добавить поле | ✅ (новый number) | ✅ (с default) |
| Удалить поле | ❌ Breaking | ✅ (если есть default) |
| Изменить тип поля | ❌ Breaking | ❌ Breaking |
| Переименовать поле | ❌ Breaking | ❌ (без aliases) |
| Добавить RPC | ✅ | N/A |
| Удалить RPC | ❌ Breaking | N/A |
| Добавить enum value | ✅ (в конец) | ✅ |
| Удалить enum value | ❌ Breaking | ❌ Breaking |
