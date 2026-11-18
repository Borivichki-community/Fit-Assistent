package dev.andre.nutritionist.catalog.grpc

import com.budgetntr.proto.catalog.v1.NumericRange
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import dev.andre.nutritionist.catalog.domain.NumericBounds
import dev.andre.nutritionist.catalog.domain.SearchFilter

fun SearchProductsRequest.toFilter(): SearchFilter {
    require(query.length <= 200 && category.length <= 100) { "Search text is too long" }
    val code = currency.ifEmpty { "RUB" }
    require(code.matches(Regex("[A-Z]{3}"))) { "Invalid currency" }
    val minPrice = if (hasMinPricePer100GMinor()) minPricePer100GMinor else null
    val maxPrice = if (hasMaxPricePer100GMinor()) maxPricePer100GMinor else null
    require(minPrice == null || minPrice in 0..100_000_000_000_000_000L) { "Invalid minimum price" }
    require(maxPrice == null || maxPrice in 0..100_000_000_000_000_000L) { "Invalid maximum price" }
    require(minPrice == null || maxPrice == null || minPrice <= maxPrice) { "Price range is reversed" }
    return SearchFilter(
        query.trim(),
        category.trim(),
        calories.bounds(1000.0),
        proteins.bounds(100.0),
        fats.bounds(100.0),
        carbs.bounds(100.0),
        minPrice,
        maxPrice,
        code,
    )
}

private fun NumericRange.bounds(upper: Double): NumericBounds {
    val lower = if (hasMin()) min else null
    val higher = if (hasMax()) max else null
    require(lower == null || (lower.isFinite() && lower in 0.0..upper)) { "Invalid nutrition minimum" }
    require(higher == null || (higher.isFinite() && higher in 0.0..upper)) { "Invalid nutrition maximum" }
    require(lower == null || higher == null || lower <= higher) { "Nutrition range is reversed" }
    return NumericBounds(lower, higher)
}
