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

        val criteria =
            dev.andre.nutritionist.catalog.persistence.repository.ProductSearchCriteria(
                query = request.query.takeIf { it.isNotBlank() },
                category = request.category.takeIf { it.isNotBlank() },
                minCalories = if (request.hasMinCalories()) BigDecimal.valueOf(request.minCalories) else null,
                maxCalories = if (request.hasMaxCalories()) BigDecimal.valueOf(request.maxCalories) else null,
                minProteins = if (request.hasMinProteins()) BigDecimal.valueOf(request.minProteins) else null,
                maxProteins = if (request.hasMaxProteins()) BigDecimal.valueOf(request.maxProteins) else null,
                pageSize = if (request.pageSize > 0) request.pageSize else 20,
                pageToken = request.pageToken.takeIf { it.isNotBlank() },
                sortBy = if (request.hasSortBy() && request.sortBy.isNotBlank()) request.sortBy else "name",
            )

        return withContext(dispatchers.io) {
            val result = catalogService.searchProducts(criteria)
            val items = result.items.map { it.toProto(regionId) }
            SearchProductsResponse
                .newBuilder()
                .addAllProducts(items)
                .setNextPageToken(result.nextPageToken)
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
