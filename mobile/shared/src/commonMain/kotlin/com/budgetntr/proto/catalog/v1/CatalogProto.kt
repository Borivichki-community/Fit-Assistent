package com.budgetntr.proto.catalog.v1

import com.budgetntr.proto.profile.v1.TimestampProto

data class CatalogNutritionFactsProto(
    val caloriesPer100g: Double = 0.0,
    val proteinsPer100g: Double = 0.0,
    val fatsPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
)

data class RegionalPriceItemProto(
    val id: String = "",
    val productId: String = "",
    val regionId: String = "",
    val price: Double = 0.0,
    val currency: String = "RUB",
    val unit: String = "кг",
    val updatedAt: TimestampProto = TimestampProto(),
    val regionName: String = "",
)

data class ProductItemProto(
    val id: String = "",
    val name: String = "",
    val barcode: String = "",
    val category: String = "",
    val nutrition: CatalogNutritionFactsProto = CatalogNutritionFactsProto(),
    val version: Long = 0L,
    val createdAt: TimestampProto = TimestampProto(),
    val prices: List<RegionalPriceItemProto> = emptyList(),
)

data class SearchProductsRequest(
    val query: String = "",
    val category: String = "",
    val minCalories: Double? = null,
    val maxCalories: Double? = null,
    val minProteins: Double? = null,
    val regionId: String? = null,
    val pageSize: Int = 20,
    val pageToken: String = "",
    val maxProteins: Double? = null,
    val sortBy: String? = null,
)

data class SearchProductsResponse(
    val products: List<ProductItemProto> = emptyList(),
    val nextPageToken: String = "",
)

data class GetProductRequest(
    val productId: String = "",
    val regionId: String? = null,
)

data class GetProductResponse(
    val product: ProductItemProto = ProductItemProto(),
)

data class AveragePriceItemProto(
    val productId: String = "",
    val averagePrice: Double = 0.0,
    val currency: String = "RUB",
    val regionCount: Int = 0,
)

data class GetAveragePricesRequest(
    val productIds: List<String> = emptyList(),
    val regionId: String? = null,
)

data class GetAveragePricesResponse(
    val prices: List<AveragePriceItemProto> = emptyList(),
)
