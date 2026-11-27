package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.theme.FitThemeTokens
import dev.andre.nutritionist.presentation.model.MealRecordUiModel

/**
 * Карточка добавленного блюда/товара в слоте приёма пищи по макету fitapp_1.
 * Отображает реальные расчёты из [MealRecordUiModel]: КБЖУ, стоимость, время и долю в дневном балансе.
 */
@Composable
fun MealItemRow(
    entry: MealRecordUiModel,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val extendedColors = FitThemeTokens.colors
    val shadowElevation = if (extendedColors.isDark) 0.dp else 2.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("meal_item_${entry.id.raw}"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
        shadowElevation = shadowElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(
                            imageVector = FitIcons.Heart,
                            contentDescription = stringResource(R.string.diary_favorite),
                            tint = extendedColors.emeraldAccent,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(onClick = onFavoriteClick),
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = entry.caloriesLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                            ),
                            color = extendedColors.emeraldAccent,
                        )
                        val costLabel = entry.costLabel
                        if (costLabel != null) {
                            DotSeparator()
                            Text(
                                text = costLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DotSeparator()
                        Text(
                            text = entry.durationLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text(
                        text = entry.compactMacrosLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        onClick = onDeleteClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("delete_meal_${entry.id.raw}"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = FitIcons.Close,
                                contentDescription = stringResource(R.string.diary_delete_meal),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }

                    DishThumbnailCircle()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Плашка доли блюда в дневном балансе
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(extendedColors.emeraldAccent),
                        )
                        Text(
                            text = stringResource(R.string.diary_balance_share_title),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = entry.dailyBalanceShareLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = extendedColors.emeraldAccent,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = extendedColors.glassBorderLevel1)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
                    ) {
                        Text(
                            text = entry.mealTypeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = extendedColors.emeraldAccent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, extendedColors.emeraldAccent.copy(alpha = 0.25f)),
                    ) {
                        Text(
                            text = stringResource(R.string.diary_tag_healthy),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = extendedColors.emeraldAccent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallCircleIconAction(
                        icon = FitIcons.Camera,
                        contentDescription = stringResource(R.string.btn_quick_camera),
                        onClick = onCameraClick,
                    )
                    SmallCircleIconAction(
                        icon = FitIcons.Edit,
                        contentDescription = stringResource(R.string.diary_edit),
                        onClick = onEditClick,
                    )
                    Surface(
                        onClick = onBookmarkClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, extendedColors.emeraldAccent.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = FitIcons.Bookmark,
                                contentDescription = null,
                                tint = extendedColors.emeraldAccent,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = stringResource(R.string.diary_btn_in_favorites),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = extendedColors.emeraldAccent,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DotSeparator() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun SmallCircleIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val extendedColors = FitThemeTokens.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
        modifier = Modifier.size(28.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun DishThumbnailCircle() {
    val extendedColors = FitThemeTokens.colors
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.5.dp, extendedColors.glassBorderLevel2),
        modifier = Modifier.size(58.dp),
    ) {
        Box(
            modifier = Modifier.background(
                Brush.radialGradient(
                    colors = listOf(
                        extendedColors.emeraldAccent.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = FitIcons.Bowl,
                contentDescription = null,
                tint = extendedColors.emeraldAccent,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
