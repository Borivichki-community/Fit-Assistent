package dev.andre.nutritionist.catalog.e2e

import com.budgetntr.proto.catalog.v1.CatalogServiceGrpcKt
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetProductRequest
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import com.budgetntr.proto.catalog.v1.UpsertPriceRequest
import dev.andre.nutritionist.catalog.AbstractPostgresIntegrationTest
import dev.andre.nutritionist.catalog.cache.CatalogCacheService
import dev.andre.nutritionist.catalog.grpc.CatalogGrpcService
import dev.andre.nutritionist.catalog.grpc.interceptor.GrpcExceptionInterceptor
import dev.andre.nutritionist.catalog.grpc.security.SecurityGrpcInterceptor
import dev.andre.nutritionist.catalog.persistence.repository.ProductJpaRepository
import dev.andre.nutritionist.catalog.persistence.repository.RegionJpaRepository
import dev.andre.nutritionist.catalog.persistence.repository.RegionalPriceJpaRepository
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.MetadataUtils
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.system.measureTimeMillis

class CatalogEndToEndIntegrationTest : AbstractPostgresIntegrationTest() {
    @Autowired
    private lateinit var catalogGrpcService: CatalogGrpcService

    @Autowired
    private lateinit var catalogCacheService: CatalogCacheService

    @Autowired
    private lateinit var grpcExceptionInterceptor: GrpcExceptionInterceptor

    @Autowired
    private lateinit var securityGrpcInterceptor: SecurityGrpcInterceptor

    @Autowired
    private lateinit var productRepository: ProductJpaRepository

    @Autowired
    private lateinit var regionRepository: RegionJpaRepository

    @Autowired
    private lateinit var regionalPriceRepository: RegionalPriceJpaRepository

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var stub: CatalogServiceGrpcKt.CatalogServiceCoroutineStub

    @BeforeEach
    fun setUp() {
        catalogCacheService.evictAll()

        val serverName = InProcessServerBuilder.generateName()
        server =
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(
                    ServerInterceptors.intercept(
                        catalogGrpcService,
                        grpcExceptionInterceptor,
                        securityGrpcInterceptor,
                    ),
                ).build()
                .start()

        channel =
            InProcessChannelBuilder
                .forName(serverName)
                .directExecutor()
                .build()

        stub = CatalogServiceGrpcKt.CatalogServiceCoroutineStub(channel)
    }

    @AfterEach
    fun tearDown() {
        if (::channel.isInitialized) channel.shutdownNow()
        if (::server.isInitialized) server.shutdownNow()
        catalogCacheService.evictAll()
    }

    private fun stubWithAuth(
        userId: String? = null,
        roles: String? = null,
    ): CatalogServiceGrpcKt.CatalogServiceCoroutineStub {
        val headers = Metadata()
        userId?.let { headers.put(SecurityGrpcInterceptor.X_USER_ID_HEADER, it) }
        roles?.let { headers.put(SecurityGrpcInterceptor.X_USER_ROLES_HEADER, it) }
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
    }

    private suspend inline fun <reified T : Throwable> assertThrowsSuspend(crossinline block: suspend () -> Unit): T {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) return e
            throw AssertionError("Expected exception of type ${T::class.java.name}, but caught ${e::class.java.name}", e)
        }
        throw AssertionError("Expected exception of type ${T::class.java.name}, but no exception was thrown")
    }

    private fun getMetricCount(
        name: String,
        cacheTag: String,
    ): Double {
        val counter =
            meterRegistry
                .find(name)
                .tag("cache", cacheTag)
                .counter()
        return counter?.count() ?: 0.0
    }

    @Test
    fun `testCompleteProductLifecycleAndRegionalPricingFlow`() =
        runTest {
            // 1. Поиск продуктов по категории DAIRY и диапазону калорий 40..200
            val searchRequest =
                SearchProductsRequest
                    .newBuilder()
                    .setCategory("DAIRY")
                    .setMinCalories(40.0)
                    .setMaxCalories(200.0)
                    .setPageSize(10)
                    .build()

            val searchResponse = stub.searchProducts(searchRequest)
            assertTrue(searchResponse.productsCount > 0, "Seed catalog must contain DAIRY products")

            // 2. Выбор продукта из результатов
            val selectedProduct = searchResponse.getProducts(0)
            val productId = selectedProduct.id
            val regions = regionRepository.findAll()
            assertTrue(regions.isNotEmpty(), "Regions must be seeded")
            val targetRegion = regions.first { it.name == "Москва" }
            val regionId = targetRegion.id.toString()

            // 3. Получение деталей продукта и цен для региона
            val initialProductHits = getMetricCount("cache_hits_total", "catalog_product")
            val initialProductMisses = getMetricCount("cache_misses_total", "catalog_product")

            val productReq =
                GetProductRequest
                    .newBuilder()
                    .setProductId(productId)
                    .setRegionId(regionId)
                    .build()

            // Первый запрос -> Cache Miss, запись в Redis
            val getProductResp1 = stub.getProduct(productReq)
            assertEquals(productId, getProductResp1.product.id)
            assertTrue(getProductResp1.product.pricesCount > 0, "Product should have price for Central FO")
            val oldPriceProto = getProductResp1.product.getPrices(0)
            val oldPrice = oldPriceProto.price
            val oldCurrency = oldPriceProto.currency
            val oldUnit = oldPriceProto.unit

            assertEquals(
                initialProductMisses + 1.0,
                getMetricCount("cache_misses_total", "catalog_product"),
                "First getProduct call must register a cache miss",
            )

            // Второй запрос -> Cache Hit, ответ из Redis
            val getProductResp2 = stub.getProduct(productReq)
            assertEquals(getProductResp1.product.name, getProductResp2.product.name)
            assertEquals(
                initialProductHits + 1.0,
                getMetricCount("cache_hits_total", "catalog_product"),
                "Second getProduct call must register a cache hit",
            )

            // 4. Запрос средних цен -> Cache Miss, затем Cache Hit
            val initialAvgHits = getMetricCount("cache_hits_total", "catalog_avg_prices")
            val initialAvgMisses = getMetricCount("cache_misses_total", "catalog_avg_prices")

            val avgReq =
                GetAveragePricesRequest
                    .newBuilder()
                    .addProductIds(productId)
                    .setRegionId(regionId)
                    .build()

            val avgResp1 = stub.getAveragePrices(avgReq)
            assertEquals(1, avgResp1.pricesCount)
            assertEquals(oldPrice, avgResp1.getPrices(0).averagePrice, 0.01)
            assertEquals(initialAvgMisses + 1.0, getMetricCount("cache_misses_total", "catalog_avg_prices"))

            val avgResp2 = stub.getAveragePrices(avgReq)
            assertEquals(1, avgResp2.pricesCount)
            assertEquals(initialAvgHits + 1.0, getMetricCount("cache_hits_total", "catalog_avg_prices"))

            // 5. Администратор обновляет региональную цену
            val adminStub = stubWithAuth(userId = "admin_master", roles = "ROLE_ADMIN")
            try {
                val newPriceValue = 299.90
                val upsertReq =
                    UpsertPriceRequest
                        .newBuilder()
                        .setProductId(productId)
                        .setRegionId(regionId)
                        .setPrice(newPriceValue)
                        .setCurrency("RUB")
                        .setUnit("KG")
                        .build()

                val upsertResp = adminStub.upsertPrice(upsertReq)
                assertEquals(newPriceValue, upsertResp.price.price, 0.001)

                // 6. Проверка инвалидации кеша и возврата обновленной цены
                val getProductResp3 = stub.getProduct(productReq)
                assertEquals(newPriceValue, getProductResp3.product.getPrices(0).price, 0.001)
                assertEquals(
                    initialProductMisses + 2.0,
                    getMetricCount("cache_misses_total", "catalog_product"),
                    "Must be a cache miss after invalidation",
                )

                val avgResp3 = stub.getAveragePrices(avgReq)
                assertEquals(newPriceValue, avgResp3.getPrices(0).averagePrice, 0.01)
                assertEquals(
                    initialAvgMisses + 2.0,
                    getMetricCount("cache_misses_total", "catalog_avg_prices"),
                    "Must be a cache miss after invalidation",
                )
            } finally {
                // Восстанавливаем исходную seed-цену, чтобы не загрязнять общую БД для других тестов
                adminStub.upsertPrice(
                    UpsertPriceRequest
                        .newBuilder()
                        .setProductId(productId)
                        .setRegionId(regionId)
                        .setPrice(oldPrice)
                        .setCurrency(oldCurrency)
                        .setUnit(oldUnit)
                        .build(),
                )
            }
        }

    @Test
    fun `testEdgeCasesAndValidationFailures`() =
        runTest {
            // 1. Запрос несуществующего productId -> NOT_FOUND
            val nonExistentId = UUID.randomUUID().toString()
            val notFoundEx =
                assertThrowsSuspend<StatusException> {
                    stub.getProduct(
                        GetProductRequest
                            .newBuilder()
                            .setProductId(nonExistentId)
                            .build(),
                    )
                }
            assertEquals(Status.Code.NOT_FOUND, notFoundEx.status.code)

            // 2. Невалидный формат UUID -> INVALID_ARGUMENT
            val invalidUuidEx =
                assertThrowsSuspend<StatusException> {
                    stub.getProduct(
                        GetProductRequest
                            .newBuilder()
                            .setProductId("not-a-valid-uuid")
                            .build(),
                    )
                }
            assertEquals(Status.Code.INVALID_ARGUMENT, invalidUuidEx.status.code)

            // 3. Отрицательная цена при обновлении -> INVALID_ARGUMENT
            val adminStub = stubWithAuth(userId = "admin_user", roles = "ROLE_ADMIN")
            val validProductId =
                productRepository
                    .findAll()
                    .first()
                    .id
                    .toString()
            val validRegionId =
                regionRepository
                    .findAll()
                    .first()
                    .id
                    .toString()

            val negativePriceEx =
                assertThrowsSuspend<StatusException> {
                    adminStub.upsertPrice(
                        UpsertPriceRequest
                            .newBuilder()
                            .setProductId(validProductId)
                            .setRegionId(validRegionId)
                            .setPrice(-15.50)
                            .build(),
                    )
                }
            assertEquals(Status.Code.INVALID_ARGUMENT, negativePriceEx.status.code)

            // 4. Попытка обновить цену для несуществующего продукта -> NOT_FOUND
            val nonExistentProductPriceEx =
                assertThrowsSuspend<StatusException> {
                    adminStub.upsertPrice(
                        UpsertPriceRequest
                            .newBuilder()
                            .setProductId(UUID.randomUUID().toString())
                            .setRegionId(validRegionId)
                            .setPrice(100.0)
                            .build(),
                    )
                }
            assertEquals(Status.Code.NOT_FOUND, nonExistentProductPriceEx.status.code)

            // 5. Пустой список ID в getAveragePrices -> пустой ответ без ошибок
            val emptyAvgResponse =
                stub.getAveragePrices(
                    GetAveragePricesRequest
                        .newBuilder()
                        .setRegionId(validRegionId)
                        .build(),
                )
            assertEquals(0, emptyAvgResponse.pricesCount)
        }

    @Test
    fun `testLoadSmokeTestConcurrentRequests`() =
        runTest {
            val sampleProductIds = productRepository.findAll().take(5).map { it.id.toString() }
            val sampleRegionId =
                regionRepository
                    .findAll()
                    .first()
                    .id
                    .toString()
            val concurrencyLevel = 50

            val totalDurationMs =
                measureTimeMillis {
                    coroutineScope {
                        val jobs =
                            (0 until concurrencyLevel).map { index ->
                                async(Dispatchers.IO) {
                                    if (index % 2 == 0) {
                                        // Четные корутины выполняют SearchProducts
                                        val response =
                                            stub.searchProducts(
                                                SearchProductsRequest
                                                    .newBuilder()
                                                    .setCategory("MEAT")
                                                    .setPageSize(10)
                                                    .build(),
                                            )
                                        assertFalse(response.productsList.isEmpty())
                                        response.productsCount
                                    } else {
                                        // Нечетные корутины запрашивают GetAveragePrices
                                        val response =
                                            stub.getAveragePrices(
                                                GetAveragePricesRequest
                                                    .newBuilder()
                                                    .addAllProductIds(sampleProductIds)
                                                    .setRegionId(sampleRegionId)
                                                    .build(),
                                            )
                                        assertFalse(response.pricesList.isEmpty())
                                        response.pricesCount
                                    }
                                }
                            }

                        val results = jobs.awaitAll()
                        assertEquals(concurrencyLevel, results.size, "All 50 concurrent requests must complete")
                    }
                }

            // Убеждаемся, что 50 параллельных запросов завершились быстро и без сбоев пула соединений
            assertTrue(
                totalDurationMs < 15000,
                "50 concurrent requests should complete within 15 seconds, took ${totalDurationMs}ms",
            )
        }
}
