package dev.andre.nutritionist.android.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.domain.model.GoalType

/**
 * Шаг 5 онбординга: Выбор цели питания (Набор массы / Похудение / Поддержание веса).
 */
@Composable
fun OnboardingGoalStep(
    selectedGoal: GoalType,
    onGoalSelected: (GoalType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.large, vertical = Spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_title_goal),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(36.dp))

        GoalOptionCard(
            title = stringResource(R.string.onboarding_goal_gain),
            isSelected = selectedGoal == GoalType.GAIN_WEIGHT,
            onClick = { onGoalSelected(GoalType.GAIN_WEIGHT) },
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        GoalOptionCard(
            title = stringResource(R.string.onboarding_goal_lose),
            isSelected = selectedGoal == GoalType.LOSE_WEIGHT,
            onClick = { onGoalSelected(GoalType.LOSE_WEIGHT) },
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        GoalOptionCard(
            title = stringResource(R.string.onboarding_goal_maintain),
            isSelected = selectedGoal == GoalType.MAINTAIN,
            onClick = { onGoalSelected(GoalType.MAINTAIN) },
        )
    }
}

@Composable
private fun GoalOptionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        label = "goalBgColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF00D09E) else Color.Transparent,
        label = "goalBorderColor",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(Spacing.innerCardRadius))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(Spacing.innerCardRadius),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
