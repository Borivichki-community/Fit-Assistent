package dev.andre.nutritionist.android.ui.screens

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
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing

/**
 * Сводная плашка целевых макронутриентов и калоража (KPI Bar) с бэкенда.
 * Отображает 5 метрик в единую горизонтальную строку:
 * Жиры, Углев, Белк, Цель (%), Калории.
 */
@Composable
fun DashboardNutritionKpiBar(
    summary: NutritionSummaryUiModel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(Spacing.cardRadius),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = Spacing.medium),
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
                modifier = Modifier.weight(1f),
            )

            // 5. Калории
            NutritionKpiColumn(
                label = stringResource(R.string.macro_calories),
                value = summary.caloriesConsumed.toString(),
                target = "/ ${summary.caloriesTarget}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NutritionKpiColumn(
    label: String,
    value: String,
    target: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(Spacing.extraSmall))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        if (target != null) {
            Text(
                text = target,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
