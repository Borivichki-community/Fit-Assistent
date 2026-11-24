package dev.andre.nutritionist.recipe.domain.model

import java.security.MessageDigest
import java.util.HexFormat

@JvmInline
value class UserId(
    val raw: String,
)

@JvmInline
value class ProductId(
    val raw: String,
)

data class MenuRequest(
    val dailyCalories: Int,
    val weeklyBudgetMinor: Long,
    val excluded: List<ProductId> = emptyList(),
    val preferred: List<ProductId> = emptyList(),
) {
    init {
        require(dailyCalories in 1..10_000) { "daily_calories must be 1..10000" }
        require(weeklyBudgetMinor in 1..1_000_000_000) { "weekly_budget_minor must be 1..1000000000" }
        require(excluded.size <= 50 && preferred.size <= 50) { "At most 50 excluded and 50 preferred products" }
    }

    val isTypical: Boolean
        get() = excluded.isEmpty() && preferred.isEmpty()
}

data class MealPlanRequest(
    val period: TargetPeriod,
    val targetNutrition: TargetNutrition,
    val maxBudgetMinor: Long,
    val currency: String = "RUB",
    val regionId: String? = null,
    val preferredProducts: List<String> = emptyList(),
    val excludedProducts: List<String> = emptyList(),
    val userNote: String = "",
) {
    init {
        require(maxBudgetMinor in 1..1_000_000_000) { "max_budget_minor must be 1..1000000000" }
        require(targetNutrition.calories in 1.0..10_000.0) { "calories must be 1..10000" }
    }
}

data class CatalogProduct(
    val id: ProductId,
    val name: String,
    val caloriesPer100g: Double,
    val proteinsPer100g: Double = 0.0,
    val fatsPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val pricePer100gMinor: Long,
)

data class RequestContext(
    val user: UserId,
    val traceparent: String? = null,
)

const val WEEK_DAYS = 7
const val CURRENCY = "RUB"
const val MENU_VERSION = "2"

fun digest(value: String): String =
    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toByteArray()))
