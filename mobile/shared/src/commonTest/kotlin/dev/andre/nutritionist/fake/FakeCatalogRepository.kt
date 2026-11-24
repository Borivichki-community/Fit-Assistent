package dev.andre.nutritionist.fake

import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RegionId
import dev.andre.nutritionist.domain.repository.CatalogRepository

class FakeCatalogRepository : CatalogRepository {

    // mutable by design: test control
    val products = mutableMapOf<ProductId, Product>()
    val averagePrices = mutableMapOf<ProductId, Money>()
    var shouldThrow: Throwable? = null

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
        val error = shouldThrow
        if (error != null) return Result.failure(error)

        val filtered = products.values.filter { product ->
            (query.isBlank() || product.name.contains(query, ignoreCase = true)) &&
                (category.isBlank() || product.category.equals(category, ignoreCase = true))
        }
        return Result.success(Pair(filtered, ""))
    }

    override suspend fun getProduct(id: ProductId, regionId: RegionId?): Result<Product> {
        val error = shouldThrow
        if (error != null) return Result.failure(error)

        val product = products[id] ?: return Result.failure(NoSuchElementException("Product $id not found"))
        return Result.success(product)
    }

    override suspend fun getAveragePrices(
        productIds: List<ProductId>,
        regionId: RegionId?,
    ): Result<Map<ProductId, Money>> {
        val error = shouldThrow
        if (error != null) return Result.failure(error)

        val result = productIds.mapNotNull { id ->
            val price = averagePrices[id]
            if (price != null) id to price else null
        }.toMap()

        return Result.success(result)
    }

    fun reset() {
        products.clear()
        averagePrices.clear()
        shouldThrow = null
    }
}
