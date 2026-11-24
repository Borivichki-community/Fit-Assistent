package dev.andre.nutritionist.recipe.domain.validator

import com.budgetntr.proto.recipe.v1.MealType
import com.budgetntr.proto.recipe.v1.MenuIngredient as ProtoMenuIngredient
import com.budgetntr.proto.recipe.v1.MenuMeal
import com.budgetntr.proto.recipe.v1.MenuSource
import com.budgetntr.proto.recipe.v1.WeekMenu
import com.budgetntr.proto.recipe.v1.WeekMenuDay
import com.budgetntr.proto.recipe.v1.WeekMenuSummary
import dev.andre.nutritionist.recipe.config.RecipeProperties
import dev.andre.nutritionist.recipe.domain.model.CURRENCY
import dev.andre.nutritionist.recipe.domain.model.CatalogProduct
import dev.andre.nutritionist.recipe.domain.model.GeneratedMeal
import dev.andre.nutritionist.recipe.domain.model.GeneratedMealPlan
import dev.andre.nutritionist.recipe.domain.model.MealPlanRequest
import dev.andre.nutritionist.recipe.domain.model.MenuIngredient
import dev.andre.nutritionist.recipe.domain.model.MenuRequest
import dev.andre.nutritionist.recipe.domain.model.ProductId
import dev.andre.nutritionist.recipe.domain.model.TargetNutrition
import dev.andre.nutritionist.recipe.domain.model.WEEK_DAYS
import dev.andre.nutritionist.recipe.infrastructure.llm.InvalidMenu
import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

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

    fun validateMealPlan(
        response: String,
        request: MealPlanRequest,
        products: List<CatalogProduct>,
    ): GeneratedMealPlan {
        val root =
            try {
                json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(response)
            } catch (_: JacksonException) {
                throw InvalidMenu("Provider returned malformed meal plan JSON")
            }
        val instructions = root.path("cooking_instructions").asText("").trim()
        val mealNodes = root.path("meals").elementsList(1..10, "meals")
        val byId = products.associateBy { it.id.raw }

        val meals =
            mealNodes.map { mealNode ->
                val typeStr = mealNode.path("type").asText("").lowercase()
                val mealType =
                    when (typeStr) {
                        "breakfast" -> MealType.MEAL_TYPE_BREAKFAST
                        "lunch" -> MealType.MEAL_TYPE_LUNCH
                        "dinner" -> MealType.MEAL_TYPE_DINNER
                        "snack" -> MealType.MEAL_TYPE_SNACK
                        else -> MealType.MEAL_TYPE_UNSPECIFIED
                    }
                val title = mealNode.path("title").asText("").trim().ifBlank { "Блюдо" }
                val cookingTime = mealNode.path("cooking_time_minutes").asInt(15).coerceIn(1, 300)
                val productNodes = mealNode.path("products").elementsList(1..8, "products")
                val ingredients =
                    productNodes.map { pNode ->
                        val id = pNode.path("id").asText("")
                        val grams = pNode.path("grams").integer(1..2000, "grams")
                        val prod =
                            byId[id] ?: products.firstOrNull { it.id.raw == id }
                                ?: CatalogProduct(ProductId(id), id, 100.0, 5.0, 2.0, 10.0, 100L)
                        MenuIngredient(
                            productId = prod.id.raw,
                            name = prod.name,
                            grams = grams,
                        )
                    }
                val mealCalories =
                    ingredients.sumOf { ing ->
                        val prod = byId[ing.productId] ?: products.firstOrNull { it.id.raw == ing.productId }
                        (prod?.caloriesPer100g ?: 100.0) * ing.grams / 100.0
                    }
                val mealProteins =
                    ingredients.sumOf { ing ->
                        val prod = byId[ing.productId] ?: products.firstOrNull { it.id.raw == ing.productId }
                        (prod?.proteinsPer100g ?: 5.0) * ing.grams / 100.0
                    }
                val mealFats =
                    ingredients.sumOf { ing ->
                        val prod = byId[ing.productId] ?: products.firstOrNull { it.id.raw == ing.productId }
                        (prod?.fatsPer100g ?: 2.0) * ing.grams / 100.0
                    }
                val mealCarbs =
                    ingredients.sumOf { ing ->
                        val prod = byId[ing.productId] ?: products.firstOrNull { it.id.raw == ing.productId }
                        (prod?.carbsPer100g ?: 10.0) * ing.grams / 100.0
                    }
                val mealCost =
                    ingredients.sumOf { ing ->
                        val prod = byId[ing.productId] ?: products.firstOrNull { it.id.raw == ing.productId }
                        ((prod?.pricePer100gMinor ?: 100L) * ing.grams + 50) / 100
                    }

                GeneratedMeal(
                    type = mealType,
                    title = title,
                    ingredients = ingredients,
                    nutrition =
                        TargetNutrition(
                            calories = mealCalories,
                            proteinsGrams = mealProteins,
                            fatsGrams = mealFats,
                            carbsGrams = mealCarbs,
                        ),
                    estimatedCostMinor = mealCost,
                    cookingTimeMinutes = cookingTime,
                )
            }

        val totalNutrition =
            TargetNutrition(
                calories = meals.sumOf { it.nutrition.calories },
                proteinsGrams = meals.sumOf { it.nutrition.proteinsGrams },
                fatsGrams = meals.sumOf { it.nutrition.fatsGrams },
                carbsGrams = meals.sumOf { it.nutrition.carbsGrams },
            )
        val totalCost = meals.sumOf { it.estimatedCostMinor }

        return GeneratedMealPlan(
            planId = UUID.randomUUID().toString(),
            period = request.period,
            meals = meals,
            totalNutrition = totalNutrition,
            estimatedCostMinor = totalCost,
            currency = request.currency,
            cookingInstructions = instructions.ifBlank { "Следуйте стандартным инструкциям по приготовлению выбранных ингредиентов." },
            source = MenuSource.MENU_SOURCE_GENERATED,
        )
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
                    ProtoMenuIngredient
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
