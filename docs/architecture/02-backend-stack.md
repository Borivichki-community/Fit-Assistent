# 02 — Backend Stack

> **Стек:** Spring Boot 4.0.x · Kotlin 2.4.x · gRPC / Buf · Kafka 3.9 / Avro · PostgreSQL 17 · Liquibase · Kubernetes Gateway API · Envoy Gateway · Resilience4j · OpenTelemetry

---

## 1. Карта компонентов системы

```text
Mobile (Room + расчёт порций/остатков)
  -> Envoy Gateway (JWT / rate limit)
     -> Profile: профиль и недельный лимит
     -> Catalog: справочные цены и SetFavorite / ListFavorites
     -> Diary: хранение клиентских снимков дневника
     -> Recipe AI: генерация по ограничениям запроса, чтение каталога
     -> Food CV: будущий сценарий распознавания
Diary -> Outbox -> diary.entry.created -> будущие потребители факта питания
```

Это целевая карта. Серверные предметные API, кроме каркаса Profile, пока не реализованы. Recipe AI не ведёт баланс по Kafka; сквозные решения по стоимости и синхронизации — в [топологии](04-database-and-contracts-topology.md).

### 1.1 Описание сервисов

| Сервис | БД | gRPC Port | Kafka Topics (produce) | Kafka Topics (consume) |
|---|---|---|---|---|
| `profile-service` | `profile_db` | 9090 | не требуется для текущего сценария | — |
| `catalog-price-service` | `catalog_db` | 9091 | `food.price.updated` (отложено) | — |
| `diary-service` | `diary_db` | 9092 | `diary.entry.created` (план, схема существует) | — |
| `recipe-ai-service` | Redis (cache) | 9093 | `recipe.generated` (отложено) | — |
| `food-cv-service` | Redis (cache) | 9094 | `food.detected` (отложено) | — |

---

## 2. API Gateway — Kubernetes Gateway API + Envoy Gateway

### 2.1 GRPCRoute — маршрутизация gRPC
```yaml
# infrastructure/k8s/gateway/grpcroute-diary.yaml
apiVersion: gateway.networking.k8s.io/v1
kind: GRPCRoute
metadata:
  name: diary-grpc-route
  namespace: budget-nutritionist
spec:
  parentRefs:
    - name: main-gateway
      namespace: envoy-gateway-system
  hostnames:
    - "api.budget-nutritionist.app"
  rules:
    - matches:
        - method:
            service: com.budgetntr.diary.v1.DiaryService
      backendRefs:
        - name: diary-service
          port: 9092
    - matches:
        - method:
            service: com.budgetntr.recipe.v1.RecipeService
      backendRefs:
        - name: recipe-ai-service
          port: 9093
```

### 2.2 SecurityPolicy — JWT валидация
```yaml
# infrastructure/k8s/gateway/security-policy.yaml
apiVersion: gateway.envoyproxy.io/v1alpha1
kind: SecurityPolicy
metadata:
  name: jwt-auth-policy
  namespace: budget-nutritionist
spec:
  targetRef:
    group: gateway.networking.k8s.io
    kind: GRPCRoute
    name: diary-grpc-route
  jwt:
    providers:
      - name: keycloak
        issuer: "https://auth.budget-nutritionist.app/realms/budget-nutritionist"
        audiences:
          - "budget-nutritionist-api"
        remoteJWKS:
          uri: "https://auth.budget-nutritionist.app/realms/budget-nutritionist/protocol/openid-connect/certs"
        claimToHeaders:
          - claim: sub
            header: x-user-id
          - claim: email
            header: x-user-email
```

### 2.3 BackendTrafficPolicy — Rate Limiting через Redis
```yaml
# infrastructure/k8s/gateway/backend-traffic-policy.yaml
apiVersion: gateway.envoyproxy.io/v1alpha1
kind: BackendTrafficPolicy
metadata:
  name: rate-limit-policy
  namespace: budget-nutritionist
spec:
  targetRef:
    group: gateway.networking.k8s.io
    kind: GRPCRoute
    name: diary-grpc-route
  rateLimit:
    type: Global
    global:
      rules:
        - clientSelectors:
            - headers:
                - name: x-user-id
                  type: Distinct
          limit:
            requests: 100
            unit: Minute
        - clientSelectors:
            - headers:
                - name: x-user-id
                  type: Distinct
          limit:
            requests: 1000
            unit: Hour
  # Redis backend для хранения счётчиков (конфигурируется в EnvoyGateway CR)
```

---

## 3. Buf — управление Protobuf

### 3.1 `buf.yaml`
```yaml
# proto/buf.yaml
version: v2
modules:
  - path: proto
lint:
  use:
    - DEFAULT
    - COMMENTS           # обязательны комментарии к RPC и message
  except:
    - PACKAGE_VERSION_SUFFIX
  enum_zero_value_suffix: _UNSPECIFIED
  rpc_allow_google_protobuf_empty_requests: true
breaking:
  use:
    - FILE
```

### 3.2 `buf.gen.yaml`
```yaml
# proto/buf.gen.yaml
version: v2
managed:
  enabled: true
  override:
    - file_option: java_package
      value: com.budgetntr.proto
    - file_option: java_multiple_files
      value: "true"
    - file_option: kotlin_package
      value: com.budgetntr.proto
plugins:
  - remote: buf.build/protocolbuffers/java
    out: server/src/main/java
  - remote: buf.build/grpc/java
    out: server/src/main/java
  - remote: buf.build/protocolbuffers/kotlin
    out: shared/src/commonMain/kotlin
```

### 3.3 Команды
```bash
# Проверка
buf lint proto/
buf breaking proto/ --against 'https://github.com/org/budget-nutritionist.git#branch=main'

# Генерация
buf generate proto/

# Push в BSR (Buf Schema Registry)
buf push proto/ --label main
```

### 3.4 Канонические контракты и проекты API

Действующий [profile.proto](../../proto/nutritionist/profile/v1/profile.proto) остаётся без изменений. Проекты ещё не реализованных API приведены в одном месте: [Catalog](../../backend/catalog-price-service/README.md), [Diary](../../backend/diary-service/README.md), [Recipe AI](../../backend/recipe-ai-service/README.md). Изменение типов в этих Markdown-примерах не является миграцией опубликованных контрактов. При реализации потребуются реальные .proto, Buf lint/breaking и кодогенерация.

Diary принимает сохранённые клиентом нутриенты и `CostSnapshot`; сервер не рассчитывает сумму порции по каталогу. Catalog использует `SetFavorite`, без toggle. Recipe AI принимает контекст генерации с доступными КБЖУ и денежным пределом. Денежные поля новых API — целые минимальные единицы с валютой; форматированные строки остаются в UiStateMapper.

---

## 4. Kafka + Avro + Schema Registry

### 4.1 Существующие схемы и их назначение

| Схема | Содержание | Применение |
|---|---|---|
| [UserRegisteredEvent](../../shared/contracts/src/main/avro/UserRegisteredEvent.avsc) | id, email, registrationTimestamp, source | Регистрация; производитель/доставка — будущая интеграция. Не обновление профиля |
| [CaloriesConsumedEvent](../../shared/contracts/src/main/avro/CaloriesConsumedEvent.avsc) | userId, mealId, calories, timestamp, mealType, notes | Факт питания; целевой topic `diary.entry.created`. Не денежный расход |

Режим совместимости BACKWARD сохраняется. Обе схемы и существующие мапперы остаются неизменными. Стоимость не добавляется в CaloriesConsumedEvent. Новые поля/схемы вводятся только при появлении реального потребителя, с проверкой совместимости.

### 4.2 Данные потребителей без лишних событий

- Регистрация и обновление профиля — разные факты. `UpdateProfile` не публикует UserRegisteredEvent; `user.profile.updated` не требуется текущему запросному сценарию.
- Мобильный клиент вычисляет оставшиеся КБЖУ и выделенный бюджет, передаёт их в Recipe AI вместе с исключениями и ID локального избранного. Так учитываются даже PENDING-записи без ожидания Kafka.
- `user.favorite.changed` и соответствующая матрица предпочтений Redis исключены из текущего плана: для персонализации достаточно контекста запроса. Нового события и Avro-файла нет.
- Дневник не потребляет `food.price.updated`; исторический снимок не пересчитывается.
- При будущей публикации CaloriesConsumedEvent значение mealId равно ID сохранённой записи, timestamp — времени приёма пищи, calories — сохранённым целым ккал. Outbox создаётся в той же транзакции. Повторная доставка допустима; потребитель дедуплицирует факт создания по (userId, mealId), а не вычитает его несколько раз.

### 4.3 Transactional Outbox Pattern

```
┌──────────────────────────────────────────────────────┐
│  @Transactional (JPA/Hibernate)                      │
│  1. INSERT INTO diary_entries (...)                  │
│  2. INSERT INTO outbox_messages (                    │
│       id, aggregate_type, aggregate_id,              │
│       topic, payload, created_at, status='PENDING'   │
│     )                                                │
└──────────────────────────────────────────────────────┘
                         │
                         │ (транзакция зафиксирована)
                         ▼
┌──────────────────────────────────────────────────────┐
│  OutboxProcessor (Scheduled / Debezium CDC)           │
│  SELECT * FROM outbox_messages WHERE status='PENDING' │
│  FOR UPDATE SKIP LOCKED                              │
│  → KafkaProducer.send(topic, avroPayload).get()      │
│  → UPDATE outbox_messages SET status='PUBLISHED'     │
└──────────────────────────────────────────────────────┘
```

#### Таблица `outbox_messages` (Liquibase)
```xml
<!-- server/src/main/resources/db/changelog/003-outbox.xml -->
<changeSet id="003-outbox" author="team">
  <createTable tableName="outbox_messages">
    <column name="id" type="uuid">
      <constraints primaryKey="true" nullable="false"/>
    </column>
    <column name="aggregate_type" type="varchar(100)"><constraints nullable="false"/></column>
    <column name="aggregate_id" type="varchar(100)"><constraints nullable="false"/></column>
    <column name="topic" type="varchar(255)"><constraints nullable="false"/></column>
    <column name="payload" type="bytea"><constraints nullable="false"/></column>
    <column name="status" type="varchar(20)" defaultValue="PENDING"><constraints nullable="false"/></column>
    <column name="created_at" type="timestamptz" defaultValueComputed="NOW()"><constraints nullable="false"/></column>
    <column name="published_at" type="timestamptz"/>
    <column name="attempts" type="int" defaultValueNumeric="0"/>
  </createTable>
  <createIndex tableName="outbox_messages" indexName="idx_outbox_pending">
    <column name="status"/>
    <column name="created_at"/>
  </createIndex>
</changeSet>
```

### 4.4 OutboxProcessor (Kotlin Spring)
```kotlin
@Component
class OutboxProcessor(
    private val outboxRepository: OutboxMessageRepository,
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
) {
    // Выполняется вне @Transactional!
    @Scheduled(fixedDelay = 500)
    fun processOutbox() {
        val messages = outboxRepository.findPendingWithLock(limit = 100)
        messages.forEach { message ->
            runCatching {
                kafkaTemplate.send(message.topic, message.aggregateId, message.payload).get()
                outboxRepository.markPublished(message.id)
            }.onFailure { ex ->
                outboxRepository.incrementAttempts(message.id)
                logger.error("Failed to publish outbox message ${message.id}", ex)
            }
        }
    }
}
```

---

## 5. Resilience4j — Circuit Breaker & Retry

### 5.1 Конфигурация `application.yml`
```yaml
resilience4j:
  circuitbreaker:
    instances:
      llm-api:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
        slow-call-duration-threshold: 5s
        slow-call-rate-threshold: 80
  retry:
    instances:
      idempotent-grpc:
        max-attempts: 3
        wait-duration: 500ms
        retry-on-exception-predicate: com.budgetntr.config.IdempotentRetryPredicate
  timelimiter:
    instances:
      llm-api:
        timeout-duration: 10s
```

### 5.2 Применение в коде
```kotlin
@Service
class RecipeAiService(
    private val llmClient: LlmApiClient,
    private val recipeCache: RecipeCacheRepository,
) {
    @CircuitBreaker(name = "llm-api", fallbackMethod = "getCachedRecipes")
    @TimeLimiter(name = "llm-api")
    suspend fun generateRecipes(profile: UserProfile): List<Recipe> =
        llmClient.generateBudgetRecipes(profile)

    // Fallback — никогда не бросает исключение
    @Suppress("UnusedPrivateMember")
    private suspend fun getCachedRecipes(profile: UserProfile, ex: Throwable): List<Recipe> {
        logger.warn("LLM circuit open, returning cached recipes", ex)
        return recipeCache.getPopularRecipes(profile.dietaryPreferences)
    }
}
```

---

## 6. Liquibase + PostgreSQL

### 6.1 Структура миграций
```
server/src/main/resources/db/
├── changelog/
│   ├── db.changelog-master.xml     # подключает все changelog'и
│   ├── 001-profile-schema.xml
│   ├── 002-catalog-schema.xml
│   ├── 003-outbox.xml
│   ├── 004-diary-schema.xml
│   └── 005-indexes.xml             # индексы на БЖУ/цены/категории
└── data/
    └── 001-seed-categories.xml     # начальные категории продуктов
```

### 6.2 Пример индексов (`005-indexes.xml`)
```xml
<changeSet id="005-indexes" author="team">
  <!-- Поиск рецептов по калориям и цене (бюджет) -->
  <createIndex tableName="recipes" indexName="idx_recipes_calories_price">
    <column name="calories_per_100g"/>
    <column name="price_per_100g"/>
  </createIndex>
  <!-- Фильтрация по категории и белкам -->
  <createIndex tableName="food_items" indexName="idx_food_category_protein">
    <column name="category_id"/>
    <column name="proteins_per_100g"/>
  </createIndex>
  <!-- Индекс для поиска рецептов по тегам (частичный для активных) -->
  <createIndex tableName="recipes" indexName="idx_recipes_active_category">
    <column name="category_id"/>
    <column name="is_active"/>
    <column name="created_at"/>
  </createIndex>
</changeSet>
```

### 6.3 Правила JPA
```kotlin
// ПРАВИЛЬНО: lazy + EntityGraph для конкретного запроса
@Entity
@Table(name = "recipes")
@NamedEntityGraph(
    name = "Recipe.withIngredients",
    attributeNodes = [NamedAttributeNode("ingredients")],
)
data class RecipeJpaEntity(
    @Id val id: UUID,
    val name: String,
    val caloriesPer100g: Double,
    val pricePer100g: BigDecimal,

    @OneToMany(mappedBy = "recipe", fetch = FetchType.LAZY) // LAZY обязательно
    val ingredients: List<IngredientJpaEntity> = emptyList(),
)

// В репозитории — JOIN FETCH для нужных запросов
@Query("SELECT r FROM RecipeJpaEntity r JOIN FETCH r.ingredients WHERE r.id = :id")
fun findByIdWithIngredients(id: UUID): RecipeJpaEntity?
```

---

## 7. Идемпотентность мутаций

### 7.1 Проверка idempotency-key в gRPC Interceptor
```kotlin
@Component
class IdempotencyServerInterceptor(
    private val idempotencyStore: IdempotencyStore, // Redis-based
) : ServerInterceptor {

    override fun <Req, Resp> interceptCall(
        call: ServerCall<Req, Resp>,
        headers: Metadata,
        next: ServerCallHandler<Req, Resp>,
    ): ServerCall.Listener<Req> {
        val key = headers.get(IDEMPOTENCY_KEY_HEADER)
        if (key != null) {
            val cached = idempotencyStore.get(key)
            if (cached != null) {
                @Suppress("UNCHECKED_CAST")
                call.sendMessage(cached as Resp)
                call.close(Status.OK, headers)
                return object : ServerCall.Listener<Req>() {}
            }
        }
        return IdempotencyCapturingListener(call, headers, key, next, idempotencyStore)
    }

    companion object {
        val IDEMPOTENCY_KEY_HEADER: Metadata.Key<String> =
            Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
    }
}
```

---

## 8. OpenTelemetry + Observability Stack

### 8.1 Конфигурация (Spring Boot 4)
```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  pattern:
    console: >
      {"timestamp":"%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ}",
       "level":"%level","service":"${spring.application.name}",
       "traceId":"%X{traceId}","spanId":"%X{spanId}",
       "userId":"%X{userId}","message":"%msg"}%n
```

### 8.2 Проброс trace context в Kafka
```kotlin
@Component
class TracingKafkaProducerInterceptor : ProducerInterceptor<String, ByteArray> {
    override fun onSend(record: ProducerRecord<String, ByteArray>): ProducerRecord<String, ByteArray> {
        val span = Span.current()
        if (span.spanContext.isValid) {
            val headers = record.headers()
            W3CTraceContextPropagator.getInstance().inject(
                Context.current(),
                headers,
                { carrier, key, value -> carrier.add(RecordHeader(key, value.toByteArray())) }
            )
        }
        return record
    }
    override fun onAcknowledgement(metadata: RecordMetadata?, exception: Exception?) {}
    override fun close() {}
    override fun configure(configs: Map<String, *>) {}
}
```

### 8.3 MDC для userId в логах
```kotlin
@Component
class UserContextGrpcInterceptor : ServerInterceptor {
    override fun <Req, Resp> interceptCall(
        call: ServerCall<Req, Resp>,
        headers: Metadata,
        next: ServerCallHandler<Req, Resp>,
    ): ServerCall.Listener<Req> {
        val userId = headers.get(USER_ID_HEADER) ?: "anonymous"
        MDC.put("userId", userId)
        return try {
            next.startCall(call, headers)
        } finally {
            MDC.remove("userId")
        }
    }
}
```

---

## 9. Observability Stack (Prometheus / Grafana / Loki / Tempo)

```
Spring Boot Services
       │
       ├─→ /actuator/prometheus → Prometheus → Grafana Dashboards
       │
       ├─→ JSON logs → Promtail/FluentBit → Loki → Grafana Explore
       │
       └─→ OTLP traces → OpenTelemetry Collector → Tempo → Grafana Explore
```

---

## 10. Тестирование Backend

### 10.1 Стратегия
| Уровень | Инструмент | Coverage цель |
|---|---|---|
| Unit | JUnit 5 + Mockk (сервисный слой) | >80% |
| Integration (DB) | Testcontainers PostgreSQL | критические пути |
| Integration (Kafka) | Testcontainers Kafka | outbox → consumer |
| Integration (Redis) | Testcontainers Redis | idempotency, cache |
| LLM Mock | WireMock | все LLM-вызовы |
| gRPC E2E | `grpcurl` / клиент в тесте | happy + error paths |
| Load | k6 | SLA: p95 < 200ms |

### 10.2 Пример Testcontainers теста
```kotlin
@SpringBootTest
@ExtendWith(SpringExtension::class)
class DiaryServiceIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("diary_db_test")
            .withUsername("test")
            .withPassword("test")

        @Container
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.0"))

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
        }
    }

    @Autowired lateinit var diaryService: DiaryService
    @Autowired lateinit var outboxRepository: OutboxMessageRepository

    @Test
    fun `adding diary entry creates outbox message in same transaction`() = runTest {
        val request = AddDiaryEntryRequest(
            userId = "user-123",
            foodItemId = "apple-001",
            quantityGrams = 150.0,
            idempotencyKey = "key-${UUID.randomUUID()}",
        )

        diaryService.addEntry(request)

        val pending = outboxRepository.findByStatus("PENDING")
        assertThat(pending).hasSize(1)
        assertThat(pending.first().aggregateType).isEqualTo("DiaryEntry")
    }
}
```

### 10.3 WireMock для LLM
```kotlin
@SpringBootTest
class RecipeAiServiceTest {

    @RegisterExtension
    val wireMock = WireMockExtension.newInstance()
        .options(WireMockConfiguration.options().dynamicPort())
        .build()

    @Test
    fun `circuit breaker activates on LLM timeout and returns cached recipes`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/generate"))
                .willReturn(aResponse().withFixedDelay(15_000)) // превышает timelimiter 10s
        )

        // После нескольких запросов circuit должен открыться
        repeat(11) {
            runCatching { recipeAiService.generateRecipes(testProfile) }
        }

        // Следующий вызов должен вернуть cached результат без обращения к LLM
        val result = recipeAiService.generateRecipes(testProfile)
        assertThat(result).isNotEmpty()
        wireMock.verify(exactly(10), postRequestedFor(urlPathEqualTo("/v1/generate")))
    }
}
```
