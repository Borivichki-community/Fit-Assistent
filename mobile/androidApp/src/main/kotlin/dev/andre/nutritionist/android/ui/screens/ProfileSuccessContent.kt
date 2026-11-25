package dev.andre.nutritionist.android.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import dev.andre.nutritionist.android.ui.theme.FitThemeTokens
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.model.UserRole
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
 * Контент экрана профиля в стиле fitapp_8 с поддержкой точечного редактирования параметров.
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
    onNotificationsClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val extendedColors = FitThemeTokens.colors
    var activeDialog by remember { mutableStateOf<ProfileEditField?>(null) }

    val cardShadow = if (extendedColors.isDark) 0.dp else 2.dp
    val budgetValue = if (profile.dailyBudgetLabel.isNotBlank()) {
        stringResource(R.string.profile_per_day_format, profile.dailyBudgetLabel)
    } else {
        profile.weeklyBudgetLabel.ifBlank { stringResource(R.string.profile_default_budget) }
    }
    val budgetSubtitle = if (profile.dailyBudgetLabel.isNotBlank()) {
        profile.weeklyBudgetFormattedLabel.ifBlank { profile.weeklyBudgetLabel }
    } else {
        null
    }
    val goalSubtitle = profile.weightGoalLabel.takeIf { it.isNotBlank() }?.let { targetWeight ->
        stringResource(R.string.profile_target_weight_format, targetWeight)
    }

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

        // 1. Карточка пользователя
        ProfileIdentityCard(
            profile = profile,
            onEditDataClick = onFillSurveyClick,
        )

        // 2. Секция параметров здоровья (2x2)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Text(
                text = stringResource(R.string.profile_health_params),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.extraSmall),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                MetricCard(
                    label = stringResource(R.string.profile_label_weight),
                    value = profile.weightLabel.ifBlank { stringResource(R.string.profile_default_weight) },
                    icon = FitIcons.Edit,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { activeDialog = ProfileEditField.WEIGHT },
                )
                MetricCard(
                    label = stringResource(R.string.profile_label_activity),
                    value = profile.activityLabel.ifBlank { stringResource(R.string.profile_default_activity) },
                    icon = FitIcons.Exercise,
                    accentColor = MaterialTheme.colorScheme.tertiary,
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
                    subtitle = goalSubtitle,
                    icon = FitIcons.Statistics,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { activeDialog = ProfileEditField.GOAL },
                )
                MetricCard(
                    label = stringResource(R.string.profile_label_budget),
                    value = budgetValue,
                    subtitle = budgetSubtitle,
                    icon = FitIcons.Document,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { activeDialog = ProfileEditField.BUDGET },
                )
            }
        }

        // 3. Карточка суточной нормы КБЖУ
        ProfileDailyKbjuCard(profile = profile)

        // 4. Меню настроек и действий
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.cardRadius),
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
            shadowElevation = cardShadow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.extraSmall),
            ) {
                SettingsActionRow(
                    icon = FitIcons.Settings,
                    title = stringResource(R.string.profile_menu_settings),
                    subtitle = stringResource(R.string.profile_menu_settings_desc),
                    onClick = onEditClick,
                )
                HorizontalDivider(
                    color = extendedColors.glassBorderLevel1,
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )
                SettingsActionRow(
                    icon = FitIcons.Document,
                    title = stringResource(R.string.profile_fill_survey_again),
                    subtitle = stringResource(R.string.profile_fill_survey_desc),
                    onClick = onFillSurveyClick,
                )
                HorizontalDivider(
                    color = extendedColors.glassBorderLevel1,
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )
                SettingsActionRow(
                    icon = FitIcons.Notifications,
                    title = stringResource(R.string.profile_menu_notifications),
                    subtitle = stringResource(R.string.profile_menu_notifications_desc),
                    onClick = onNotificationsClick,
                )
                HorizontalDivider(
                    color = extendedColors.glassBorderLevel1,
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )
                SettingsActionRow(
                    icon = FitIcons.Help,
                    title = stringResource(R.string.profile_menu_help),
                    subtitle = stringResource(R.string.profile_menu_help_desc),
                    onClick = onHelpClick,
                )
            }
        }

        // 5. Выделенная кнопка выхода из аккаунта
        Surface(
            onClick = onLogoutClick,
            enabled = !isLoggingOut,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.innerCardRadius),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
            contentColor = MaterialTheme.colorScheme.error,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = FitIcons.Logout,
                    contentDescription = null,
                    tint = if (isLoggingOut) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.profile_logout_button),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isLoggingOut) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
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

@Preview(name = "ProfileSuccessContent - Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun ProfileSuccessContentLightPreview() {
    FitAssistantTheme(darkTheme = false) {
        ProfileSuccessContent(
            profile = ProfileUiModel(
                id = ProfileId("p1"),
                userId = UserId("u1"),
                displayName = "Алексей Кодинцев",
                email = "alexey@example.com",
                userRole = UserRole.CLIENT,
                heightLabel = "180 см",
                weightLabel = "75.0 кг",
                genderLabel = "Мужской",
                activityLabel = "Умеренная активность",
                goalLabel = "Снижение веса",
                caloriesGoalLabel = "2200 ккал",
                proteinsGoalLabel = "140 г",
                fatsGoalLabel = "70 г",
                carbsGoalLabel = "250 г",
                weightGoalLabel = "70 кг",
                weeklyBudgetLabel = "3500 ₽ / нед.",
                dailyBudgetLabel = "500 ₽",
                weeklyBudgetFormattedLabel = "3 500 ₽",
                caloriesGoalFormatted = "2200 ккал",
                proteinsGoalFormatted = "140 г",
                fatsGoalFormatted = "70 г",
                carbsGoalFormatted = "250 г",
            ),
        )
    }
}

@Preview(name = "ProfileSuccessContent - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileSuccessContentDarkPreview() {
    FitAssistantTheme(darkTheme = true) {
        ProfileSuccessContent(
            profile = ProfileUiModel(
                id = ProfileId("p1"),
                userId = UserId("u1"),
                displayName = "Алексей Кодинцев",
                email = "alexey@example.com",
                userRole = UserRole.COACH,
                heightLabel = "180 см",
                weightLabel = "75.0 кг",
                genderLabel = "Мужской",
                activityLabel = "Умеренная активность",
                goalLabel = "Снижение веса",
                caloriesGoalLabel = "2200 ккал",
                proteinsGoalLabel = "140 г",
                fatsGoalLabel = "70 г",
                carbsGoalLabel = "250 г",
                weightGoalLabel = "70 кг",
                weeklyBudgetLabel = "3500 ₽ / нед.",
                dailyBudgetLabel = "500 ₽",
                weeklyBudgetFormattedLabel = "3 500 ₽",
                caloriesGoalFormatted = "2200 ккал",
                proteinsGoalFormatted = "140 г",
                fatsGoalFormatted = "70 г",
                carbsGoalFormatted = "250 г",
            ),
        )
    }
}
