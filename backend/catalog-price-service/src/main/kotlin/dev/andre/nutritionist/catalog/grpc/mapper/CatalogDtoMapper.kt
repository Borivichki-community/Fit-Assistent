package dev.andre.nutritionist.catalog.grpc.mapper

import com.budgetntr.proto.catalog.v1.NutritionFacts
import com.budgetntr.proto.catalog.v1.ProductItem
import com.budgetntr.proto.catalog.v1.RegionalPriceItem
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.catalog.persistence.entity.ProductEntity
import dev.andre.nutritionist.catalog.persistence.entity.RegionalPriceEntity
import java.time.Instant
import java.util.UUID

fun Instant.toProtoTimestamp(): Timestamp =
    Timestamp
        .newBuilder()
        .setSeconds(epochSecond)
        .setNanos(nano)
        .build()

fun RegionalPriceEntity.toProto(): RegionalPriceItem =
    RegionalPriceItem
        .newBuilder()
        .setId(id.toString())
        .setProductId(product.id.toString())
        .setRegionId(region.id.toString())
        .setRegionName(region.name)
        .setPrice(price.toDouble())
        .setCurrency(currency)
        .setUnit(unit)
        .setUpdatedAt(updatedAt.toProtoTimestamp())
        .build()

fun ProductEntity.toProto(regionFilterId: UUID? = null): ProductItem {
    val builder =
        ProductItem
            .newBuilder()
            .setId(id.toString())
            .setName(name)
            .setBarcode(barcode ?: "")
            .setCategory(category)
            .setNutrition(
                NutritionFacts
                    .newBuilder()
                    .setCaloriesPer100G(caloriesPer100g.toDouble())
                    .setProteinsPer100G(proteinsPer100g.toDouble())
                    .setFatsPer100G(fatsPer100g.toDouble())
                    .setCarbsPer100G(carbsPer100g.toDouble())
                    .build(),
            ).setVersion(version)
            .setCreatedAt(createdAt.toProtoTimestamp())

    val filteredPrices =
        if (regionFilterId != null) {
            prices.filter { it.region.id == regionFilterId }
        } else {
            prices
        }
    builder.addAllPrices(filteredPrices.map { it.toProto() })

    return builder.build()
}
