package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.screens.EmeraldAccent
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.android.ui.theme.TextMidEmphasis
import dev.andre.nutritionist.presentation.airecipe.AiRecipeGeneratingUiModel

/**
 * Экран генерации рецепта с высокотехнологичным кинетическим ядром (Aether Zero-G Luxury HUD).
 * Полностью соответствует screenshots/экран при генерации.png и fitapp_ai_2/code.html.
 */
@Composable
fun AiRecipeGeneratingContent(
    generating: AiRecipeGeneratingUiModel,
    modifier: Modifier = Modifier,
    targetCalories: Int = 600,
    targetBudgetRub: Int = 350,
    onCancel: () -> Unit = {},
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_hud_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    val beaconRadius by infiniteTransition.animateFloat(
        initialValue = 55f,
        targetValue = 110f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "beacon_radius",
    )

    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "beacon_alpha",
    )

    val rotateCw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotate_cw",
    )

    val rotateCcw by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotate_ccw",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06080B))
            .padding(horizontal = Spacing.medium, vertical = Spacing.medium)
            .testTag("ai_recipe_generating_content"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Top HUD Header Row: FitApp AI Core Pill & Progress Percent
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFF161923),
                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent),
                    )
                    Text(
                        text = "FITAPP AI CORE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp,
                        ),
                        color = EmeraldAccent,
                    )
                }
            }

            Text(
                text = generating.progressPercentText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
                color = Color(0xFF94A3B8),
                modifier = Modifier.testTag("ai_recipe_progress_percent"),
            )
        }

        // Center Visual: Kinetic Glowing Orbital Core Graphic
        Box(
            modifier = Modifier
                .size(160.dp)
                .testTag("ai_recipe_breathing_orb"),
            contentAlignment = Alignment.Center,
        ) {
            // Ambient Aura Halo
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                EmeraldAccent.copy(alpha = 0.22f),
                                Color(0xFF00C475).copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            // Animated Orbit Canvas (Rotating HUD rings, beacon wave, and corner reticles)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Expanding beacon wave
                drawCircle(
                    color = EmeraldAccent.copy(alpha = beaconAlpha),
                    radius = beaconRadius * (size.width / 240f),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )

                // Outer guidance circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = 70.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )

                // 4 Corner HUD Reticles / Brackets
                val bracketLen = 10.dp.toPx()
                val bracketStroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                val margin = 10.dp.toPx()

                // Top-Left
                drawLine(EmeraldAccent.copy(alpha = 0.45f), Offset(margin, margin + bracketLen), Offset(margin, margin), strokeWidth = bracketStroke.width, cap = StrokeCap.Round)
                drawLine(EmeraldAccent.copy(alpha = 0.45f), Offset(margin, margin), Offset(margin + bracketLen, margin), strokeWidth = bracketStroke.width, cap = StrokeCap.Round)

                // Top-Right
                drawLine(EmeraldAccent.copy(alpha = 0.45f), Offset(size.width - margin - bracketLen, margin), Offset(size.width - margin, margin), strokeWidth = bracketStroke.width, cap = StrokeCap.Round)
                drawLine(EmeraldAccent.copy(alpha = 0.45f), Offset(size.width - margin, margin), Offset(size.width - margin, margin + bracketLen), strokeWidth = bracketStroke.width, cap = StrokeCap.Round)

                // Bottom-Left
                drawLine(EmeraldAccent.copy(alpha = 0.45f), Offset(margin, size.height - margin - bracketLen), Offset(margin, size.height - margin), strokeWidth = bracketStroke.width, cap = StrokeCap.Round)
                drawLine(EmeraldAccent.copy(alpha = 0.45f), Offset(margin, size.height - margin), Offset(margin + bracketLen, size.height - margin), strokeWidth = bracketStroke.width, cap = StrokeCap.Round)

                // Bottom-Right
                drawLine(EmeraldAccent.copy(alpha = 0.45f), Offset(size.width - margin - bracketLen, size.height - margin), Offset(size.width - margin, size.height - margin), strokeWidth = bracketStroke.width, cap = StrokeCap.Round)
                drawLine(EmeraldAccent.copy(alpha = 0.45f), Offset(size.width - margin, size.height - margin - bracketLen), Offset(size.width - margin, size.height - margin), strokeWidth = bracketStroke.width, cap = StrokeCap.Round)
            }

            // Segmented Outer Orbital Ring (Clockwise)
            Canvas(
                modifier = Modifier
                    .size(136.dp)
                    .rotate(rotateCw),
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(EmeraldAccent, Color(0xFF00C475), Color.Transparent, EmeraldAccent),
                    ),
                    startAngle = 0f,
                    sweepAngle = 260f,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // Counter-Rotating Inner HUD Ring
            Canvas(
                modifier = Modifier
                    .size(105.dp)
                    .rotate(rotateCcw),
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFF38EF7D), Color(0xFF11998E), Color.Transparent),
                    ),
                    startAngle = 45f,
                    sweepAngle = 210f,
                    useCenter = false,
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // Central Glowing AI Nutrition Core with Emblem
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(0xFF0D1117))
                    .border(BorderStroke(1.5.dp, EmeraldAccent.copy(alpha = 0.8f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FitIcons.Sparkles,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // Title & Phase Description Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "ИИ синтезирует рацион...",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        fontSize = 19.sp,
                    ),
                    color = Color.White,
                )
                Text(
                    text = "✨",
                    fontSize = 18.sp,
                )
            }

            Text(
                text = "Анализируем БЖУ ($targetCalories ккал) и подбираем рецепты под бюджет $targetBudgetRub ₽",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color(0xFFCBD5E1),
                modifier = Modifier.padding(horizontal = Spacing.small),
            )

            // Subtitle Live Phase text
            Text(
                text = generating.phaseText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
                color = EmeraldAccent,
                modifier = Modifier.testTag("ai_recipe_phase_text"),
            )
        }

        // Progress Bar & Remaining Time Row
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LinearProgressIndicator(
                progress = { generating.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .testTag("ai_recipe_progress_bar"),
                color = EmeraldAccent,
                trackColor = Color(0xFF161923),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Генерация меню",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Color(0xFF94A3B8),
                )
                Text(
                    text = if (generating.progress > 0.8f) "Завершение..." else "Осталось ~2 сек",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                    color = EmeraldAccent,
                )
            }
        }

        // Checklist of Synthesis Steps
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Step 1: КБЖУ оптимизировано (Complete)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF12151E).copy(alpha = 0.8f),
                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "✓",
                            color = EmeraldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "КБЖУ оптимизировано",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFFE2E8F0),
                    )
                }
            }

            // Step 2: Поиск полезных ингредиентов (Active / Done)
            val step2Done = generating.progress >= 0.70f
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (step2Done) Color(0xFF12151E).copy(alpha = 0.8f) else Color(0xFF161923),
                border = BorderStroke(1.dp, if (step2Done) EmeraldAccent.copy(alpha = 0.25f) else EmeraldAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (step2Done) EmeraldAccent.copy(alpha = 0.2f)
                                else Color(0xFFFFB020).copy(alpha = 0.2f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (step2Done) "✓" else "⏳",
                            color = if (step2Done) EmeraldAccent else Color(0xFFFFB020),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "Поиск полезных ингредиентов...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (step2Done) FontWeight.Medium else FontWeight.SemiBold,
                        ),
                        color = if (step2Done) Color(0xFFE2E8F0) else EmeraldAccent,
                    )
                }
            }

            // Step 3: Финальный расчет стоимости (Pending / Active)
            val step3Done = generating.progress >= 0.95f
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF12151E).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (step3Done) EmeraldAccent.copy(alpha = 0.25f) else GlassBorderLevel1),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (step3Done) EmeraldAccent.copy(alpha = 0.2f)
                                else Color(0xFF334155).copy(alpha = 0.4f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (step3Done) "✓" else "○",
                            color = if (step3Done) EmeraldAccent else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "Финальный расчет стоимости",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = if (step3Done) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                    )
                }
            }
        }

        // Bottom Action Button: Cancel
        Button(
            onClick = onCancel,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF161923),
                contentColor = Color(0xFF94A3B8),
            ),
            border = BorderStroke(1.dp, GlassBorderLevel2),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = "Отменить генерацию",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF94A3B8),
            )
        }
    }
}
