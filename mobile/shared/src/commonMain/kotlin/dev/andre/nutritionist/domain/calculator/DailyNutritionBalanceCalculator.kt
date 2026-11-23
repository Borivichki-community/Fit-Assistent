package dev.andre.nutritionist.domain.calculator

import dev.andre.nutritionist.domain.model.ConsumedNutrition
import dev.andre.nutritionist.domain.model.DailyMacroBalance
import dev.andre.nutritionist.domain.model.DailyNutritionBalance
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.NutritionGoal
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Чистый, офлайн-готовый доменный калькулятор баланса КБЖУ за день.
 *
 * Клиентский компонент, обеспечивающий мгновенный расчёт остатка калорий и макросов,
 * прогресса выполнения целей и флагов перебора нормы без обращения к сети.
 * Источником целевых показателей (SSOT) является [NutritionGoal], рассчитанный бэкендом.
 */
object DailyNutritionBalanceCalculator {

    fun calculate(
        goal: NutritionGoal,
        consumed: ConsumedNutrition,
    ): DailyNutritionBalance {
        val targetCalories = goal.targetCalories
        val consumedCalories = consumed.calories
        val remainingCalories = targetCalories - consumedCalories
        val isCaloriesExceeded = consumedCalories > targetCalories

        val caloriesProgressPercent = if (targetCalories > 0) {
            ((consumedCalories.toDouble() / targetCalories.toDouble()) * 100.0).roundToInt()
        } else {
            0
        }

        val proteins = calculateMacroBalance(
            targetGrams = goal.targetProteinsGrams,
            consumedGrams = consumed.proteinsGrams,
        )

        val fats = calculateMacroBalance(
            targetGrams = goal.targetFatsGrams,
            consumedGrams = consumed.fatsGrams,
        )

        val carbs = calculateMacroBalance(
            targetGrams = goal.targetCarbsGrams,
            consumedGrams = consumed.carbsGrams,
        )

        return DailyNutritionBalance(
            targetCalories = targetCalories,
            consumedCalories = consumedCalories,
            remainingCalories = remainingCalories,
            caloriesProgressPercent = caloriesProgressPercent,
            isCaloriesExceeded = isCaloriesExceeded,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
        )
    }

    private fun calculateMacroBalance(
        targetGrams: Double,
        consumedGrams: Double,
    ): DailyMacroBalance {
        val remainingGrams = roundOneDecimal(max(0.0, targetGrams - consumedGrams))
        val progressPercent = if (targetGrams > 0.0) {
            ((consumedGrams / targetGrams) * 100.0).roundToInt()
        } else {
            0
        }
        val isExceeded = consumedGrams > targetGrams

        return DailyMacroBalance(
            targetGrams = targetGrams,
            consumedGrams = consumedGrams,
            remainingGrams = remainingGrams,
            progressPercent = progressPercent,
            isExceeded = isExceeded,
        )
    }

    /**
     * Вычисляет рекомендуемый доступный бюджет КБЖУ на конкретный приём пищи
     * исходя из текущего дневного баланса.
     *
     * Распределение суточного калоража по приёмам пищи:
     * - Завтрак (BREAKFAST): 25%
     * - Обед (LUNCH): 35%
     * - Ужин (DINNER): 25%
     * - Перекус (SNACK): 15%
     *
     * Если остаток бюджета дня исчерпан или отрицателен, возвращаются нулевые значения.
     */
    fun getRemainingBudgetForMeal(
        balance: DailyNutritionBalance,
        mealType: MealType,
    ): ConsumedNutrition {
        val factor = when (mealType) {
            MealType.BREAKFAST -> 0.25
            MealType.LUNCH -> 0.35
            MealType.DINNER -> 0.25
            MealType.SNACK -> 0.15
        }

        val availableCalories = max(0, balance.remainingCalories)
        val mealCalories = (availableCalories * factor).roundToInt()

        val availableProteins = max(0.0, balance.proteins.remainingGrams)
        val availableFats = max(0.0, balance.fats.remainingGrams)
        val availableCarbs = max(0.0, balance.carbs.remainingGrams)

        return ConsumedNutrition(
            calories = mealCalories,
            proteinsGrams = roundOneDecimal(availableProteins * factor),
            fatsGrams = roundOneDecimal(availableFats * factor),
            carbsGrams = roundOneDecimal(availableCarbs * factor),
        )
    }

    private fun roundOneDecimal(value: Double): Double =
        (value * 10.0).roundToInt() / 10.0
}
