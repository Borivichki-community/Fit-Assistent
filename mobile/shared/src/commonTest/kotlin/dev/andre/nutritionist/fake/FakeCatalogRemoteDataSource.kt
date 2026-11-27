package dev.andre.nutritionist.fake

import dev.andre.nutritionist.data.remote.CatalogRemoteDataSource
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RegionId

class FakeCatalogRemoteDataSource : CatalogRemoteDataSource {

    // mutable by design: test control
    val products = mutableMapOf<ProductId, Product>()
    val averagePrices = mutableMapOf<ProductId, Money>()
    var shouldFail: Throwable? = null

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
    ): Result<Pair<List<Product>, String>> {
        shouldFail?.let { return Result.failure(it) }
        val filtered = products.values.filter { product ->
            (query.isBlank() || product.name.contains(query, ignoreCase = true)) &&
                (category.isBlank() || product.category.equals(category, ignoreCase = true))
        }
        return Result.success(filtered to "")
    }

    override suspend fun getProduct(id: ProductId, regionId: RegionId?): Result<Product> {
        shouldFail?.let { return Result.failure(it) }
        val product = products[id] ?: return Result.failure(NoSuchElementException("Not found: ${id.raw}"))
        return Result.success(product)
    }

    override suspend fun getAveragePrices(
        productIds: List<ProductId>,
        regionId: RegionId?,
    ): Result<Map<ProductId, Money>> {
        shouldFail?.let { return Result.failure(it) }
        return Result.success(
            productIds.mapNotNull { id -> averagePrices[id]?.let { id to it } }.toMap(),
        )
    }

    fun reset() {
        products.clear()
        averagePrices.clear()
        shouldFail = null
    }
}
