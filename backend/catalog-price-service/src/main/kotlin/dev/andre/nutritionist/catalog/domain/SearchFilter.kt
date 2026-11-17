package dev.andre.nutritionist.catalog.domain

data class NumericBounds(
    val min: Double? = null,
    val max: Double? = null,
)

data class SearchFilter(
    val query: String = "",
    val category: String = "",
    val calories: NumericBounds = NumericBounds(),
    val proteins: NumericBounds = NumericBounds(),
    val fats: NumericBounds = NumericBounds(),
    val carbs: NumericBounds = NumericBounds(),
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val currency: String = "RUB",
)
