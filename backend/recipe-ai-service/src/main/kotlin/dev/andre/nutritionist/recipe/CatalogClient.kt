package dev.andre.nutritionist.recipe

import com.budgetntr.proto.catalog.v1.CatalogServiceGrpcKt
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetProductRequest
import com.budgetntr.proto.catalog.v1.Product
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
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
                    .setCurrency(CURRENCY)
                    .setMaxPricePer100GMinor(request.weeklyBudgetMinor)
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
                    .distinctBy { it.productId }
                    .filter { it.productId.isNotBlank() && it.hasNutrition() && ProductId(it.productId) !in request.excluded }
            val prices =
                products
                    .chunked(100)
                    .flatMap { batch ->
                        val query =
                            GetAveragePricesRequest
                                .newBuilder()
                                .setCurrency(CURRENCY)
                                .addAllProductIds(batch.map(Product::getProductId))
                                .build()
                        read { getAveragePrices(query, headers) }.pricesList
                    }.filter { it.currency == CURRENCY && it.storeCount > 0 && it.pricePer100GMinor >= 0 }
                    .associate { it.productId to it.pricePer100GMinor }
            val ranked =
                products
                    .mapNotNull { product ->
                        val price = prices[product.productId] ?: return@mapNotNull null
                        val calories = product.nutrition.caloriesPer100G
                        if (price > request.weeklyBudgetMinor || !calories.isFinite() || calories !in 0.0..1_000.0) return@mapNotNull null
                        CatalogProduct(ProductId(product.productId), product.name.take(120), calories, price)
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

    private suspend fun <T> read(call: suspend CatalogServiceGrpcKt.CatalogServiceCoroutineStub.() -> T): T =
        retry.executeSuspendFunction { stub.withDeadlineAfter(settings.catalogDeadline.toMillis(), TimeUnit.MILLISECONDS).call() }

    companion object {
        internal val userHeader: Metadata.Key<String> = Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER)
        internal val traceHeader: Metadata.Key<String> = Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER)
        private val transientStatuses = setOf(Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED)
    }
}
