package dev.andre.nutritionist.domain.model

import kotlinx.datetime.LocalDate

/**
 * Аналитика питания и расходов за один день периода.
 */
data class DailyNutritionPoint(
    val date: LocalDate,
    val calories: Int,
    val proteinsGrams: Double,
    val fatsGrams: Double,
    val carbsGrams: Double,
    val spent: Money,
    val isCalorieGoalMet: Boolean,
)

/**
 * Сводная аналитика питания, соблюдения норм КБЖУ и бюджета за выбранный период.
 */
data class PeriodAnalytics(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val daysCount: Int,
    val activeDaysCount: Int,
    val totalCalories: Int,
    val averageCalories: Int,
    val averageProteinsGrams: Double,
    val averageFatsGrams: Double,
    val averageCarbsGrams: Double,
    val targetCaloriesPerDay: Int,
    val targetProteinsPerDay: Double,
    val targetFatsPerDay: Double,
    val targetCarbsPerDay: Double,
    val totalSpent: Money,
    val budgetLimit: Money,
    val budgetUsagePercent: Int,
    val isBudgetExceeded: Boolean,
    val calorieAdherencePercent: Int,
    val dailyBreakdown: List<DailyNutritionPoint>,
)
