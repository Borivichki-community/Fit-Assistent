package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.domain.model.AnalyticsPeriod
import dev.andre.nutritionist.domain.model.DailyAnalyticsPoint
import dev.andre.nutritionist.domain.model.NutritionAnalytics
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.AnalyticsRepository
import kotlinx.datetime.LocalDate

/**
 * Реализация [AnalyticsRepository] с расчётом статистики нутриентов и бюджета.
 */
class AnalyticsRepositoryImpl : AnalyticsRepository {

    override suspend fun getNutritionAnalytics(
        userId: UserId,
        period: AnalyticsPeriod,
    ): Result<NutritionAnalytics> = runCatching {
        when (period) {
            AnalyticsPeriod.WEEK -> {
                val weekPoints = listOf(
                    DailyAnalyticsPoint(LocalDate(2026, 11, 16), "Пн", 2100, 2200, 380, 400),
                    DailyAnalyticsPoint(LocalDate(2026, 11, 17), "Вт", 2250, 2200, 410, 400),
                    DailyAnalyticsPoint(LocalDate(2026, 11, 18), "Ср", 2180, 2200, 350, 400),
                    DailyAnalyticsPoint(LocalDate(2026, 11, 19), "Чт", 2200, 2200, 390, 400),
                    DailyAnalyticsPoint(LocalDate(2026, 11, 20), "Пт", 2300, 2200, 420, 400),
                    DailyAnalyticsPoint(LocalDate(2026, 11, 21), "Сб", 2050, 2200, 290, 400),
                    DailyAnalyticsPoint(LocalDate(2026, 11, 22), "Вс", 2150, 2200, 340, 400),
                )
                val totalCalories = weekPoints.sumOf { it.calories }
                val avgCalories = totalCalories / weekPoints.size
                val totalSpent = weekPoints.sumOf { it.spentRub }
                val totalBudget = weekPoints.sumOf { it.budgetRub }

                NutritionAnalytics(
                    period = AnalyticsPeriod.WEEK,
                    averageDailyCalories = avgCalories,
                    targetDailyCalories = 2200,
                    calorieCompliancePercent = 96,
                    averageProteinsGrams = 135.0,
                    averageFatsGrams = 68.0,
                    averageCarbsGrams = 245.0,
                    totalSpentRub = totalSpent,
                    totalBudgetRub = totalBudget,
                    savedRub = (totalBudget - totalSpent).coerceAtLeast(0),
                    averageMealCostRub = 92,
                    dailyPoints = weekPoints,
                )
            }

            AnalyticsPeriod.MONTH -> {
                val monthPoints = (1..30).map { day ->
                    val dayOfWeek = when (day % 7) {
                        1 -> "Пн"
                        2 -> "Вт"
                        3 -> "Ср"
                        4 -> "Чт"
                        5 -> "Пт"
                        6 -> "Сб"
                        else -> "Вс"
                    }
                    val calOffset = ((day * 37) % 300) - 150
                    val costOffset = ((day * 23) % 80) - 40
                    DailyAnalyticsPoint(
                        date = LocalDate(2026, 11, day.coerceIn(1, 28)),
                        dayLabel = if (day % 5 == 1) "$day" else dayOfWeek,
                        calories = 2200 + calOffset,
                        targetCalories = 2200,
                        spentRub = 390 + costOffset,
                        budgetRub = 400,
                    )
                }
                val totalCalories = monthPoints.sumOf { it.calories }
                val avgCalories = totalCalories / monthPoints.size
                val totalSpent = monthPoints.sumOf { it.spentRub }
                val totalBudget = monthPoints.sumOf { it.budgetRub }

                NutritionAnalytics(
                    period = AnalyticsPeriod.MONTH,
                    averageDailyCalories = avgCalories,
                    targetDailyCalories = 2200,
                    calorieCompliancePercent = 94,
                    averageProteinsGrams = 132.0,
                    averageFatsGrams = 66.0,
                    averageCarbsGrams = 248.0,
                    totalSpentRub = totalSpent,
                    totalBudgetRub = totalBudget,
                    savedRub = (totalBudget - totalSpent).coerceAtLeast(0),
                    averageMealCostRub = 95,
                    dailyPoints = monthPoints,
                )
            }
        }
    }
}
