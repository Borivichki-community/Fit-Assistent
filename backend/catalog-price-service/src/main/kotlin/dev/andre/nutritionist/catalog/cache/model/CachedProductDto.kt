package dev.andre.nutritionist.catalog.cache.model

import com.budgetntr.proto.catalog.v1.NutritionFacts
import com.budgetntr.proto.catalog.v1.ProductItem
import com.budgetntr.proto.catalog.v1.RegionalPriceItem
import com.google.protobuf.Timestamp

data class CachedProductDto(
    val id: String = "",
    val name: String = "",
    val barcode: String? = null,
    val category: String = "",
    val caloriesPer100g: Double = 0.0,
    val proteinsPer100g: Double = 0.0,
    val fatsPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val version: Long = 0,
    val createdAtEpochMilli: Long = 0,
    val prices: List<CachedRegionalPriceDto> = emptyList(),
) {
    fun toProto(): ProductItem {
        val builder =
            ProductItem
                .newBuilder()
                .setId(id)
                .setName(name)
                .setCategory(category)
                .setNutrition(
                    NutritionFacts
                        .newBuilder()
                        .setCaloriesPer100G(caloriesPer100g)
                        .setProteinsPer100G(proteinsPer100g)
                        .setFatsPer100G(fatsPer100g)
                        .setCarbsPer100G(carbsPer100g)
                        .build(),
                ).setVersion(version)
                .setCreatedAt(
                    Timestamp
                        .newBuilder()
                        .setSeconds(createdAtEpochMilli / 1000)
                        .setNanos(((createdAtEpochMilli % 1000) * 1_000_000).toInt())
                        .build(),
                )

        barcode?.let { builder.setBarcode(it) }
        prices.forEach { builder.addPrices(it.toProto()) }
        return builder.build()
    }

    companion object {
        fun fromProto(proto: ProductItem): CachedProductDto =
            CachedProductDto(
                id = proto.id,
                name = proto.name,
                barcode = if (proto.barcode.isNotBlank()) proto.barcode else null,
                category = proto.category,
                caloriesPer100g = proto.nutrition.caloriesPer100G,
                proteinsPer100g = proto.nutrition.proteinsPer100G,
                fatsPer100g = proto.nutrition.fatsPer100G,
                carbsPer100g = proto.nutrition.carbsPer100G,
                version = proto.version,
                createdAtEpochMilli = proto.createdAt.seconds * 1000 + proto.createdAt.nanos / 1_000_000,
                prices = proto.pricesList.map { CachedRegionalPriceDto.fromProto(it) },
            )
    }
}

data class CachedRegionalPriceDto(
    val id: String = "",
    val productId: String = "",
    val regionId: String = "",
    val price: Double = 0.0,
    val currency: String = "RUB",
    val unit: String = "PIECE",
    val updatedAtEpochMilli: Long = 0,
    val regionName: String = "",
) {
    fun toProto(): RegionalPriceItem =
        RegionalPriceItem
            .newBuilder()
            .setId(id)
            .setProductId(productId)
            .setRegionId(regionId)
            .setPrice(price)
            .setCurrency(currency)
            .setUnit(unit)
            .setRegionName(regionName)
            .setUpdatedAt(
                Timestamp
                    .newBuilder()
                    .setSeconds(updatedAtEpochMilli / 1000)
                    .setNanos(((updatedAtEpochMilli % 1000) * 1_000_000).toInt())
                    .build(),
            ).build()

    companion object {
        fun fromProto(proto: RegionalPriceItem): CachedRegionalPriceDto =
            CachedRegionalPriceDto(
                id = proto.id,
                productId = proto.productId,
                regionId = proto.regionId,
                price = proto.price,
                currency = proto.currency,
                unit = proto.unit,
                regionName = proto.regionName,
                updatedAtEpochMilli = proto.updatedAt.seconds * 1000 + proto.updatedAt.nanos / 1_000_000,
            )
    }
}
