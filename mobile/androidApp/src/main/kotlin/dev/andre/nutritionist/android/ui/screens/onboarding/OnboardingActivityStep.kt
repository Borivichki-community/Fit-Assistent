package dev.andre.nutritionist.android.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.domain.model.ActivityLevel

/**
 * Шаг 3 онбординга: Выбор уровня физической активности (слайдер и кнопки «Меньше» / «Больше»).
 */
@Composable
fun OnboardingActivityStep(
    firstName: String,
    activityLevel: ActivityLevel,
    onActivityLevelChanged: (ActivityLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val levels = listOf(
        ActivityLevel.SEDENTARY,
        ActivityLevel.LIGHT,
        ActivityLevel.MODERATE,
        ActivityLevel.VERY_ACTIVE,
    )
    val currentIndex = levels.indexOf(activityLevel).coerceAtLeast(0)

    val currentLabel = when (activityLevel) {
        ActivityLevel.SEDENTARY -> stringResource(R.string.onboarding_activity_sedentary)
        ActivityLevel.LIGHT -> stringResource(R.string.onboarding_activity_light)
        ActivityLevel.MODERATE -> stringResource(R.string.onboarding_activity_moderate)
        ActivityLevel.VERY_ACTIVE -> stringResource(R.string.onboarding_activity_very_active)
        ActivityLevel.UNKNOWN -> stringResource(R.string.onboarding_activity_moderate)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.large, vertical = Spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
    ) {
        val displayName = firstName.trim().ifBlank { stringResource(R.string.profile_default_user_name) }
        Text(
            text = stringResource(R.string.onboarding_title_activity_format, displayName),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Text(
            text = stringResource(R.string.onboarding_subtitle_activity),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Заголовок текущего уровня
        Text(
            text = currentLabel,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        // Плавный M3 Slider
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { floatVal ->
                val newIndex = floatVal.toInt().coerceIn(0, levels.size - 1)
                onActivityLevelChanged(levels[newIndex])
            },
            valueRange = 0f..(levels.size - 1).toFloat(),
            steps = levels.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00D09E),
                activeTrackColor = Color(0xFF00D09E),
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Spacing.large))

        // Кнопки «Меньше» / «Больше»
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            ActivityStepButton(
                title = stringResource(R.string.onboarding_btn_less),
                enabled = currentIndex > 0,
                onClick = {
                    if (currentIndex > 0) {
                        onActivityLevelChanged(levels[currentIndex - 1])
                    }
                },
                modifier = Modifier.weight(1f),
            )
            ActivityStepButton(
                title = stringResource(R.string.onboarding_btn_more),
                enabled = currentIndex < levels.size - 1,
                onClick = {
                    if (currentIndex < levels.size - 1) {
                        onActivityLevelChanged(levels[currentIndex + 1])
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActivityStepButton(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(Spacing.innerCardRadius))
            .background(
                if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}
