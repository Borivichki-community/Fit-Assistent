package dev.andre.nutritionist.android.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing

/**
 * Шаг 1 онбординга: Ввод имени и фамилии пользователя.
 */
@Composable
fun OnboardingNameStep(
    firstName: String,
    lastName: String,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    firstNameError: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.large, vertical = Spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_title_welcome),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Text(
            text = stringResource(R.string.onboarding_subtitle_name),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.extraLarge))

        // Поле ввода имени
        OutlinedTextField(
            value = firstName,
            onValueChange = onFirstNameChanged,
            placeholder = { Text(text = stringResource(R.string.onboarding_first_name_hint)) },
            singleLine = true,
            isError = firstNameError != null,
            supportingText = {
                if (firstNameError != null) {
                    Text(
                        text = firstNameError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            shape = RoundedCornerShape(Spacing.cardRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        // Поле ввода фамилии
        OutlinedTextField(
            value = lastName,
            onValueChange = onLastNameChanged,
            placeholder = { Text(text = stringResource(R.string.onboarding_last_name_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(Spacing.cardRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
