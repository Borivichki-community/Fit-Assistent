/**
 * Вариант 1: Официальный стартер Resilience4j (AOP аннотации).
 *
 * Демонстрация использования @CircuitBreaker и @Retry со Spring Boot стартером.
 * Показывает особенности и скрытые проблемы работы AOP-прокси с Kotlin suspend функциями.
 */

package dev.andre.nutritionist.recipe.infrastructure.resilience.approach1

import dev.andre.nutritionist.recipe.domain.model.Recipe
import dev.andre.nutritionist.recipe.domain.model.RecipeId
import dev.andre.nutritionist.recipe.domain.model.UserProfile
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

/**
 * Инфраструктурный клиент к удаленному AI-сервису генерации рецептов.
 */
interface RemoteRecipeLlmClient {
    suspend fun generateBudgetRecipes(profile: UserProfile): List<Recipe>
    suspend fun fetchRecipeById(id: RecipeId): Recipe
}

interface LocalRecipeCache {
    suspend fun getPopular(preferences: List<String>): List<Recipe>
    suspend fun get(id: RecipeId): Recipe?
}

/**
 * Вариант 1: Сервис с AOP-аннотациями @CircuitBreaker и @Retry.
 *
 * ПРОБЛЕМАТИКА СО СТЕКОМ KOTLIN COROUTINES:
 * 1. AOP Proxy: При вызове suspend-функции компилятор Kotlin добавляет Continuation<T>.
 *    Java AOP-аспект (CircuitBreakerAspect) видит COROUTINE_SUSPENDED при первой приостановке корутины
 *    и может преждевременно зарегистрировать успешный вызов.
 * 2. Потеря исключений: Если исключение возникло после возобновления корутины на другом потоке,
 *    стандартный AOP try/catch его не перехватит.
 * 3. Ложное срабатывание при отмене: Если вызывающая корутина отменена клиентом (gRPC client disconnect),
 *    выбрасывается CancellationException. AOP-аспект перехватывает Throwable и ошибочно инкрементирует
 *    счетчик сбоев в Circuit Breaker, если тип исключения явно не настроен в ignoredExceptions!
 * 4. Нарушение Clean Architecture: Инфраструктурные аннотации resilience4j размазываются по бизнес-логике.
 */
@Service
class RecipeAiStarterService(
    private val llmClient: RemoteRecipeLlmClient,
    private val recipeCache: LocalRecipeCache,
) {
    private val logger = LoggerFactory.getLogger(RecipeAiStarterService::class.java)

    /**
     * Вызов внешнего сервиса с AOP Circuit Breaker.
     * В конфигурации application.yml ОБЯЗАТЕЛЬНО указывать:
     *   ignore-exceptions:
     *     - kotlinx.coroutines.CancellationException
     */
    @CircuitBreaker(name = "llm-recipe-service", fallbackMethod = "fallbackGenerateRecipes")
    suspend fun generateRecipes(profile: UserProfile): List<Recipe> {
        return llmClient.generateBudgetRecipes(profile)
    }

    /**
     * Fallback метод для Circuit Breaker.
     * ПРИМЕЧАНИЕ: Сигнатура должна в точности повторять метод + Throwable в конце.
     * Для suspend fun требуется передавать точно те же параметры корутины.
     */
    @Suppress("UnusedPrivateMember")
    private suspend fun fallbackGenerateRecipes(profile: UserProfile, ex: Throwable): List<Recipe> {
        // Защита от подавления отмены: если это отмена корутины, fallback НЕ должен маскировать её
        if (ex is CancellationException) {
            throw ex
        }
        logger.warn("LLM API недоступен (${ex.javaClass.simpleName}: ${ex.message}), откат на кэш")
        return recipeCache.getPopular(profile.dietaryPreferences)
    }

    /**
     * Идемпотентный запрос по ID с аннотацией @Retry.
     * ВНИМАНИЕ: При использовании @Retry с suspend fun есть риск повторного вызова с исчерпанным
     * объектом Continuation, что может приводить к IllegalStateException: Already resumed.
     */
    @Retry(name = "idempotent-recipe-fetch", fallbackMethod = "fallbackGetRecipe")
    suspend fun getRecipeById(id: RecipeId): Recipe? {
        val cached = recipeCache.get(id)
        if (cached != null) {
            return cached
        }
        return llmClient.fetchRecipeById(id)
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun fallbackGetRecipe(id: RecipeId, ex: Throwable): Recipe? {
        if (ex is CancellationException) {
            throw ex
        }
        logger.warn("Не удалось получить рецепт $id после повторных попыток: ${ex.message}")
        return null
    }
}
