package dev.andre.nutritionist.recipe

import com.budgetntr.proto.recipe.v1.MealType
import com.budgetntr.proto.recipe.v1.MenuIngredient
import com.budgetntr.proto.recipe.v1.MenuMeal
import com.budgetntr.proto.recipe.v1.WeekMenu
import com.budgetntr.proto.recipe.v1.WeekMenuDay
import com.budgetntr.proto.recipe.v1.WeekMenuSummary
import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class InvalidMenu(
    message: String,
) : RuntimeException(message)

@Component
class MenuValidator(
    private val json: JsonMapper,
    private val settings: RecipeProperties,
) {
    fun validate(
        response: String,
        request: MenuRequest,
        products: List<CatalogProduct>,
    ): WeekMenu {
        val root =
            try {
                json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(response)
            } catch (_: JacksonException) {
                throw InvalidMenu("Provider returned malformed menu JSON")
            }
        val dayNodes = root.path("days").elementsList(WEEK_DAYS..WEEK_DAYS, "days")
        val byId = products.associateBy { it.id }
        val days = dayNodes.map { day(it, byId) }.sortedBy { it.dayNumber }
        checked(days.map { it.dayNumber } == (1..WEEK_DAYS).toList(), "Expected seven distinct day numbers")
        checked(days.all { withinCalorieRange(it.calories, request.dailyCalories) }, "Daily calories are outside tolerance")
        val cost = days.sumOf { it.estimatedCostMinor }
        checked(cost <= request.weeklyBudgetMinor, "Menu exceeds the weekly budget")
        return WeekMenu
            .newBuilder()
            .addAllDays(days)
            .setSummary(
                WeekMenuSummary
                    .newBuilder()
                    .setCalories(days.sumOf { it.calories })
                    .setEstimatedCostMinor(cost)
                    .setCurrency(CURRENCY),
            ).build()
    }

    fun withinCalorieRange(
        actual: Double,
        target: Int,
    ): Boolean = kotlin.math.abs(actual - target) <= target * settings.calorieTolerance

    private fun day(
        node: JsonNode,
        products: Map<ProductId, CatalogProduct>,
    ): WeekMenuDay {
        val number = node.path("day").integer(1..WEEK_DAYS, "day")
        val meals =
            node
                .path("meals")
                .elementsList(3..3, "meals")
                .map { meal(it, products) }
                .sortedBy { it.type.number }
        checked(meals.map { it.type }.toSet() == mealTypes.values.toSet(), "Expected breakfast, lunch and dinner")
        return WeekMenuDay
            .newBuilder()
            .setDayNumber(number)
            .addAllMeals(meals)
            .setCalories(meals.sumOf { it.calories })
            .setEstimatedCostMinor(meals.sumOf { it.estimatedCostMinor })
            .build()
    }

    private fun meal(
        node: JsonNode,
        products: Map<ProductId, CatalogProduct>,
    ): MenuMeal {
        val type = mealTypes[node.path("type").asString("")] ?: throw InvalidMenu("Unknown meal type")
        val name =
            node
                .path("name")
                .takeIf { it.isString }
                ?.asString()
                ?.trim()
                .orEmpty()
        checked(name.isNotBlank() && name.length <= 120, "Invalid meal name")
        val portions =
            node.path("products").elementsList(1..8, "products").map { item ->
                val id = ProductId(item.path("id").asString(""))
                val product = products[id] ?: throw InvalidMenu("Product is outside the catalog candidates")
                product to item.path("grams").integer(1..2_000, "grams")
            }
        checked(portions.map { it.first.id }.distinct().size == portions.size, "Duplicate product in meal")
        return MenuMeal
            .newBuilder()
            .setType(type)
            .setName(name)
            .setCalories(portions.sumOf { (product, grams) -> product.caloriesPer100g * grams / 100 })
            .setEstimatedCostMinor(portions.sumOf { (product, grams) -> (product.pricePer100gMinor * grams + 50) / 100 })
            .addAllIngredients(
                portions.map { (product, grams) ->
                    MenuIngredient
                        .newBuilder()
                        .setProductId(product.id.raw)
                        .setName(product.name)
                        .setGrams(grams)
                        .build()
                },
            ).build()
    }

    private fun JsonNode.elementsList(
        size: IntRange,
        field: String,
    ): List<JsonNode> {
        checked(isArray && size() in size, "Invalid $field count")
        return toList()
    }

    private fun JsonNode.integer(
        range: IntRange,
        field: String,
    ): Int {
        checked(isIntegralNumber && canConvertToInt() && asInt() in range, "Invalid $field")
        return asInt()
    }

    private fun checked(
        condition: Boolean,
        message: String,
    ) {
        if (!condition) throw InvalidMenu(message)
    }

    companion object {
        private val mealTypes =
            mapOf("breakfast" to MealType.MEAL_TYPE_BREAKFAST, "lunch" to MealType.MEAL_TYPE_LUNCH, "dinner" to MealType.MEAL_TYPE_DINNER)
    }
}
