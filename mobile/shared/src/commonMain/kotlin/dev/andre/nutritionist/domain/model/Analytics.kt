package dev.andre.nutritionist.domain.model

import kotlinx.datetime.LocalDate

/**
 * Период анализа статистики питания и бюджета.
 */
enum class AnalyticsPeriod {
    WEEK,
    MONTH,
}

/**
 * Точка данных за один день для графиков калорий и затрат.
 */
data class DailyAnalyticsPoint(
    val date: LocalDate,
    val dayLabel: String,
    val calories: Int,
    val targetCalories: Int,
    val spentRub: Int,
    val budgetRub: Int,
)

/**
 * Агрегированная аналитика питания и бюджета за выбранный период.
 */
data class NutritionAnalytics(
    val period: AnalyticsPeriod,
    val averageDailyCalories: Int,
    val targetDailyCalories: Int,
    val calorieCompliancePercent: Int,
    val averageProteinsGrams: Double,
    val averageFatsGrams: Double,
    val averageCarbsGrams: Double,
    val totalSpentRub: Int,
    val totalBudgetRub: Int,
    val savedRub: Int,
    val averageMealCostRub: Int,
    val dailyPoints: List<DailyAnalyticsPoint>,
)
