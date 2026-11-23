package dev.andre.nutritionist.recipe

import com.budgetntr.proto.recipe.v1.MenuSource
import com.budgetntr.proto.recipe.v1.WeekMenu
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
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

    private fun fallbackKey(request: MenuRequest): String =
        "recipe:fallback:${digest("$MENU_VERSION/${settings.model}/${request.dailyCalories}/${request.weeklyBudgetMinor}")}"

    private fun WeekMenu.withSource(source: MenuSource): WeekMenu = toBuilder().setSummary(summary.toBuilder().setSource(source)).build()

    companion object {
        internal fun isDependencyFailure(error: Exception): Boolean =
            error is MenuUnavailable || error is ProviderFailure || error is InvalidMenu || error is TimeoutException ||
                error is CallNotPermittedException || error is StatusException || error is StatusRuntimeException
    }
}
