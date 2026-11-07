package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.MetricCard
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.presentation.profile.ProfileUiModel

/**
 * Контент экрана профиля при успешной загрузке данных.
 */
@Composable
fun ProfileSuccessContent(
    profile: ProfileUiModel,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        // 1. Карточка аватара и имени пользователя
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(Spacing.cardRadius),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FitIcons.Profile,
                        contentDescription = stringResource(R.string.content_avatar),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(52.dp),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                Text(
                    text = stringResource(R.string.profile_default_user_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // 2. Сетка 2x2 параметров профиля
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            MetricCard(
                label = stringResource(R.string.profile_label_weight),
                value = profile.weightLabel.ifBlank { stringResource(R.string.profile_default_weight) },
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = stringResource(R.string.profile_label_activity),
                value = profile.activityLabel.ifBlank { stringResource(R.string.profile_default_activity) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            MetricCard(
                label = stringResource(R.string.profile_label_goal),
                value = profile.goalLabel.ifBlank { stringResource(R.string.profile_default_goal) },
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = stringResource(R.string.profile_label_budget),
                value = profile.weeklyBudgetLabel.ifBlank { stringResource(R.string.profile_default_budget) },
                modifier = Modifier.weight(1f),
            )
        }

        // 3. Меню действий
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(Spacing.cardRadius),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.small),
            ) {
                SettingsActionRow(
                    icon = FitIcons.Settings,
                    title = stringResource(R.string.profile_menu_settings),
                    onClick = onEditClick,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )
                SettingsActionRow(
                    icon = FitIcons.Notifications,
                    title = stringResource(R.string.profile_menu_notifications),
                    onClick = { /* уведомления */ },
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )
                SettingsActionRow(
                    icon = FitIcons.Help,
                    title = stringResource(R.string.profile_menu_help),
                    onClick = { /* справка */ },
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.medium))
    }
}
