package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.theme.FitThemeTokens

/**
 * Капсульный чип активного плана питания («СУШКА 2400 ККАЛ», «КЕТО БАЛАНС») по макету fitapp_1.
 */
@Composable
fun PlanChip(
    title: String,
    isPrimaryAccent: Boolean = true,
    onDismissClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val extendedColors = FitThemeTokens.colors
    val dotColor = if (isPrimaryAccent) extendedColors.emeraldAccent else extendedColors.activityAccent
    val shadowElevation = if (extendedColors.isDark) 0.dp else 1.dp

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
        shadowElevation = shadowElevation,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Icon(
                imageVector = FitIcons.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismissClick),
            )
        }
    }
}
