package dev.andre.nutritionist.presentation.statistics

import dev.andre.nutritionist.domain.model.AnalyticsPeriod

/**
 * UI-модель столбца дневного графика калорий.
 */
data class DailyBarUiModel(
    val dayLabel: String,
    val caloriesText: String,
    val heightFraction: Float,
    val isTargetMet: Boolean,
)

/**
 * UI-модель аналитики питания и бюджета.
 */
data class StatisticsSuccessUiModel(
    val caloriesSummaryText: String,
    val targetCaloriesText: String,
    val complianceBadgeText: String,
    val isComplianceHigh: Boolean,
    val caloriesChartBars: List<DailyBarUiModel>,
    val macroProteinsText: String,
    val macroFatsText: String,
    val macroCarbsText: String,
    val proteinsFraction: Float,
    val fatsFraction: Float,
    val carbsFraction: Float,
    val totalSpentText: String,
    val totalBudgetText: String,
    val savedText: String,
    val budgetBurnPercentText: String,
    val budgetBurnFraction: Float,
    val averageMealCostText: String,
)

/**
 * Состояние экрана аналитики для Compose UI слоя.
 */
data class StatisticsUiState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.WEEK,
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val model: StatisticsSuccessUiModel) : Content
        data class Error(val message: String) : Content
    }
}
