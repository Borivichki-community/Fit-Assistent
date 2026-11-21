package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.presentation.airecipe.AiRecipeGeneratingUiModel

/**
 * Экран генерации рецепта с атмосферной плавной пульсирующей анимацией (Aether Zero-G Luxury).
 */
@Composable
fun AiRecipeGeneratingContent(
    generating: AiRecipeGeneratingUiModel,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_generating_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.large)
            .testTag("ai_recipe_generating_content"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Atmospheric Breathing Orb
        Box(
            modifier = Modifier
                .size(160.dp)
                .testTag("ai_recipe_breathing_orb"),
            contentAlignment = Alignment.Center,
        ) {
            // Outer Glowing Halo
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = glowAlpha * 0.35f)),
            )

            // Mid Orbit Ring
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = glowAlpha),
                        ),
                        CircleShape,
                    ),
            )

            // Inner Core with Rotating Sparkles
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FitIcons.Sparkles,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(36.dp)
                        .rotate(rotation),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.large))

        // Phase Title
        Text(
            text = "AI Шеф-нутрициолог",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        // Live Subtitle Phase
        Text(
            text = generating.phaseText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("ai_recipe_phase_text"),
        )

        Spacer(modifier = Modifier.height(Spacing.extraLarge))

        // Progress Bar
        LinearProgressIndicator(
            progress = { generating.progress },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(6.dp)
                .clip(CircleShape)
                .testTag("ai_recipe_progress_bar"),
            color = MaterialTheme.colorScheme.primaryContainer,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        // Progress Percentage
        Text(
            text = generating.progressPercentText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.testTag("ai_recipe_progress_percent"),
        )
    }
}
