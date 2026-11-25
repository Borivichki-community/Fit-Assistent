package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.local.dao.FavoriteProductDao
import dev.andre.nutritionist.data.mapper.FavoriteProductEntityMapper.toDomain
import dev.andre.nutritionist.data.mapper.FavoriteProductEntityMapper.toFavoriteEntity
import dev.andre.nutritionist.data.remote.CatalogRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RegionId
import dev.andre.nutritionist.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CatalogRepositoryImpl(
    private val remoteDataSource: CatalogRemoteDataSource,
    private val favoriteProductDao: FavoriteProductDao,
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

    override suspend fun toggleFavoriteProduct(product: Product): Result<Boolean> =
        withContext(dispatchers.io) {
            try {
                val alreadyFavorite = favoriteProductDao.existsById(product.id.raw)
                if (alreadyFavorite) {
                    favoriteProductDao.deleteById(product.id.raw)
                    Result.success(false)
                } else {
                    favoriteProductDao.upsert(product.toFavoriteEntity())
                    Result.success(true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun observeFavoriteProducts(): Flow<List<Product>> =
        favoriteProductDao.observeAll()
            .distinctUntilChanged()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
}
