---
name: spring-grpc-service
description: >
  Разработка микросервисов на Spring Boot 4.0.x. Используй этот Skill при создании или
  изменении gRPC-сервисов (profile-service, catalog-price-service, diary-service,
  recipe-ai-service, food-cv-service), Liquibase-миграций, JPA-сущностей (LAZY, EntityGraph),
  Transactional Outbox для Kafka, Resilience4j (CircuitBreaker + Retry), actuator health,
  OpenTelemetry traceparent, JSON-логов с traceId/spanId/userId. Запрещает сетевые вызовы
  внутри @Transactional и прямую публикацию в Kafka.
---

# Skill: Spring Boot gRPC Service

## Назначение

Этот Skill регулирует разработку всех бэкенд-микросервисов: от gRPC-имплементации
до Liquibase-миграций, Outbox, Resilience4j и наблюдаемости.

---

## Workflow

### Шаг 1. Прочитать STATE.md и архитектурный документ

```
1. Прочитать STATE.md — понять текущий статус.
2. Прочитать docs/architecture/02-backend-stack.md — все компоненты и правила.
3. Проверить: существует ли .proto контракт? Если нет — сначала contracts-proto-and-avro.
```

### Шаг 2. gRPC Service Implementation

#### 2.1 Структура Spring сервиса
```
server/<service-name>/
├── src/main/
│   ├── kotlin/com/budgetntr/<service>/
│   │   ├── grpc/
│   │   │   └── <Service>GrpcImpl.kt       # @GrpcService implements generated stub
│   │   ├── service/
│   │   │   └── <Service>Service.kt        # бизнес-логика
│   │   ├── domain/
│   │   │   └── <Entity>.kt                # JPA Entity
│   │   ├── repository/
│   │   │   └── <Entity>JpaRepository.kt   # Spring Data JPA
│   │   ├── outbox/
│   │   │   ├── OutboxMessage.kt
│   │   │   ├── OutboxMessageRepository.kt
│   │   │   └── OutboxProcessor.kt
│   │   └── config/
│   │       ├── ResilienceConfig.kt
│   │       └── ObservabilityConfig.kt
│   └── resources/
│       ├── application.yml
│       └── db/changelog/
│           ├── db.changelog-master.xml
│           ├── 001-<entity>-schema.xml
│           ├── 002-outbox.xml
│           └── 003-indexes.xml
```

#### 2.2 gRPC Implementation
```kotlin
@GrpcService
class DiaryGrpcImpl(
    private val diaryService: DiaryService,
) : DiaryServiceGrpcKt.DiaryServiceCoroutineImplBase() {

    override suspend fun addEntry(request: AddEntryRequest): AddEntryResponse {
        // Идемпотентность: ключ берётся из gRPC metadata через ServerInterceptor
        return diaryService.addEntry(request.toServiceRequest())
            .toGrpcResponse()
    }

    override suspend fun getDailyEntries(request: GetDailyEntriesRequest): GetDailyEntriesResponse {
        return diaryService.getDailyEntries(request.date)
            .toGrpcResponse()
    }
}
```

#### 2.3 Service Layer — ЗАПРЕТ сетевых вызовов в @Transactional
```kotlin
@Service
class DiaryService(
    private val diaryRepository: DiaryJpaRepository,
    private val outboxRepository: OutboxMessageRepository,
    private val idempotencyStore: IdempotencyStore,
) {
    // @Transactional: только DB-операции, никаких Kafka/gRPC/HTTP вызовов!
    @Transactional
    fun addEntry(request: AddEntryServiceRequest): DiaryEntryResult {
        // 1. Проверить idempotency-key (через IdempotencyServerInterceptor до этого)
        // 2. Сохранить бизнес-запись
        val entry = DiaryEntry(
            id = UUID.randomUUID(),
            userId = request.userId,
            foodItemId = request.foodItemId,
            quantityGrams = request.quantityGrams,
            recordedAt = request.recordedAt,
        )
        diaryRepository.save(entry)

        // 3. Записать в Outbox (в ТОЙ ЖЕ транзакции)
        outboxRepository.save(
            OutboxMessage(
                id = UUID.randomUUID(),
                aggregateType = "DiaryEntry",
                aggregateId = entry.id.toString(),
                topic = "diary.entry.created",
                payload = entry.toAvroPayload(),
                status = "PENDING",
            )
        )

        return entry.toResult()
    }
    // Kafka НЕ вызывается здесь — только в OutboxProcessor!
}
```

### Шаг 3. Transactional Outbox

#### 3.1 OutboxMessage Entity
```kotlin
@Entity(tableName = "outbox_messages")
data class OutboxMessage(
    @Id val id: UUID,
    val aggregateType: String,
    val aggregateId: String,
    val topic: String,

    @Column(columnDefinition = "bytea")
    val payload: ByteArray,

    val status: String = "PENDING",   // PENDING | PUBLISHED | FAILED
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val publishedAt: OffsetDateTime? = null,
    val attempts: Int = 0,
)
```

#### 3.2 OutboxProcessor — вне @Transactional
```kotlin
@Component
class OutboxProcessor(
    private val outboxRepository: OutboxMessageRepository,
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // Намеренно НЕ @Transactional — publish и DB-update разделены
    @Scheduled(fixedDelay = 500)
    fun processOutbox() {
        val messages = outboxRepository.findPendingWithLock(limit = 100)
        messages.forEach { message ->
            runCatching {
                kafkaTemplate.send(message.topic, message.aggregateId, message.payload).get()
                outboxRepository.markPublished(message.id)
            }.onFailure { ex ->
                logger.error("Failed to publish outbox message ${message.id}", ex)
                outboxRepository.incrementAttempts(message.id)
            }
        }
    }
}
```

#### 3.3 OutboxMessageRepository
```kotlin
interface OutboxMessageRepository : JpaRepository<OutboxMessage, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT m FROM OutboxMessage m
        WHERE m.status = 'PENDING'
        ORDER BY m.createdAt ASC
        LIMIT :limit
    """)
    fun findPendingWithLock(@Param("limit") limit: Int): List<OutboxMessage>

    @Modifying
    @Query("UPDATE OutboxMessage m SET m.status = 'PUBLISHED', m.publishedAt = NOW() WHERE m.id = :id")
    fun markPublished(@Param("id") id: UUID): Int

    @Modifying
    @Query("UPDATE OutboxMessage m SET m.attempts = m.attempts + 1 WHERE m.id = :id")
    fun incrementAttempts(@Param("id") id: UUID): Int
}
```

### Шаг 4. JPA — правила

```kotlin
@Entity
@Table(name = "diary_entries")
@NamedEntityGraph(
    name = "DiaryEntry.withFoodItem",
    attributeNodes = [NamedAttributeNode("foodItem")],
)
data class DiaryEntry(
    @Id val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY)     // LAZY — ОБЯЗАТЕЛЬНО
    @JoinColumn(name = "food_item_id")
    val foodItem: FoodItem,                // N+1 решается через @EntityGraph

    val quantityGrams: Double,
    val recordedAt: OffsetDateTime,
    val userId: String,
)

// Репозиторий — JOIN FETCH для конкретных запросов
interface DiaryJpaRepository : JpaRepository<DiaryEntry, UUID> {
    @Query("SELECT d FROM DiaryEntry d JOIN FETCH d.foodItem WHERE d.userId = :userId AND d.recordedAt >= :from")
    fun findWithFoodItemsByUserAndDate(
        @Param("userId") userId: String,
        @Param("from") from: OffsetDateTime,
    ): List<DiaryEntry>
}
```

**Запрещено:**
- `fetch = FetchType.EAGER` — никогда.
- Lazy-коллекции без `@EntityGraph` или `JOIN FETCH` в Query.

### Шаг 5. Resilience4j

#### 5.1 Конфигурация application.yml
```yaml
resilience4j:
  circuitbreaker:
    instances:
      llm-api:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        slow-call-duration-threshold: 5s
        slow-call-rate-threshold: 80
  retry:
    instances:
      idempotent-calls:    # ТОЛЬКО для идемпотентных операций!
        max-attempts: 3
        wait-duration: 500ms
  timelimiter:
    instances:
      llm-api:
        timeout-duration: 10s
```

#### 5.2 Применение аннотаций
```kotlin
@Service
class RecipeAiService(
    private val llmClient: LlmApiClient,
    private val recipeCache: RecipeCacheRepository,
) {
    // CircuitBreaker с fallback на Redis-кэш
    @CircuitBreaker(name = "llm-api", fallbackMethod = "getCachedRecipes")
    @TimeLimiter(name = "llm-api")
    suspend fun generateRecipes(profile: UserProfile): List<Recipe> =
        llmClient.generate(profile)

    // Fallback — никогда не бросает исключение
    @Suppress("UnusedPrivateMember")
    private suspend fun getCachedRecipes(profile: UserProfile, ex: Throwable): List<Recipe> {
        logger.warn("LLM circuit open, using cache", ex)
        return recipeCache.getPopular(profile.dietaryPreferences)
    }

    // @Retry ТОЛЬКО для идемпотентного GET
    @Retry(name = "idempotent-calls")
    suspend fun getRecipeById(id: RecipeId): Recipe? =
        recipeCache.get(id) ?: llmClient.fetchById(id)
}
```

**Запрещено:** `@Retry` на не-идемпотентные мутации без `Idempotency-Key`.

### Шаг 6. Liquibase Migrations

#### 6.1 Создать новый changeset для таблицы
```xml
<!-- db/changelog/001-diary-schema.xml -->
<changeSet id="001-diary-entries" author="team">
  <createTable tableName="diary_entries">
    <column name="id" type="uuid"><constraints primaryKey="true" nullable="false"/></column>
    <column name="user_id" type="varchar(255)"><constraints nullable="false"/></column>
    <column name="food_item_id" type="uuid"><constraints nullable="false"/></column>
    <column name="quantity_grams" type="double precision"><constraints nullable="false"/></column>
    <column name="recorded_at" type="timestamptz"><constraints nullable="false"/></column>
    <column name="created_at" type="timestamptz" defaultValueComputed="NOW()"/>
  </createTable>
</changeSet>
```

#### 6.2 Outbox changeset
```xml
<!-- db/changelog/002-outbox.xml -->
<changeSet id="002-outbox" author="team">
  <createTable tableName="outbox_messages">
    <column name="id" type="uuid"><constraints primaryKey="true"/></column>
    <column name="aggregate_type" type="varchar(100)"><constraints nullable="false"/></column>
    <column name="aggregate_id" type="varchar(100)"><constraints nullable="false"/></column>
    <column name="topic" type="varchar(255)"><constraints nullable="false"/></column>
    <column name="payload" type="bytea"><constraints nullable="false"/></column>
    <column name="status" type="varchar(20)" defaultValue="PENDING"/>
    <column name="created_at" type="timestamptz" defaultValueComputed="NOW()"/>
    <column name="published_at" type="timestamptz"/>
    <column name="attempts" type="int" defaultValueNumeric="0"/>
  </createTable>
  <createIndex tableName="outbox_messages" indexName="idx_outbox_pending">
    <column name="status"/><column name="created_at"/>
  </createIndex>
</changeSet>
```

#### 6.3 Indexes changeset — обязательны для БЖУ/цены/категории
```xml
<!-- db/changelog/003-indexes.xml -->
<changeSet id="003-performance-indexes" author="team">
  <createIndex tableName="food_items" indexName="idx_food_nutrition">
    <column name="proteins_per_100g"/>
    <column name="fats_per_100g"/>
    <column name="carbs_per_100g"/>
  </createIndex>
  <createIndex tableName="food_items" indexName="idx_food_price_category">
    <column name="price_per_100g"/>
    <column name="category_id"/>
  </createIndex>
</changeSet>
```

### Шаг 7. Observability — JSON-логи + OTel

#### 7.1 logback-spring.xml (JSON формат)
```xml
<!-- resources/logback-spring.xml -->
<configuration>
  <appender name="JSON_STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeMdcKeyName>traceId</includeMdcKeyName>
      <includeMdcKeyName>spanId</includeMdcKeyName>
      <includeMdcKeyName>userId</includeMdcKeyName>
      <customFields>{"service":"${spring.application.name}"}</customFields>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="JSON_STDOUT"/>
  </root>
</configuration>
```

#### 7.2 application.yml — actuator + OTel
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true        # /actuator/health/liveness и /readiness
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

### Шаг 8. Идемпотентность — Server Interceptor

```kotlin
@Component
class IdempotencyServerInterceptor(
    private val idempotencyStore: RedisIdempotencyStore,
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
        return next.startCall(IdempotencyCapturingCall(call, key, idempotencyStore), headers)
    }

    companion object {
        val IDEMPOTENCY_KEY_HEADER: Metadata.Key<String> =
            Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
    }
}
```

### Шаг 9. Тесты — Testcontainers (НИКАКОГО H2)

```bash
./gradlew :server:test                    # Запуск с Testcontainers
```

Шаблон теста — см. `.agents/skills/testing-and-verification/SKILL.md §2.5`.

### Шаг 10. Обновить STATE.md

---

## Чеклист перед коммитом

```
[ ] Нет сетевых вызовов (Kafka/gRPC/HTTP) внутри @Transactional методов
[ ] Kafka публикуется только через OutboxProcessor (вне @Transactional)
[ ] Все мутирующие RPC проверяют Idempotency-Key через ServerInterceptor
[ ] JPA: все @OneToMany/@ManyToOne — FetchType.LAZY (нет EAGER)
[ ] JPA: N+1 решён через @EntityGraph или JOIN FETCH в конкретных Query
[ ] Liquibase: новый changeset для каждой новой таблицы/изменения схемы
[ ] Liquibase: индексы для новых полей фильтрации
[ ] @CircuitBreaker с fallbackMethod для всех внешних вызовов
[ ] @Retry только на идемпотентные операции
[ ] /actuator/health/readiness и /liveness настроены в application.yml
[ ] JSON-логи с traceId, spanId, userId (через MDC)
[ ] traceparent прокидывается в Kafka через ProducerInterceptor
[ ] Секреты не в application.yml (Vault / K8s Secrets)
[ ] Тесты — Testcontainers (PostgreSQL + Redis + Kafka), нет H2
[ ] ./gradlew :server:test — GREEN
```
