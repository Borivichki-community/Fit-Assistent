package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.theme.FitThemeTokens
import dev.andre.nutritionist.presentation.addmeal.AddMealEvent
import dev.andre.nutritionist.presentation.addmeal.AddMealItemKind
import dev.andre.nutritionist.presentation.addmeal.AddMealSearchItemUiModel
import dev.andre.nutritionist.presentation.addmeal.AddMealUiState

/**
 * Модальная шторка выбора продукта или рецепта по макету fitapp_4.
 * Полностью stateless: принимает [AddMealUiState] и отправляет [AddMealEvent.Ui].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealBottomSheet(
    uiState: AddMealUiState,
    onEvent: (AddMealEvent.Ui) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extendedColors = FitThemeTokens.colors

    ModalBottomSheet(
        onDismissRequest = { onEvent(AddMealEvent.Ui.Dismiss) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = null,
        modifier = modifier.testTag("add_meal_bottom_sheet"),
    ) {
        AddMealBottomSheetContent(
            uiState = uiState,
            onEvent = onEvent,
            borderColor = extendedColors.glassBorderLevel2,
        )
    }
}

/**
 * Внутреннее содержимое шторки @fitapp_4 (выделено для изолированного тестирования без окна ModalBottomSheet при необходимости).
 */
@Composable
internal fun AddMealBottomSheetContent(
    uiState: AddMealUiState,
    onEvent: (AddMealEvent.Ui) -> Unit,
    borderColor: Color = FitThemeTokens.colors.glassBorderLevel2,
    modifier: Modifier = Modifier,
) {
    val extendedColors = FitThemeTokens.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Drag handle по центру + круглая кнопка закрытия справа
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)),
            )

            Surface(
                onClick = { onEvent(AddMealEvent.Ui.Dismiss) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)
                    .testTag("add_meal_close_button"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = FitIcons.Close,
                        contentDescription = stringResource(R.string.add_meal_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // Поисковая строка продуктов и рецептов
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { onEvent(AddMealEvent.Ui.QueryChanged(it)) },
            placeholder = {
                Text(
                    text = stringResource(R.string.add_meal_search_placeholder),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                if (uiState.query.isNotEmpty()) {
                    IconButton(
                        onClick = { onEvent(AddMealEvent.Ui.ClearQuery) },
                        modifier = Modifier.testTag("add_meal_clear_search"),
                    ) {
                        Icon(
                            imageVector = FitIcons.Close,
                            contentDescription = stringResource(R.string.add_meal_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = extendedColors.emeraldAccent.copy(alpha = 0.55f),
                unfocusedBorderColor = extendedColors.glassBorderLevel1,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = extendedColors.emeraldAccent,
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_meal_search_input"),
        )

        when (val content = uiState.content) {
            AddMealUiState.Content.Idle -> {
                QuickActionsSection(onEvent = onEvent)
            }

            AddMealUiState.Content.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .testTag("add_meal_loading"),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = extendedColors.emeraldAccent)
                }
            }

            is AddMealUiState.Content.Success -> {
                SearchResultsSection(
                    items = uiState.items,
                    isSaving = content.isSaving,
                    onItemClick = { item ->
                        onEvent(AddMealEvent.Ui.ItemClicked(id = item.id, kind = item.kind))
                    },
                )
            }

            AddMealUiState.Content.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .testTag("add_meal_empty"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.add_meal_empty_results),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.add_meal_empty_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            is AddMealUiState.Content.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .testTag("add_meal_error"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = content.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = { onEvent(AddMealEvent.Ui.Retry) }) {
                        Text(
                            text = stringResource(R.string.retry_button),
                            color = extendedColors.emeraldAccent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onEvent: (AddMealEvent.Ui) -> Unit,
) {
    val extendedColors = FitThemeTokens.colors

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 3 колонки быстрых действий (Сгенерировать с ИИ, Сделать фото, Умный ассистент)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickActionTile(
                title = stringResource(R.string.add_meal_action_ai),
                icon = FitIcons.Sparkle,
                containerBrush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.emeraldAccent.copy(alpha = 0.14f),
                        extendedColors.emeraldAccent.copy(alpha = 0.05f),
                    ),
                ),
                borderColor = extendedColors.emeraldAccent.copy(alpha = 0.30f),
                iconBoxColor = extendedColors.emeraldAccent.copy(alpha = 0.16f),
                iconBoxBorderColor = extendedColors.emeraldAccent.copy(alpha = 0.30f),
                iconTint = extendedColors.emeraldAccent,
                textColor = MaterialTheme.colorScheme.onSurface,
                isBold = true,
                onClick = { onEvent(AddMealEvent.Ui.AiGenerateClicked) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("add_meal_stub_ai"),
            )

            QuickActionTile(
                title = stringResource(R.string.add_meal_action_photo),
                icon = FitIcons.Camera,
                containerBrush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ),
                borderColor = extendedColors.glassBorderLevel1,
                iconBoxColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                iconBoxBorderColor = extendedColors.glassBorderLevel1,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurface,
                isBold = false,
                onClick = { onEvent(AddMealEvent.Ui.TakePhotoClicked) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("add_meal_stub_photo"),
            )

            QuickActionTile(
                title = stringResource(R.string.add_meal_action_assistant),
                icon = FitIcons.Mic,
                containerBrush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.activityAccent.copy(alpha = 0.16f),
                        extendedColors.activityAccent.copy(alpha = 0.06f),
                    ),
                ),
                borderColor = extendedColors.activityAccent.copy(alpha = 0.34f),
                iconBoxColor = extendedColors.activityAccent,
                iconBoxBorderColor = extendedColors.activityAccent,
                iconTint = MaterialTheme.colorScheme.surfaceContainerLowest,
                textColor = if (extendedColors.isDark) {
                    extendedColors.activityAccent
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                isBold = true,
                onClick = { onEvent(AddMealEvent.Ui.VoiceAssistantClicked) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("add_meal_stub_assistant"),
            )
        }

        // Нижняя широкая кнопка «Моя сохранённая еда»
        Surface(
            onClick = { onEvent(AddMealEvent.Ui.SavedFoodClicked) },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_meal_stub_saved_food"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = extendedColors.emeraldAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, extendedColors.emeraldAccent.copy(alpha = 0.24f)),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = FitIcons.SavedFoodBox,
                            contentDescription = null,
                            tint = extendedColors.emeraldAccent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.add_meal_saved_food_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.add_meal_saved_food_subtitle),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = FitIcons.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    containerBrush: Brush,
    borderColor: Color,
    iconBoxColor: Color,
    iconBoxBorderColor: Color,
    iconTint: Color,
    textColor: Color,
    isBold: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerBrush)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBoxColor,
                border = BorderStroke(1.dp, iconBoxBorderColor),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchResultsSection(
    items: List<AddMealSearchItemUiModel>,
    isSaving: Boolean,
    onItemClick: (AddMealSearchItemUiModel) -> Unit,
) {
    val extendedColors = FitThemeTokens.colors

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isSaving) {
            LinearProgressIndicator(
                color = extendedColors.emeraldAccent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 340.dp)
                .testTag("add_meal_results_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = items,
                key = { "${it.kind.name}_${it.id}" },
            ) { item ->
                AddMealSearchResultRow(
                    item = item,
                    enabled = !isSaving,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
private fun AddMealSearchResultRow(
    item: AddMealSearchItemUiModel,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val extendedColors = FitThemeTokens.colors
    val badgeAccent = when (item.kind) {
        AddMealItemKind.PRODUCT -> extendedColors.emeraldAccent
        AddMealItemKind.RECIPE -> extendedColors.budgetAccent
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_meal_result_${item.kind.name}_${item.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Surface(
                        shape = CircleShape,
                        color = badgeAccent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.28f)),
                    ) {
                        Text(
                            text = item.typeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = badgeAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }

                Text(
                    text = "${item.portionLabel} • ${item.macrosLabel}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.caloriesLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = extendedColors.emeraldAccent,
                    maxLines = 1,
                )
                val priceLabel = item.priceLabel
                if (priceLabel != null) {
                    Text(
                        text = priceLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
