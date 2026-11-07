package dev.andre.nutritionist.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class WizardNavDirection {
    BACK,
    FORWARD,
    FINISH,
}

/**
 * Круглая кнопка навигации онбординга (минималистичный стиль Antigravity).
 */
@Composable
fun WizardNavButton(
    direction: WizardNavDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val backgroundColor = if (enabled) {
        Color.White
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val iconColor = if (enabled) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, color = Color.Black.copy(alpha = 0.2f)),
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = direction,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "wizardNavIcon",
        ) { targetDirection ->
            val symbol = when (targetDirection) {
                WizardNavDirection.BACK -> "←"
                WizardNavDirection.FORWARD -> "→"
                WizardNavDirection.FINISH -> "✓"
            }
            Text(
                text = symbol,
                color = iconColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
