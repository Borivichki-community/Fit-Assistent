package dev.andre.nutritionist.catalog.grpc

import com.budgetntr.proto.catalog.v1.Nutrition
import com.budgetntr.proto.catalog.v1.Product
import dev.andre.nutritionist.catalog.domain.AveragePrice

fun dev.andre.nutritionist.catalog.domain.Product.toProto(): Product =
    Product
        .newBuilder()
        .setProductId(id.raw)
        .setName(name)
        .setCategory(category)
        .setNutrition(
            Nutrition
                .newBuilder()
                .setCaloriesPer100G(nutrition.calories)
                .setProteinsPer100G(nutrition.proteins)
                .setFatsPer100G(nutrition.fats)
                .setCarbsPer100G(nutrition.carbs),
        ).build()

fun AveragePrice.toProto(): com.budgetntr.proto.catalog.v1.AveragePrice =
    com.budgetntr.proto.catalog.v1.AveragePrice
        .newBuilder()
        .setProductId(productId.raw)
        .setPricePer100GMinor(pricePer100gMinor)
        .setCurrency(currency)
        .setStoreCount(storeCount)
        .build()
