package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.presentation.dashboard.NutritionSummaryUiModel

/**
 * Сводная плашка целевых макронутриентов и калоража (KPI Bar) в стиле Aether Zero-G Luxury.
 * Отображает 5 метрик в горизонтальный консольный ряд:
 * Жиры, Углев, Белк, Цель (%), Калории с акцентом data-metric.
 */
@Composable
fun DashboardNutritionKpiBar(
    summary: NutritionSummaryUiModel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, GlassBorderLevel1),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. Жиры
            NutritionKpiColumn(
                label = stringResource(R.string.macro_fats),
                value = summary.fats.displayCurrent,
                target = "/ ${summary.fats.displayTarget}",
                modifier = Modifier.weight(1f),
            )

            // 2. Углеводы
            NutritionKpiColumn(
                label = stringResource(R.string.macro_carbs),
                value = summary.carbs.displayCurrent,
                target = "/ ${summary.carbs.displayTarget}",
                modifier = Modifier.weight(1f),
            )

            // 3. Белки
            NutritionKpiColumn(
                label = stringResource(R.string.macro_proteins),
                value = summary.proteins.displayCurrent,
                target = "/ ${summary.proteins.displayTarget}",
                modifier = Modifier.weight(1f),
            )

            // 4. Цель %
            NutritionKpiColumn(
                label = stringResource(R.string.macro_goal_percent),
                value = "${summary.percentCompleted}%",
                target = null,
                isHighlight = true,
                modifier = Modifier.weight(1f),
            )

            // 5. Калории
            NutritionKpiColumn(
                label = stringResource(R.string.macro_calories),
                value = summary.caloriesConsumed.toString(),
                target = "/ ${summary.caloriesTarget}",
                isDataMetric = true,
                modifier = Modifier.weight(1.1f),
            )
        }
    }
}

@Composable
private fun NutritionKpiColumn(
    label: String,
    value: String,
    target: String?,
    isHighlight: Boolean = false,
    isDataMetric: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = value,
            style = if (isDataMetric) {
                MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
            } else if (isHighlight) {
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        if (target != null) {
            Text(
                text = target,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
