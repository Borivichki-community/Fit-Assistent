package dev.andre.nutritionist.presentation.statistics

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.NutritionAnalytics

/**
 * Маппер состояния экрана аналитики в [StatisticsUiState].
 */
class StatisticsUiStateMapper : UiStateMapper<StatisticsState, StatisticsUiState> {

    override fun map(state: StatisticsState): StatisticsUiState {
        val content = when (val c = state.content) {
            is StatisticsState.Content.Loading -> StatisticsUiState.Content.Loading
            is StatisticsState.Content.Error -> StatisticsUiState.Content.Error(c.message)
            is StatisticsState.Content.Success -> StatisticsUiState.Content.Success(mapModel(c.analytics))
        }
        return StatisticsUiState(
            selectedPeriod = state.period,
            content = content,
        )
    }

    private fun mapModel(analytics: NutritionAnalytics): StatisticsSuccessUiModel {
        val maxCalories = analytics.dailyPoints.maxOfOrNull { it.calories.coerceAtLeast(it.targetCalories) }
            ?.toFloat()?.coerceAtLeast(1000f) ?: 2500f

        val bars = analytics.dailyPoints.map { point ->
            DailyBarUiModel(
                dayLabel = point.dayLabel,
                caloriesText = "${point.calories}",
                heightFraction = (point.calories.toFloat() / maxCalories).coerceIn(0.08f, 1f),
                isTargetMet = kotlin.math.abs(point.calories - point.targetCalories) <= 150,
            )
        }

        val totalMacroGrams = (analytics.averageProteinsGrams + analytics.averageFatsGrams + analytics.averageCarbsGrams)
            .coerceAtLeast(1.0)

        val totalBudget = analytics.totalBudgetRub.coerceAtLeast(1)
        val burnFraction = (analytics.totalSpentRub.toFloat() / totalBudget).coerceIn(0f, 1.5f)

        return StatisticsSuccessUiModel(
            caloriesSummaryText = "${analytics.averageDailyCalories} ккал",
            targetCaloriesText = "из ${analytics.targetDailyCalories} ккал/день",
            complianceBadgeText = "${analytics.calorieCompliancePercent}% норма",
            isComplianceHigh = analytics.calorieCompliancePercent >= 85,
            caloriesChartBars = bars,
            macroProteinsText = "${analytics.averageProteinsGrams.toInt()} г",
            macroFatsText = "${analytics.averageFatsGrams.toInt()} г",
            macroCarbsText = "${analytics.averageCarbsGrams.toInt()} г",
            proteinsFraction = (analytics.averageProteinsGrams / totalMacroGrams).toFloat().coerceIn(0f, 1f),
            fatsFraction = (analytics.averageFatsGrams / totalMacroGrams).toFloat().coerceIn(0f, 1f),
            carbsFraction = (analytics.averageCarbsGrams / totalMacroGrams).toFloat().coerceIn(0f, 1f),
            totalSpentText = "${analytics.totalSpentRub} ₽",
            totalBudgetText = "${analytics.totalBudgetRub} ₽",
            savedText = "+${analytics.savedRub} ₽ экономия",
            budgetBurnPercentText = "${(burnFraction * 100).toInt()}% бюджета",
            budgetBurnFraction = burnFraction.coerceIn(0f, 1f),
            averageMealCostText = "~${analytics.averageMealCostRub} ₽ / порция",
        )
    }
}
