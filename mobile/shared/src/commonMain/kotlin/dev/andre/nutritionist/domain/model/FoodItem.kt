package dev.andre.nutritionist.domain.model

/**
 * Доменная модель продукта питания или блюда из каталога.
 * Содержит нутриенты в расчёте на 100 грамм продукта.
 */
data class FoodItem(
    val id: FoodItemId,
    val name: String,
    val brand: String = "",
    val caloriesPer100g: Int,
    val proteinsPer100g: Double,
    val fatsPer100g: Double,
    val carbsPer100g: Double,
    val defaultPortionGrams: Double = 100.0,
    val category: String = "Общее",
    val barcode: String? = null,
)
