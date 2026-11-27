package dev.andre.nutritionist.presentation.mappers

import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.presentation.formatters.MoneyFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter
import dev.andre.nutritionist.presentation.model.ProductUiModel

object ProductUiMapper {

    fun Product.toUiModel(isFavorite: Boolean = false): ProductUiModel {
        val calories = NutritionFormatter.formatCalories(nutritionPer100g.calories)
        val proteins = NutritionFormatter.formatGram(nutritionPer100g.proteins)
        val fats = NutritionFormatter.formatGram(nutritionPer100g.fats)
        val carbs = NutritionFormatter.formatGram(nutritionPer100g.carbs)
        val firstPrice = prices.firstOrNull()?.price
        val priceLabel = if (firstPrice != null) {
            "${MoneyFormatter.format(firstPrice.amount)} / ${firstPrice.unit}"
        } else {
            "Цена не указана"
        }

        return ProductUiModel(
            id = id,
            name = name,
            category = category,
            caloriesLabel = calories,
            proteinsLabel = proteins,
            fatsLabel = fats,
            carbsLabel = carbs,
            macrosLabel = "Б $proteins • Ж $fats • У $carbs",
            priceLabel = priceLabel,
            isFavorite = isFavorite,
        )
    }
}
