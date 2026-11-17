package dev.andre.nutritionist.catalog

import com.budgetntr.proto.catalog.v1.CatalogServiceGrpcKt
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetProductRequest
import com.budgetntr.proto.catalog.v1.NumericRange
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import com.budgetntr.proto.catalog.v1.UpsertPriceRequest
import dev.andre.nutritionist.catalog.grpc.CatalogGrpcService
import dev.andre.nutritionist.catalog.grpc.CatalogSecurityInterceptor
import dev.andre.nutritionist.catalog.persistence.CatalogRepository
import io.grpc.Metadata
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.MetadataUtils
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import liquibase.integration.spring.SpringLiquibase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Phaser
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@SpringBootTest(properties = ["spring.grpc.server.port=0", "server.port=0", "catalog.cache.ttl-seconds=2"])
@Testcontainers
class CatalogIntegrationTest
    @Autowired
    constructor(
        private val service: CatalogGrpcService,
        private val security: CatalogSecurityInterceptor,
        private val source: DataSource,
        private val repository: CatalogRepository,
        private val redis: StringRedisTemplate,
        private val meters: MeterRegistry,
    ) {
        private val jdbc = JdbcTemplate(source)

        @BeforeEach
        fun reset() {
            source.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource("catalog/fixture.sql")) }
            val keys = redis.keys("catalog:*")
            if (keys.isNotEmpty()) redis.delete(keys)
        }

        @Test
        fun migrationsRequireExplicitDemoContextAndDoNotRepeatSeed() {
            jdbc.execute("CREATE SCHEMA seed_check")
            try {
                val isolated =
                    DriverManagerDataSource(postgres.jdbcUrl + "?currentSchema=seed_check,public", postgres.username, postgres.password)

                fun migrate(context: String?) =
                    SpringLiquibase().apply {
                        dataSource = isolated
                        changeLog = "classpath:db/changelog/db.changelog-master.yaml"
                        contexts = context
                        afterPropertiesSet()
                    }
                val isolatedJdbc = JdbcTemplate(isolated)
                migrate(null)
                assertThat(isolatedJdbc.queryForObject("SELECT count(*) FROM products", Long::class.java)).isZero()
                migrate("catalog-demo")
                assertThat(isolatedJdbc.queryForObject("SELECT count(*) FROM products", Long::class.java)).isEqualTo(6)
                isolatedJdbc.update("UPDATE prices SET unit_price_minor=42")
                migrate("catalog-demo")
                assertThat(isolatedJdbc.queryForObject("SELECT count(*) FROM prices WHERE unit_price_minor=42", Long::class.java))
                    .isEqualTo(5)
            } finally {
                jdbc.execute("DROP SCHEMA seed_check CASCADE")
            }
        }

        @Test
        fun productsFiltersPaginationAndUnknownPrices() =
            client { stub ->
                val first = stub.searchProducts(SearchProductsRequest.newBuilder().setPageSize(2).build())
                val second =
                    stub.searchProducts(
                        SearchProductsRequest
                            .newBuilder()
                            .setPageSize(2)
                            .setPageToken(first.nextPageToken)
                            .build(),
                    )
                assertThat(first.productsList + second.productsList).hasSize(3)
                assertThat((first.productsList + second.productsList).map { it.productId }.distinct()).hasSize(3)
                assertThat(second.nextPageToken).isEmpty()
                val filtered =
                    stub.searchProducts(
                        SearchProductsRequest
                            .newBuilder()
                            .setCategory("grain")
                            .setProteins(NumericRange.newBuilder().setMin(10.0))
                            .setMaxPricePer100GMinor(200)
                            .build(),
                    )
                assertThat(filtered.productsList.map { it.productId }).containsExactly(PRODUCT)
                val literal = stub.searchProducts(SearchProductsRequest.newBuilder().setQuery("%").build())
                assertThat(literal.productsList.map { it.productId }).containsExactly(PRODUCT)
                assertThat(stub.getProduct(GetProductRequest.newBuilder().setProductId(PRODUCT).build()).product.name)
                    .isEqualTo("Alpha 100%")
                assertCode(Status.Code.NOT_FOUND) {
                    stub.getProduct(GetProductRequest.newBuilder().setProductId("00000000-0000-0000-0000-000000000999").build())
                }
                assertCode(Status.Code.INVALID_ARGUMENT) {
                    stub.searchProducts(SearchProductsRequest.newBuilder().setPageSize(-1).build())
                }
            }

        @Test
        fun averagesNormalizeMassRoundHalfUpAndKeepCurrenciesSeparate() =
            client { stub ->
                val request =
                    GetAveragePricesRequest
                        .newBuilder()
                        .addProductIds(PRODUCT)
                        .addProductIds(PRODUCT)
                        .addProductIds("00000000-0000-0000-0000-000000000103")
                        .build()
                val average = stub.getAveragePrices(request).pricesList.single()
                assertThat(average.pricePer100GMinor).isEqualTo(176)
                assertThat(average.storeCount).isEqualTo(2)
                assertThat(
                    stub
                        .getAveragePrices(request.toBuilder().setCurrency("USD").build())
                        .pricesList
                        .single()
                        .pricePer100GMinor,
                ).isEqualTo(999)
            }

        @Test
        fun mutationReplaysOriginalResponseRejectsChangedPayloadAndChangesRevisionOnce() =
            client { stub ->
                val initial = repository.priceRevision()
                val first = stub.upsertPrice(price())
                val duplicate = stub.upsertPrice(price())
                assertThat(duplicate).isEqualTo(first)
                assertThat(repository.priceRevision()).isEqualTo(initial + 1)
                assertThat(jdbc.queryForObject("SELECT count(*) FROM price_requests", Long::class.java)).isEqualTo(1)
                assertCode(Status.Code.INVALID_ARGUMENT) { stub.upsertPrice(price().toBuilder().setUnitPriceMinor(2).build()) }
                assertCode(Status.Code.INVALID_ARGUMENT) {
                    stub.upsertPrice(price().toBuilder().setPriceBasisMilligrams(0).build())
                }
            }

        @Test
        fun concurrentDuplicatesProduceOneMutation() =
            client { stub ->
                val before = repository.priceRevision()
                val barrier = Phaser(2)
                val executor = Executors.newFixedThreadPool(2)
                try {
                    val calls =
                        List(2) {
                            Callable {
                                barrier.arriveAndAwaitAdvance()
                                runBlocking { stub.upsertPrice(price()) }
                            }
                        }
                    val results = executor.invokeAll(calls).map { it.get(5, TimeUnit.SECONDS) }
                    assertThat(results[0]).isEqualTo(results[1])
                    assertThat(repository.priceRevision()).isEqualTo(before + 1)
                } finally {
                    executor.shutdownNow()
                }
            }

        @Test
        fun roleAndKeyAreRequiredAndUnsignedBearerIsNotIdentity() {
            client(admin = false) { stub -> assertCode(Status.Code.PERMISSION_DENIED) { stub.upsertPrice(price()) } }
            client(key = null) { stub -> assertCode(Status.Code.INVALID_ARGUMENT) { stub.upsertPrice(price()) } }
            client(authenticated = false) { stub ->
                assertCode(Status.Code.UNAUTHENTICATED) { stub.searchProducts(SearchProductsRequest.getDefaultInstance()) }
            }
            client(key = "missing-store") { stub ->
                assertCode(Status.Code.NOT_FOUND) {
                    stub.upsertPrice(price().toBuilder().setStoreId("00000000-0000-0000-0000-000000000999").build())
                }
            }
            assertThat(jdbc.queryForObject("SELECT count(*) FROM price_requests", Long::class.java)).isZero()
        }

        @Test
        fun cacheHitsTtlCorruptionAndPriceGeneration() =
            client { stub ->
                val get = GetProductRequest.newBuilder().setProductId(PRODUCT).build()
                val before = hits("product")
                val response = stub.getProduct(get)
                assertThat(stub.getProduct(get)).isEqualTo(response)
                assertThat(hits("product")).isEqualTo(before + 1)
                val key = redis.keys("catalog:v1:product:*").single()
                assertThat(redis.getExpire(key, TimeUnit.MILLISECONDS)).isBetween(1, 2000)
                redis.opsForValue().set(key, "broken cache", Duration.ofSeconds(2))
                assertThat(stub.getProduct(get)).isEqualTo(response)
                redis.expire(key, Duration.ZERO)
                assertThat(stub.getProduct(get)).isEqualTo(response)
                val averageRequest = GetAveragePricesRequest.newBuilder().addProductIds(PRODUCT).build()
                val search = SearchProductsRequest.newBuilder().setMaxPricePer100GMinor(200).build()
                assertThat(
                    stub
                        .getAveragePrices(averageRequest)
                        .pricesList
                        .single()
                        .pricePer100GMinor,
                ).isEqualTo(176)
                assertThat(stub.searchProducts(search).productsCount).isEqualTo(1)
                stub.upsertPrice(price())
                assertThat(
                    stub
                        .getAveragePrices(averageRequest)
                        .pricesList
                        .single()
                        .pricePer100GMinor,
                ).isEqualTo(625)
                assertThat(stub.searchProducts(search).productsCount).isZero()
            }

        private fun hits(operation: String): Double =
            meters
                .find("catalog.cache.requests")
                .tag("operation", operation)
                .tag("result", "hit")
                .counter()
                ?.count() ?: 0.0

        private fun client(
            admin: Boolean = true,
            key: String? = "test-key",
            authenticated: Boolean = true,
            action: suspend (CatalogServiceGrpcKt.CatalogServiceCoroutineStub) -> Unit,
        ) = runBlocking {
            val name = InProcessServerBuilder.generateName()
            val server =
                InProcessServerBuilder
                    .forName(name)
                    .directExecutor()
                    .addService(ServerInterceptors.intercept(service, security))
                    .build()
                    .start()
            val channel = InProcessChannelBuilder.forName(name).directExecutor().build()
            try {
                val headers = Metadata()
                if (authenticated) {
                    headers.put(header("x-user-id"), "test-admin")
                    headers.put(header("x-user-roles"), if (admin) "[\"admin\"]" else "[\"user\"]")
                } else {
                    headers.put(header("authorization"), "Bearer e30.eyJzdWIiOiJhZG1pbiJ9.unsigned")
                }
                key?.let { headers.put(header("idempotency-key"), it) }
                val stub =
                    CatalogServiceGrpcKt
                        .CatalogServiceCoroutineStub(channel)
                        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                        .withDeadlineAfter(10, TimeUnit.SECONDS)
                action(stub)
            } finally {
                channel.shutdownNow()
                server.shutdownNow()
            }
        }

        private suspend fun assertCode(
            code: Status.Code,
            action: suspend () -> Unit,
        ) {
            val result = runCatching { action() }
            assertThat(result.isFailure).isTrue()
            assertThat(Status.fromThrowable(checkNotNull(result.exceptionOrNull())).code).isEqualTo(code)
        }

        private fun price(): UpsertPriceRequest =
            UpsertPriceRequest
                .newBuilder()
                .setProductId(PRODUCT)
                .setStoreId(STORE)
                .setUnitPriceMinor(1000)
                .setCurrency("RUB")
                .setPriceBasisMilligrams(100000)
                .build()

        companion object {
            private const val PRODUCT = "00000000-0000-0000-0000-000000000101"
            private const val STORE = "00000000-0000-0000-0000-000000000001"

            @Container
            @JvmField
            val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))

            @Container
            @JvmField
            val redisContainer: GenericContainer<*> =
                GenericContainer(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)

            @JvmStatic
            @DynamicPropertySource
            fun properties(registry: DynamicPropertyRegistry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl)
                registry.add("spring.datasource.username", postgres::getUsername)
                registry.add("spring.datasource.password", postgres::getPassword)
                registry.add("spring.data.redis.host", redisContainer::getHost)
                registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            }

            private fun header(name: String): Metadata.Key<String> = Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER)
        }
    }
