package dev.andre.nutritionist.android.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Stateful Route для пошагового мастера онбординга.
 */
@Composable
fun RegistrationWizardRoute(
    onFinish: (OnboardingData) -> Unit,
    onNavigateBack: () -> Unit = {},
    initialData: OnboardingData = OnboardingData(),
    modifier: Modifier = Modifier,
) {
    RegistrationWizardScreen(
        onFinish = onFinish,
        initialData = initialData,
        modifier = modifier,
    )
}
