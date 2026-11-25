package dev.andre.nutritionist.data.mapper

import dev.andre.nutritionist.data.local.entity.FavoriteProductEntity
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Price
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RegionId
import dev.andre.nutritionist.domain.model.RegionalPrice
import kotlin.time.Clock
import kotlin.time.Instant

object FavoriteProductEntityMapper {

    fun FavoriteProductEntity.toDomain(): Product {
        val productIdValue = ProductId(productId)
        val regionalPrices = priceAmountMinor?.let { minor ->
            listOf(
                RegionalPrice(
                    id = "fav-price-${productId}",
                    productId = productIdValue,
                    regionId = RegionId(""),
                    price = Price(amount = Money.fromMinor(minor)),
                ),
            )
        } ?: emptyList()

        return Product(
            id = productIdValue,
            name = name,
            category = category,
            nutritionPer100g = NutritionFacts(
                calories = calories,
                proteins = proteins,
                fats = fats,
                carbs = carbs,
            ),
            prices = regionalPrices,
        )
    }

    fun Product.toFavoriteEntity(
        savedAt: Instant = Clock.System.now(),
    ): FavoriteProductEntity = FavoriteProductEntity(
        id = id.raw,
        productId = id.raw,
        name = name,
        category = category,
        calories = nutritionPer100g.calories,
        proteins = nutritionPer100g.proteins,
        fats = nutritionPer100g.fats,
        carbs = nutritionPer100g.carbs,
        priceAmountMinor = prices.firstOrNull()?.price?.amount?.amountMinor,
        savedAt = savedAt.toEpochMilliseconds(),
    )
}
