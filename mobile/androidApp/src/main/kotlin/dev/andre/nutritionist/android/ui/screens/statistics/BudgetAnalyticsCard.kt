package dev.andre.nutritionist.android.ui.screens.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1

/**
 * Карточка финансовой аналитики рациона в стиле Aether Zero-G Luxury.
 */
@Composable
fun BudgetAnalyticsCard(
    totalSpentText: String,
    totalBudgetText: String,
    savedText: String,
    budgetBurnPercentText: String,
    budgetBurnFraction: Float,
    averageMealCostText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("statistics_budget_card"),
        shape = RoundedCornerShape(Spacing.cardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, GlassBorderLevel1),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Бюджет и экономия",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Surface(
                    shape = RoundedCornerShape(Spacing.small),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = savedText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .padding(horizontal = Spacing.small, vertical = 2.dp)
                            .testTag("statistics_budget_saved_text"),
                    )
                }
            }

            // Spend Overview
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Text(
                    text = totalSpentText,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("statistics_budget_spent_text"),
                )
                Text(
                    text = "из $totalBudgetText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            // Burn Rate Progress Bar
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LinearProgressIndicator(
                    progress = { budgetBurnFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (budgetBurnFraction <= 1f) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = budgetBurnPercentText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = averageMealCostText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.testTag("statistics_average_meal_cost_text"),
                    )
                }
            }
        }
    }
}
