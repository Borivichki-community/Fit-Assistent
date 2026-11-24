package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RegionId

interface CatalogRepository {
    suspend fun searchProducts(
        query: String = "",
        category: String = "",
        minCalories: Double? = null,
        maxCalories: Double? = null,
        minProteins: Double? = null,
        maxProteins: Double? = null,
        regionId: RegionId? = null,
        pageSize: Int = 20,
        pageToken: String = "",
    ): Result<Pair<List<Product>, String>>

    suspend fun getProduct(id: ProductId, regionId: RegionId? = null): Result<Product>

    suspend fun getAveragePrices(productIds: List<ProductId>, regionId: RegionId? = null): Result<Map<ProductId, Money>>
}
