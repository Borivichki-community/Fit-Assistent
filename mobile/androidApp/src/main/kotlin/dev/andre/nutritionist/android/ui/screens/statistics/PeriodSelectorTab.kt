package dev.andre.nutritionist.android.ui.screens.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.domain.model.AnalyticsPeriod

/**
 * Переключатель периода аналитики (Неделя / Месяц) в стиле Aether Zero-G Luxury.
 */
@Composable
fun PeriodSelectorTab(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("statistics_period_selector"),
        shape = RoundedCornerShape(Spacing.cardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, GlassBorderLevel1),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val periods = listOf(
                AnalyticsPeriod.WEEK to "Неделя",
                AnalyticsPeriod.MONTH to "Месяц",
            )

            periods.forEach { (period, title) ->
                val isSelected = selectedPeriod == period
                Surface(
                    shape = RoundedCornerShape(Spacing.buttonRadius),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else GlassBorderLevel1,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPeriodSelected(period) }
                        .testTag("statistics_period_${period.name}"),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = Spacing.small),
                    )
                }
            }
        }
    }
}
