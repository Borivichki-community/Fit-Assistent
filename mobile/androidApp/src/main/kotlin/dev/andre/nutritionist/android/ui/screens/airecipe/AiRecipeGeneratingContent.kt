package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.presentation.airecipe.AiRecipeUiState
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun AiRecipeGeneratingContent(
    state: AiRecipeUiState.Content.Generating,
    onCancel: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "Орбита генерации")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2400, easing = LinearEasing)),
        label = "Поворот",
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(128.dp)
                .clickable(onClickLabel = "Отменить генерацию", onClick = onCancel)
                .semantics {
                    contentDescription = state.phase
                    stateDescription = state.percent
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension * .39f
                val coreRadius = size.minDimension * .19f
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0x221C2537), Color.Transparent),
                        radius = size.minDimension / 2,
                    ),
                )
                repeat(42) { step ->
                    val radians = (step + 0.5) * (2 * Math.PI / 42)
                    val distance = size.minDimension * .48f
                    val position = center + Offset(cos(radians).toFloat(), sin(radians).toFloat()) * distance
                    drawCircle(Color.White.copy(alpha = if ((step + 10) % 21 == 0) .45f else .12f),
                        if (step % 21 == 0) 1.7.dp.toPx() else .55.dp.toPx(), position)
                }
                drawCircle(Color.White.copy(alpha = .16f), radius, style = Stroke(1.dp.toPx()))
                rotate(angle) {
                    drawCircle(
                        Brush.sweepGradient(
                            listOf(Color.White.copy(alpha = .62f), Color.Transparent, Color.Transparent, Color.White.copy(alpha = .62f)),
                        ),
                        radius,
                        style = Stroke(2.dp.toPx()),
                    )
                    val dot = center + Offset(radius, 0f)
                    drawCircle(Color.White.copy(alpha = .17f), 6.dp.toPx(), dot)
                    drawCircle(Color.White, 2.8.dp.toPx(), dot, style = Stroke(1.4.dp.toPx()))
                }
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xFF33323C), Color(0xFF181820)), radius = coreRadius),
                    coreRadius,
                )
                drawCircle(Color.White.copy(alpha = .13f), coreRadius, style = Stroke(1.dp.toPx()))
            }
            Icon(FitIcons.Sparkle, contentDescription = null, Modifier.size(23.dp), tint = Color.White)
        }
    }
}
