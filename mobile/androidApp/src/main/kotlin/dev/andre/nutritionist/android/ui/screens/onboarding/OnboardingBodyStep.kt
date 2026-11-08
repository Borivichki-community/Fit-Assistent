package dev.andre.nutritionist.android.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.domain.model.Gender

/**
 * Шаг 2 онбординга: Рост, вес, возраст и выбор пола.
 */
@Composable
fun OnboardingBodyStep(
    firstName: String,
    heightCm: String,
    weightKg: String,
    age: String,
    gender: Gender,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onAgeChanged: (String) -> Unit,
    onGenderChanged: (Gender) -> Unit,
    heightError: String? = null,
    weightError: String? = null,
    ageError: String? = null,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.large, vertical = Spacing.large),
        verticalArrangement = Arrangement.Center,
    ) {
        val displayName = firstName.trim().ifBlank { stringResource(R.string.profile_default_user_name) }
        Text(
            text = stringResource(R.string.onboarding_title_about_format, displayName),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Spacing.large))

        // Рост
        Text(
            text = stringResource(R.string.onboarding_label_height),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(Spacing.small))
        OutlinedTextField(
            value = heightCm,
            onValueChange = onHeightChanged,
            placeholder = { Text(text = stringResource(R.string.onboarding_hint_height)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = heightError != null,
            supportingText = {
                if (heightError != null) {
                    Text(text = heightError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            shape = RoundedCornerShape(Spacing.cardRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        // Вес
        Text(
            text = stringResource(R.string.onboarding_label_weight),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(Spacing.small))
        OutlinedTextField(
            value = weightKg,
            onValueChange = onWeightChanged,
            placeholder = { Text(text = stringResource(R.string.onboarding_hint_weight)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = weightError != null,
            supportingText = {
                if (weightError != null) {
                    Text(text = weightError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            shape = RoundedCornerShape(Spacing.cardRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        // Возраст
        Text(
            text = stringResource(R.string.onboarding_label_age),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(Spacing.small))
        OutlinedTextField(
            value = age,
            onValueChange = onAgeChanged,
            placeholder = { Text(text = stringResource(R.string.onboarding_hint_age)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = ageError != null,
            supportingText = {
                if (ageError != null) {
                    Text(text = ageError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            shape = RoundedCornerShape(Spacing.cardRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Spacing.large))

        // Выбор пола (Мужской / Женский)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            GenderPill(
                title = stringResource(R.string.onboarding_gender_male),
                isSelected = gender == Gender.MALE,
                onClick = { onGenderChanged(Gender.MALE) },
                modifier = Modifier.weight(1f),
            )
            GenderPill(
                title = stringResource(R.string.onboarding_gender_female),
                isSelected = gender == Gender.FEMALE,
                onClick = { onGenderChanged(Gender.FEMALE) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GenderPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        label = "genderBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "genderTextColor",
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(Spacing.innerCardRadius))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = textColor,
        )
    }
}
