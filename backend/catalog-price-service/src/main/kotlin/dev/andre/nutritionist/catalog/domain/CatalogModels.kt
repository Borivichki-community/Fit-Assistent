package dev.andre.nutritionist.catalog.domain

@JvmInline
value class ProductId(
    val raw: String,
)

@JvmInline
value class StoreId(
    val raw: String,
)

@JvmInline
value class IdempotencyKey(
    val raw: String,
)

@JvmInline
value class UserId(
    val raw: String,
)

data class Nutrition(
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
)

data class Product(
    val id: ProductId,
    val name: String,
    val category: String,
    val nutrition: Nutrition,
)

data class Price(
    val productId: ProductId,
    val storeId: StoreId,
    val unitPriceMinor: Long,
    val currency: String,
    val priceBasisMilligrams: Long,
    val updatedAtEpochMillis: Long,
)

data class AveragePrice(
    val productId: ProductId,
    val pricePer100gMinor: Long,
    val currency: String,
    val storeCount: Int,
)
