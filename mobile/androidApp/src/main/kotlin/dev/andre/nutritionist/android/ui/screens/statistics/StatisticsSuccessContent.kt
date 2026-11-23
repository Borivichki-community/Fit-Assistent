package dev.andre.nutritionist.android.ui.screens.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.domain.model.AnalyticsPeriod
import dev.andre.nutritionist.presentation.statistics.StatisticsSuccessUiModel

/**
 * Контент экрана статистики в состоянии успешной загрузки.
 */
@Composable
fun StatisticsSuccessContent(
    selectedPeriod: AnalyticsPeriod,
    model: StatisticsSuccessUiModel,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.medium)
            .testTag("statistics_success_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        // Period selector
        PeriodSelectorTab(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected,
        )

        // Calories chart card
        CaloriesBarChartCard(
            caloriesSummaryText = model.caloriesSummaryText,
            targetCaloriesText = model.targetCaloriesText,
            complianceBadgeText = model.complianceBadgeText,
            isComplianceHigh = model.isComplianceHigh,
            bars = model.caloriesChartBars,
        )

        // Macro distribution card
        MacroDistributionCard(
            proteinsText = model.macroProteinsText,
            fatsText = model.macroFatsText,
            carbsText = model.macroCarbsText,
            proteinsFraction = model.proteinsFraction,
            fatsFraction = model.fatsFraction,
            carbsFraction = model.carbsFraction,
        )

        // Budget analytics card
        BudgetAnalyticsCard(
            totalSpentText = model.totalSpentText,
            totalBudgetText = model.totalBudgetText,
            savedText = model.savedText,
            budgetBurnPercentText = model.budgetBurnPercentText,
            budgetBurnFraction = model.budgetBurnFraction,
            averageMealCostText = model.averageMealCostText,
        )
    }
}
