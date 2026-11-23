package dev.andre.nutritionist.android.ui.screens.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import dev.andre.nutritionist.android.auth.AuthRepository
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * Stateful Splash Route: отображает анимацию заставки и выполняет
 * асинхронную проверку авторизации пользователя для маршрутизации.
 */
@Composable
fun SplashRoute(
    onNavigateToDashboard: () -> Unit,
    onNavigateToAuth: () -> Unit,
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = koinInject(),
) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DISPLAY_DURATION_MS)
        if (authRepository.isAuthenticated()) {
            onNavigateToDashboard()
        } else {
            onNavigateToAuth()
        }
    }

    SplashScreen(modifier = modifier)
}

private const val SPLASH_DISPLAY_DURATION_MS = 1200L
