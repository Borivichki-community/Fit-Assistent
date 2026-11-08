package dev.andre.nutritionist.android.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing

/**
 * Шаг 4 онбординга: Ввод недельного бюджета и карточка рекомендаций.
 */
@Composable
fun OnboardingBudgetStep(
    firstName: String,
    weeklyBudget: String,
    onWeeklyBudgetChanged: (String) -> Unit,
    budgetError: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.large, vertical = Spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
    ) {
        val displayName = firstName.trim().ifBlank { stringResource(R.string.profile_default_user_name) }
        Text(
            text = stringResource(R.string.onboarding_title_about_format, displayName),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Spacing.large))

        Text(
            text = stringResource(R.string.onboarding_label_budget),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        OutlinedTextField(
            value = weeklyBudget,
            onValueChange = onWeeklyBudgetChanged,
            placeholder = { Text(text = stringResource(R.string.onboarding_budget_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = budgetError != null,
            supportingText = {
                if (budgetError != null) {
                    Text(
                        text = budgetError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
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

        // Карточка рекомендаций (как на скриншоте 5)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.innerCardRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_recommendation_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                Text(
                    text = stringResource(R.string.onboarding_recommendation_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
