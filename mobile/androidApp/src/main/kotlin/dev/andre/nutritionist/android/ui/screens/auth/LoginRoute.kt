package dev.andre.nutritionist.android.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Stateful контейнер экрана авторизации (LoginRoute).
 * Управляет навигацией и событиями входа.
 */
@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    onGoogleLogin: () -> Unit = onLoginSuccess,
    onTelegramLogin: () -> Unit = onLoginSuccess,
) {
    LoginScreen(
        onLoginClick = { _, _ ->
            onLoginSuccess()
        },
        onCreateAccountClick = onNavigateToSignUp,
        onTelegramClick = onTelegramLogin,
        onGoogleClick = onGoogleLogin,
        modifier = modifier,
    )
}
