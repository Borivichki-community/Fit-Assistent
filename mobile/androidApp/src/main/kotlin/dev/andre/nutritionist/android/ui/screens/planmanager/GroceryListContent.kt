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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.domain.model.GroceryItemId
import dev.andre.nutritionist.presentation.planmanager.GroceryCategoryUiModel
import dev.andre.nutritionist.presentation.planmanager.GroceryItemUiModel

/**
 * Содержимое списка покупок на неделю в стиле Aether Zero-G Luxury.
 */
@Composable
fun GroceryListSection(
    categories: List<GroceryCategoryUiModel>,
    progressText: String,
    progressPercent: Float,
    totalPriceText: String,
    onToggleGrocery: (GroceryItemId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("grocery_list_section"),
    ) {
        // Карточка прогресса покупок
        Surface(
            modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = stringResource(R.string.plan_manager_groceries_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(Spacing.pill)),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.plan_manager_estimated_sum),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = totalPriceText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Группы категорий продуктов
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            categories.forEach { category ->
                GroceryCategoryBlock(
                    category = category,
                    onToggleGrocery = onToggleGrocery,
                )
            }
        }
    }
}

/**
 * Блок одной категории продуктов (например, "Овощи и зелень").
 */
@Composable
fun GroceryCategoryBlock(
    category: GroceryCategoryUiModel,
    onToggleGrocery: (GroceryItemId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("grocery_category_${category.categoryName}"),
        shape = RoundedCornerShape(Spacing.md),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, GlassBorderLevel2),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
        ) {
            // Заголовок категории
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = category.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = category.countText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Список позиций категории
            category.items.forEach { item ->
                GroceryItemRow(
                    item = item,
                    onToggle = { onToggleGrocery(item.id) },
                )
            }
        }
    }
}

/**
 * Строка отдельного продукта с чекбоксом.
 */
@Composable
fun GroceryItemRow(
    item: GroceryItemUiModel,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.sm))
            .clickable(onClick = onToggle)
            .padding(horizontal = Spacing.xs, vertical = 2.dp)
            .testTag("grocery_item_${item.id.raw}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.isBought,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None,
                color = if (item.isBought) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = item.amount,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Surface(
            shape = RoundedCornerShape(Spacing.pill),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, GlassBorderLevel1),
        ) {
            Text(
                text = item.estimatedPriceText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            )
        }
    }
}
