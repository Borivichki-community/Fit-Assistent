package dev.andre.nutritionist.android.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.theme.FitThemeTokens
import dev.andre.nutritionist.presentation.model.DailyNutritionUiModel

/**
 * Блок «СУТОЧНАЯ НОРМА» по макету fitapp_1:
 * - Заголовок + кнопка `+` справа
 * - 5-колоночная сетка (Fats, Carbs, Protein, Goal %, Calories) из [DailyNutritionUiModel]
 * - Нижняя строка: «Общая стоимость блюд: ...» + пилюля дневного бюджета
 */
@Composable
fun DashboardNutritionKpiBar(
    summary: DailyNutritionUiModel,
    onAddClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val extendedColors = FitThemeTokens.colors
    val shadowElevation = if (extendedColors.isDark) 0.dp else 2.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
        shadowElevation = shadowElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.diary_daily_norm_title),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                    onClick = onAddClick,
                    shape = CircleShape,
                    color = extendedColors.emeraldAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, extendedColors.emeraldAccent.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("daily_norm_add_button"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = FitIcons.Plus,
                            contentDescription = stringResource(R.string.action_add_plan),
                            tint = extendedColors.emeraldAccent,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NutritionKpiColumn(
                    label = stringResource(R.string.macro_fats),
                    value = summary.fatsConsumedValue,
                    sublabel = summary.fatsTargetShortLabel,
                    isAccentValue = false,
                    modifier = Modifier.weight(1f),
                )

                NutritionKpiColumn(
                    label = stringResource(R.string.macro_carbs),
                    value = summary.carbsConsumedValue,
                    sublabel = summary.carbsTargetShortLabel,
                    isAccentValue = false,
                    modifier = Modifier.weight(1f),
                )

                NutritionKpiColumn(
                    label = stringResource(R.string.macro_proteins),
                    value = summary.proteinsConsumedValue,
                    sublabel = summary.proteinsTargetShortLabel,
                    isAccentValue = false,
                    modifier = Modifier.weight(1f),
                )

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(38.dp)
                            .background(extendedColors.glassBorderLevel1),
                    )
                    NutritionKpiColumn(
                        label = stringResource(R.string.macro_goal_percent),
                        value = summary.caloriesProgressLabel,
                        sublabel = stringResource(R.string.diary_day_label),
                        isAccentValue = true,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(38.dp)
                            .background(extendedColors.glassBorderLevel1),
                    )
                }

                NutritionKpiColumn(
                    label = stringResource(R.string.macro_calories),
                    value = summary.caloriesConsumedValue,
                    sublabel = summary.caloriesTargetValue,
                    isAccentValue = false,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = extendedColors.glassBorderLevel1)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = extendedColors.emeraldAccent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, extendedColors.emeraldAccent.copy(alpha = 0.28f)),
                        modifier = Modifier.size(24.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = FitIcons.Wallet,
                                contentDescription = null,
                                tint = extendedColors.emeraldAccent,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }

                    val totalCostPrefix = stringResource(R.string.diary_total_cost_label)
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append(totalCostPrefix)
                                append(' ')
                            }
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                ),
                            ) {
                                append(summary.totalMealCostLabel)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        maxLines = 2,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
                ) {
                    Text(
                        text = summary.dailyBudgetLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionKpiColumn(
    label: String,
    value: String,
    sublabel: String,
    isAccentValue: Boolean,
    modifier: Modifier = Modifier,
) {
    val extendedColors = FitThemeTokens.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                fontWeight = if (isAccentValue) FontWeight.ExtraBold else FontWeight.Bold,
            ),
            color = if (isAccentValue) extendedColors.emeraldAccent else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Text(
            text = sublabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
