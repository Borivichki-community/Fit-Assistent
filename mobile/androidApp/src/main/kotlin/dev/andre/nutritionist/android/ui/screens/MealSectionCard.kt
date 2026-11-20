package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing

/**
 * Карточка секции приёма пищи (Завтрак, Обед, Ужин, Перекус/Другое).
 * Поддерживает компактное состояние (когда блюд нет) и развёрнутое состояние со списком блюд и быстрыми действиями.
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

    val iconTint = when (meal.type) {
        MealType.BREAKFAST -> Color(0xFFFFD54F)
        MealType.LUNCH -> Color(0xFF4FC3F7)
        MealType.DINNER -> Color(0xFFFF8A65)
        MealType.SNACK -> Color(0xFFBA68C8)
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(Spacing.cardRadius),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        ) {
            // Заголовок карточки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasEntries) { onToggleExpand(meal.type) }
                    .padding(vertical = Spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Иконка + Название приёма пищи
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp),
                    )

                    Spacer(modifier = Modifier.width(Spacing.medium))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Правая часть: Калории, шеврон и кнопка добавления +
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                ) {
                    if (hasEntries) {
                        Column(
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = meal.caloriesTotal.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.macro_calories),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        IconButton(
                            onClick = { onToggleExpand(meal.type) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = if (meal.isExpanded) FitIcons.ChevronUp else FitIcons.ChevronDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        Icon(
                            imageVector = FitIcons.Plus,
                            contentDescription = "+",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // Раскрывающаяся часть с блюдами и быстрыми действиями
            if (hasEntries && meal.isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Мини-сводка макросов по приёму пищи (Жиры | Углев | Белк)
                    val totalFats = meal.entries.sumOf { it.fats }
                    val totalCarbs = meal.entries.sumOf { it.carbs }
                    val totalProteins = meal.entries.sumOf { it.proteins }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.extraSmall),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.large),
                    ) {
                        Text(
                            text = formatMacroGrams(totalFats),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatMacroGrams(totalCarbs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatMacroGrams(totalProteins),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.extraSmall),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )

                    // Список блюд
                    meal.entries.forEach { entry ->
                        MealItemRow(
                            entry = entry,
                            onClick = { onMealItemClick(entry.id) },
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.small))

                    // Быстрые действия: Копировать | + Сохранить Еду | + Камера
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                        ) {
                            TextButton(onClick = { onCopyClick(meal.type) }) {
                                Icon(
                                    imageVector = FitIcons.Copy,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_quick_copy),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF00E676),
                                )
                            }

                            TextButton(onClick = { onSaveFoodClick(meal.type) }) {
                                Text(
                                    text = stringResource(R.string.btn_quick_save_food),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF00E676),
                                )
                            }

                            TextButton(onClick = { onCameraClick(meal.type) }) {
                                Text(
                                    text = stringResource(R.string.btn_quick_camera),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF00E676),
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
