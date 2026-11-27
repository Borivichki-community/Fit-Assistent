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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.FitThemeTokens
import dev.andre.nutritionist.presentation.model.DayOfWeekUiModel

/**
 * Карточка недельного календаря в стиле fitapp_1:
 * месяц/год, бейдж номера недели, стрелки переключения недель (< / >) и 7 дней недели.
 */
@Composable
fun DashboardWeekStreakBar(
    monthYearLabel: String,
    weekNumberLabel: String,
    weekDays: List<DayOfWeekUiModel>,
    onDaySelected: (Long) -> Unit,
    onPreviousWeekClick: () -> Unit,
    onNextWeekClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extendedColors = FitThemeTokens.colors
    val shadowElevation = if (extendedColors.isDark) 0.dp else 2.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
        shadowElevation = shadowElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = monthYearLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (weekNumberLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = extendedColors.emeraldAccent.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = weekNumberLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.6.sp,
                                ),
                                color = extendedColors.emeraldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WeekArrowButton(
                        icon = FitIcons.ChevronLeft,
                        contentDescription = stringResource(R.string.diary_prev_week),
                        onClick = onPreviousWeekClick,
                        modifier = Modifier.testTag("calendar_prev_week"),
                    )
                    WeekArrowButton(
                        icon = FitIcons.ChevronRight,
                        contentDescription = stringResource(R.string.diary_next_week),
                        onClick = onNextWeekClick,
                        modifier = Modifier.testTag("calendar_next_week"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                weekDays.forEach { day ->
                    DayPillItem(
                        day = day,
                        onClick = { onDaySelected(day.dateEpochMs) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calendar_day_${day.date}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extendedColors = FitThemeTokens.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
        modifier = modifier.size(28.dp),
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
private fun DayPillItem(
    day: DayOfWeekUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extendedColors = FitThemeTokens.colors
    val containerColor = if (day.isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.Transparent
    }
    val dayNameColor = if (day.isSelected) {
        MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dayNumberColor = if (day.isSelected) {
        MaterialTheme.colorScheme.inverseOnSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderStroke = if (day.isSelected) {
        BorderStroke(1.dp, extendedColors.emeraldAccent.copy(alpha = 0.45f))
    } else {
        null
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = borderStroke,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = day.dayOfWeekName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (day.isSelected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = dayNameColor,
                maxLines = 1,
            )

            Text(
                text = day.dayOfMonth,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = if (day.isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                ),
                color = dayNumberColor,
                maxLines = 1,
            )

            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (day.hasLoggedMeals) extendedColors.emeraldAccent else Color.Transparent,
                    ),
            )
        }
    }
}
