package dev.andre.nutritionist.domain.calculator

import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.RecipeIngredient
import kotlin.math.round
import kotlin.math.roundToLong

data class RecipeNutritionSummary(
    val totalWeightGrams: Double,
    val totalNutrition: NutritionFacts,
    val nutritionPer100g: NutritionFacts,
)

/**
 * Чистый клиентский калькулятор пищевой ценности рецепта для превью в редакторе
 * и расчёта порций при добавлении в дневник.
 * Итоговым SSOT при сохранении рецепта остаётся сервер.
 */
object RecipeNutritionCalculator {

    private const val GRAMS_PER_100G = 100.0

    fun calculateSummary(ingredients: Iterable<RecipeIngredient>): RecipeNutritionSummary {
        var totalWeight = 0.0
        var totalCalories = 0.0
        var totalProteins = 0.0
        var totalFats = 0.0
        var totalCarbs = 0.0

        for (ingredient in ingredients) {
            val weight = ingredient.weightGrams
            if (weight <= 0.0 || weight.isNaN() || weight.isInfinite()) continue

            totalWeight += weight
            val factor = weight / GRAMS_PER_100G
            totalCalories += ingredient.nutritionPer100g.calories * factor
            totalProteins += ingredient.nutritionPer100g.proteins * factor
            totalFats += ingredient.nutritionPer100g.fats * factor
            totalCarbs += ingredient.nutritionPer100g.carbs * factor
        }

        if (totalWeight <= 0.0) {
            return RecipeNutritionSummary(
                totalWeightGrams = 0.0,
                totalNutrition = NutritionFacts.ZERO,
                nutritionPer100g = NutritionFacts.ZERO,
            )
        }

        val totalNutrition = NutritionFacts(
            calories = roundTwoDecimals(totalCalories),
            proteins = roundTwoDecimals(totalProteins),
            fats = roundTwoDecimals(totalFats),
            carbs = roundTwoDecimals(totalCarbs),
        )

        val per100gFactor = GRAMS_PER_100G / totalWeight
        val nutritionPer100g = NutritionFacts(
            calories = roundTwoDecimals(totalCalories * per100gFactor),
            proteins = roundTwoDecimals(totalProteins * per100gFactor),
            fats = roundTwoDecimals(totalFats * per100gFactor),
            carbs = roundTwoDecimals(totalCarbs * per100gFactor),
        )

        return RecipeNutritionSummary(
            totalWeightGrams = roundTwoDecimals(totalWeight),
            totalNutrition = totalNutrition,
            nutritionPer100g = nutritionPer100g,
        )
    }

    fun calculatePer100g(ingredients: Iterable<RecipeIngredient>): NutritionFacts =
        calculateSummary(ingredients).nutritionPer100g

    fun calculateTotalNutrition(ingredients: Iterable<RecipeIngredient>): NutritionFacts =
        calculateSummary(ingredients).totalNutrition

    fun calculatePortion(
        nutritionPer100g: NutritionFacts,
        weightGrams: Double,
    ): NutritionFacts {
        if (weightGrams <= 0.0 || weightGrams.isNaN() || weightGrams.isInfinite()) {
            return NutritionFacts.ZERO
        }

        val factor = weightGrams / GRAMS_PER_100G
        return NutritionFacts(
            calories = roundTwoDecimals(nutritionPer100g.calories * factor),
            proteins = roundTwoDecimals(nutritionPer100g.proteins * factor),
            fats = roundTwoDecimals(nutritionPer100g.fats * factor),
            carbs = roundTwoDecimals(nutritionPer100g.carbs * factor),
        )
    }

    /**
     * Пропорциональный расчёт стоимости порции рецепта.
     * Если общая стоимость или вес неизвестны (<= 0), возвращает null, а не Money.ZERO.
     */
    fun calculatePortionCost(
        totalCost: Money,
        totalWeightGrams: Double,
        portionWeightGrams: Double,
    ): Money? {
        if (
            totalCost.amountMinor <= 0L ||
            totalWeightGrams <= 0.0 ||
            totalWeightGrams.isNaN() ||
            totalWeightGrams.isInfinite() ||
            portionWeightGrams <= 0.0 ||
            portionWeightGrams.isNaN() ||
            portionWeightGrams.isInfinite()
        ) {
            return null
        }

        val minor = (totalCost.amountMinor * (portionWeightGrams / totalWeightGrams)).roundToLong()
        return Money.fromMinor(minor.coerceAtLeast(0L))
    }

    private fun roundTwoDecimals(value: Double): Double =
        round(value * 100.0) / 100.0
}
