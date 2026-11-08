/**
 * Вариант 3: Абстракция Spring Cloud CircuitBreaker (ReactiveCircuitBreakerFactory).
 *
 * Демонстрация интеграции через абстракцию Spring Cloud CircuitBreaker с реактивным мостом.
 * Показывает необходимость конвертации Coroutines <-> Project Reactor Mono и возникающий оверхед.
 */

package dev.andre.nutritionist.recipe.infrastructure.resilience.approach3

import dev.andre.nutritionist.recipe.domain.model.Recipe
import dev.andre.nutritionist.recipe.domain.model.RecipeId
import dev.andre.nutritionist.recipe.domain.model.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

interface RawLlmClient {
    suspend fun generateBudgetRecipes(profile: UserProfile): List<Recipe>
    suspend fun fetchRecipeById(id: RecipeId): Recipe
}

interface FallbackRecipeCache {
    suspend fun getPopular(preferences: List<String>): List<Recipe>
    suspend fun get(id: RecipeId): Recipe?
}

/**
 * Реализация клиента на базе Spring Cloud ReactiveCircuitBreaker.
 *
 * ОСОБЕННОСТИ И НЕДОСТАТКИ:
 * 1. Необходимость конвертации: каждый suspend-вызов должен оборачиваться в Mono через `mono { ... }`,
 *    а затем распаковываться обратно через `.awaitSingle()`.
 * 2. Оверхед: порождается цепочка реактивных операторов (MonoJust, MonoFlatMap, Subscriber),
 *    что увеличивает нагрузку на GC и задержки в высоконагруженных gRPC-сервисах.
 * 3. Потеря контекста CoroutineContext: для проброса W3C traceparent (OpenTelemetry) и MDC
 *    требуется дополнительный перенос контекста корутины в Reactor ContextView и обратно.
 * 4. Зависимость от Spring Cloud BOM: навязывает синхронизацию релизов Spring Cloud
 *    с мажорной версией Spring Boot 4.1.
 */
@Component
class SpringCloudResilientLlmGateway(
    private val rawClient: RawLlmClient,
    private val cache: FallbackRecipeCache,
    circuitBreakerFactory: ReactiveCircuitBreakerFactory<*, *>,
) {
    private val logger = LoggerFactory.getLogger(SpringCloudResilientLlmGateway::class.java)

    private val circuitBreaker: ReactiveCircuitBreaker =
        circuitBreakerFactory.create("llm-recipe-service")

    /**
     * Вызов внешнего сервиса через ReactiveCircuitBreaker с мостом в Coroutines.
     */
    suspend fun generateBudgetRecipes(profile: UserProfile): List<Recipe> {
        // Шаг 1: Конвертируем suspend-вызов в Mono<List<Recipe>>
        val sourceMono: Mono<List<Recipe>> = mono {
            rawClient.generateBudgetRecipes(profile)
        }

        // Шаг 2: Оборачиваем через circuitBreaker.run с реактивным fallback
        val resilientMono: Mono<List<Recipe>> = circuitBreaker.run(sourceMono) { throwable ->
            if (throwable is CancellationException) {
                // Если корутина отменена, пробрасываем ошибку в реактивный поток
                return@run Mono.error(throwable)
            }
            logger.warn("Spring Cloud CB сработал: ${throwable.message}. Возврат из кэша.")
            mono {
                cache.getPopular(profile.dietaryPreferences)
            }
        }

        // Шаг 3: Асинхронно ожидаем завершения в вызывающей корутине
        return resilientMono.awaitSingle()
    }

    /**
     * Запрос по ID. Заметьте, что Spring Cloud CircuitBreaker не имеет встроенного
     * реактивного Retry-пайплайна уровня той же абстракции без подключения Reactor Retry.
     */
    suspend fun fetchRecipeById(id: RecipeId): Recipe {
        val sourceMono: Mono<Recipe> = mono {
            cache.get(id) ?: rawClient.fetchRecipeById(id)
        }

        return circuitBreaker.run(sourceMono) { throwable ->
            if (throwable is CancellationException) {
                Mono.error(throwable)
            } else {
                logger.error("Сбой чтения рецепта $id через Spring Cloud CB: ${throwable.message}")
                Mono.error(throwable)
            }
        }.awaitSingle()
    }
}
