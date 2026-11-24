package dev.andre.nutritionist.data.mapper

import com.budgetntr.proto.catalog.v1.CatalogNutritionFactsProto
import com.budgetntr.proto.catalog.v1.ProductItemProto
import com.budgetntr.proto.catalog.v1.RegionalPriceItemProto
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Price
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RegionId
import dev.andre.nutritionist.domain.model.RegionalPrice

object CatalogDtoMapper {

    fun ProductItemProto.toDomain(): Product = Product(
        id = ProductId(id),
        name = name,
        barcode = barcode.ifEmpty { null },
        category = category,
        nutritionPer100g = nutrition.toDomain(),
        prices = prices.map { it.toDomain() },
    )

    fun RegionalPriceItemProto.toDomain(): RegionalPrice = RegionalPrice(
        id = id,
        productId = ProductId(productId),
        regionId = RegionId(regionId),
        price = Price(
            amount = Money.fromRubles(price),
            unit = unit.ifEmpty { "кг" },
        ),
        regionName = regionName,
    )

    fun CatalogNutritionFactsProto.toDomain(): NutritionFacts = NutritionFacts(
        calories = caloriesPer100g,
        proteins = proteinsPer100g,
        fats = fatsPer100g,
        carbs = carbsPer100g,
    )
}
