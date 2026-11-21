package dev.andre.nutritionist.recipe

import com.budgetntr.proto.recipe.v1.WeekMenu
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.kotlin.timelimiter.executeSuspendFunction
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiter
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class ResilientMenuGenerator(
    private val client: LlmClient,
    private val validator: MenuValidator,
    private val json: JsonMapper,
    breakers: CircuitBreakerRegistry,
    retries: RetryRegistry,
    private val timeout: TimeLimiter,
) {
    private val breaker = breakers.circuitBreaker("llm")
    private val retry = retries.retry("llm")

    suspend fun generate(
        request: MenuRequest,
        products: List<CatalogProduct>,
        traceparent: String?,
        reserveAttempt: suspend () -> Unit,
    ): WeekMenu {
        val prompt = prompt(request, products)
        return retry.executeSuspendFunction {
            breaker.executeSuspendFunction {
                reserveAttempt()
                timeout.executeSuspendFunction {
                    validator.validate(client.generate(prompt, traceparent), request, products)
                }
            }
        }
    }

    private fun prompt(
        request: MenuRequest,
        products: List<CatalogProduct>,
    ): String {
        val candidates =
            products.map { product ->
                mapOf(
                    "id" to product.id.raw,
                    "name" to product.name,
                    "kcalPer100g" to product.caloriesPer100g,
                    "priceMinorPer100g" to product.pricePer100gMinor,
                    "preferred" to (product.id in request.preferred),
                )
            }
        return """
            Составь меню из 7 дней (day от 1 до 7). На каждый день нужны breakfast, lunch и dinner.
            Дай каждому приёму пищи краткое название на русском. Для каждого из 1–8 продуктов укажи id и целые граммы (1–2000).
            Используй только продукты ниже, без повторов продукта в одном приёме пищи. Отдавай предпочтение preferred.
            Цель на каждый день: ${request.dailyCalories} ккал. Стоимость за 7 дней не выше ${request.weeklyBudgetMinor} копеек.
            Считай пропорционально граммам по данным на 100 г. Верни JSON по заданной схеме.
            Каталог продуктов: ${json.writeValueAsString(candidates)}
            """.trimIndent()
    }
}
