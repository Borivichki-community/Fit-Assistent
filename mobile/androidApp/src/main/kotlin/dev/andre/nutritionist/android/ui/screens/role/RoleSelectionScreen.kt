package dev.andre.nutritionist.android.ui.screens.role

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import dev.andre.nutritionist.domain.model.UserRole

const val ROLE_CARD_CLIENT_TAG = "role_card_client"
const val ROLE_CARD_COACH_TAG = "role_card_coach"

/**
 * Чистый stateless экран выбора роли («Пользователь» / «Тренер»).
 * Содержит ровно две кнопки/карточки выбора роли без лишних элементов навигации.
 */
@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.large)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(Spacing.extraLarge))

            Text(
                text = stringResource(R.string.role_selection_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = stringResource(R.string.role_selection_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.small),
            )

            Spacer(modifier = Modifier.height(40.dp))

            RoleOptionCard(
                title = stringResource(R.string.role_client_title),
                description = stringResource(R.string.role_client_desc),
                icon = FitIcons.Profile,
                accentColor = MaterialTheme.colorScheme.primary,
                tag = ROLE_CARD_CLIENT_TAG,
                onClick = { onRoleSelected(UserRole.CLIENT) },
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            RoleOptionCard(
                title = stringResource(R.string.role_coach_title),
                description = stringResource(R.string.role_coach_desc),
                icon = FitIcons.Coach,
                accentColor = MaterialTheme.colorScheme.secondary,
                tag = ROLE_CARD_COACH_TAG,
                onClick = { onRoleSelected(UserRole.COACH) },
            )

            Spacer(modifier = Modifier.height(Spacing.extraLarge))
        }
    }
}

@Preview(name = "RoleSelectionScreen - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "RoleSelectionScreen - Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun RoleSelectionScreenPreview() {
    FitAssistantTheme {
        RoleSelectionScreen(
            onRoleSelected = {},
        )
    }
}
