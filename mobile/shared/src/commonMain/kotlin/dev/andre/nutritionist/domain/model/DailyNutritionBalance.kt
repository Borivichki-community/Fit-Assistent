package dev.andre.nutritionist.domain.model

/**
 * Баланс отдельного макронутриента (белки, жиры или углеводы) за день.
 */
data class DailyMacroBalance(
    val targetGrams: Double,
    val consumedGrams: Double,
    val remainingGrams: Double, // max(0.0, target - consumed)
    val progressPercent: Int, // 0..100+
    val isExceeded: Boolean, // consumed > target
)

/**
 * Сводный баланс питания (КБЖУ) за день: целевые значения, фактически съеденное,
 * оставшийся бюджет калорий и макросов, процент выполнения и флаги превышения нормы.
 */
data class DailyNutritionBalance(
    val targetCalories: Int,
    val consumedCalories: Int,
    val remainingCalories: Int, // target - consumed (может быть отрицательным при переборе)
    val caloriesProgressPercent: Int,
    val isCaloriesExceeded: Boolean,
    val proteins: DailyMacroBalance,
    val fats: DailyMacroBalance,
    val carbs: DailyMacroBalance,
)
