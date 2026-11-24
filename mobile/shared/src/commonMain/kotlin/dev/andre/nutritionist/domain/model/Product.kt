package dev.andre.nutritionist.domain.model

data class Product(
    val id: ProductId,
    val name: String,
    val barcode: String? = null,
    val category: String,
    val nutritionPer100g: NutritionFacts,
    val prices: List<RegionalPrice> = emptyList(),
)
