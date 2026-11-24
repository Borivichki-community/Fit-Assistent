package dev.andre.nutritionist.domain.calculator

import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.model.RecipeIngredientId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecipeNutritionCalculatorTest {

    @Test
    fun `calculateSummary returns zero when ingredients list is empty`() {
        val summary = RecipeNutritionCalculator.calculateSummary(emptyList())

        assertEquals(0.0, summary.totalWeightGrams)
        assertEquals(NutritionFacts.ZERO, summary.totalNutrition)
        assertEquals(NutritionFacts.ZERO, summary.nutritionPer100g)
    }

    @Test
    fun `calculateSummary guards against zero and negative ingredient weights`() {
        val ingredients = listOf(
            RecipeIngredient(
                id = RecipeIngredientId("ing-0"),
                name = "Пустой",
                weightGrams = 0.0,
                nutritionPer100g = NutritionFacts(calories = 100.0, proteins = 10.0, fats = 5.0, carbs = 10.0),
            ),
            RecipeIngredient(
                id = RecipeIngredientId("ing-neg"),
                name = "Отрицательный",
                weightGrams = -50.0,
                nutritionPer100g = NutritionFacts(calories = 200.0, proteins = 20.0, fats = 10.0, carbs = 20.0),
            ),
        )

        val summary = RecipeNutritionCalculator.calculateSummary(ingredients)

        assertEquals(0.0, summary.totalWeightGrams)
        assertEquals(NutritionFacts.ZERO, summary.totalNutrition)
        assertEquals(NutritionFacts.ZERO, summary.nutritionPer100g)
    }

    @Test
    fun `calculateSummary computes total and weighted per 100g nutrition accurately`() {
        val ingredients = listOf(
            RecipeIngredient(
                id = RecipeIngredientId("ing-chicken"),
                productId = ProductId("prod-chicken"),
                name = "Куриное филе",
                weightGrams = 200.0,
                nutritionPer100g = NutritionFacts(calories = 110.0, proteins = 23.0, fats = 1.5, carbs = 0.0),
            ),
            RecipeIngredient(
                id = RecipeIngredientId("ing-rice"),
                productId = ProductId("prod-rice"),
                name = "Рис",
                weightGrams = 100.0,
                nutritionPer100g = NutritionFacts(calories = 340.0, proteins = 7.0, fats = 1.0, carbs = 75.0),
            ),
        )

        val summary = RecipeNutritionCalculator.calculateSummary(ingredients)

        assertEquals(300.0, summary.totalWeightGrams)
        assertEquals(NutritionFacts(calories = 560.0, proteins = 53.0, fats = 4.0, carbs = 75.0), summary.totalNutrition)
        // 560 / 300 * 100 = 186.67; 53 / 3 = 17.67; 4 / 3 = 1.33; 75 / 3 = 25.0
        assertEquals(
            NutritionFacts(calories = 186.67, proteins = 17.67, fats = 1.33, carbs = 25.0),
            summary.nutritionPer100g,
        )
        assertEquals(summary.nutritionPer100g, RecipeNutritionCalculator.calculatePer100g(ingredients))
        assertEquals(summary.totalNutrition, RecipeNutritionCalculator.calculateTotalNutrition(ingredients))
    }

    @Test
    fun `calculatePortion scales nutrition per 100g to portion weight and guards non-positive weight`() {
        val per100g = NutritionFacts(calories = 186.67, proteins = 17.67, fats = 1.33, carbs = 25.0)

        val portion = RecipeNutritionCalculator.calculatePortion(per100g, 150.0)
        assertEquals(
            NutritionFacts(calories = 280.0, proteins = 26.51, fats = 2.0, carbs = 37.5),
            portion,
        )

        assertEquals(NutritionFacts.ZERO, RecipeNutritionCalculator.calculatePortion(per100g, 0.0))
        assertEquals(NutritionFacts.ZERO, RecipeNutritionCalculator.calculatePortion(per100g, -10.0))
        assertEquals(NutritionFacts.ZERO, RecipeNutritionCalculator.calculatePortion(per100g, Double.NaN))
    }

    @Test
    fun `calculatePortionCost computes proportional cost and returns null when cost or weight unknown`() {
        val portionCost = RecipeNutritionCalculator.calculatePortionCost(
            totalCost = Money.fromMinor(30000L),
            totalWeightGrams = 300.0,
            portionWeightGrams = 150.0,
        )
        assertEquals(Money.fromMinor(15000L), portionCost)

        assertNull(
            RecipeNutritionCalculator.calculatePortionCost(
                totalCost = Money.ZERO,
                totalWeightGrams = 300.0,
                portionWeightGrams = 150.0,
            ),
        )
        assertNull(
            RecipeNutritionCalculator.calculatePortionCost(
                totalCost = Money.fromMinor(30000L),
                totalWeightGrams = 0.0,
                portionWeightGrams = 150.0,
            ),
        )
        assertNull(
            RecipeNutritionCalculator.calculatePortionCost(
                totalCost = Money.fromMinor(30000L),
                totalWeightGrams = 300.0,
                portionWeightGrams = 0.0,
            ),
        )
    }
}
