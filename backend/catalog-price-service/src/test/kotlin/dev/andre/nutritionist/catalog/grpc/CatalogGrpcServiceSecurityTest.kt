package dev.andre.nutritionist.catalog.grpc

import com.budgetntr.proto.catalog.v1.CatalogServiceGrpcKt
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetProductRequest
import com.budgetntr.proto.catalog.v1.UpsertPriceRequest
import dev.andre.nutritionist.catalog.AbstractPostgresIntegrationTest
import dev.andre.nutritionist.catalog.grpc.interceptor.GrpcExceptionInterceptor
import dev.andre.nutritionist.catalog.grpc.security.SecurityGrpcInterceptor
import dev.andre.nutritionist.catalog.persistence.entity.ProductEntity
import dev.andre.nutritionist.catalog.persistence.entity.RegionEntity
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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

class CatalogGrpcServiceSecurityTest : AbstractPostgresIntegrationTest() {
    @Autowired
    private lateinit var catalogGrpcService: CatalogGrpcService

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

    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var stub: CatalogServiceGrpcKt.CatalogServiceCoroutineStub

    private lateinit var testRegion: RegionEntity
    private lateinit var testProduct: ProductEntity

    @BeforeEach
    fun setUp() {
        testRegion =
            regionRepository.findByName("Центральный ФО")
                ?: regionRepository.save(RegionEntity(name = "Центральный ФО"))
        testProduct =
            productRepository.findByBarcode("4609999999999")
                ?: productRepository.save(
                    ProductEntity(
                        name = "Овсянка 500г",
                        barcode = "4609999999999",
                        category = "GRAINS",
                        caloriesPer100g = BigDecimal("350.00"),
                        proteinsPer100g = BigDecimal("12.00"),
                        fatsPer100g = BigDecimal("6.00"),
                        carbsPer100g = BigDecimal("60.00"),
                    ),
                )
        regionalPriceRepository.deleteAll(regionalPriceRepository.findByProductId(testProduct.id))

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
        channel.shutdownNow()
        server.shutdownNow()
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

    @Test
    fun `upsertPrice by unauthenticated caller returns UNAUTHENTICATED`() =
        runTest {
            val request =
                UpsertPriceRequest
                    .newBuilder()
                    .setProductId(testProduct.id.toString())
                    .setRegionId(testRegion.id.toString())
                    .setPrice(89.90)
                    .build()

            val exception =
                assertThrowsSuspend<StatusException> {
                    stub.upsertPrice(request)
                }

            assertEquals(Status.Code.UNAUTHENTICATED, exception.status.code)
        }

    @Test
    fun `upsertPrice by regular user without admin role returns PERMISSION_DENIED`() =
        runTest {
            val client = stubWithAuth(userId = "usr_regular_1", roles = "ROLE_USER")

            val request =
                UpsertPriceRequest
                    .newBuilder()
                    .setProductId(testProduct.id.toString())
                    .setRegionId(testRegion.id.toString())
                    .setPrice(89.90)
                    .build()

            val exception =
                assertThrowsSuspend<StatusException> {
                    client.upsertPrice(request)
                }

            assertEquals(Status.Code.PERMISSION_DENIED, exception.status.code)
        }

    @Test
    fun `upsertPrice by admin role succeeds and creates regional price`() =
        runTest {
            val client = stubWithAuth(userId = "usr_admin_1", roles = "ROLE_ADMIN,ROLE_USER")

            val request =
                UpsertPriceRequest
                    .newBuilder()
                    .setProductId(testProduct.id.toString())
                    .setRegionId(testRegion.id.toString())
                    .setPrice(89.90)
                    .setCurrency("RUB")
                    .setUnit("PIECE")
                    .build()

            val response = client.upsertPrice(request)
            assertNotNull(response.price.id)
            assertEquals(89.90, response.price.price, 0.001)
            assertEquals("RUB", response.price.currency)
            assertEquals("PIECE", response.price.unit)
            assertEquals(testProduct.id.toString(), response.price.productId)
            assertEquals(testRegion.id.toString(), response.price.regionId)
        }

    @Test
    fun `upsertPrice by service role succeeds and updates existing price`() =
        runTest {
            val client = stubWithAuth(userId = "srv_catalog_crawler", roles = "ROLE_SERVICE")

            val request1 =
                UpsertPriceRequest
                    .newBuilder()
                    .setProductId(testProduct.id.toString())
                    .setRegionId(testRegion.id.toString())
                    .setPrice(95.00)
                    .build()
            val resp1 = client.upsertPrice(request1)

            val request2 =
                UpsertPriceRequest
                    .newBuilder()
                    .setProductId(testProduct.id.toString())
                    .setRegionId(testRegion.id.toString())
                    .setPrice(85.50)
                    .build()
            val resp2 = client.upsertPrice(request2)

            assertEquals(resp1.price.id, resp2.price.id)
            assertEquals(85.50, resp2.price.price, 0.001)
        }

    @Test
    fun `getProduct can be queried without authentication`() =
        runTest {
            val request =
                GetProductRequest
                    .newBuilder()
                    .setProductId(testProduct.id.toString())
                    .build()

            val response = stub.getProduct(request)
            assertEquals(testProduct.id.toString(), response.product.id)
            assertEquals("Овсянка 500г", response.product.name)
            assertEquals("GRAINS", response.product.category)
            assertEquals(350.00, response.product.nutrition.caloriesPer100G, 0.001)
        }

    @Test
    fun `getAveragePrices can be queried without authentication`() =
        runTest {
            val client = stubWithAuth(userId = "admin_user", roles = "ROLE_ADMIN")
            client.upsertPrice(
                UpsertPriceRequest
                    .newBuilder()
                    .setProductId(testProduct.id.toString())
                    .setRegionId(testRegion.id.toString())
                    .setPrice(100.00)
                    .build(),
            )

            val request =
                GetAveragePricesRequest
                    .newBuilder()
                    .addProductIds(testProduct.id.toString())
                    .build()

            val response = stub.getAveragePrices(request)
            assertEquals(1, response.pricesCount)
            val avgPrice = response.getPrices(0)
            assertEquals(testProduct.id.toString(), avgPrice.productId)
            assertEquals(100.00, avgPrice.averagePrice, 0.001)
            assertEquals(1, avgPrice.regionCount)
        }
}
