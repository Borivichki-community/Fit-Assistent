package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.presentation.airecipe.AiRecipeUiState

@Composable
internal fun AiRecipeGeneratingContent(
    state: AiRecipeUiState.Content.Generating,
    onCancel: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "fitapp_ai_orbit")
    val orbitAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3200, easing = LinearEasing)),
        label = "orbit_spin",
    )
    val outerAngle by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 9000, easing = LinearEasing)),
        label = "orbit_outer_spin",
    )
    val rippleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple_pulse",
    )
    val starPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "star_pulse",
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(224.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Отменить генерацию",
                    onClick = onCancel,
                )
                .semantics {
                    contentDescription = state.phase
                    stateDescription = state.percent
                },
            contentAlignment = Alignment.Center,
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val unit = size.minDimension / 240f
                        val cacheCenter = Offset(size.width / 2f, size.height / 2f)
                        val cx = cacheCenter.x
                        val cy = cacheCenter.y
                        val outerR = 68f * unit
                        val orbitR = 54f * unit
                        val discR = 26f * unit
                        val ambientBrush = Brush.radialGradient(
                            0.0f to Color.White.copy(alpha = 0.18f),
                            0.5f to Color(0xFF71717A).copy(alpha = 0.06f),
                            1.0f to Color.Transparent,
                            center = cacheCenter,
                            radius = 75f * unit,
                        )
                        val dashedStroke = Stroke(
                            width = 1.2f * unit,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * unit, 10f * unit)),
                        )
                        val guideStroke = Stroke(width = 1.2f * unit)
                        val orbitSweepBrush = Brush.sweepGradient(
                            0.0f to Color.White.copy(alpha = 0.95f),
                            0.60f to Color.Transparent,
                            0.72f to Color.White.copy(alpha = 0.08f),
                            0.88f to Color(0xFFA1A1AA).copy(alpha = 0.35f),
                            1.0f to Color.White.copy(alpha = 0.95f),
                            center = cacheCenter,
                        )
                        val orbitStroke = Stroke(width = 2.5f * unit, cap = StrokeCap.Round)
                        val orbitTopLeft = Offset(cx - orbitR, cy - orbitR)
                        val orbitSize = Size(orbitR * 2f, orbitR * 2f)
                        val beadCenter = Offset(cx + orbitR, cy)
                        val beadGlowBrush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.50f), Color.Transparent),
                            center = beadCenter,
                            radius = 9f * unit,
                        )
                        val discGlowBrush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                            center = cacheCenter,
                            radius = 38f * unit,
                        )
                        val starPath = Path().apply {
                            moveTo(cx, cy - 18f * unit)
                            cubicTo(cx, cy - 8f * unit, cx + 2f * unit, cy - 2f * unit, cx + 12f * unit, cy)
                            cubicTo(cx + 2f * unit, cy + 2f * unit, cx, cy + 8f * unit, cx, cy + 18f * unit)
                            cubicTo(cx, cy + 8f * unit, cx - 2f * unit, cy + 2f * unit, cx - 12f * unit, cy)
                            cubicTo(cx - 2f * unit, cy - 2f * unit, cx, cy - 8f * unit, cx, cy - 18f * unit)
                            close()
                        }
                        val starHaloStroke = Stroke(width = 5f * unit)

                        onDrawBehind {
                            // 1. Ambient soft radial halo glow
                            drawCircle(brush = ambientBrush, radius = 75f * unit, center = center)

                            // 2. Outermost reverse-spinning dotted track + 2 opposite dots
                            rotate(degrees = outerAngle, pivot = center) {
                                drawCircle(
                                    color = Color(0xFF52525B).copy(alpha = 0.45f),
                                    radius = outerR,
                                    center = center,
                                    style = dashedStroke,
                                )
                                drawCircle(
                                    color = Color(0xFFD4D4D8).copy(alpha = 0.65f),
                                    radius = 1.8f * unit,
                                    center = Offset(cx, cy - outerR),
                                )
                                drawCircle(
                                    color = Color(0xFFD4D4D8).copy(alpha = 0.65f),
                                    radius = 1.8f * unit,
                                    center = Offset(cx, cy + outerR),
                                )
                            }

                            // 3. Expanding ripple wave
                            val rippleR = (42f + (76f - 42f) * rippleProgress) * unit
                            val rippleAlpha = 0.6f * (1f - rippleProgress)
                            val rippleStrokeWidth = (1.5f - 1.0f * rippleProgress) * unit
                            drawCircle(
                                color = Color(0xFFE4E4E7).copy(alpha = rippleAlpha),
                                radius = rippleR,
                                center = center,
                                style = Stroke(width = rippleStrokeWidth),
                            )

                            // 4. Static subtle guide circle
                            drawCircle(
                                color = Color.White.copy(alpha = 0.12f),
                                radius = orbitR,
                                center = center,
                                style = guideStroke,
                            )

                            // 5. Main spinning orbit arc (148deg comet tail) + glowing bead
                            rotate(degrees = orbitAngle, pivot = center) {
                                drawArc(
                                    brush = orbitSweepBrush,
                                    startAngle = -148f,
                                    sweepAngle = 148f,
                                    useCenter = false,
                                    topLeft = orbitTopLeft,
                                    size = orbitSize,
                                    style = orbitStroke,
                                )
                                drawCircle(brush = beadGlowBrush, radius = 9f * unit, center = beadCenter)
                                drawCircle(color = Color.White, radius = 3.5f * unit, center = beadCenter)
                                drawCircle(color = Color(0xFF18181B), radius = 1.6f * unit, center = beadCenter)
                            }

                            // 6. Central disc & pulsing 4-pointed astroid star
                            drawCircle(
                                brush = discGlowBrush,
                                radius = 38f * unit,
                                center = center,
                                alpha = 0.55f + 0.45f * starPulse,
                            )
                            drawCircle(color = Color(0xFF14151A), radius = discR, center = center)
                            drawCircle(
                                color = Color(0xFF3F3F46).copy(alpha = 0.65f),
                                radius = discR,
                                center = center,
                                style = guideStroke,
                            )

                            val starScale = 0.92f + 0.16f * starPulse
                            rotate(degrees = 6f * starPulse, pivot = center) {
                                scale(scale = starScale, pivot = center) {
                                    drawPath(
                                        path = starPath,
                                        color = Color.White.copy(alpha = 0.25f + 0.25f * starPulse),
                                        style = starHaloStroke,
                                    )
                                    drawPath(path = starPath, color = Color.White)
                                    drawCircle(
                                        color = Color(0xFF71717A),
                                        radius = 2f * unit,
                                        center = center,
                                    )
                                }
                            }
                        }
                    },
            )
        }
    }
}
