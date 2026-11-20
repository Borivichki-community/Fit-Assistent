package dev.andre.nutritionist.android.ui.screens.onboarding

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.presentation.profile.validation.toMessageRes
import dev.andre.nutritionist.android.ui.components.DotsIndicator
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.components.WizardNavButton
import dev.andre.nutritionist.android.ui.components.WizardNavDirection
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import dev.andre.nutritionist.domain.validation.FieldValidationError
import dev.andre.nutritionist.domain.validation.ProfileValidator
import kotlinx.coroutines.launch

/**
 * 4-шаговый визард онбординга / регистрации в минималистичном стиле Antigravity.
 * Имя пользователя берётся напрямую из Keycloak (OIDC / OAuth 2.1 PKCE).
 * Визард собирает физические параметры и цели питания:
 * - Шаг 0: Рост, вес, возраст, пол
 * - Шаг 1: Уровень физической активности
 * - Шаг 2: Недельный бюджет и рекомендации
 * - Шаг 3: Выбор цели питания
 */
@Composable
fun RegistrationWizardScreen(
    onFinish: (OnboardingData) -> Unit,
    modifier: Modifier = Modifier,
    initialData: OnboardingData = OnboardingData(),
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 4 }
    var data by remember { mutableStateOf(initialData) }

    // Состояния доменных ошибок валидации полей
    var heightError by remember { mutableStateOf<FieldValidationError?>(null) }
    var weightError by remember { mutableStateOf<FieldValidationError?>(null) }
    var ageError by remember { mutableStateOf<FieldValidationError?>(null) }
    var budgetError by remember { mutableStateOf<FieldValidationError?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Контейнер страниц визарда
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 -> OnboardingBodyStep(
                            firstName = data.firstName,
                            heightCm = data.heightCm,
                            weightKg = data.weightKg,
                            age = data.age,
                            gender = data.gender,
                            onHeightChanged = {
                                data = data.copy(heightCm = it)
                                if (heightError != null) heightError = null
                            },
                            onWeightChanged = {
                                data = data.copy(weightKg = it)
                                if (weightError != null) weightError = null
                            },
                            onAgeChanged = {
                                data = data.copy(age = it)
                                if (ageError != null) ageError = null
                            },
                            onGenderChanged = { data = data.copy(gender = it) },
                            heightError = heightError?.toMessageRes()?.let { stringResource(it) },
                            weightError = weightError?.toMessageRes()?.let { stringResource(it) },
                            ageError = ageError?.toMessageRes()?.let { stringResource(it) },
                        )
                        1 -> OnboardingActivityStep(
                            firstName = data.firstName,
                            activityLevel = data.activityLevel,
                            onActivityLevelChanged = { data = data.copy(activityLevel = it) },
                        )
                        2 -> OnboardingBudgetStep(
                            firstName = data.firstName,
                            weeklyBudget = data.weeklyBudget,
                            onWeeklyBudgetChanged = {
                                data = data.copy(weeklyBudget = it)
                                if (budgetError != null) budgetError = null
                            },
                            budgetError = budgetError?.toMessageRes()?.let { stringResource(it) },
                        )
                        3 -> OnboardingGoalStep(
                            selectedGoal = data.goalType,
                            onGoalSelected = { data = data.copy(goalType = it) },
                        )
                    }
                }
            }

            // Нижняя панель навигации (Назад - Точки-индикатор - Далее / Завершить)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large, vertical = Spacing.large),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Кнопка «Назад» (скрыта на первом шаге)
                Box(modifier = Modifier.size(56.dp)) {
                    if (pagerState.currentPage > 0) {
                        WizardNavButton(
                            direction = WizardNavDirection.BACK,
                            contentDescription = stringResource(R.string.onboarding_nav_back),
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                        )
                    }
                }

                // Индикатор шагов (4 точки)
                DotsIndicator(
                    totalDots = 4,
                    currentDot = pagerState.currentPage,
                )

                // Кнопка «Далее» / «Завершить»
                val isFinalStep = pagerState.currentPage == 3
                WizardNavButton(
                    direction = if (isFinalStep) WizardNavDirection.FINISH else WizardNavDirection.FORWARD,
                    contentDescription = if (isFinalStep) {
                        stringResource(R.string.onboarding_nav_finish)
                    } else {
                        stringResource(R.string.onboarding_nav_next)
                    },
                    onClick = {
                        when (pagerState.currentPage) {
                            0 -> {
                                val heightVal = ProfileValidator.validateHeight(data.heightCm)
                                val weightVal = ProfileValidator.validateWeight(data.weightKg)
                                val ageVal = ProfileValidator.validateAge(data.age)

                                heightError = heightVal
                                weightError = weightVal
                                ageError = ageVal

                                if (heightVal == null && weightVal == null && ageVal == null) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                }
                            }
                            1 -> {
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            }
                            2 -> {
                                val budgetVal = ProfileValidator.validateWeeklyBudget(data.weeklyBudget)
                                budgetError = budgetVal
                                if (budgetVal != null) {
                                    return@WizardNavButton
                                }
                                coroutineScope.launch { pagerState.animateScrollToPage(3) }
                            }
                            3 -> {
                                onFinish(data)
                            }
                        }
                    },
                )
            }
        }
    }
}

// ==========================================
// Previews
// ==========================================

@Preview(name = "Onboarding - Light Theme", showBackground = true)
@Composable
fun RegistrationWizardScreenLightPreview() {
    FitAssistantTheme(darkTheme = false) {
        RegistrationWizardScreen(
            onFinish = {},
            initialData = OnboardingData(firstName = "Aleksey", lastName = "Codintsev"),
        )
    }
}

@Preview(name = "Onboarding - Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RegistrationWizardScreenDarkPreview() {
    FitAssistantTheme(darkTheme = true) {
        RegistrationWizardScreen(
            onFinish = {},
            initialData = OnboardingData(firstName = "Aleksey", lastName = "Codintsev"),
        )
    }
}
