package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.ErrorContent
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.LoadingContent
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.android.ui.theme.SurfaceDark
import dev.andre.nutritionist.android.ui.theme.TextMidEmphasis
import dev.andre.nutritionist.presentation.addmeal.AddMealSuccessUiModel
import dev.andre.nutritionist.presentation.addmeal.AddMealUiState
import dev.andre.nutritionist.presentation.addmeal.FoodCategoryUiModel
import dev.andre.nutritionist.presentation.addmeal.FoodItemUiModel
import dev.andre.nutritionist.presentation.addmeal.PortionSummaryUiModel

/**
 * Модальная шторка (Modal BottomSheet) добавления блюда в стиле Aether Zero-G Luxury.
 * Полностью соответствует screenshots/темная тема/всплывающее окно добавления рецепта.png и fitapp_4/code.html.
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
    onNavigateToSavedRecipes: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .background(
                        color = Color(0xFF32353C),
                        shape = RoundedCornerShape(Spacing.pill),
                    ),
            )
        },
        modifier = modifier,
    ) {
        when (val content = uiState.content) {
            AddMealUiState.Content.Loading -> {
                LoadingContent(modifier = Modifier.fillMaxWidth().height(360.dp))
            }
            is AddMealUiState.Content.Error -> {
                ErrorContent(
                    message = content.message,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth().height(360.dp),
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
                    onNavigateToSavedRecipes = onNavigateToSavedRecipes,
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
    modifier: Modifier = Modifier,
    onScanBarcode: () -> Unit = {},
    onAiCamera: () -> Unit = {},
    onNavigateToSavedRecipes: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(scrollState),
    ) {
        // 1. Верхний заголовок модалки с кнопкой закрытия
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.add_meal_title_format, data.mealTypeTitle),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = Color.White,
                )
                Text(
                    text = "Выберите из каталога или сгенерируйте с ИИ",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMidEmphasis,
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF191F2B))
                    .border(BorderStroke(1.dp, GlassBorderLevel2), CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FitIcons.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Строка поиска в стиле Obsidian Capsule с кнопкой сканирования штрихкода
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
                    color = TextMidEmphasis,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = FitIcons.Search,
                    contentDescription = null,
                    tint = TextMidEmphasis,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    if (data.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = FitIcons.Close,
                                contentDescription = "Clear",
                                tint = TextMidEmphasis,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    IconButton(onClick = onScanBarcode) {
                        Icon(
                            imageVector = FitIcons.Barcode,
                            contentDescription = "Barcode",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0B0E14),
                unfocusedContainerColor = Color(0xFF0B0E14),
                focusedBorderColor = EmeraldAccent,
                unfocusedBorderColor = GlassBorderLevel1,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Сетка из 3 карточек быстрых действий (Сгенерировать с ИИ, Сделать фото, Умный ассистент)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Карточка 1: Сгенерировать с ИИ
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAiCamera),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0E281D), Color(0xFF091A14)),
                            ),
                        )
                        .padding(12.dp),
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Sparkles,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Сгенерировать\nс ИИ",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 16.sp,
                            ),
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Под КБЖУ",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = EmeraldAccent,
                        )
                    }
                }
            }

            // Карточка 2: Сделать фото
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAiCamera),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
                border = BorderStroke(1.dp, GlassBorderLevel1),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1B202A)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Camera,
                                contentDescription = null,
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Сделать\nфото",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 16.sp,
                            ),
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Анализ блюда",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextMidEmphasis,
                        )
                    }
                }
            }

            // Карточка 3: Умный ассистент
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onScanBarcode),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color(0xFFE4C277).copy(alpha = 0.35f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF261D0C), Color(0xFF1A1408)),
                            ),
                        )
                        .padding(12.dp),
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE4C277).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Lightning,
                                contentDescription = null,
                                tint = Color(0xFFE4C277),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Умный\nассистент",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 16.sp,
                            ),
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Голосовой ввод",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFFE4C277),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Карточка «Кулинарная книга / Моя сохранённая еда»
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToSavedRecipes),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
            border = BorderStroke(1.dp, GlassBorderLevel1),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B202A)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = FitIcons.Bookmark,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column {
                        Text(
                            text = "Кулинарная книга",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = "Моя сохранённая еда и рецепты",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMidEmphasis,
                        )
                    }
                }

                Icon(
                    imageVector = FitIcons.ChevronRight,
                    contentDescription = null,
                    tint = TextMidEmphasis,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Горизонтальный ряд категорий (Luxury Pills)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("categories_row")
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            data.categories.forEach { category ->
                val isSelected = category.isSelected
                Surface(
                    onClick = { onCategorySelect(category.title) },
                    shape = RoundedCornerShape(Spacing.pill),
                    color = if (isSelected) EmeraldAccent else Color(0xFF191F2B),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) EmeraldAccent else GlassBorderLevel1,
                    ),
                    modifier = Modifier.testTag("category_tab_${category.title}"),
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (isSelected) Color(0xFF091A14) else Color(0xFFE2E8F0),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Список продуктов
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            data.foods.forEach { food ->
                FoodItemRow(
                    food = food,
                    onClick = { onFoodSelect(food) },
                )
            }
        }

        // 7. Блок выбора граммовки (если продукт выбран)
        val portionSummary = data.portionSummary
        if (portionSummary != null) {
            Spacer(modifier = Modifier.height(14.dp))
            PortionSelectorCard(
                portionSummary = portionSummary,
                onPortionChange = onPortionChange,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 8. Главная кнопка действия (Luminous Emerald CTA)
        Button(
            onClick = onConfirmAdd,
            enabled = data.isAddEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("add_meal_confirm_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldAccent,
                contentColor = Color(0xFF091A14),
                disabledContainerColor = Color(0xFF222834),
                disabledContentColor = TextMidEmphasis,
            ),
        ) {
            Text(
                text = data.addButtonText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                ),
            )
        }
    }
}

/**
 * Строка отдельного продукта питания в каталоге поиска в стиле Obsidian Card.
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (food.isSelected) {
                Color(0xFF0E281D)
            } else {
                CardDarkSurface
            },
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (food.isSelected) EmeraldAccent.copy(alpha = 0.5f) else GlassBorderLevel1,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                if (food.brand.isNotBlank()) {
                    Text(
                        text = food.brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMidEmphasis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = food.macroDescription,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMidEmphasis,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${food.caloriesPer100g}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = EmeraldAccent,
                )
                Text(
                    text = "ккал / 100г",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextMidEmphasis,
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
        border = BorderStroke(1.dp, GlassBorderLevel2),
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
                    text = stringResource(R.string.portion_size_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Text(
                    text = portionSummary.caloriesLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = EmeraldAccent,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Пресеты граммовок: 50г, 100г, 150г, 200г, 300г
            val presets = listOf(50.0, 100.0, 150.0, 200.0, 300.0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                presets.forEach { grams ->
                    val isSelected = portionSummary.portionGrams == grams
                    Surface(
                        onClick = { onPortionChange(grams) },
                        shape = RoundedCornerShape(Spacing.pill),
                        color = if (isSelected) {
                            Color(0xFF0E281D)
                        } else {
                            Color(0xFF1B202A)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) EmeraldAccent else GlassBorderLevel1,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("portion_preset_${grams.toInt()}"),
                    ) {
                        Text(
                            text = "${grams.toInt()}г",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) EmeraldAccent else Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Сводная строка макросов порции
            Text(
                text = portionSummary.macroSummaryText,
                style = MaterialTheme.typography.labelSmall,
                color = TextMidEmphasis,
            )
        }
    }
}
