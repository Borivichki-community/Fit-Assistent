package dev.andre.nutritionist.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.dashboard.MealSectionUiModel
import dev.andre.nutritionist.presentation.dashboard.formatMacroGrams

/**
 * Карточка секции приёма пищи (Завтрак, Обед, Ужин, Перекус/Другое) в стиле Aether Zero-G Luxury.
 * Соответствует скриншоту screenshots/темная тема/дневник.png и fitapp_1/code.html.
 */
@Composable
fun MealSectionCard(
    meal: MealSectionUiModel,
    onAddMealClick: (MealType) -> Unit,
    onToggleExpand: (MealType) -> Unit,
    onMealItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSaveFoodClick: (MealType) -> Unit = {},
    onCameraClick: (MealType) -> Unit = {},
    onCopyClick: (MealType) -> Unit = {},
) {
    val icon = when (meal.type) {
        MealType.BREAKFAST -> FitIcons.Sun
        MealType.LUNCH -> FitIcons.Noon
        MealType.DINNER -> FitIcons.Sunset
        MealType.SNACK -> FitIcons.Moon
    }

    val iconBoxBg = when (meal.type) {
        MealType.BREAKFAST -> Color(0xFF2C2214)
        MealType.LUNCH -> Color(0xFF14242C)
        MealType.DINNER -> Color(0xFF2B1D16)
        MealType.SNACK -> Color(0xFF241A2E)
    }

    val iconTint = when (meal.type) {
        MealType.BREAKFAST -> Color(0xFFFF9F1C)
        MealType.LUNCH -> Color(0xFF22D3EE)
        MealType.DINNER -> Color(0xFFFF7A45)
        MealType.SNACK -> Color(0xFFB072FF)
    }

    val slotSubtitle = when (meal.type) {
        MealType.BREAKFAST -> "Утренний слот"
        MealType.LUNCH -> "Дневной слот"
        MealType.DINNER -> "Вечерний слот"
        MealType.SNACK -> "Дополнительный слот"
    }

    val title = when (meal.type) {
        MealType.BREAKFAST -> stringResource(R.string.meal_breakfast)
        MealType.LUNCH -> stringResource(R.string.meal_lunch)
        MealType.DINNER -> stringResource(R.string.meal_dinner)
        MealType.SNACK -> stringResource(R.string.meal_snack)
    }

    val hasEntries = meal.entries.isNotEmpty() || meal.caloriesTotal > 0

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
                .padding(14.dp),
        ) {
            // Заголовок слота приёма пищи
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand(meal.type) }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Иконка в скругленном квадрате + Название + Слот
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconBoxBg)
                            .border(BorderStroke(1.dp, iconTint.copy(alpha = 0.3f)), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF64748B)),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = slotSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMidEmphasis,
                            )
                        }
                    }
                }

                // Правая часть: Калории, шеврон и кнопка добавления +
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    if (hasEntries) {
                        Column(
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = meal.caloriesTotal.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Text(
                                text = stringResource(R.string.macro_calories),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMidEmphasis,
                            )
                        }

                        IconButton(
                            onClick = { onToggleExpand(meal.type) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = if (meal.isExpanded) FitIcons.ChevronUp else FitIcons.ChevronDown,
                                contentDescription = null,
                                tint = TextMidEmphasis,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = { onAddMealClick(meal.type) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("add_meal_${meal.type.name}"),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B1F2B))
                                .border(BorderStroke(1.dp, GlassBorderLevel1), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Plus,
                                contentDescription = "+",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // Если слот пустой и это Завтрак (или раскрыт) — отображаем карточку генератора с ИИ
            if (!hasEntries && (meal.type == MealType.BREAKFAST || meal.isExpanded)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Вилка и нож в светящемся контейнере
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0E281D))
                            .border(BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f)), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = FitIcons.MealForkKnife,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Ничего не запланировано",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Вы можете сгенерировать персональный приём пищи с ИИ или составить меню вручную",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMidEmphasis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Кнопки действий: Сгенерировать с ИИ + Добавить блюдо
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { onAddMealClick(meal.type) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAccent,
                                contentColor = Color(0xFF003921),
                            ),
                        ) {
                            Icon(
                                imageVector = FitIcons.Sparkles,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Сгенерировать с ИИ",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }

                        Surface(
                            onClick = { onAddMealClick(meal.type) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1B1F2B),
                            border = BorderStroke(1.dp, Color(0xFF2B3242)),
                            modifier = Modifier.height(40.dp),
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "+ Добавить",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Быстрые пресеты блюд
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val presets = listOf(
                            "Быстрый омлет" to "200 ₽",
                            "Овсянка с ягодами" to "120 ₽",
                            "Творог с мёдом" to "150 ₽",
                        )
                        presets.forEach { (name, price) ->
                            Surface(
                                onClick = { onAddMealClick(meal.type) },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF161922),
                                border = BorderStroke(1.dp, GlassBorderLevel1),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE2E8F0),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = price,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldAccent,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Плавный раскрывающийся аккордеон с блюдами и быстрыми действиями
            AnimatedVisibility(
                visible = hasEntries && meal.isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val totalFats = meal.entries.sumOf { it.fats }
                    val totalCarbs = meal.entries.sumOf { it.carbs }
                    val totalProteins = meal.entries.sumOf { it.proteins }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        Text(
                            text = "${stringResource(R.string.macro_fats)}: ${formatMacroGrams(totalFats)}г",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMidEmphasis,
                        )
                        Text(
                            text = "${stringResource(R.string.macro_carbs)}: ${formatMacroGrams(totalCarbs)}г",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMidEmphasis,
                        )
                        Text(
                            text = "${stringResource(R.string.macro_proteins)}: ${formatMacroGrams(totalProteins)}г",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMidEmphasis,
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.xs),
                        color = GlassBorderLevel1,
                    )

                    meal.entries.forEach { entry ->
                        MealItemRow(
                            entry = entry,
                            onClick = { onMealItemClick(entry.id) },
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            TextButton(onClick = { onCopyClick(meal.type) }) {
                                Icon(
                                    imageVector = FitIcons.Copy,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_quick_copy),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = EmeraldAccent,
                                )
                            }

                            TextButton(onClick = { onSaveFoodClick(meal.type) }) {
                                Text(
                                    text = stringResource(R.string.btn_quick_save_food),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = EmeraldAccent,
                                )
                            }

                            TextButton(onClick = { onCameraClick(meal.type) }) {
                                Icon(
                                    imageVector = FitIcons.Camera,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_quick_camera),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = EmeraldAccent,
                                )
                            }
                        }

                        IconButton(
                            onClick = { onToggleExpand(meal.type) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = FitIcons.ChevronUp,
                                contentDescription = null,
                                tint = TextMidEmphasis,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
