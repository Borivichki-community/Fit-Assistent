package dev.andre.nutritionist.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Плавный минималистичный индикатор шагов онбординга (точки-индикаторы).
 */
@Composable
fun DotsIndicator(
    totalDots: Int,
    currentDot: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF00D09E),
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalDots) { index ->
            val isSelected = index == currentDot
            val width by animateDpAsState(
                targetValue = if (isSelected) 10.dp else 8.dp,
                animationSpec = tween(durationMillis = 250),
                label = "dotWidth",
            )
            val height by animateDpAsState(
                targetValue = if (isSelected) 10.dp else 8.dp,
                animationSpec = tween(durationMillis = 250),
                label = "dotHeight",
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) activeColor else inactiveColor,
                animationSpec = tween(durationMillis = 250),
                label = "dotColor",
            )

            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
