package dev.andre.nutritionist.domain.calculator

import dev.andre.nutritionist.domain.model.ConsumedNutrition
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.NutritionGoal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyNutritionBalanceCalculatorTest {

    @Test
    fun `basic day calculates remaining calories and 60 percent progress without exceeding`() {
        val goal = NutritionGoal(
            targetCalories = 2000,
            targetProteinsGrams = 140.0,
            targetFatsGrams = 70.0,
            targetCarbsGrams = 200.0,
        )
        val consumed = ConsumedNutrition(
            calories = 1200,
            proteinsGrams = 84.0,
            fatsGrams = 42.0,
            carbsGrams = 120.0,
        )

        val balance = DailyNutritionBalanceCalculator.calculate(goal, consumed)

        assertEquals(2000, balance.targetCalories)
        assertEquals(1200, balance.consumedCalories)
        assertEquals(800, balance.remainingCalories)
        assertEquals(60, balance.caloriesProgressPercent)
        assertFalse(balance.isCaloriesExceeded)

        // Proteins
        assertEquals(140.0, balance.proteins.targetGrams)
        assertEquals(84.0, balance.proteins.consumedGrams)
        assertEquals(56.0, balance.proteins.remainingGrams)
        assertEquals(60, balance.proteins.progressPercent)
        assertFalse(balance.proteins.isExceeded)

        // Fats
        assertEquals(70.0, balance.fats.targetGrams)
        assertEquals(42.0, balance.fats.consumedGrams)
        assertEquals(28.0, balance.fats.remainingGrams)
        assertEquals(60, balance.fats.progressPercent)
        assertFalse(balance.fats.isExceeded)

        // Carbs
        assertEquals(200.0, balance.carbs.targetGrams)
        assertEquals(120.0, balance.carbs.consumedGrams)
        assertEquals(80.0, balance.carbs.remainingGrams)
        assertEquals(60, balance.carbs.progressPercent)
        assertFalse(balance.carbs.isExceeded)
    }

    @Test
    fun `calorie excess day returns negative remaining calories and marks calories as exceeded`() {
        val goal = NutritionGoal(
            targetCalories = 2000,
            targetProteinsGrams = 140.0,
            targetFatsGrams = 70.0,
            targetCarbsGrams = 200.0,
        )
        val consumed = ConsumedNutrition(
            calories = 2250,
            proteinsGrams = 160.0,
            fatsGrams = 75.0,
            carbsGrams = 240.0,
        )

        val balance = DailyNutritionBalanceCalculator.calculate(goal, consumed)

        assertEquals(2000, balance.targetCalories)
        assertEquals(2250, balance.consumedCalories)
        assertEquals(-250, balance.remainingCalories)
        assertEquals(113, balance.caloriesProgressPercent)
        assertTrue(balance.isCaloriesExceeded)

        // Exceeded macros have remainingGrams = 0.0 and isExceeded = true
        assertEquals(0.0, balance.proteins.remainingGrams)
        assertEquals(114, balance.proteins.progressPercent)
        assertTrue(balance.proteins.isExceeded)

        assertEquals(0.0, balance.fats.remainingGrams)
        assertEquals(107, balance.fats.progressPercent)
        assertTrue(balance.fats.isExceeded)

        assertEquals(0.0, balance.carbs.remainingGrams)
        assertEquals(120, balance.carbs.progressPercent)
        assertTrue(balance.carbs.isExceeded)
    }

    @Test
    fun `zero target goals does not throw ArithmeticException and returns zero percent progress`() {
        val emptyGoal = NutritionGoal.EMPTY
        val consumed = ConsumedNutrition(
            calories = 500,
            proteinsGrams = 30.0,
            fatsGrams = 20.0,
            carbsGrams = 50.0,
        )

        val balance = DailyNutritionBalanceCalculator.calculate(emptyGoal, consumed)

        assertEquals(0, balance.targetCalories)
        assertEquals(500, balance.consumedCalories)
        assertEquals(-500, balance.remainingCalories)
        assertEquals(0, balance.caloriesProgressPercent)
        assertTrue(balance.isCaloriesExceeded)

        assertEquals(0, balance.proteins.progressPercent)
        assertEquals(0.0, balance.proteins.remainingGrams)
        assertTrue(balance.proteins.isExceeded)

        assertEquals(0, balance.fats.progressPercent)
        assertEquals(0.0, balance.fats.remainingGrams)
        assertTrue(balance.fats.isExceeded)

        assertEquals(0, balance.carbs.progressPercent)
        assertEquals(0.0, balance.carbs.remainingGrams)
        assertTrue(balance.carbs.isExceeded)
    }

    @Test
    fun `macro boundary precision and rounding to one decimal place`() {
        val goal = NutritionGoal(
            targetCalories = 1850,
            targetProteinsGrams = 125.5,
            targetFatsGrams = 62.3,
            targetCarbsGrams = 197.8,
        )
        val consumed = ConsumedNutrition(
            calories = 1110,
            proteinsGrams = 45.33,
            fatsGrams = 21.17,
            carbsGrams = 83.25,
        )

        val balance = DailyNutritionBalanceCalculator.calculate(goal, consumed)

        // 125.5 - 45.33 = 80.17 -> rounded to 80.2
        assertEquals(80.2, balance.proteins.remainingGrams)
        assertEquals(36, balance.proteins.progressPercent)
        assertFalse(balance.proteins.isExceeded)

        // 62.3 - 21.17 = 41.13 -> rounded to 41.1
        assertEquals(41.1, balance.fats.remainingGrams)
        assertEquals(34, balance.fats.progressPercent)
        assertFalse(balance.fats.isExceeded)

        // 197.8 - 83.25 = 114.55 -> rounded to 114.6 (or 114.55 * 10 = 1145.5 -> 1146 -> 114.6)
        assertEquals(114.6, balance.carbs.remainingGrams)
        assertEquals(42, balance.carbs.progressPercent)
        assertFalse(balance.carbs.isExceeded)
    }

    @Test
    fun `getRemainingBudgetForMeal allocates proportional budget for each meal type`() {
        val goal = NutritionGoal(
            targetCalories = 2000,
            targetProteinsGrams = 100.0,
            targetFatsGrams = 60.0,
            targetCarbsGrams = 200.0,
        )
        val consumed = ConsumedNutrition(
            calories = 1000,
            proteinsGrams = 50.0,
            fatsGrams = 30.0,
            carbsGrams = 100.0,
        )
        val balance = DailyNutritionBalanceCalculator.calculate(goal, consumed)
        // remainingCalories = 1000, proteins = 50.0, fats = 30.0, carbs = 100.0

        val breakfastBudget = DailyNutritionBalanceCalculator.getRemainingBudgetForMeal(balance, MealType.BREAKFAST)
        assertEquals(250, breakfastBudget.calories) // 1000 * 0.25
        assertEquals(12.5, breakfastBudget.proteinsGrams) // 50 * 0.25
        assertEquals(7.5, breakfastBudget.fatsGrams) // 30 * 0.25
        assertEquals(25.0, breakfastBudget.carbsGrams) // 100 * 0.25

        val lunchBudget = DailyNutritionBalanceCalculator.getRemainingBudgetForMeal(balance, MealType.LUNCH)
        assertEquals(350, lunchBudget.calories) // 1000 * 0.35
        assertEquals(17.5, lunchBudget.proteinsGrams)
        assertEquals(10.5, lunchBudget.fatsGrams)
        assertEquals(35.0, lunchBudget.carbsGrams)

        val dinnerBudget = DailyNutritionBalanceCalculator.getRemainingBudgetForMeal(balance, MealType.DINNER)
        assertEquals(250, dinnerBudget.calories) // 1000 * 0.25
        assertEquals(12.5, dinnerBudget.proteinsGrams)
        assertEquals(7.5, dinnerBudget.fatsGrams)
        assertEquals(25.0, dinnerBudget.carbsGrams)

        val snackBudget = DailyNutritionBalanceCalculator.getRemainingBudgetForMeal(balance, MealType.SNACK)
        assertEquals(150, snackBudget.calories) // 1000 * 0.15
        assertEquals(7.5, snackBudget.proteinsGrams)
        assertEquals(4.5, snackBudget.fatsGrams)
        assertEquals(15.0, snackBudget.carbsGrams)
    }

    @Test
    fun `getRemainingBudgetForMeal returns zero when daily budget is exceeded`() {
        val goal = NutritionGoal(
            targetCalories = 2000,
            targetProteinsGrams = 100.0,
            targetFatsGrams = 60.0,
            targetCarbsGrams = 200.0,
        )
        val consumed = ConsumedNutrition(
            calories = 2500,
            proteinsGrams = 120.0,
            fatsGrams = 80.0,
            carbsGrams = 250.0,
        )
        val balance = DailyNutritionBalanceCalculator.calculate(goal, consumed)

        val mealBudget = DailyNutritionBalanceCalculator.getRemainingBudgetForMeal(balance, MealType.LUNCH)
        assertEquals(0, mealBudget.calories)
        assertEquals(0.0, mealBudget.proteinsGrams)
        assertEquals(0.0, mealBudget.fatsGrams)
        assertEquals(0.0, mealBudget.carbsGrams)
    }
}
