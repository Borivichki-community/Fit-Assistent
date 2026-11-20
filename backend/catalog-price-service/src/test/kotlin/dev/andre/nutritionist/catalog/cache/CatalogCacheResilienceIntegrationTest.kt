package dev.andre.nutritionist.catalog.cache

import com.budgetntr.proto.catalog.v1.CatalogServiceGrpcKt
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetProductRequest
import com.fasterxml.jackson.databind.ObjectMapper
import dev.andre.nutritionist.catalog.AbstractPostgresIntegrationTest
import dev.andre.nutritionist.catalog.coroutines.DefaultCoroutineDispatchers
import dev.andre.nutritionist.catalog.grpc.CatalogGrpcService
import dev.andre.nutritionist.catalog.grpc.interceptor.GrpcExceptionInterceptor
import dev.andre.nutritionist.catalog.persistence.entity.ProductEntity
import dev.andre.nutritionist.catalog.persistence.entity.RegionEntity
import dev.andre.nutritionist.catalog.persistence.repository.ProductJpaRepository
import dev.andre.nutritionist.catalog.persistence.repository.RegionJpaRepository
import dev.andre.nutritionist.catalog.persistence.repository.RegionalPriceJpaRepository
import dev.andre.nutritionist.catalog.service.CatalogService
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.math.BigDecimal
import java.util.UUID

class CatalogCacheResilienceIntegrationTest : AbstractPostgresIntegrationTest() {
    @Autowired
    private lateinit var catalogService: CatalogService

    @Autowired
    private lateinit var catalogCacheService: CatalogCacheService

    @Autowired
    private lateinit var productRepository: ProductJpaRepository

    @Autowired
    private lateinit var regionRepository: RegionJpaRepository

    @Autowired
    private lateinit var regionalPriceRepository: RegionalPriceJpaRepository

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Autowired
    private lateinit var redisObjectMapper: ObjectMapper

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Autowired
    private lateinit var grpcExceptionInterceptor: GrpcExceptionInterceptor

    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var stub: CatalogServiceGrpcKt.CatalogServiceCoroutineStub

    private lateinit var testProduct: ProductEntity
    private lateinit var testRegion: RegionEntity

    @BeforeEach
    fun setUp() {
        catalogCacheService.evictAll()

        testRegion =
            regionRepository.findByName("Центральный ФО")
                ?: regionRepository.save(RegionEntity(name = "Центральный ФО"))

        testProduct =
            productRepository.findByBarcode("4608888888888")
                ?: productRepository.save(
                    ProductEntity(
                        name = "Творог 9% тест кэша",
                        barcode = "4608888888888",
                        category = "DAIRY_TEST",
                        caloriesPer100g = BigDecimal("157.00"),
                        proteinsPer100g = BigDecimal("16.00"),
                        fatsPer100g = BigDecimal("9.00"),
                        carbsPer100g = BigDecimal("3.00"),
                    ),
                )

        catalogService.upsertRegionalPrice(
            productId = testProduct.id,
            regionId = testRegion.id,
            price = BigDecimal("145.00"),
            currency = "RUB",
            unit = "KG",
        )

        val grpcService = CatalogGrpcService(catalogService, DefaultCoroutineDispatchers(), catalogCacheService)
        val serverName = InProcessServerBuilder.generateName()
        server =
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(grpcService, grpcExceptionInterceptor))
                .build()
                .start()

        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
        stub = CatalogServiceGrpcKt.CatalogServiceCoroutineStub(channel)
    }

    @AfterEach
    fun tearDown() {
        if (::channel.isInitialized) channel.shutdownNow()
        if (::server.isInitialized) server.shutdownNow()
        catalogCacheService.evictAll()
    }

    @Test
    fun `testProductCacheAsideAndMetricsVerification`() =
        runTest {
            val initialHits = getMetricCount("cache_hits_total", "catalog_product")
            val initialMisses = getMetricCount("cache_misses_total", "catalog_product")

            val request =
                GetProductRequest
                    .newBuilder()
                    .setProductId(testProduct.id.toString())
                    .setRegionId(testRegion.id.toString())
                    .build()

            // First call: Cache MISS -> Fetches from DB and populates Redis
            val response1 = stub.getProduct(request)
            assertEquals("Творог 9% тест кэша", response1.product.name)
            assertEquals(1, response1.product.pricesCount)
            assertEquals(145.00, response1.product.getPrices(0).price, 0.001)

            val missesAfterFirst = getMetricCount("cache_misses_total", "catalog_product")
            val hitsAfterFirst = getMetricCount("cache_hits_total", "catalog_product")
            assertEquals(initialMisses + 1.0, missesAfterFirst, "First call must be a cache miss")
            assertEquals(initialHits, hitsAfterFirst, "First call should not register a cache hit")

            // Second call: Cache HIT -> Served from Redis
            val response2 = stub.getProduct(request)
            assertEquals("Творог 9% тест кэша", response2.product.name)
            assertEquals(1, response2.product.pricesCount)
            assertEquals(145.00, response2.product.getPrices(0).price, 0.001)

            val hitsAfterSecond = getMetricCount("cache_hits_total", "catalog_product")
            val missesAfterSecond = getMetricCount("cache_misses_total", "catalog_product")
            assertEquals(initialHits + 1.0, hitsAfterSecond, "Second call must register a cache hit")
            assertEquals(missesAfterFirst, missesAfterSecond, "Second call must not increment misses")
        }

    @Test
    fun `testAveragePricesCacheAsideAndInvalidationOnUpsertPrice`() =
        runTest {
            val initialHits = getMetricCount("cache_hits_total", "catalog_avg_prices")
            val initialMisses = getMetricCount("cache_misses_total", "catalog_avg_prices")

            val request =
                GetAveragePricesRequest
                    .newBuilder()
                    .addProductIds(testProduct.id.toString())
                    .setRegionId(testRegion.id.toString())
                    .build()

            // 1. Initial request -> cache miss
            val res1 = stub.getAveragePrices(request)
            assertEquals(1, res1.pricesCount)
            assertEquals(145.00, res1.getPrices(0).averagePrice, 0.001)
            assertEquals(initialMisses + 1.0, getMetricCount("cache_misses_total", "catalog_avg_prices"))

            // 2. Second request -> cache hit
            val res2 = stub.getAveragePrices(request)
            assertEquals(1, res2.pricesCount)
            assertEquals(145.00, res2.getPrices(0).averagePrice, 0.001)
            assertEquals(initialHits + 1.0, getMetricCount("cache_hits_total", "catalog_avg_prices"))

            // 3. Upsert price -> evicts cache
            catalogService.upsertRegionalPrice(
                productId = testProduct.id,
                regionId = testRegion.id,
                price = BigDecimal("155.00"),
            )

            // 4. Third request -> cache miss again, loads updated 155.00 price
            val res3 = stub.getAveragePrices(request)
            assertEquals(1, res3.pricesCount)
            assertEquals(155.00, res3.getPrices(0).averagePrice, 0.001)
            assertEquals(initialMisses + 2.0, getMetricCount("cache_misses_total", "catalog_avg_prices"))
        }

    @Test
    fun `testGracefulDegradationWhenRedisIsDownViaCircuitBreaker`() =
        runTest {
            // Mock Redis failure
            @Suppress("UNCHECKED_CAST")
            val mockRedisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, Any>

            @Suppress("UNCHECKED_CAST")
            val mockValueOps = mock(ValueOperations::class.java) as ValueOperations<String, Any>

            `when`(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps)
            `when`(mockValueOps.get(anyString()))
                .thenThrow(RedisConnectionFailureException("Simulated Redis outage"))

            val failingCacheService =
                CatalogCacheService(
                    redisTemplate = mockRedisTemplate,
                    redisObjectMapper = redisObjectMapper,
                    meterRegistry = meterRegistry,
                    circuitBreakerRegistry = circuitBreakerRegistry,
                )

            val resilientGrpcService = CatalogGrpcService(catalogService, DefaultCoroutineDispatchers(), failingCacheService)
            val serverName = InProcessServerBuilder.generateName()
            val failoverServer =
                InProcessServerBuilder
                    .forName(serverName)
                    .directExecutor()
                    .addService(ServerInterceptors.intercept(resilientGrpcService, grpcExceptionInterceptor))
                    .build()
                    .start()

            val failoverChannel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
            val failoverStub = CatalogServiceGrpcKt.CatalogServiceCoroutineStub(failoverChannel)

            try {
                // Should NOT throw an exception despite Redis failure; seamlessly falls back to PostgreSQL!
                val productResponse =
                    failoverStub.getProduct(
                        GetProductRequest
                            .newBuilder()
                            .setProductId(testProduct.id.toString())
                            .setRegionId(testRegion.id.toString())
                            .build(),
                    )
                assertNotNull(productResponse.product)
                assertEquals("Творог 9% тест кэша", productResponse.product.name)

                val avgPricesResponse =
                    failoverStub.getAveragePrices(
                        GetAveragePricesRequest
                            .newBuilder()
                            .addProductIds(testProduct.id.toString())
                            .setRegionId(testRegion.id.toString())
                            .build(),
                    )
                assertEquals(1, avgPricesResponse.pricesCount)
                assertTrue(avgPricesResponse.getPrices(0).averagePrice > 0)
            } finally {
                failoverChannel.shutdownNow()
                failoverServer.shutdownNow()
            }
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
}
