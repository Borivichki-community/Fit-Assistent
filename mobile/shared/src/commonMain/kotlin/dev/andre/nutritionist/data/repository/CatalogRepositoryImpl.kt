package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.remote.CatalogRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RegionId
import dev.andre.nutritionist.domain.repository.CatalogRepository
import kotlinx.coroutines.withContext

class CatalogRepositoryImpl(
    private val remoteDataSource: CatalogRemoteDataSource,
    private val dispatchers: CoroutineDispatchers,
) : CatalogRepository {

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
        remoteDataSource.searchProducts(
            query = query,
            category = category,
            minCalories = minCalories,
            maxCalories = maxCalories,
            minProteins = minProteins,
            maxProteins = maxProteins,
            regionId = regionId,
            pageSize = pageSize,
            pageToken = pageToken,
        )
    }

    override suspend fun getProduct(id: ProductId, regionId: RegionId?): Result<Product> =
        withContext(dispatchers.io) {
            remoteDataSource.getProduct(id, regionId)
        }

    override suspend fun getAveragePrices(
        productIds: List<ProductId>,
        regionId: RegionId?,
    ): Result<Map<ProductId, Money>> = withContext(dispatchers.io) {
        remoteDataSource.getAveragePrices(productIds, regionId)
    }
}
