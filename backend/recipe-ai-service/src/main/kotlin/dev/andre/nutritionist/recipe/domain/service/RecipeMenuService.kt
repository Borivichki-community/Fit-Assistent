package dev.andre.nutritionist.recipe.domain.service

import com.budgetntr.proto.recipe.v1.MealType
import com.budgetntr.proto.recipe.v1.MenuSource
import com.budgetntr.proto.recipe.v1.WeekMenu
import dev.andre.nutritionist.recipe.config.RecipeProperties
import dev.andre.nutritionist.recipe.domain.model.CURRENCY
import dev.andre.nutritionist.recipe.domain.model.CatalogProduct
import dev.andre.nutritionist.recipe.domain.model.GeneratedMeal
import dev.andre.nutritionist.recipe.domain.model.GeneratedMealPlan
import dev.andre.nutritionist.recipe.domain.model.MENU_VERSION
import dev.andre.nutritionist.recipe.domain.model.MealPlanRequest
import dev.andre.nutritionist.recipe.domain.model.MenuIngredient
import dev.andre.nutritionist.recipe.domain.model.MenuRequest
import dev.andre.nutritionist.recipe.domain.model.RequestContext
import dev.andre.nutritionist.recipe.domain.model.TargetNutrition
import dev.andre.nutritionist.recipe.domain.model.TargetPeriod
import dev.andre.nutritionist.recipe.domain.model.UserId
import dev.andre.nutritionist.recipe.domain.model.digest
import dev.andre.nutritionist.recipe.domain.validator.MenuValidator
import dev.andre.nutritionist.recipe.infrastructure.grpc.client.CatalogClient
import dev.andre.nutritionist.recipe.infrastructure.llm.InvalidMenu
import dev.andre.nutritionist.recipe.infrastructure.llm.MenuUnavailable
import dev.andre.nutritionist.recipe.infrastructure.llm.ProviderFailure
import dev.andre.nutritionist.recipe.infrastructure.redis.RedisMenuStore
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import java.util.concurrent.TimeoutException

@Service
class RecipeMenuService(
    private val catalog: CatalogClient,
    private val generator: ResilientMenuGenerator,
    private val validator: MenuValidator,
    private val store: RedisMenuStore,
    private val settings: RecipeProperties,
    private val json: JsonMapper,
    private val meters: MeterRegistry,
) {
    suspend fun generate(
        request: MenuRequest,
        context: RequestContext,
    ): WeekMenu =
        serveOrFallback(request) {
            val products = catalog.candidates(request, context)
            val key = resultKey(request, context.user, products)
            val cached = store.read(key)
            if (cached != null) {
                cached.withSource(MenuSource.MENU_SOURCE_CACHE)
            } else {
                val menu = generateFresh(request, context, products)
                store.write(key, menu, settings.cacheTtl)
                if (request.isTypical) store.write(fallbackKey(request), menu, settings.fallbackTtl)
                menu.withSource(MenuSource.MENU_SOURCE_GENERATED)
            }
        }.also { meters.counter("recipe.menu.responses", "source", it.summary.source.name).increment() }

    suspend fun generateMealPlan(
        request: MealPlanRequest,
        context: RequestContext,
    ): GeneratedMealPlan {
        val products = catalog.candidatesForMealPlan(request, context)
        val key = mealPlanKey(request, context.user, products)
        val cached = store.readMealPlan(key)
        if (cached != null) {
            return cached.copy(source = MenuSource.MENU_SOURCE_CACHE)
        }
        return try {
            if (settings.llmApiKey.isBlank()) throw MenuUnavailable("LLM API key is unavailable")
            val generated =
                generator.generateMealPlan(request, products, context.traceparent) {
                    store.reserveAttempt(context.user)
                }
            store.writeMealPlan(key, generated, settings.cacheTtl)
            generated.copy(source = MenuSource.MENU_SOURCE_GENERATED)
        } catch (failure: Exception) {
            if (!isDependencyFailure(failure)) throw failure
            createFallbackMealPlan(request, products)
        }.also { meters.counter("recipe.mealplan.responses", "source", it.source.name).increment() }
    }

    private fun createFallbackMealPlan(
        request: MealPlanRequest,
        products: List<CatalogProduct>,
    ): GeneratedMealPlan {
        val candidate =
            products.firstOrNull { it.caloriesPer100g > 0 }
                ?: CatalogProduct(
                    dev.andre.nutritionist.recipe.domain.model
                        .ProductId("fallback"),
                    "Базовый продукт",
                    150.0,
                    10.0,
                    3.0,
                    20.0,
                    100L,
                )
        val grams = ((request.targetNutrition.calories / candidate.caloriesPer100g) * 100).toInt().coerceIn(50, 1000)
        val mealType =
            when (request.period) {
                TargetPeriod.BREAKFAST -> MealType.MEAL_TYPE_BREAKFAST
                TargetPeriod.LUNCH -> MealType.MEAL_TYPE_LUNCH
                TargetPeriod.DINNER -> MealType.MEAL_TYPE_DINNER
                TargetPeriod.SNACK -> MealType.MEAL_TYPE_SNACK
                else -> MealType.MEAL_TYPE_DINNER
            }
        val cost = (candidate.pricePer100gMinor * grams + 50) / 100
        val meal =
            GeneratedMeal(
                type = mealType,
                title = "Сбалансированное блюдо: ${candidate.name}",
                ingredients = listOf(MenuIngredient(candidate.id.raw, candidate.name, grams)),
                nutrition =
                    TargetNutrition(
                        calories = candidate.caloriesPer100g * grams / 100.0,
                        proteinsGrams = candidate.proteinsPer100g * grams / 100.0,
                        fatsGrams = candidate.fatsPer100g * grams / 100.0,
                        carbsGrams = candidate.carbsPer100g * grams / 100.0,
                    ),
                estimatedCostMinor = cost,
                cookingTimeMinutes = 20,
            )
        return GeneratedMealPlan(
            planId = UUID.randomUUID().toString(),
            period = request.period,
            meals = listOf(meal),
            totalNutrition = meal.nutrition,
            estimatedCostMinor = meal.estimatedCostMinor,
            currency = request.currency,
            cookingInstructions =
                "1. Подготовьте ${candidate.name} ($grams г).\n" +
                    "2. Доведите до готовности привычным способом (варка/запекание).\n" +
                    "3. Приятного аппетита!",
            source = MenuSource.MENU_SOURCE_FALLBACK,
        )
    }

    suspend fun warmup(request: MenuRequest) {
        require(request.isTypical)
        val key = fallbackKey(request)
        if (store.read(key) != null || !store.beginWarmup(key)) return
        val context = RequestContext(UserId("recipe-warmup"))
        val products = catalog.candidates(request, context)
        val menu = generateFresh(request, context, products)
        store.write(key, menu, settings.fallbackTtl)
    }

    private suspend fun generateFresh(
        request: MenuRequest,
        context: RequestContext,
        products: List<CatalogProduct>,
    ): WeekMenu {
        if (settings.llmApiKey.isBlank()) throw MenuUnavailable("LLM API key is unavailable")
        return generator.generate(request, products, context.traceparent) {
            store.reserveAttempt(context.user)
        }
    }

    private suspend fun serveOrFallback(
        request: MenuRequest,
        operation: suspend () -> WeekMenu,
    ): WeekMenu {
        try {
            return operation()
        } catch (failure: Exception) {
            if (!isDependencyFailure(failure)) throw failure
            val cached = if (request.isTypical) store.read(fallbackKey(request)) else null
            if (cached != null &&
                cached.summary.currency == CURRENCY &&
                cached.summary.estimatedCostMinor in 0..request.weeklyBudgetMinor &&
                cached.daysList.all { validator.withinCalorieRange(it.calories, request.dailyCalories) }
            ) {
                return cached.withSource(MenuSource.MENU_SOURCE_FALLBACK)
            }
            throw MenuUnavailable("No menu available for these parameters")
        }
    }

    private fun resultKey(
        request: MenuRequest,
        user: UserId,
        products: List<CatalogProduct>,
    ): String {
        val canonical =
            listOf(
                MENU_VERSION,
                settings.model,
                settings.calorieTolerance,
                user.raw,
                request.dailyCalories,
                request.weeklyBudgetMinor,
                request.excluded.map { it.raw }.sorted(),
                request.preferred.map { it.raw },
                products.map { listOf(it.id.raw, it.name, it.caloriesPer100g, it.pricePer100gMinor) },
            )
        return "recipe:result:${digest(json.writeValueAsString(canonical))}"
    }

    private fun mealPlanKey(
        request: MealPlanRequest,
        user: UserId,
        products: List<CatalogProduct>,
    ): String {
        val canonical =
            listOf(
                MENU_VERSION,
                settings.model,
                user.raw,
                request.period.name,
                request.targetNutrition.calories,
                request.maxBudgetMinor,
                request.preferredProducts.sorted(),
                request.excludedProducts.sorted(),
                products.map { listOf(it.id.raw, it.name, it.caloriesPer100g, it.pricePer100gMinor) },
            )
        return "recipe:mealplan:${digest(json.writeValueAsString(canonical))}"
    }

    private fun fallbackKey(request: MenuRequest): String =
        "recipe:fallback:${digest("$MENU_VERSION/${settings.model}/${request.dailyCalories}/${request.weeklyBudgetMinor}")}"

    private fun WeekMenu.withSource(source: MenuSource): WeekMenu = toBuilder().setSummary(summary.toBuilder().setSource(source)).build()

    companion object {
        internal fun isDependencyFailure(error: Exception): Boolean =
            error is MenuUnavailable || error is ProviderFailure || error is InvalidMenu || error is TimeoutException ||
                error is CallNotPermittedException || error is StatusException || error is StatusRuntimeException
    }
}
