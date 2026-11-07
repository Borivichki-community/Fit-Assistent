package dev.andre.nutritionist.android.presentation.profile.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.ErrorContent
import dev.andre.nutritionist.android.ui.components.FitTopBar
import dev.andre.nutritionist.android.ui.components.LoadingContent
import dev.andre.nutritionist.android.ui.components.OfflineBanner
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileState

/**
 * Stateless экран профиля (форма регистрации / редактирования).
 * Принимает иммутабельный [ProfileState] и лямбду отправки событий [onEvent].
 */
@Composable
fun ProfileScreen(
    state: ProfileState,
    onEvent: (ProfileEvent.Ui) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FitTopBar(
                title = stringResource(R.string.profile_form_title),
                onMenuClick = { /* открытие навигационного меню */ },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (state.isOffline) {
                OfflineBanner()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                when (val content = state.content) {
                    ProfileState.Content.Loading -> {
                        LoadingContent()
                    }
                    is ProfileState.Content.Error -> {
                        ErrorContent(
                            message = content.message,
                            onRetry = { onEvent(ProfileEvent.Ui.Retry) },
                        )
                    }
                    is ProfileState.Content.Form -> {
                        ProfileFormContent(
                            name = content.name,
                            age = content.age,
                            targetWeight = content.targetWeight,
                            dailyCalories = content.dailyCalories,
                            nameError = null,
                            ageError = null,
                            targetWeightError = null,
                            dailyCaloriesError = null,
                            onEvent = onEvent,
                        )
                    }
                    is ProfileState.Content.ValidationError -> {
                        ProfileFormContent(
                            name = content.name,
                            age = content.age,
                            targetWeight = content.targetWeight,
                            dailyCalories = content.dailyCalories,
                            nameError = content.nameError,
                            ageError = content.ageError,
                            targetWeightError = content.targetWeightError,
                            dailyCaloriesError = content.dailyCaloriesError,
                            onEvent = onEvent,
                        )
                    }
                    is ProfileState.Content.Success -> {
                        ProfileFormContent(
                            name = stringResource(R.string.profile_default_user_name),
                            age = "28",
                            targetWeight = "${content.profile.nutritionGoal.targetWeightKg.toInt()}",
                            dailyCalories = "${content.profile.nutritionGoal.targetCalories}",
                            nameError = null,
                            ageError = null,
                            targetWeightError = null,
                            dailyCaloriesError = null,
                            onEvent = onEvent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFormContent(
    name: String,
    age: String,
    targetWeight: String,
    dailyCalories: String,
    nameError: String?,
    ageError: String?,
    targetWeightError: String?,
    dailyCaloriesError: String?,
    onEvent: (ProfileEvent.Ui) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.medium, vertical = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = stringResource(R.string.profile_personal_info),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Поле 1: Имя
        OutlinedTextField(
            value = name,
            onValueChange = { onEvent(ProfileEvent.Ui.NameChanged(it)) },
            label = { Text(text = stringResource(R.string.profile_name_label)) },
            placeholder = { Text(text = stringResource(R.string.profile_name_placeholder)) },
            isError = nameError != null,
            supportingText = {
                if (nameError != null) {
                    Text(
                        text = nameError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(Spacing.cardRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Поле 2: Возраст
        OutlinedTextField(
            value = age,
            onValueChange = { onEvent(ProfileEvent.Ui.AgeChanged(it)) },
            label = { Text(text = stringResource(R.string.profile_age_label)) },
            placeholder = { Text(text = stringResource(R.string.profile_age_placeholder)) },
            isError = ageError != null,
            supportingText = {
                if (ageError != null) {
                    Text(
                        text = ageError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(Spacing.cardRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(R.string.profile_nutrition_goals),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = Spacing.small),
        )

        // Поле 3: Целевой вес
        OutlinedTextField(
            value = targetWeight,
            onValueChange = { onEvent(ProfileEvent.Ui.TargetWeightChanged(it)) },
            label = { Text(text = stringResource(R.string.profile_target_weight_label)) },
            placeholder = { Text(text = stringResource(R.string.profile_target_weight_placeholder)) },
            isError = targetWeightError != null,
            supportingText = {
                if (targetWeightError != null) {
                    Text(
                        text = targetWeightError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(Spacing.cardRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Поле 4: Дневной калораж
        OutlinedTextField(
            value = dailyCalories,
            onValueChange = { onEvent(ProfileEvent.Ui.DailyCaloriesChanged(it)) },
            label = { Text(text = stringResource(R.string.profile_daily_calories_label)) },
            placeholder = { Text(text = stringResource(R.string.profile_daily_calories_placeholder)) },
            isError = dailyCaloriesError != null,
            supportingText = {
                if (dailyCaloriesError != null) {
                    Text(
                        text = dailyCaloriesError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(Spacing.cardRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        // Кнопка сохранения
        Button(
            onClick = {
                onEvent(
                    ProfileEvent.Ui.SaveProfile(
                        name = name,
                        age = age,
                        targetWeight = targetWeight,
                        dailyCalories = dailyCalories,
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(Spacing.cardRadius),
        ) {
            Text(
                text = stringResource(R.string.profile_save_button),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.medium))
    }
}

// ==========================================
// Preview-функции для разных состояний экрана
// ==========================================

@Preview(name = "Loading - Light Theme", showBackground = true)
@Composable
fun ProfileScreenLoadingLightPreview() {
    FitAssistantTheme(darkTheme = false) {
        ProfileScreen(
            state = ProfileState(content = ProfileState.Content.Loading),
            onEvent = {},
        )
    }
}

@Preview(name = "Loading - Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileScreenLoadingDarkPreview() {
    FitAssistantTheme(darkTheme = true) {
        ProfileScreen(
            state = ProfileState(content = ProfileState.Content.Loading),
            onEvent = {},
        )
    }
}

@Preview(name = "Content - Light Theme", showBackground = true)
@Composable
fun ProfileScreenContentLightPreview() {
    FitAssistantTheme(darkTheme = false) {
        ProfileScreen(
            state = ProfileState(
                content = ProfileState.Content.Form(
                    name = "Aleksey",
                    age = "25",
                    targetWeight = "72.0",
                    dailyCalories = "2300",
                )
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Content - Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileScreenContentDarkPreview() {
    FitAssistantTheme(darkTheme = true) {
        ProfileScreen(
            state = ProfileState(
                content = ProfileState.Content.Form(
                    name = "Aleksey",
                    age = "25",
                    targetWeight = "72.0",
                    dailyCalories = "2300",
                )
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "ValidationError - Light Theme", showBackground = true)
@Composable
fun ProfileScreenValidationErrorLightPreview() {
    FitAssistantTheme(darkTheme = false) {
        ProfileScreen(
            state = ProfileState(
                content = ProfileState.Content.ValidationError(
                    name = "",
                    age = "abc",
                    targetWeight = "-5",
                    dailyCalories = "200",
                    nameError = "Имя не может быть пустым",
                    ageError = "Возраст должен быть числом",
                    targetWeightError = "Вес должен быть больше нуля",
                    dailyCaloriesError = "Значение вне допустимого диапазона (500-10000)",
                )
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "ValidationError - Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileScreenValidationErrorDarkPreview() {
    FitAssistantTheme(darkTheme = true) {
        ProfileScreen(
            state = ProfileState(
                content = ProfileState.Content.ValidationError(
                    name = "",
                    age = "abc",
                    targetWeight = "-5",
                    dailyCalories = "200",
                    nameError = "Имя не может быть пустым",
                    ageError = "Возраст должен быть числом",
                    targetWeightError = "Вес должен быть больше нуля",
                    dailyCaloriesError = "Значение вне допустимого диапазона (500-10000)",
                )
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Error - Light Theme", showBackground = true)
@Composable
fun ProfileScreenErrorLightPreview() {
    FitAssistantTheme(darkTheme = false) {
        ProfileScreen(
            state = ProfileState(
                content = ProfileState.Content.Error("Сетевая ошибка при загрузке данных")
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Error - Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileScreenErrorDarkPreview() {
    FitAssistantTheme(darkTheme = true) {
        ProfileScreen(
            state = ProfileState(
                content = ProfileState.Content.Error("Сетевая ошибка при загрузке данных")
            ),
            onEvent = {},
        )
    }
}
