package dev.andre.nutritionist.recipe

import com.budgetntr.proto.catalog.v1.AveragePrice
import com.budgetntr.proto.catalog.v1.CatalogServiceGrpcKt
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetAveragePricesResponse
import com.budgetntr.proto.catalog.v1.Nutrition
import com.budgetntr.proto.catalog.v1.Product
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import com.budgetntr.proto.catalog.v1.SearchProductsResponse
import com.budgetntr.proto.recipe.v1.GenerateWeekMenuRequest
import com.budgetntr.proto.recipe.v1.MenuSource
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpcKt
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.MetadataUtils
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.startsWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import tools.jackson.databind.json.JsonMapper
import java.util.concurrent.atomic.AtomicInteger

class RecipeFlowTest {
    @Test
    fun `recipes stream validated days then final and use fallback when breaker is open`() {
        val settings = RecipeProperties(llmApiKey = "fake")
        val configuration = RecipeConfiguration()
        val json = JsonMapper.builder().build()
        val validator = MenuValidator(json, settings)
        val redis = mock(RedisMenuStore::class.java)
        val meters = SimpleMeterRegistry()
        val breakers = configuration.recipeCircuitBreakers(settings, meters)
        val request = MenuRequest(600, 5000)
        val product = CatalogProduct(ProductId(PRODUCT_ID), "Рис", 200.0, 150L)
        val payload = RecipeLlmTest.menuJson(PRODUCT_ID)
        val precomputed = validator.validate(payload, request, listOf(product))
        val paidCalls = AtomicInteger()
        val generator =
            ResilientMenuGenerator(
                { _, _ ->
                    paidCalls.incrementAndGet()
                    payload
                },
                validator,
                json,
                breakers,
                configuration.recipeRetries(settings),
                configuration.recipeTimeLimiter(settings),
            )
        val catalog = TestCatalog()
        val catalogName = InProcessServerBuilder.generateName()
        val catalogServer =
            InProcessServerBuilder
                .forName(catalogName)
                .directExecutor()
                .addService(catalog)
                .build()
                .start()
        val catalogChannel = InProcessChannelBuilder.forName(catalogName).directExecutor().build()
        try {
            val service = RecipeMenuService(CatalogClient(catalogChannel, settings), generator, validator, redis, settings, json, meters)
            val rpc = RecipeGrpcService(service)
            val requestProto =
                GenerateWeekMenuRequest
                    .newBuilder()
                    .setDailyCalories(600)
                    .setWeeklyBudgetMinor(5000)
                    .build()
            val context =
                Context.current().withValue(RecipeIdentityInterceptor.requestContext, RequestContext(UserId("test-user")))
            runBlocking {
                val flow = context.call { rpc.generateWeekMenu(requestProto) }
                val generated = flow.toList()
                assertEquals((1..7).toList(), generated.dropLast(1).map { it.dayReady.dayNumber })
                assertTrue(generated.last().hasFinal())
                assertEquals(MenuSource.MENU_SOURCE_GENERATED, generated.last().final.source)
                assertEquals(2, catalog.priceRequests.get())

                val resultKey = startsWith("recipe:result:")
                `when`(redis.read(resultKey)).thenReturn(precomputed)
                assertEquals(
                    MenuSource.MENU_SOURCE_CACHE,
                    flow
                        .toList()
                        .last()
                        .final.source,
                )
                `when`(redis.read(startsWith("recipe:result:"))).thenReturn(null)
                `when`(redis.read(startsWith("recipe:fallback:"))).thenReturn(precomputed)
                breakers.circuitBreaker("llm").transitionToOpenState()
                assertEquals(
                    MenuSource.MENU_SOURCE_FALLBACK,
                    flow
                        .toList()
                        .last()
                        .final.source,
                )
                assertEquals(1, paidCalls.get())
            }
            val recipeName = InProcessServerBuilder.generateName()
            val recipeServer =
                InProcessServerBuilder
                    .forName(recipeName)
                    .directExecutor()
                    .intercept(RecipeIdentityInterceptor())
                    .addService(rpc)
                    .build()
                    .start()
            val recipeChannel = InProcessChannelBuilder.forName(recipeName).directExecutor().build()
            try {
                val spoofed = Metadata().apply { put(CatalogClient.userHeader, "test-user") }
                val client =
                    RecipeAiServiceGrpcKt
                        .RecipeAiServiceCoroutineStub(recipeChannel)
                        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(spoofed))
                val error = assertThrows(StatusException::class.java) { runBlocking { client.generateWeekMenu(requestProto).toList() } }
                assertEquals(Status.Code.UNAUTHENTICATED, error.status.code)
                assertEquals(1, paidCalls.get())
            } finally {
                recipeChannel.shutdownNow()
                recipeServer.shutdownNow()
            }
        } finally {
            catalogChannel.shutdownNow()
            catalogServer.shutdownNow()
        }
    }

    private class TestCatalog : CatalogServiceGrpcKt.CatalogServiceCoroutineImplBase() {
        val priceRequests = AtomicInteger()

        override suspend fun searchProducts(request: SearchProductsRequest): SearchProductsResponse =
            SearchProductsResponse
                .newBuilder()
                .addProducts(
                    Product
                        .newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setName("Рис")
                        .setNutrition(Nutrition.newBuilder().setCaloriesPer100G(200.0)),
                ).build()

        override suspend fun getAveragePrices(request: GetAveragePricesRequest): GetAveragePricesResponse {
            if (priceRequests.incrementAndGet() == 1) throw Status.UNAVAILABLE.asException()
            assertEquals(listOf(PRODUCT_ID), request.productIdsList)
            assertEquals("RUB", request.currency)
            return GetAveragePricesResponse
                .newBuilder()
                .addPrices(
                    AveragePrice
                        .newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setCurrency("RUB")
                        .setStoreCount(1)
                        .setPricePer100GMinor(150),
                ).build()
        }
    }

    companion object {
        private const val PRODUCT_ID = "44b07415-2762-4e4d-82bb-bcb925805420"
    }
}
