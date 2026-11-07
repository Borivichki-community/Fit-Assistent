package dev.andre.nutritionist.android.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Stateful контейнер экрана регистрации (SignUpRoute).
 * По завершении регистрации (подтверждение кода или вход через Google)
 * передаёт управление в 5-шаговый мастер онбординга.
 */
@Composable
fun SignUpRoute(
    onRegistrationSuccess: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onGoogleSignUp: () -> Unit = { onRegistrationSuccess("google_user@gmail.com") },
) {
    SignUpScreen(
        onRegistrationSuccess = onRegistrationSuccess,
        onNavigateBack = onNavigateBack,
        onGoogleSignUpClick = onGoogleSignUp,
        modifier = modifier,
    )
}
