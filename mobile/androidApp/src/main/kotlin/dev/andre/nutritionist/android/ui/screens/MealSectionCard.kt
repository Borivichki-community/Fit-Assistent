package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.theme.FitThemeTokens
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.model.DiaryMealSectionUiModel

/**
 * Сворачиваемая секция приёма пищи в дизайне fitapp_1:
 * - Заголовок слота (цветной бейдж времени суток + точка + подпись + `+` и шеврон)
 * - Развёрнутая карточка блюд ([MealItemRow]), либо пустой AI-блок (завтрак), либо компактный плейсхолдер.
 */
@Composable
fun MealSectionCard(
    meal: DiaryMealSectionUiModel,
    onAddMealClick: (MealType) -> Unit,
    onToggleExpand: (MealType) -> Unit,
    onMealItemClick: (MealRecordId) -> Unit,
    onDeleteRecordClick: (MealRecordId) -> Unit = {},
    onAiGenerateClick: (MealType) -> Unit = {},
    onQuickPresetClick: (MealType, String) -> Unit = { _, _ -> },
    onStubActionClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val extendedColors = FitThemeTokens.colors

    val slotIcon = when (meal.type) {
        MealType.BREAKFAST -> FitIcons.Sun
        MealType.LUNCH -> FitIcons.Noon
        MealType.DINNER -> FitIcons.Sunset
        MealType.SNACK -> FitIcons.Moon
    }

    val slotAccent = when (meal.type) {
        MealType.BREAKFAST -> extendedColors.activityAccent
        MealType.LUNCH -> extendedColors.budgetAccent
        MealType.DINNER -> extendedColors.sunsetAccent
        MealType.SNACK -> extendedColors.planAccent
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Внешняя панель заголовка слота
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .clickable { onToggleExpand(meal.type) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = slotAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, slotAccent.copy(alpha = 0.25f)),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = slotIcon,
                            contentDescription = meal.title,
                            tint = slotAccent,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )

                Text(
                    text = meal.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (meal.type != MealType.BREAKFAST || meal.entries.isNotEmpty()) {
                    Surface(
                        onClick = { onAddMealClick(meal.type) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("add_meal_${meal.type.name}"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = FitIcons.Plus,
                                contentDescription = "+",
                                tint = extendedColors.emeraldAccent,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }

                Surface(
                    onClick = { onToggleExpand(meal.type) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("toggle_meal_${meal.type.name}"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (meal.isExpanded) FitIcons.ChevronUp else FitIcons.ChevronDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }

        if (meal.isExpanded) {
            if (meal.entries.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    meal.entries.forEach { entry ->
                        MealItemRow(
                            entry = entry,
                            onClick = { onMealItemClick(entry.id) },
                            onDeleteClick = { onDeleteRecordClick(entry.id) },
                            onFavoriteClick = onStubActionClick,
                            onCameraClick = onStubActionClick,
                            onEditClick = onStubActionClick,
                            onBookmarkClick = onStubActionClick,
                        )
                    }
                }
            } else {
                when (meal.type) {
                    MealType.BREAKFAST -> EmptyAiMealSlotCard(
                        onAiGenerateClick = { onAiGenerateClick(meal.type) },
                        onCreateManualClick = { onAddMealClick(meal.type) },
                        onQuickPresetClick = { preset -> onQuickPresetClick(meal.type, preset) },
                    )
                    MealType.LUNCH -> CompactEmptyMealSlotCard(
                        icon = FitIcons.MealForkKnife,
                        title = stringResource(R.string.diary_empty_dinner_title),
                        subtitle = stringResource(R.string.diary_empty_lunch_subtitle),
                        onFillClick = { onAddMealClick(meal.type) },
                    )
                    MealType.DINNER -> CompactEmptyMealSlotCard(
                        icon = FitIcons.MenuLines,
                        title = stringResource(R.string.diary_empty_dinner_title),
                        subtitle = stringResource(R.string.diary_empty_dinner_subtitle),
                        onFillClick = { onAddMealClick(meal.type) },
                    )
                    MealType.SNACK -> CompactEmptyMealSlotCard(
                        icon = FitIcons.Clock,
                        title = stringResource(R.string.diary_empty_snack_title),
                        subtitle = stringResource(R.string.diary_empty_snack_subtitle),
                        onFillClick = { onAddMealClick(meal.type) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyAiMealSlotCard(
    onAiGenerateClick: () -> Unit,
    onCreateManualClick: () -> Unit,
    onQuickPresetClick: (String) -> Unit,
) {
    val extendedColors = FitThemeTokens.colors
    val shadowElevation = if (extendedColors.isDark) 0.dp else 2.dp
    val primaryButtonContainer = MaterialTheme.colorScheme.primaryContainer
    val primaryButtonContent = if (extendedColors.isDark) {
        MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    val oatmealLabel = stringResource(R.string.diary_preset_oatmeal)
    val omeletteLabel = stringResource(R.string.diary_preset_omelette)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
        shadowElevation = shadowElevation,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Мягкое фоновое свечение в верхней части карточки
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 160.dp, height = 72.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                extendedColors.emeraldAccent.copy(alpha = 0.14f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = extendedColors.emeraldAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, extendedColors.emeraldAccent.copy(alpha = 0.25f)),
                    modifier = Modifier.size(54.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = FitIcons.MealForkKnife,
                            contentDescription = null,
                            tint = extendedColors.emeraldAccent,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.diary_empty_ai_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.diary_empty_ai_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onAiGenerateClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonContainer,
                        contentColor = primaryButtonContent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ai_generate_button"),
                ) {
                    Icon(
                        imageVector = FitIcons.Sparkle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.diary_btn_ai_generate),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    onClick = onCreateManualClick,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("add_meal_BREAKFAST"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.diary_btn_create_manually),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.diary_quick_presets_label),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    QuickPresetChip(
                        label = oatmealLabel,
                        onClick = { onQuickPresetClick(oatmealLabel) },
                    )
                    QuickPresetChip(
                        label = omeletteLabel,
                        onClick = { onQuickPresetClick(omeletteLabel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPresetChip(
    label: String,
    onClick: () -> Unit,
) {
    val extendedColors = FitThemeTokens.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun CompactEmptyMealSlotCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onFillClick: () -> Unit,
) {
    val extendedColors = FitThemeTokens.colors
    val shadowElevation = if (extendedColors.isDark) 0.dp else 2.dp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
        shadowElevation = shadowElevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = onFillClick,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, extendedColors.emeraldAccent.copy(alpha = 0.25f)),
            ) {
                Text(
                    text = stringResource(R.string.diary_btn_fill),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    ),
                    color = extendedColors.emeraldAccent,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
