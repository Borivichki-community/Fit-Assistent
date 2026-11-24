package dev.andre.nutritionist.domain.calculator

import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Price
import dev.andre.nutritionist.domain.model.RecipeIngredient
import kotlin.math.round

/**
 * Чистый функциональный калькулятор пищевой ценности и стоимости.
 * Не содержит платформозависимого кода и сайд-эффектов.
 */
object NutritionCalculator {

    /**
     * Расчёт КБЖУ порции продукта на основе пищевой ценности на 100г.
     * N_portion = (N_100g * weightGrams) / 100.0
     * Округление до сотых долей.
     */
    fun calculatePortion(
        nutritionPer100g: NutritionFacts,
        weightGrams: Double,
    ): NutritionFacts {
        if (weightGrams <= 0.0) return NutritionFacts.ZERO

        val factor = weightGrams / 100.0
        return NutritionFacts(
            calories = roundTwoDecimals(nutritionPer100g.calories * factor),
            proteins = roundTwoDecimals(nutritionPer100g.proteins * factor),
            fats = roundTwoDecimals(nutritionPer100g.fats * factor),
            carbs = roundTwoDecimals(nutritionPer100g.carbs * factor),
        )
    }

    /**
     * Суммирование списка объектов NutritionFacts.
     */
    fun sumNutrition(items: Iterable<NutritionFacts>): NutritionFacts {
        var totalCalories = 0.0
        var totalProteins = 0.0
        var totalFats = 0.0
        var totalCarbs = 0.0

        for (item in items) {
            totalCalories += item.calories
            totalProteins += item.proteins
            totalFats += item.fats
            totalCarbs += item.carbs
        }

        return NutritionFacts(
            calories = roundTwoDecimals(totalCalories),
            proteins = roundTwoDecimals(totalProteins),
            fats = roundTwoDecimals(totalFats),
            carbs = roundTwoDecimals(totalCarbs),
        )
    }

    /**
     * Суммирование КБЖУ всех записей приёма пищи.
     */
    fun sumMealRecords(records: Iterable<MealRecord>): NutritionFacts =
        sumNutrition(records.map { it.nutrition })

    /**
     * Суммирование стоимости всех записей приёма пищи.
     */
    fun sumMealCost(records: Iterable<MealRecord>): Money {
        var totalMinor = 0L
        for (record in records) {
            val cost = record.cost
            if (cost != null) {
                totalMinor += cost.amountMinor
            }
        }
        return Money.fromMinor(totalMinor)
    }

    /**
     * Расчёт стоимости порции по цене за единицу/кг.
     * По умолчанию цена указывается за 1 кг (1000 г).
     */
    fun calculatePortionCost(
        price: Price,
        weightGrams: Double,
    ): Money {
        if (weightGrams <= 0.0) return Money.ZERO

        val factor = when (price.unit.trim().lowercase()) {
            "г", "g", "грамм", "gram" -> weightGrams
            "шт", "unit", "piece" -> 1.0
            else -> weightGrams / 1000.0 // по умолчанию цена за кг
        }

        val amountMinor = round(price.amount.amountMinor * factor).toLong()
        return Money.fromMinor(amountMinor.coerceAtLeast(0L))
    }

    /**
     * Расчёт стоимости порции при известной цене за 1 кг.
     */
    fun calculatePortionCost(
        pricePerKg: Money,
        weightGrams: Double,
    ): Money {
        if (weightGrams <= 0.0) return Money.ZERO
        val amountMinor = round(pricePerKg.amountMinor * (weightGrams / 1000.0)).toLong()
        return Money.fromMinor(amountMinor.coerceAtLeast(0L))
    }

    /**
     * Суммирование стоимости ингредиентов рецепта.
     */
    fun sumRecipeCost(ingredients: Iterable<RecipeIngredient>): Money {
        var totalMinor = 0L
        for (ingredient in ingredients) {
            val cost = ingredient.cost
            if (cost != null) {
                totalMinor += cost.amountMinor
            }
        }
        return Money.fromMinor(totalMinor)
    }

    private fun roundTwoDecimals(value: Double): Double =
        round(value * 100.0) / 100.0
}
