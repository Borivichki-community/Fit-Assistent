/**
 * Вариант 2: Чистое ядро Resilience4j + resilience4j-kotlin (Рекомендованный выбор в ADR-002).
 *
 * Демонстрация полностью неблокирующего, типобезопасного и идиоматичного подхода для Kotlin Coroutines.
 * Отсутствуют AOP-прокси, сохраняется чистота Domain-слоя, гарантирована защита от thread starvation
 * и безопасность при отмене корутин (CancellationException).
 */

package dev.andre.nutritionist.recipe.infrastructure.resilience.approach2

import dev.andre.nutritionist.recipe.domain.model.Recipe
import dev.andre.nutritionist.recipe.domain.model.RecipeId
import dev.andre.nutritionist.recipe.domain.model.UserProfile
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspend
import io.github.resilience4j.kotlin.retry.executeSuspend
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.TimeUnit

// ─── 1. Конфигурация реестров Resilience4j (Spring Bean без AOP) ─────────────

@Configuration(proxyBeanMethods = false)
class ResilienceCoreConfiguration {

    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry {
        val defaultConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slowCallDurationThreshold(Duration.ofSeconds(5))
            .slowCallRateThreshold(80.0f)
            .permittedNumberOfCallsInHalfOpenState(3)
            // Игнорируем отмену корутин на уровне конфигурации ядра для абсолютной защиты
            .ignoreExceptions(CancellationException::class.java)
            .build()

        return CircuitBreakerRegistry.of(defaultConfig)
    }

    @Bean
    fun retryRegistry(): RetryRegistry {
        val defaultConfig = RetryConfig.custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .ignoreExceptions(CancellationException::class.java)
            .build()

        return RetryRegistry.of(defaultConfig)
    }
}

// ─── 2. Идиоматичные безопасные Extension-функции для Coroutines ─────────────

/**
 * Расширение для безопасного выполнения suspend-блока внутри CircuitBreaker.
 * Гарантии:
 * 1. Не перехватывает и не регистрирует как сбой CancellationException (Structured Concurrency);
 * 2. Точно замеряет длительность выполнения реальной корутины;
 * 3. Не блокирует ни одного потока (Zero Thread Starvation).
 */
suspend inline fun <T> CircuitBreaker.withCircuitBreaker(
    crossinline block: suspend () -> T,
): T {
    if (!tryAcquirePermission()) {
        acquirePermission() // Бросает CallNotPermittedException при открытом circuit
    }
    val startNanos = System.nanoTime()
    return try {
        val result = block()
        val durationNanos = System.nanoTime() - startNanos
        onSuccess(durationNanos, TimeUnit.NANOSECONDS)
        result
    } catch (cancellation: CancellationException) {
        // КРИТИЧЕСКИ ВАЖНО: Не сообщать в CircuitBreaker об ошибке при отмене клиентом!
        // Пробрасываем немедленно без вызова onError.
        throw cancellation
    } catch (throwable: Throwable) {
        val durationNanos = System.nanoTime() - startNanos
        onError(durationNanos, TimeUnit.NANOSECONDS, throwable)
        throw throwable
    }
}

/**
 * Расширение для безопасного повторения идемпотентных suspend-операций.
 */
suspend inline fun <T> Retry.withRetry(
    crossinline block: suspend () -> T,
): T {
    return this.executeSuspend {
        block()
    }
}

// ─── 3. Чистый инфраструктурный Gateway / Адаптер (Clean Architecture) ───────

interface ExternalLlmGateway {
    suspend fun generateBudgetRecipes(profile: UserProfile): List<Recipe>
    suspend fun fetchRecipeById(id: RecipeId): Recipe
}

interface RecipeCacheGateway {
    suspend fun getPopular(preferences: List<String>): List<Recipe>
    suspend fun get(id: RecipeId): Recipe?
}

/**
 * Инфраструктурная реализация клиента, оборачивающая вызовы к внешнему сервису.
 * Бизнес-логика (Domain/UseCase) вызывает этот интерфейс напрямую, не зная о существовании Resilience4j.
 */
class ResilientRecipeLlmGateway(
    private val rawClient: ExternalLlmGateway,
    private val cache: RecipeCacheGateway,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    retryRegistry: RetryRegistry,
    private val ioDispatcher: CoroutineDispatcher,
) : ExternalLlmGateway {

    private val logger = LoggerFactory.getLogger(ResilientRecipeLlmGateway::class.java)
    private val llmCircuitBreaker = circuitBreakerRegistry.circuitBreaker("llm-recipe-service")
    private val idempotentRetry = retryRegistry.retry("idempotent-recipe-fetch")

    /**
     * Вызов генерации рецептов с Circuit Breaker и fallback на локальный кэш.
     */
    override suspend fun generateBudgetRecipes(profile: UserProfile): List<Recipe> =
        withContext(ioDispatcher) {
            try {
                llmCircuitBreaker.withCircuitBreaker {
                    rawClient.generateBudgetRecipes(profile)
                }
            } catch (cancellation: CancellationException) {
                // Корректная поддержка отмены корутины
                throw cancellation
            } catch (callNotPermitted: CallNotPermittedException) {
                logger.warn("CircuitBreaker OPEN для LLM сервиса. Немедленный откат на кэш.")
                cache.getPopular(profile.dietaryPreferences)
            } catch (ex: Throwable) {
                logger.error("Сбой вызова LLM API: ${ex.message}. Переход на резервный кэш.", ex)
                cache.getPopular(profile.dietaryPreferences)
            }
        }

    /**
     * Идемпотентный GET-вызов с Retry + Circuit Breaker.
     * Retry применяется ТОЛЬКО к идемпотентному чтению!
     */
    override suspend fun fetchRecipeById(id: RecipeId): Recipe =
        withContext(ioDispatcher) {
            val cached = cache.get(id)
            if (cached != null) {
                return@withContext cached
            }

            // Повторные попытки идемпотентного вызова
            idempotentRetry.withRetry {
                llmCircuitBreaker.withCircuitBreaker {
                    rawClient.fetchRecipeById(id)
                }
            }
        }
}
