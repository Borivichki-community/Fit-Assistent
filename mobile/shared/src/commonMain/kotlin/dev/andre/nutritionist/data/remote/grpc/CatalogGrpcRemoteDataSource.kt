package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetAveragePricesResponse
import com.budgetntr.proto.catalog.v1.GetProductRequest
import com.budgetntr.proto.catalog.v1.GetProductResponse
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import com.budgetntr.proto.catalog.v1.SearchProductsResponse
import dev.andre.nutritionist.data.mapper.CatalogDtoMapper.toDomain
import dev.andre.nutritionist.data.remote.CatalogRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RegionId
import kotlinx.coroutines.withContext

interface CatalogGrpcServiceClient {
    suspend fun searchProducts(request: SearchProductsRequest): SearchProductsResponse
    suspend fun getProduct(request: GetProductRequest): GetProductResponse
    suspend fun getAveragePrices(request: GetAveragePricesRequest): GetAveragePricesResponse
}

class CatalogGrpcRemoteDataSource(
    private val client: CatalogGrpcServiceClient,
    private val dispatchers: CoroutineDispatchers,
) : CatalogRemoteDataSource {

    override suspend fun searchProducts(
        query: String,
        category: String,
        minCalories: Double?,
        maxCalories: Double?,
        minProteins: Double?,
        maxProteins: Double?,
        regionId: RegionId?,
        pageSize: Int,
        pageToken: String,
    ): Result<Pair<List<Product>, String>> = withContext(dispatchers.io) {
        runCatching {
            val request = SearchProductsRequest(
                query = query,
                category = category,
                minCalories = minCalories,
                maxCalories = maxCalories,
                minProteins = minProteins,
                maxProteins = maxProteins,
                regionId = regionId?.raw,
                pageSize = pageSize,
                pageToken = pageToken,
            )
            val response = client.searchProducts(request)
            val products = response.products.map { it.toDomain() }
            Pair(products, response.nextPageToken)
        }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
    }

    override suspend fun getProduct(id: ProductId, regionId: RegionId?): Result<Product> =
        withContext(dispatchers.io) {
            runCatching {
                val request = GetProductRequest(
                    productId = id.raw,
                    regionId = regionId?.raw,
                )
                val response = client.getProduct(request)
                response.product.toDomain()
            }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
        }

    override suspend fun getAveragePrices(
        productIds: List<ProductId>,
        regionId: RegionId?,
    ): Result<Map<ProductId, Money>> = withContext(dispatchers.io) {
        runCatching {
            val request = GetAveragePricesRequest(
                productIds = productIds.map { it.raw },
                regionId = regionId?.raw,
            )
            val response = client.getAveragePrices(request)
            response.prices.associate { item ->
                ProductId(item.productId) to Money.fromRubles(item.averagePrice)
            }
        }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
    }
}
