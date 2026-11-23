package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.TextMidEmphasis
import dev.andre.nutritionist.presentation.dashboard.NutritionSummaryUiModel


/**
 * Сводная плашка суточной нормы КБЖУ и бюджета в стиле Aether Zero-G Luxury.
 * Полностью соответствует screenshots/темная тема/дневник.png и fitapp_1/code.html.
 */
@Composable
fun DashboardNutritionKpiBar(
    summary: NutritionSummaryUiModel,
    modifier: Modifier = Modifier,
    totalCostLabel: String = "1575 ₽",
    budgetLimitLabel: String = "Бюджет: ~1800 ₽",
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardDarkSurface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, GlassBorderLevel1),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Верхняя строка заголовка: СУТОЧНАЯ НОРМА + иконка со звездочкой
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "СУТОЧНАЯ НОРМА",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = TextMidEmphasis,
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0E281D))
                        .border(BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f)), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FitIcons.Sparkles,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Сетка 5 колонок КБЖУ: Жиры, Углеводы, Белки, Цель %, Калории
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                // 1. Жиры (Fats)
                NutritionKpiColumn(
                    label = stringResource(R.string.macro_fats),
                    value = summary.fats.displayCurrent,
                    target = "/ ${summary.fats.displayTarget}",
                    modifier = Modifier.weight(1f),
                )

                // 2. Углеводы (Carbs)
                NutritionKpiColumn(
                    label = stringResource(R.string.macro_carbs),
                    value = summary.carbs.displayCurrent,
                    target = "/ ${summary.carbs.displayTarget}",
                    modifier = Modifier.weight(1f),
                )

                // 3. Белки (Protein)
                NutritionKpiColumn(
                    label = stringResource(R.string.macro_proteins),
                    value = summary.proteins.displayCurrent,
                    target = "/ ${summary.proteins.displayTarget}",
                    modifier = Modifier.weight(1f),
                )

                // 4. Цель % (Goal)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            BorderStroke(1.dp, Color(0xFF262C38)),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.macro_goal_percent),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextMidEmphasis,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${summary.percentCompleted}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent,
                            ),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "день",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextMidEmphasis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // 5. Калории (Calories)
                NutritionKpiColumn(
                    label = stringResource(R.string.macro_calories),
                    value = summary.caloriesConsumed.toString(),
                    target = "/ ${summary.caloriesTarget}",
                    isDataMetric = true,
                    modifier = Modifier.weight(1.1f),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = GlassBorderLevel1)
            Spacer(modifier = Modifier.height(10.dp))

            // Нижняя строка: Общая стоимость блюд + Бюджет
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0E281D))
                            .border(BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f)), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = FitIcons.Wallet,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Общая стоимость блюд: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0B7C3),
                    )
                    Text(
                        text = totalCostLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B1F2B),
                    border = BorderStroke(1.dp, Color(0xFF2B3242)),
                ) {
                    Text(
                        text = budgetLimitLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
            color = TextMidEmphasis,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            style = if (isDataMetric) {
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            } else if (isHighlight) {
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldAccent,
                )
            } else {
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        if (target != null) {
            Text(
                text = target,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMidEmphasis,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
