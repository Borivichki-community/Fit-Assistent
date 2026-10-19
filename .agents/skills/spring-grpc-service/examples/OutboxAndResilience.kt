/**
 * Эталонный пример: Transactional Outbox + @CircuitBreaker для LLM.
 *
 * ПРАВИЛА (из AGENTS.md):
 * - Kafka НИКОГДА не вызывается внутри @Transactional метода
 * - Запись в outbox_messages происходит В ТОЙ ЖЕ транзакции, что и бизнес-запись
 * - OutboxProcessor работает вне @Transactional
 * - @CircuitBreaker с fallbackMethod для всех внешних вызовов (LLM)
 * - @Retry только для идемпотентных операций
 * - FetchType.LAZY — обязательно для всех JPA связей
 */

package com.budgetntr.diaryservice

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import jakarta.persistence.*
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

// ─── Domain Entity ────────────────────────────────────────────────────────────

@Entity
@Table(name = "diary_entries")
@NamedEntityGraph(
    name = "DiaryEntry.withFoodItem",
    attributeNodes = [NamedAttributeNode("foodItem")],
)
data class DiaryEntry(
    @Id val id: UUID,
    val userId: String,

    @ManyToOne(fetch = FetchType.LAZY)      // LAZY — обязательно, НИКАКОГО EAGER
    @JoinColumn(name = "food_item_id")
    val foodItem: FoodItem,                  // N+1 решается через @EntityGraph ниже

    val quantityGrams: Double,
    val recordedAt: OffsetDateTime,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "food_items")
data class FoodItem(
    @Id val id: UUID,
    val name: String,
    val caloriesPer100g: Double,
    val proteinsPer100g: Double,
    val fatsPer100g: Double,
    val carbsPer100g: Double,
    val pricePer100g: java.math.BigDecimal,
)

// ─── Outbox Entity ────────────────────────────────────────────────────────────

@Entity
@Table(name = "outbox_messages")
data class OutboxMessage(
    @Id val id: UUID = UUID.randomUUID(),
    val aggregateType: String,
    val aggregateId: String,
    val topic: String,

    @Column(columnDefinition = "bytea")
    val payload: ByteArray,

    val status: String = "PENDING",          // PENDING | PUBLISHED | FAILED
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val publishedAt: OffsetDateTime? = null,
    val attempts: Int = 0,
) {
    // ByteArray требует явного equals/hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OutboxMessage) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}

// ─── Repositories ─────────────────────────────────────────────────────────────

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DiaryJpaRepository : JpaRepository<DiaryEntry, UUID> {
    // JOIN FETCH для конкретного сценария — избегает N+1
    @Query("""
        SELECT d FROM DiaryEntry d 
        JOIN FETCH d.foodItem 
        WHERE d.userId = :userId AND d.recordedAt >= :from 
        ORDER BY d.recordedAt DESC
    """)
    fun findWithFoodItemByUserAndDate(
        @Param("userId") userId: String,
        @Param("from") from: OffsetDateTime,
    ): List<DiaryEntry>
}

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

    @Modifying
    @Query("UPDATE OutboxMessage m SET m.status = 'FAILED' WHERE m.id = :id")
    fun markFailed(@Param("id") id: UUID): Int
}

// ─── Service — Transactional Outbox Pattern ───────────────────────────────────

data class AddEntryServiceRequest(
    val userId: String,
    val foodItemId: UUID,
    val quantityGrams: Double,
    val recordedAt: OffsetDateTime,
    val idempotencyKey: String,
)

data class DiaryEntryResult(
    val entryId: UUID,
    val caloriesLabel: String,
    val priceLabel: String,
)

@Service
class DiaryService(
    private val diaryRepository: DiaryJpaRepository,
    private val foodItemRepository: FoodItemJpaRepository,
    private val outboxRepository: OutboxMessageRepository,
    private val avroSerializer: DiaryEntryAvroSerializer,
) {
    private val logger = LoggerFactory.getLogger(DiaryService::class.java)

    /**
     * Сохраняет запись дневника И записывает в Outbox — в ОДНОЙ транзакции.
     *
     * КРИТИЧЕСКИ ВАЖНО: Kafka НЕ вызывается здесь.
     * Публикация происходит в OutboxProcessor вне @Transactional.
     */
    @Transactional
    fun addEntry(request: AddEntryServiceRequest): DiaryEntryResult {
        // 1. Загрузить связанный объект (JOIN FETCH избегает N+1)
        val foodItem = foodItemRepository.findById(request.foodItemId)
            .orElseThrow { IllegalArgumentException("FoodItem not found: ${request.foodItemId}") }

        // 2. Создать и сохранить бизнес-сущность
        val entry = DiaryEntry(
            id = UUID.randomUUID(),
            userId = request.userId,
            foodItem = foodItem,
            quantityGrams = request.quantityGrams,
            recordedAt = request.recordedAt,
        )
        diaryRepository.save(entry)

        // 3. Записать в Outbox (В ТОЙ ЖЕ транзакции!)
        // Если транзакция откатится — Outbox тоже откатится. Консистентность гарантирована.
        val avroPayload = avroSerializer.serialize(entry, request.idempotencyKey)
        outboxRepository.save(
            OutboxMessage(
                aggregateType = "DiaryEntry",
                aggregateId = entry.id.toString(),
                topic = "diary.entry.created",
                payload = avroPayload,
            )
        )

        logger.info("DiaryEntry ${entry.id} saved with outbox message. userId=${request.userId}")

        // 4. Вернуть результат (Kafka ещё не вызвана!)
        return DiaryEntryResult(
            entryId = entry.id,
            caloriesLabel = formatCalories(foodItem.caloriesPer100g, request.quantityGrams),
            priceLabel = formatPrice(foodItem.pricePer100g, request.quantityGrams),
        )
    }

    // Форматирование строк для ответа gRPC — здесь, не в Kotlin/commonMain UI
    private fun formatCalories(caloriesPer100g: Double, quantity: Double): String {
        val total = (caloriesPer100g * quantity / 100).toLong()
        return "$total ккал"
    }

    private fun formatPrice(pricePer100g: java.math.BigDecimal, quantity: Double): String {
        val total = pricePer100g.multiply(java.math.BigDecimal(quantity / 100))
            .setScale(0, java.math.RoundingMode.HALF_UP)
        return "$total ₽"
    }
}

// ─── Outbox Processor — работает ВНЕ @Transactional ─────────────────────────

@Component
class OutboxProcessor(
    private val outboxRepository: OutboxMessageRepository,
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
) {
    private val logger = LoggerFactory.getLogger(OutboxProcessor::class.java)

    /**
     * Читает PENDING-сообщения из Outbox и публикует в Kafka.
     *
     * Намеренно НЕ помечен @Transactional:
     * - publish в Kafka и UPDATE статуса — разные операции
     * - При падении Kafka запись останется PENDING и будет повторно обработана
     */
    @Scheduled(fixedDelay = 500)
    fun processOutbox() {
        val messages = outboxRepository.findPendingWithLock(limit = 100)
        if (messages.isEmpty()) return

        messages.forEach { message ->
            runCatching {
                // .get() — синхронное ожидание подтверждения от Kafka (важно для at-least-once)
                kafkaTemplate.send(message.topic, message.aggregateId, message.payload).get()
                outboxRepository.markPublished(message.id)
                logger.debug("Published outbox message ${message.id} to ${message.topic}")
            }.onFailure { ex ->
                logger.error("Failed to publish outbox message ${message.id}: ${ex.message}")
                outboxRepository.incrementAttempts(message.id)
                if (message.attempts >= MAX_ATTEMPTS) {
                    outboxRepository.markFailed(message.id)
                    logger.error("Outbox message ${message.id} marked as FAILED after $MAX_ATTEMPTS attempts")
                }
            }
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 10
    }
}

// ─── Recipe AI Service — @CircuitBreaker + @Retry ─────────────────────────────

interface LlmApiClient {
    suspend fun generateBudgetRecipes(profile: UserProfile): List<RecipeResult>
    suspend fun fetchRecipeById(id: String): RecipeResult?
}

interface RecipeCacheRepository {
    fun getPopularRecipes(preferences: List<String>): List<RecipeResult>
    fun get(id: String): RecipeResult?
    fun put(id: String, recipe: RecipeResult)
}

data class UserProfile(val userId: String, val dietaryPreferences: List<String>, val budget: Double)
data class RecipeResult(val id: String, val name: String, val caloriesLabel: String, val priceLabel: String)

@Service
class RecipeAiService(
    private val llmClient: LlmApiClient,
    private val recipeCache: RecipeCacheRepository,
) {
    private val logger = LoggerFactory.getLogger(RecipeAiService::class.java)

    /**
     * Генерирует рецепты через LLM.
     *
     * @CircuitBreaker — если LLM недоступен (>50% ошибок или timeout):
     *   - circuit открывается на 30 секунд
     *   - вызывается fallback — возвращает кэшированные рецепты
     *
     * @TimeLimiter — если LLM отвечает >10 секунд → timeout → fallback
     */
    @CircuitBreaker(name = "llm-api", fallbackMethod = "getCachedRecipes")
    @TimeLimiter(name = "llm-api")
    suspend fun generateRecipes(profile: UserProfile): List<RecipeResult> =
        llmClient.generateBudgetRecipes(profile)

    /**
     * Fallback — вызывается автоматически при открытом circuit или timeout.
     * НИКОГДА не бросает исключение — возвращает деградированный результат.
     */
    @Suppress("UnusedPrivateMember")  // вызывается через Resilience4j reflection
    private suspend fun getCachedRecipes(profile: UserProfile, ex: Throwable): List<RecipeResult> {
        logger.warn("LLM circuit open or timeout (${ex.javaClass.simpleName}), using cache for user ${profile.userId}")
        return recipeCache.getPopularRecipes(profile.dietaryPreferences)
    }

    /**
     * Получение рецепта по ID — идемпотентный GET.
     *
     * @Retry — допустим ТОЛЬКО для идемпотентных операций!
     * Здесь: GET по ID — безопасно повторять.
     *
     * ЗАПРЕЩЕНО применять @Retry к мутациям без Idempotency-Key.
     */
    @Retry(name = "idempotent-calls")
    suspend fun getRecipeById(id: String): RecipeResult? {
        // Сначала кэш — потом LLM
        return recipeCache.get(id) ?: llmClient.fetchRecipeById(id)?.also {
            recipeCache.put(id, it)
        }
    }
}

// ─── gRPC Server Interceptor для Idempotency-Key ──────────────────────────────

import io.grpc.*
import org.springframework.stereotype.Component as GrpcComponent

/**
 * Interceptor проверяет idempotency-key ПЕРЕД вызовом сервиса.
 * Если ключ уже есть в Redis — возвращает закэшированный ответ без вызова Service.
 */
@GrpcComponent
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
            @Suppress("UNCHECKED_CAST")
            val cached = idempotencyStore.get(key) as? Resp
            if (cached != null) {
                call.sendMessage(cached)
                call.close(Status.OK, Metadata())
                // Возвращаем no-op listener — сервис не вызывается
                return object : ServerCall.Listener<Req>() {}
            }
        }

        return IdempotencyCapturingListener(call, key, next, idempotencyStore, headers)
    }

    companion object {
        val IDEMPOTENCY_KEY_HEADER: Metadata.Key<String> =
            Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
    }
}

/**
 * Listener, который перехватывает ответ и сохраняет его в Redis по idempotency-key.
 */
private class IdempotencyCapturingListener<Req, Resp>(
    private val call: ServerCall<Req, Resp>,
    private val idempotencyKey: String?,
    private val next: ServerCallHandler<Req, Resp>,
    private val store: RedisIdempotencyStore,
    private val headers: Metadata,
) : ForwardingServerCallListener.SimpleForwardingServerCallListener<Req>(
    next.startCall(
        object : ForwardingServerCall.SimpleForwardingServerCall<Req, Resp>(call) {
            override fun sendMessage(message: Resp) {
                if (idempotencyKey != null) {
                    store.put(idempotencyKey, message as Any, ttlSeconds = 86400) // 24 часа
                }
                super.sendMessage(message)
            }
        },
        headers,
    )
)

// ─── Redis Idempotency Store (интерфейс) ──────────────────────────────────────

interface RedisIdempotencyStore {
    fun get(key: String): Any?
    fun put(key: String, value: Any, ttlSeconds: Long)
}

// ─── Вспомогательные типы (заглушки для компиляции примера) ───────────────────

interface FoodItemJpaRepository : JpaRepository<FoodItem, UUID>
interface DiaryEntryAvroSerializer {
    fun serialize(entry: DiaryEntry, idempotencyKey: String): ByteArray
}
