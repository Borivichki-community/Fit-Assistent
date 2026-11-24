package dev.andre.nutritionist.domain.calculator

import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Price
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.model.SyncStatus
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutritionCalculatorTest {

    @Test
    fun `calculatePortion returns zero when weight is zero or negative`() {
        val per100g = NutritionFacts(calories = 200.0, proteins = 15.0, fats = 5.0, carbs = 25.0)

        assertEquals(NutritionFacts.ZERO, NutritionCalculator.calculatePortion(per100g, 0.0))
        assertEquals(NutritionFacts.ZERO, NutritionCalculator.calculatePortion(per100g, -50.0))
    }

    @Test
    fun `calculatePortion scales correctly for standard portion`() {
        // Chicken breast: 113 kcal, 23.6g P, 1.9g F, 0.0g C per 100g
        val chicken100g = NutritionFacts(calories = 113.0, proteins = 23.6, fats = 1.9, carbs = 0.0)

        // 250g portion
        val portion = NutritionCalculator.calculatePortion(chicken100g, 250.0)

        assertEquals(282.5, portion.calories)
        assertEquals(59.0, portion.proteins)
        assertEquals(4.75, portion.fats)
        assertEquals(0.0, portion.carbs)
    }

    @Test
    fun `calculatePortion rounds to two decimal places`() {
        val per100g = NutritionFacts(calories = 100.0, proteins = 10.333, fats = 5.666, carbs = 20.123)
        val portion = NutritionCalculator.calculatePortion(per100g, 33.3)

        assertEquals(33.3, portion.calories)
        assertEquals(3.44, portion.proteins)
        assertEquals(1.89, portion.fats)
        assertEquals(6.7, portion.carbs)
    }

    @Test
    fun `calculatePortion handles extreme weights without overflow`() {
        val per100g = NutritionFacts(calories = 500.0, proteins = 30.0, fats = 20.0, carbs = 50.0)
        val portion = NutritionCalculator.calculatePortion(per100g, 10_000.0) // 10 kg

        assertEquals(50_000.0, portion.calories)
        assertEquals(3_000.0, portion.proteins)
        assertEquals(2_000.0, portion.fats)
        assertEquals(5_000.0, portion.carbs)
    }

    @Test
    fun `sumNutrition correctly sums list of items`() {
        val item1 = NutritionFacts(calories = 250.0, proteins = 20.0, fats = 10.0, carbs = 15.0)
        val item2 = NutritionFacts(calories = 150.0, proteins = 5.0, fats = 2.0, carbs = 30.0)
        val item3 = NutritionFacts(calories = 100.0, proteins = 8.0, fats = 1.0, carbs = 12.0)

        val total = NutritionCalculator.sumNutrition(listOf(item1, item2, item3))

        assertEquals(500.0, total.calories)
        assertEquals(33.0, total.proteins)
        assertEquals(13.0, total.fats)
        assertEquals(57.0, total.carbs)
    }

    @Test
    fun `sumMealRecords and sumMealCost aggregate diary records`() {
        val record1 = MealRecord(
            id = MealRecordId("rec-1"),
            mealType = MealType.BREAKFAST,
            recordedAt = Instant.fromEpochMilliseconds(1700000000000L),
            sourceType = EntrySourceType.PRODUCT,
            nameSnapshot = "Овсянка",
            weightGrams = 200.0,
            nutrition = NutritionFacts(calories = 180.0, proteins = 6.0, fats = 3.0, carbs = 32.0),
            cost = Money.fromMinor(3500L), // 35.00 RUB
            idempotencyKey = IdempotencyKey("idem-1"),
            syncStatus = SyncStatus.SYNCED,
        )
        val record2 = MealRecord(
            id = MealRecordId("rec-2"),
            mealType = MealType.LUNCH,
            recordedAt = Instant.fromEpochMilliseconds(1700003600000L),
            sourceType = EntrySourceType.RECIPE,
            nameSnapshot = "Курица с рисом",
            weightGrams = 350.0,
            nutrition = NutritionFacts(calories = 450.0, proteins = 35.0, fats = 12.0, carbs = 50.0),
            cost = Money.fromMinor(12000L), // 120.00 RUB
            idempotencyKey = IdempotencyKey("idem-2"),
            syncStatus = SyncStatus.PENDING,
        )

        val totalNutrition = NutritionCalculator.sumMealRecords(listOf(record1, record2))
        val totalCost = NutritionCalculator.sumMealCost(listOf(record1, record2))

        assertEquals(630.0, totalNutrition.calories)
        assertEquals(41.0, totalNutrition.proteins)
        assertEquals(15.0, totalNutrition.fats)
        assertEquals(82.0, totalNutrition.carbs)
        assertEquals(15500L, totalCost.amountMinor)
        assertEquals(155.0, totalCost.toRubles())
    }

    @Test
    fun `calculatePortionCost calculates cost by kg, g and unit correctly`() {
        // Price: 400.00 RUB per kg
        val pricePerKg = Price(amount = Money.fromRubles(400.0), unit = "кг")

        // 250 grams portion: 400 * 0.25 = 100.00 RUB (10000 minor)
        val cost250g = NutritionCalculator.calculatePortionCost(pricePerKg, 250.0)
        assertEquals(10000L, cost250g.amountMinor)
        assertEquals(100.0, cost250g.toRubles())

        // 0 grams: 0
        val costZero = NutritionCalculator.calculatePortionCost(pricePerKg, 0.0)
        assertEquals(Money.ZERO, costZero)

        // Price per gram
        val pricePerGram = Price(amount = Money.fromMinor(50L), unit = "г") // 0.50 RUB per gram
        val cost50g = NutritionCalculator.calculatePortionCost(pricePerGram, 50.0)
        assertEquals(2500L, cost50g.amountMinor)

        // Price per piece/unit
        val pricePerPiece = Price(amount = Money.fromRubles(25.0), unit = "шт")
        val costPiece = NutritionCalculator.calculatePortionCost(pricePerPiece, 1.0)
        assertEquals(2500L, costPiece.amountMinor)
    }

    @Test
    fun `sumRecipeCost calculates total from ingredients`() {
        val ingredients = listOf(
            RecipeIngredient(
                productId = ProductId("prod-1"),
                name = "Куриное филе",
                weightGrams = 200.0,
                cost = Money.fromRubles(80.0),
            ),
            RecipeIngredient(
                productId = ProductId("prod-2"),
                name = "Рис",
                weightGrams = 80.0,
                cost = Money.fromRubles(15.0),
            ),
            RecipeIngredient(
                productId = null,
                name = "Соль, специи",
                weightGrams = 5.0,
                cost = null,
            ),
        )

        val totalCost = NutritionCalculator.sumRecipeCost(ingredients)
        assertEquals(9500L, totalCost.amountMinor)
        assertEquals(95.0, totalCost.toRubles())
    }

    @Test
    fun `Money operations work correctly`() {
        val m1 = Money.fromRubles(150.50)
        val m2 = Money.fromRubles(49.50)

        val sum = m1 + m2
        assertEquals(20000L, sum.amountMinor)
        assertEquals(200.0, sum.toRubles())

        val diff = m1 - m2
        assertEquals(10100L, diff.amountMinor)
        assertEquals(101.0, diff.toRubles())

        val multiplied = m2 * 2.0
        assertEquals(9900L, multiplied.amountMinor)

        val divided = sum / 2
        assertEquals(10000L, divided.amountMinor)

        assertTrue(m1 > m2)
    }
}
