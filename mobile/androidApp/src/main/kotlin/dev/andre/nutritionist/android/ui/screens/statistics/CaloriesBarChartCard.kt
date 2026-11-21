package dev.andre.nutritionist.android.ui.screens.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.presentation.statistics.DailyBarUiModel

/**
 * Карточка графика ежедневной калорийности в стиле Aether Zero-G Luxury.
 */
@Composable
fun CaloriesBarChartCard(
    caloriesSummaryText: String,
    targetCaloriesText: String,
    complianceBadgeText: String,
    isComplianceHigh: Boolean,
    bars: List<DailyBarUiModel>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("statistics_calories_card"),
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    Icon(
                        imageVector = FitIcons.Flame,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Калорийность рациона",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(Spacing.small),
                    color = if (isComplianceHigh) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    },
                ) {
                    Text(
                        text = complianceBadgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isComplianceHigh) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = Spacing.small, vertical = 2.dp),
                    )
                }
            }

            // Metric Overview
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Text(
                    text = caloriesSummaryText,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("statistics_calories_summary_text"),
                )
                Text(
                    text = targetCaloriesText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            // Bar Chart Area
            Surface(
                shape = RoundedCornerShape(Spacing.buttonRadius),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("statistics_calories_chart"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.small, vertical = Spacing.medium),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    bars.forEach { bar ->
                        val barHeightDp = (bar.heightFraction * 100).coerceIn(12f, 100f).dp
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                            modifier = Modifier.testTag("statistics_bar_${bar.dayLabel}"),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(if (bars.size > 14) 6.dp else 18.dp)
                                    .height(barHeightDp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (bar.isTargetMet) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        },
                                    ),
                            )
                            Text(
                                text = bar.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
