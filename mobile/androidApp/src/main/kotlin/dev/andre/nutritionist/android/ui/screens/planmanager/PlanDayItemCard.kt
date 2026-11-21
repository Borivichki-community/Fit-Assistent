package dev.andre.nutritionist.android.ui.screens.planmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.presentation.planmanager.PlanDayUiModel
import dev.andre.nutritionist.presentation.planmanager.PlanMealUiModel

/**
 * Расписание и приемы пищи выбранного дня плана питания в стиле Aether Zero-G Luxury.
 */
@Composable
fun PlanDayScheduleSection(
    days: List<PlanDayUiModel>,
    currentDay: PlanDayUiModel,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("plan_day_schedule_section"),
    ) {
        // Горизонтальный ряд дней недели (Пн, Вт, Ср, Чт, Пт, Сб, Вс)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            days.forEachIndexed { index, day ->
                val isSelected = day.isSelected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .testTag("day_selector_pill_$index")
                        .clickable { onSelectDay(index) },
                    shape = RoundedCornerShape(Spacing.sm),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else GlassBorderLevel1,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = day.shortDayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${day.dayNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Заголовок текущего выбранного дня и целевые калории
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = currentDay.dayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = RoundedCornerShape(Spacing.pill),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, GlassBorderLevel1),
            ) {
                Text(
                    text = currentDay.targetCaloriesText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Список приемов пищи дня
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            currentDay.meals.forEach { meal ->
                PlanMealItemCard(meal = meal)
            }
        }
    }
}

/**
 * Карточка отдельного приема пищи в расписании дня.
 */
@Composable
fun PlanMealItemCard(
    meal: PlanMealUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("plan_meal_item_${meal.mealType}"),
        shape = RoundedCornerShape(Spacing.md),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, GlassBorderLevel2),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(Spacing.pill),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, GlassBorderLevel1),
                ) {
                    Text(
                        text = meal.mealType,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
                    )
                }

                Text(
                    text = meal.caloriesText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = meal.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = "${meal.proteinsText} • ${meal.fatsText} • ${meal.carbsText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
