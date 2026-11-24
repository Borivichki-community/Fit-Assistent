package dev.andre.nutritionist.recipe.domain.service

import com.budgetntr.proto.recipe.v1.WeekMenu
import dev.andre.nutritionist.recipe.domain.model.CatalogProduct
import dev.andre.nutritionist.recipe.domain.model.GeneratedMealPlan
import dev.andre.nutritionist.recipe.domain.model.MealPlanRequest
import dev.andre.nutritionist.recipe.domain.model.MenuRequest
import dev.andre.nutritionist.recipe.domain.validator.MenuValidator
import dev.andre.nutritionist.recipe.infrastructure.llm.LlmClient
import dev.andre.nutritionist.recipe.infrastructure.llm.prompt.MealPlanPromptBuilder
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.kotlin.timelimiter.executeSuspendFunction
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiter
import org.springframework.stereotype.Component

@Component
class ResilientMenuGenerator(
    private val client: LlmClient,
    private val validator: MenuValidator,
    private val promptBuilder: MealPlanPromptBuilder,
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
        val prompt = promptBuilder.buildWeekMenuPrompt(request, products)
        return retry.executeSuspendFunction {
            breaker.executeSuspendFunction {
                reserveAttempt()
                timeout.executeSuspendFunction {
                    validator.validate(client.generate(prompt, traceparent), request, products)
                }
            }
        }
    }

    suspend fun generateMealPlan(
        request: MealPlanRequest,
        products: List<CatalogProduct>,
        traceparent: String?,
        reserveAttempt: suspend () -> Unit,
    ): GeneratedMealPlan {
        val prompt = promptBuilder.buildMealPlanPrompt(request, products)
        return retry.executeSuspendFunction {
            breaker.executeSuspendFunction {
                reserveAttempt()
                timeout.executeSuspendFunction {
                    validator.validateMealPlan(client.generate(prompt, traceparent), request, products)
                }
            }
        }
    }
}
