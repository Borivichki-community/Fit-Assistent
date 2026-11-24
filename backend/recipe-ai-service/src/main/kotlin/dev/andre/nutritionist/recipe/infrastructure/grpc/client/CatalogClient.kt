package dev.andre.nutritionist.recipe.infrastructure.grpc.client

import com.budgetntr.proto.catalog.v1.CatalogServiceGrpcKt
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetProductRequest
import com.budgetntr.proto.catalog.v1.ProductItem
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import dev.andre.nutritionist.recipe.config.RecipeProperties
import dev.andre.nutritionist.recipe.domain.model.CatalogProduct
import dev.andre.nutritionist.recipe.domain.model.MealPlanRequest
import dev.andre.nutritionist.recipe.domain.model.MenuRequest
import dev.andre.nutritionist.recipe.domain.model.ProductId
import dev.andre.nutritionist.recipe.domain.model.RequestContext
import dev.andre.nutritionist.recipe.domain.model.WEEK_DAYS
import dev.andre.nutritionist.recipe.infrastructure.llm.MenuUnavailable
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class CatalogClient(
    channel: ManagedChannel,
    private val settings: RecipeProperties,
) {
    private val stub = CatalogServiceGrpcKt.CatalogServiceCoroutineStub(channel)
    private val retry =
        Retry.of(
            "catalog-reads",
            RetryConfig
                .custom<Any>()
                .maxAttempts(3)
                .intervalFunction { attempt -> 100L * (1L shl (attempt - 1)) }
                .retryOnException { Status.fromThrowable(it).code in transientStatuses }
                .build(),
        )

    suspend fun candidates(
        request: MenuRequest,
        context: RequestContext,
    ): List<CatalogProduct> =
        coroutineScope {
            val headers =
                Metadata().apply {
                    put(userHeader, context.user.raw)
                    context.traceparent?.let { put(traceHeader, it) }
                }
            val search =
                SearchProductsRequest
                    .newBuilder()
                    .setPageSize(80)
                    .build()
            val preferred =
                request.preferred
                    .filterNot { it in request.excluded }
                    .take(12)
                    .map { id ->
                        async {
                            val lookup = GetProductRequest.newBuilder().setProductId(id.raw).build()
                            try {
                                read { getProduct(lookup, headers) }.product
                            } catch (failure: StatusException) {
                                if (failure.status.code != Status.Code.NOT_FOUND) throw failure
                                null
                            }
                        }
                    }
            val first = read { searchProducts(search, headers) }
            val next =
                if (first.nextPageToken.isBlank()) {
                    emptyList()
                } else {
                    read { searchProducts(search.toBuilder().setPageToken(first.nextPageToken).build(), headers) }.productsList
                }
            val products =
                (preferred.mapNotNull { it.await() } + first.productsList + next)
                    .distinctBy { it.id }
                    .filter { it.id.isNotBlank() && it.hasNutrition() && ProductId(it.id) !in request.excluded }
            val prices =
                products
                    .chunked(100)
                    .flatMap { batch ->
                        val query =
                            GetAveragePricesRequest
                                .newBuilder()
                                .addAllProductIds(batch.map(ProductItem::getId))
                                .build()
                        read { getAveragePrices(query, headers) }.pricesList
                    }.filter { it.regionCount > 0 && it.averagePrice >= 0 }
                    .associate { it.productId to (it.averagePrice * 10).toLong().coerceAtLeast(1L) }
            val ranked =
                products
                    .mapNotNull { product ->
                        val price = prices[product.id] ?: return@mapNotNull null
                        val calories = product.nutrition.caloriesPer100G
                        if (price > request.weeklyBudgetMinor || !calories.isFinite() || calories !in 0.0..1_000.0) return@mapNotNull null
                        CatalogProduct(
                            id = ProductId(product.id),
                            name = product.name.take(120),
                            caloriesPer100g = calories,
                            proteinsPer100g = product.nutrition.proteinsPer100G,
                            fatsPer100g = product.nutrition.fatsPer100G,
                            carbsPer100g = product.nutrition.carbsPer100G,
                            pricePer100gMinor = price,
                        )
                    }.sortedWith(
                        compareBy<CatalogProduct> { it.pricePer100gMinor / it.caloriesPer100g.coerceAtLeast(1.0) }.thenBy { it.id.raw },
                    )
            val energetic = ranked.filter { it.caloriesPer100g > 0 }
            val bestRate =
                energetic.minOfOrNull { it.pricePer100gMinor / it.caloriesPer100g }
                    ?: throw MenuUnavailable("Catalog contains no priced menu candidates")
            val minWeekCalories = request.dailyCalories * (1 - settings.calorieTolerance) * WEEK_DAYS
            val maxRoundingSavings = WEEK_DAYS * 3 * 8 * 0.5
            if (bestRate * minWeekCalories - maxRoundingSavings > request.weeklyBudgetMinor) {
                throw MenuUnavailable("Catalog candidates cannot satisfy the budget")
            }
            (energetic.take(12) + ranked.filter { it.id in request.preferred } + ranked).distinctBy { it.id }.take(35)
        }

    suspend fun candidatesForMealPlan(
        request: MealPlanRequest,
        context: RequestContext,
    ): List<CatalogProduct> =
        coroutineScope {
            val headers =
                Metadata().apply {
                    put(userHeader, context.user.raw)
                    context.traceparent?.let { put(traceHeader, it) }
                }

            val preferredQueries =
                request.preferredProducts.take(5).map { query ->
                    async {
                        val searchReq =
                            SearchProductsRequest
                                .newBuilder()
                                .setQuery(query)
                                .apply { request.regionId?.let { setRegionId(it) } }
                                .setPageSize(10)
                                .build()
                        try {
                            read { searchProducts(searchReq, headers) }.productsList
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                }

            val baseSearch =
                SearchProductsRequest
                    .newBuilder()
                    .setPageSize(50)
                    .apply { request.regionId?.let { setRegionId(it) } }
                    .build()
            val baseProducts =
                try {
                    read { searchProducts(baseSearch, headers) }.productsList
                } catch (_: Exception) {
                    emptyList()
                }

            val preferredFound = preferredQueries.flatMap { it.await() }
            val allProducts =
                (preferredFound + baseProducts)
                    .distinctBy { it.id }
                    .filter { it.hasNutrition() && !request.excludedProducts.any { excl -> it.name.contains(excl, ignoreCase = true) } }

            if (allProducts.isEmpty()) {
                throw MenuUnavailable("No catalog products available for meal planning")
            }

            val prices =
                allProducts
                    .chunked(100)
                    .flatMap { batch ->
                        val query =
                            GetAveragePricesRequest
                                .newBuilder()
                                .addAllProductIds(batch.map { it.id })
                                .apply { request.regionId?.let { setRegionId(it) } }
                                .build()
                        try {
                            read { getAveragePrices(query, headers) }.pricesList
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }.associate { it.productId to (it.averagePrice * 10).toLong().coerceAtLeast(1L) }

            val candidates =
                allProducts.map { product ->
                    val price = prices[product.id] ?: 100L
                    CatalogProduct(
                        id = ProductId(product.id),
                        name = product.name,
                        caloriesPer100g = product.nutrition.caloriesPer100G,
                        proteinsPer100g = product.nutrition.proteinsPer100G,
                        fatsPer100g = product.nutrition.fatsPer100G,
                        carbsPer100g = product.nutrition.carbsPer100G,
                        pricePer100gMinor = price,
                    )
                }
            candidates.take(35)
        }

    private suspend fun <T> read(call: suspend CatalogServiceGrpcKt.CatalogServiceCoroutineStub.() -> T): T =
        retry.executeSuspendFunction { stub.withDeadlineAfter(settings.catalogDeadline.toMillis(), TimeUnit.MILLISECONDS).call() }

    companion object {
        internal val userHeader: Metadata.Key<String> = Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER)
        internal val traceHeader: Metadata.Key<String> = Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER)
        internal val devBypassHeader: Metadata.Key<String> = Metadata.Key.of("x-dev-bypass", Metadata.ASCII_STRING_MARSHALLER)
        private val transientStatuses = setOf(Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED)
    }
}
