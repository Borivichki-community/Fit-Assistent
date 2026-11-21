package dev.andre.nutritionist.android.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.CompleteProfileBanner
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.MetricCard
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.dialogs.EditActivityDialog
import dev.andre.nutritionist.android.ui.dialogs.EditBudgetDialog
import dev.andre.nutritionist.android.ui.dialogs.EditGoalDialog
import dev.andre.nutritionist.android.ui.dialogs.EditWeightDialog
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileUiModel

/**
 * Перечисление редактируемых полей метрик профиля.
 */
enum class ProfileEditField {
    WEIGHT,
    ACTIVITY,
    GOAL,
    BUDGET,
}

/**
 * Контент экрана профиля при успешной загрузке данных.
 * Поддерживает точечное модальное редактирование параметров (Вес, Активность, Цель, Бюджет).
 */
@Composable
fun ProfileSuccessContent(
    profile: ProfileUiModel,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    isLoggingOut: Boolean = false,
    onFillSurveyClick: () -> Unit = {},
    onEvent: (ProfileEvent.Ui) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    var activeDialog by remember { mutableStateOf<ProfileEditField?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        if (!profile.isComplete) {
            CompleteProfileBanner(
                onCompleteClick = onFillSurveyClick,
                buttonText = stringResource(R.string.profile_fill_survey_again),
            )
        }

        // 1. Карточка аватара и имени пользователя
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(Spacing.cardRadius),
            border = BorderStroke(1.dp, GlassBorderLevel2),
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
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(BorderStroke(1.dp, GlassBorderLevel1), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FitIcons.Profile,
                        contentDescription = stringResource(R.string.content_avatar),
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                Text(
                    text = profile.displayName.ifBlank { profile.userId.raw },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // 2. Сетка 2x2 параметров профиля с точечным редактированием
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            MetricCard(
                label = stringResource(R.string.profile_label_weight),
                value = profile.weightLabel.ifBlank { stringResource(R.string.profile_default_weight) },
                modifier = Modifier.weight(1f),
                onClick = { activeDialog = ProfileEditField.WEIGHT },
            )
            MetricCard(
                label = stringResource(R.string.profile_label_activity),
                value = profile.activityLabel.ifBlank { stringResource(R.string.profile_default_activity) },
                modifier = Modifier.weight(1f),
                onClick = { activeDialog = ProfileEditField.ACTIVITY },
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
                onClick = { activeDialog = ProfileEditField.GOAL },
            )
            MetricCard(
                label = stringResource(R.string.profile_label_budget),
                value = profile.weeklyBudgetLabel.ifBlank { stringResource(R.string.profile_default_budget) },
                modifier = Modifier.weight(1f),
                onClick = { activeDialog = ProfileEditField.BUDGET },
            )
        }

        // 3. Меню действий
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(Spacing.cardRadius),
            border = BorderStroke(1.dp, GlassBorderLevel2),
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
                    color = GlassBorderLevel1,
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )
                SettingsActionRow(
                    icon = FitIcons.Notifications,
                    title = stringResource(R.string.profile_menu_notifications),
                    onClick = { /* системные уведомления */ },
                )
                HorizontalDivider(
                    color = GlassBorderLevel1,
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )
                SettingsActionRow(
                    icon = FitIcons.Help,
                    title = stringResource(R.string.profile_menu_help),
                    onClick = { /* справка */ },
                )
                HorizontalDivider(
                    color = GlassBorderLevel1,
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )
                SettingsActionRow(
                    icon = FitIcons.Document,
                    title = stringResource(R.string.profile_fill_survey_again),
                    onClick = onFillSurveyClick,
                )
                HorizontalDivider(
                    color = GlassBorderLevel1,
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )
                SettingsActionRow(
                    icon = FitIcons.Logout,
                    title = stringResource(R.string.profile_logout_button),
                    iconTint = MaterialTheme.colorScheme.error,
                    textColor = MaterialTheme.colorScheme.error,
                    enabled = !isLoggingOut,
                    onClick = onLogoutClick,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.medium))
    }

    // Модальные диалоги редактирования конкретных параметров
    when (activeDialog) {
        ProfileEditField.WEIGHT -> {
            EditWeightDialog(
                initialWeight = profile.rawWeightKg,
                onDismiss = { activeDialog = null },
                onConfirm = { newWeight ->
                    onEvent(ProfileEvent.Ui.ChangeWeight(newWeight))
                    activeDialog = null
                },
            )
        }
        ProfileEditField.ACTIVITY -> {
            EditActivityDialog(
                initialActivity = profile.rawActivityLevel,
                onDismiss = { activeDialog = null },
                onConfirm = { newActivity ->
                    onEvent(ProfileEvent.Ui.ChangeActivity(newActivity))
                    activeDialog = null
                },
            )
        }
        ProfileEditField.GOAL -> {
            EditGoalDialog(
                initialGoal = profile.rawGoalType,
                onDismiss = { activeDialog = null },
                onConfirm = { newGoal ->
                    onEvent(ProfileEvent.Ui.ChangeGoal(newGoal))
                    activeDialog = null
                },
            )
        }
        ProfileEditField.BUDGET -> {
            EditBudgetDialog(
                initialBudget = profile.rawWeeklyBudget,
                onDismiss = { activeDialog = null },
                onConfirm = { newBudget ->
                    onEvent(ProfileEvent.Ui.ChangeBudget(newBudget))
                    activeDialog = null
                },
            )
        }
        null -> Unit
    }
}

@Preview(name = "ProfileSuccessContent - Light Theme", showBackground = true)
@Composable
fun ProfileSuccessContentLightPreview() {
    FitAssistantTheme(darkTheme = false) {
        ProfileSuccessContent(
            profile = ProfileUiModel(
                id = ProfileId("p1"),
                userId = UserId("u1"),
                heightLabel = "180 см",
                weightLabel = "75 кг",
                genderLabel = "Мужской",
                activityLabel = "Умеренная",
                goalLabel = "Похудение",
                caloriesGoalLabel = "2200 ккал",
                proteinsGoalLabel = "140 г",
                fatsGoalLabel = "70 г",
                carbsGoalLabel = "250 г",
                weightGoalLabel = "70 кг",
                weeklyBudgetLabel = "3500 ₽",
            ),
            onEditClick = {},
            onLogoutClick = {},
        )
    }
}

@Preview(name = "ProfileSuccessContent - Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileSuccessContentDarkPreview() {
    FitAssistantTheme(darkTheme = true) {
        ProfileSuccessContent(
            profile = ProfileUiModel(
                id = ProfileId("p1"),
                userId = UserId("u1"),
                heightLabel = "180 см",
                weightLabel = "75 кг",
                genderLabel = "Мужской",
                activityLabel = "Умеренная",
                goalLabel = "Похудение",
                caloriesGoalLabel = "2200 ккал",
                proteinsGoalLabel = "140 г",
                fatsGoalLabel = "70 г",
                carbsGoalLabel = "250 г",
                weightGoalLabel = "70 кг",
                weeklyBudgetLabel = "3500 ₽",
            ),
            onEditClick = {},
            onLogoutClick = {},
        )
    }
}
