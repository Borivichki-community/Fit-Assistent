package dev.andre.nutritionist.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.dashboard.MealSectionUiModel
import dev.andre.nutritionist.presentation.dashboard.formatMacroGrams

/**
 * Карточка секции приёма пищи (Завтрак, Обед, Ужин, Перекус/Другое) в стиле Aether Zero-G Luxury.
 * Выполнена на основе Level 1 Glass со скруглением squircle 24dp и плавной анимацией аккордеона.
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
        MealType.BREAKFAST -> MaterialTheme.colorScheme.secondary
        MealType.LUNCH -> MaterialTheme.colorScheme.tertiaryContainer
        MealType.DINNER -> MaterialTheme.colorScheme.primaryContainer
        MealType.SNACK -> MaterialTheme.colorScheme.tertiary
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
        shape = RoundedCornerShape(Spacing.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, GlassBorderLevel1),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            // Заголовок карточки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasEntries) { onToggleExpand(meal.type) }
                    .padding(vertical = Spacing.xs),
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

                    Spacer(modifier = Modifier.width(Spacing.md))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
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
                    // Мини-сводка макросов по приёму пищи (Жиры | Углев | Белк)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${stringResource(R.string.macro_carbs)}: ${formatMacroGrams(totalCarbs)}г",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${stringResource(R.string.macro_proteins)}: ${formatMacroGrams(totalProteins)}г",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.xs),
                        color = GlassBorderLevel1,
                    )

                    // Список съеденных блюд
                    meal.entries.forEach { entry ->
                        MealItemRow(
                            entry = entry,
                            onClick = { onMealItemClick(entry.id) },
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    // Быстрые действия: Копировать | + Сохранить Еду | + Камера
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
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_quick_copy),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                )
                            }

                            TextButton(onClick = { onSaveFoodClick(meal.type) }) {
                                Text(
                                    text = stringResource(R.string.btn_quick_save_food),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                )
                            }

                            TextButton(onClick = { onCameraClick(meal.type) }) {
                                Icon(
                                    imageVector = FitIcons.Camera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_quick_camera),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primaryContainer,
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
