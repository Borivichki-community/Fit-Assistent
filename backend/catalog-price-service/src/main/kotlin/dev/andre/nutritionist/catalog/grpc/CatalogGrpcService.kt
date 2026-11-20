package dev.andre.nutritionist.catalog.grpc

import com.budgetntr.proto.catalog.v1.AveragePriceItem
import com.budgetntr.proto.catalog.v1.CatalogServiceGrpcKt
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetAveragePricesResponse
import com.budgetntr.proto.catalog.v1.GetProductRequest
import com.budgetntr.proto.catalog.v1.GetProductResponse
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import com.budgetntr.proto.catalog.v1.SearchProductsResponse
import com.budgetntr.proto.catalog.v1.UpsertPriceRequest
import com.budgetntr.proto.catalog.v1.UpsertPriceResponse
import dev.andre.nutritionist.catalog.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.catalog.grpc.mapper.toProto
import dev.andre.nutritionist.catalog.grpc.security.currentAuthContext
import dev.andre.nutritionist.catalog.service.CatalogService
import io.grpc.Status
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.service.GrpcService
import java.math.BigDecimal
import java.util.UUID

@GrpcService
class CatalogGrpcService(
    private val catalogService: CatalogService,
    private val dispatchers: CoroutineDispatchers,
) : CatalogServiceGrpcKt.CatalogServiceCoroutineImplBase() {
    private val logger = LoggerFactory.getLogger(CatalogGrpcService::class.java)

    override suspend fun getProduct(request: GetProductRequest): GetProductResponse {
        val productId =
            try {
                UUID.fromString(request.productId)
            } catch (_: IllegalArgumentException) {
                throw Status.INVALID_ARGUMENT
                    .withDescription("Invalid productId format: ${request.productId}")
                    .asException()
            }

        val regionId =
            if (request.hasRegionId() && request.regionId.isNotBlank()) {
                try {
                    UUID.fromString(request.regionId)
                } catch (_: IllegalArgumentException) {
                    throw Status.INVALID_ARGUMENT
                        .withDescription("Invalid regionId format: ${request.regionId}")
                        .asException()
                }
            } else {
                null
            }

        return withContext(dispatchers.io) {
            logger.info("getProduct called: productId={}, regionId={}", productId, regionId)
            val product = catalogService.getProductWithPrices(productId)
            GetProductResponse
                .newBuilder()
                .setProduct(product.toProto(regionId))
                .build()
        }
    }

    override suspend fun getAveragePrices(request: GetAveragePricesRequest): GetAveragePricesResponse {
        val productIds =
            request.productIdsList.mapNotNull { idStr ->
                try {
                    UUID.fromString(idStr)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }

        val regionId =
            if (request.hasRegionId() && request.regionId.isNotBlank()) {
                try {
                    UUID.fromString(request.regionId)
                } catch (_: IllegalArgumentException) {
                    null
                }
            } else {
                null
            }

        return withContext(dispatchers.io) {
            val results = catalogService.getAveragePrices(productIds, regionId)
            val items =
                results.map {
                    AveragePriceItem
                        .newBuilder()
                        .setProductId(it.productId.toString())
                        .setAveragePrice(it.averagePrice.toDouble())
                        .setCurrency(it.currency)
                        .setRegionCount(it.regionCount)
                        .build()
                }
            GetAveragePricesResponse
                .newBuilder()
                .addAllPrices(items)
                .build()
        }
    }

    override suspend fun upsertPrice(request: UpsertPriceRequest): UpsertPriceResponse {
        checkAdminOrServiceAccess()

        val productId =
            try {
                UUID.fromString(request.productId)
            } catch (_: IllegalArgumentException) {
                throw Status.INVALID_ARGUMENT
                    .withDescription("Invalid productId format: ${request.productId}")
                    .asException()
            }

        val regionId =
            try {
                UUID.fromString(request.regionId)
            } catch (_: IllegalArgumentException) {
                throw Status.INVALID_ARGUMENT
                    .withDescription("Invalid regionId format: ${request.regionId}")
                    .asException()
            }

        if (request.price <= 0.0) {
            throw Status.INVALID_ARGUMENT
                .withDescription("Price must be positive, got: ${request.price}")
                .asException()
        }

        val auth = currentAuthContext()
        val userId = auth.userId ?: "unknown"

        return withContext(MDCContext(mapOf("userId" to userId)) + dispatchers.io) {
            logger.info(
                "upsertPrice called by userId={}: productId={}, regionId={}, price={}",
                userId,
                productId,
                regionId,
                request.price,
            )

            val priceEntity =
                catalogService.upsertRegionalPrice(
                    productId = productId,
                    regionId = regionId,
                    price = BigDecimal.valueOf(request.price),
                    currency = request.currency.ifBlank { "RUB" },
                    unit = request.unit.ifBlank { "PIECE" },
                )

            UpsertPriceResponse
                .newBuilder()
                .setPrice(priceEntity.toProto())
                .build()
        }
    }

    override suspend fun searchProducts(request: SearchProductsRequest): SearchProductsResponse {
        val regionId =
            if (request.hasRegionId() && request.regionId.isNotBlank()) {
                try {
                    UUID.fromString(request.regionId)
                } catch (_: IllegalArgumentException) {
                    null
                }
            } else {
                null
            }

        return withContext(dispatchers.io) {
            val products =
                if (request.category.isNotBlank()) {
                    catalogService.findProductsByCategory(request.category)
                } else if (request.hasMinCalories() && request.hasMaxCalories() && request.hasMinProteins()) {
                    catalogService.findProductsByNutrition(
                        minCalories = BigDecimal.valueOf(request.minCalories),
                        maxCalories = BigDecimal.valueOf(request.maxCalories),
                        minProteins = BigDecimal.valueOf(request.minProteins),
                    )
                } else {
                    emptyList()
                }

            val items = products.map { it.toProto(regionId) }
            SearchProductsResponse
                .newBuilder()
                .addAllProducts(items)
                .build()
        }
    }

    private fun checkAdminOrServiceAccess() {
        val auth = currentAuthContext()
        if (!auth.isAuthenticated || auth.userId.isNullOrBlank()) {
            throw Status.UNAUTHENTICATED
                .withDescription("Authentication required")
                .asException()
        }
        if (!auth.isAdmin) {
            throw Status.PERMISSION_DENIED
                .withDescription("Access denied: only ADMIN or SERVICE role can update prices")
                .asException()
        }
    }
}
