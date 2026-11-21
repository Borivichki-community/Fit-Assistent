package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.ErrorContent
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.LoadingContent
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.presentation.addmeal.AddMealSuccessUiModel
import dev.andre.nutritionist.presentation.addmeal.AddMealUiState
import dev.andre.nutritionist.presentation.addmeal.FoodCategoryUiModel
import dev.andre.nutritionist.presentation.addmeal.FoodItemUiModel
import dev.andre.nutritionist.presentation.addmeal.PortionSummaryUiModel

/**
 * Модальная шторка (Modal BottomSheet) добавления блюда в стиле Aether Zero-G Luxury.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealBottomSheet(
    uiState: AddMealUiState,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onFoodSelect: (FoodItemUiModel) -> Unit,
    onPortionChange: (Double) -> Unit,
    onConfirmAdd: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onScanBarcode: () -> Unit = {},
    onAiCamera: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = Spacing.squircleXl, topEnd = Spacing.squircleXl),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = Spacing.sm)
                    .size(width = 40.dp, height = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(Spacing.pill),
                    ),
            )
        },
        modifier = modifier,
    ) {
        when (val content = uiState.content) {
            AddMealUiState.Content.Loading -> {
                LoadingContent(modifier = Modifier.fillMaxWidth().height(320.dp))
            }
            is AddMealUiState.Content.Error -> {
                ErrorContent(
                    message = content.message,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                )
            }
            is AddMealUiState.Content.Success -> {
                AddMealSuccessContent(
                    data = content.data,
                    onQueryChange = onQueryChange,
                    onCategorySelect = onCategorySelect,
                    onFoodSelect = onFoodSelect,
                    onPortionChange = onPortionChange,
                    onConfirmAdd = onConfirmAdd,
                    onDismiss = onDismiss,
                    onScanBarcode = onScanBarcode,
                    onAiCamera = onAiCamera,
                )
            }
        }
    }
}

/**
 * Контент экрана добавления блюда в дневник питания.
 */
@Composable
fun AddMealSuccessContent(
    data: AddMealSuccessUiModel,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onFoodSelect: (FoodItemUiModel) -> Unit,
    onPortionChange: (Double) -> Unit,
    onConfirmAdd: () -> Unit,
    onDismiss: () -> Unit,
    onScanBarcode: () -> Unit = {},
    onAiCamera: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .padding(bottom = Spacing.lg)
            .verticalScroll(scrollState),
    ) {
        // Заголовок с кнопкой закрытия
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.add_meal_title_format, data.mealTypeTitle),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = FitIcons.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Поисковая строка в стиле Luxury Pill
        OutlinedTextField(
            value = data.searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_meal_search_input"),
            placeholder = {
                Text(
                    text = stringResource(R.string.add_meal_search_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = FitIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (data.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = FitIcons.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            shape = RoundedCornerShape(Spacing.pill),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedBorderColor = GlassBorderLevel1,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Быстрые кнопки: Штрихкод и AI Камера
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Surface(
                onClick = onScanBarcode,
                shape = RoundedCornerShape(Spacing.pill),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, GlassBorderLevel1),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.padding(vertical = Spacing.xs, horizontal = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = FitIcons.Barcode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(R.string.btn_scan_barcode),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Surface(
                onClick = onAiCamera,
                shape = RoundedCornerShape(Spacing.pill),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, GlassBorderLevel1),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.padding(vertical = Spacing.xs, horizontal = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = FitIcons.Camera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(R.string.btn_ai_camera),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Горизонтальный скролл категорий
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            data.categories.forEach { category ->
                val isSelected = category.isSelected
                Surface(
                    onClick = { onCategorySelect(category.title) },
                    shape = RoundedCornerShape(Spacing.pill),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else GlassBorderLevel1,
                    ),
                    modifier = Modifier.testTag("category_tab_${category.title}"),
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Список продуктов
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            data.foods.forEach { food ->
                FoodItemRow(
                    food = food,
                    onClick = { onFoodSelect(food) },
                )
            }
        }

        // Блок выбора граммовки (если продукт выбран)
        val portionSummary = data.portionSummary
        if (portionSummary != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            PortionSelectorCard(
                portionSummary = portionSummary,
                onPortionChange = onPortionChange,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Главная кнопка действия (Luminous Emerald CTA)
        Button(
            onClick = onConfirmAdd,
            enabled = data.isAddEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("add_meal_confirm_btn"),
            shape = RoundedCornerShape(Spacing.pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Text(
                text = data.addButtonText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

/**
 * Строка отдельного продукта питания в каталоге поиска.
 */
@Composable
fun FoodItemRow(
    food: FoodItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("food_item_${food.id}"),
        shape = RoundedCornerShape(Spacing.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (food.isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (food.isSelected) MaterialTheme.colorScheme.primaryContainer else GlassBorderLevel1,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (food.brand.isNotBlank()) {
                    Text(
                        text = food.brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = food.macroDescription,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${food.caloriesPer100g}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
                Text(
                    text = "ккал / 100г",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Карточка настройки размера порции с быстрыми чипсами и пересчётом КБЖУ.
 */
@Composable
fun PortionSelectorCard(
    portionSummary: PortionSummaryUiModel,
    onPortionChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
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
                    text = stringResource(R.string.portion_size_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = portionSummary.caloriesLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Пресеты граммовок: 50г, 100г, 150г, 200г, 300г
            val presets = listOf(50.0, 100.0, 150.0, 200.0, 300.0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                presets.forEach { grams ->
                    val isSelected = portionSummary.portionGrams == grams
                    Surface(
                        onClick = { onPortionChange(grams) },
                        shape = RoundedCornerShape(Spacing.pill),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else GlassBorderLevel1,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("portion_preset_${grams.toInt()}"),
                    ) {
                        Text(
                            text = "${grams.toInt()}г",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xs),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Сводная строка макросов порции
            Text(
                text = portionSummary.macroSummaryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
