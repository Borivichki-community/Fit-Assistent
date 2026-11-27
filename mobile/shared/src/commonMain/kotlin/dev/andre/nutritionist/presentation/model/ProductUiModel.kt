package dev.andre.nutritionist.presentation.model

import dev.andre.nutritionist.domain.model.ProductId

data class ProductUiModel(
    val id: ProductId,
    val name: String,
    val category: String,
    val caloriesLabel: String,
    val proteinsLabel: String,
    val fatsLabel: String,
    val carbsLabel: String,
    val macrosLabel: String,
    val priceLabel: String,
    val isFavorite: Boolean = false,
)
