package dev.andre.nutritionist.android.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.auth.AuthEvent
import dev.andre.nutritionist.android.auth.AuthNews
import dev.andre.nutritionist.android.auth.AuthViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful контейнер экрана авторизации (LoginRoute).
 * Управляет подпиской на AuthViewModel, запуском AppAuth Custom Tabs и навигацией.
 */
@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (data != null) {
            viewModel.dispatch(AuthEvent.Ui.AuthResponseReceived(data))
        } else {
            viewModel.dispatch(AuthEvent.Ui.AuthCancelled)
        }
    }

    LaunchedEffect(viewModel.news) {
        viewModel.news.collect { news ->
            when (news) {
                is AuthNews.LaunchAuthTab -> {
                    launcher.launch(news.intent)
                }
                is AuthNews.NavigateToMain -> {
                    onLoginSuccess()
                }
                is AuthNews.NavigateToAuth -> Unit
                is AuthNews.ShowSnackbar -> {
                    // Разовое уведомление об ошибке
                }
            }
        }
    }

    LoginScreen(
        state = state,
        onLoginClick = {
            viewModel.dispatch(AuthEvent.Ui.StartLogin(prompt = "login"))
        },
        onGoogleClick = {
            viewModel.dispatch(AuthEvent.Ui.StartLogin(idpHint = "google", prompt = "login"))
        },
        onCreateAccountClick = onNavigateToSignUp,
        modifier = modifier,
    )
}
