package dev.andre.nutritionist.catalog.grpc

import com.budgetntr.proto.catalog.v1.CatalogServiceGrpcKt
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetAveragePricesResponse
import com.budgetntr.proto.catalog.v1.GetProductRequest
import com.budgetntr.proto.catalog.v1.GetProductResponse
import com.budgetntr.proto.catalog.v1.Price
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import com.budgetntr.proto.catalog.v1.SearchProductsResponse
import com.budgetntr.proto.catalog.v1.UpsertPriceRequest
import com.budgetntr.proto.catalog.v1.UpsertPriceResponse
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.catalog.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.catalog.domain.ProductId
import dev.andre.nutritionist.catalog.domain.StoreId
import dev.andre.nutritionist.catalog.persistence.CatalogRepository
import dev.andre.nutritionist.catalog.service.PriceService
import dev.andre.nutritionist.catalog.service.PriceWrite
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.service.GrpcService
import java.util.UUID

@GrpcService
class CatalogGrpcService(
    private val repository: CatalogRepository,
    private val prices: PriceService,
    private val dispatchers: CoroutineDispatchers,
    private val cache: dev.andre.nutritionist.catalog.cache.CatalogCache,
) : CatalogServiceGrpcKt.CatalogServiceCoroutineImplBase() {
    override suspend fun getProduct(request: GetProductRequest): GetProductResponse =
        rpc {
            val id = productId(request.productId)
            cache.getOrLoad("product", id.raw.toByteArray(), GetProductResponse::parseFrom, { it.toByteArray() }) {
                val product =
                    repository.getProduct(id)
                        ?: throw Status.NOT_FOUND.withDescription("Product not found").asRuntimeException()
                GetProductResponse.newBuilder().setProduct(product.toProto()).build()
            }
        }

    override suspend fun searchProducts(request: SearchProductsRequest): SearchProductsResponse =
        rpc {
            require(request.pageSize in 0..100) { "page_size must be 0..100" }
            val limit = request.pageSize.takeIf { it > 0 } ?: 50
            val filter = request.toFilter()
            val after = SearchCursor.decode(request)
            cache.getOrLoad(
                "search",
                versioned(request.toByteArray()),
                SearchProductsResponse::parseFrom,
                { it.toByteArray() },
            ) {
                val rows = repository.search(filter, after, limit + 1)
                val page = rows.take(limit)
                SearchProductsResponse
                    .newBuilder()
                    .addAllProducts(page.map { it.toProto() })
                    .setNextPageToken(if (rows.size > limit) SearchCursor.encode(page.last().id, request) else "")
                    .build()
            }
        }

    override suspend fun getAveragePrices(request: GetAveragePricesRequest): GetAveragePricesResponse =
        rpc {
            require(request.productIdsCount in 1..100) { "product_ids must contain 1..100 IDs" }
            val currency = currency(request.currency)
            val ids = request.productIdsList.map(::productId).distinct()
            cache.getOrLoad(
                "average",
                versioned(request.toByteArray()),
                GetAveragePricesResponse::parseFrom,
                { it.toByteArray() },
            ) {
                GetAveragePricesResponse
                    .newBuilder()
                    .addAllPrices(repository.averagePrices(ids, currency).map { it.toProto() })
                    .build()
            }
        }

    override suspend fun upsertPrice(request: UpsertPriceRequest): UpsertPriceResponse =
        rpc {
            val principal = checkNotNull(CatalogSecurityInterceptor.PRINCIPAL.get())
            if (!principal.admin) throw Status.PERMISSION_DENIED.withDescription("Admin role required").asRuntimeException()
            val key =
                principal.idempotencyKey
                    ?: throw IllegalArgumentException("idempotency-key is required")
            require(key.raw.matches(Regex("[A-Za-z0-9._:-]{1,128}"))) { "Invalid idempotency-key" }
            val command =
                PriceWrite(
                    productId(request.productId),
                    StoreId(uuid(request.storeId)),
                    request.unitPriceMinor,
                    currency(request.currency),
                    request.priceBasisMilligrams,
                )
            require(command.amount in 0..1_000_000_000_000L) { "Invalid unit_price_minor" }
            require(command.basis in 1..1_000_000_000_000L) { "Invalid price_basis_milligrams" }
            val result =
                prices.upsert(principal.userId, key, command, request.toByteArray()) { instant ->
                    UpsertPriceResponse
                        .newBuilder()
                        .setPrice(
                            Price
                                .newBuilder()
                                .setProductId(command.productId.raw)
                                .setStoreId(command.storeId.raw)
                                .setUnitPriceMinor(command.amount)
                                .setCurrency(command.currency)
                                .setPriceBasisMilligrams(command.basis)
                                .setUpdatedAt(Timestamp.newBuilder().setSeconds(instant.epochSecond).setNanos(instant.nano)),
                        ).build()
                        .toByteArray()
                }
            UpsertPriceResponse.parseFrom(result)
        }

    private suspend fun <T> rpc(block: () -> T): T {
        val principal =
            CatalogSecurityInterceptor.PRINCIPAL.get()
                ?: throw Status.UNAUTHENTICATED.asRuntimeException()
        return withContext(dispatchers.io + MDCContext(mapOf("userId" to principal.userId.raw))) {
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: StatusException) {
                throw exception
            } catch (exception: StatusRuntimeException) {
                throw exception
            } catch (exception: IllegalArgumentException) {
                throw Status.INVALID_ARGUMENT.withDescription(exception.message).asRuntimeException()
            } catch (exception: Exception) {
                LoggerFactory.getLogger(CatalogGrpcService::class.java).error("Catalog request failed", exception)
                throw Status.INTERNAL.withDescription("Catalog request failed").asRuntimeException()
            }
        }
    }

    private fun versioned(request: ByteArray): ByteArray = repository.priceRevision().toString().toByteArray() + byteArrayOf(0) + request

    private fun productId(raw: String): ProductId = ProductId(uuid(raw))

    private fun uuid(raw: String): String {
        require(raw.matches(Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))) { "Invalid UUID" }
        return UUID.fromString(raw).toString()
    }

    private fun currency(raw: String): String {
        val result = raw.ifEmpty { "RUB" }
        require(result.matches(Regex("[A-Z]{3}"))) { "currency must be an uppercase ISO code" }
        return result
    }
}
