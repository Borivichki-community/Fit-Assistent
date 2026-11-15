package dev.andre.nutritionist.android.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import dev.andre.nutritionist.android.auth.AuthEvent
import dev.andre.nutritionist.android.auth.AuthNews
import dev.andre.nutritionist.android.auth.AuthViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful контейнер экрана регистрации (SignUpRoute).
 * Запускает AppAuth регистрационный flow Keycloak (prompt=create или kc_idp_hint=google),
 * а по завершении перенаправляет в онбординг-визард.
 */
@Composable
fun SignUpRoute(
    onRegistrationSuccess: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
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
                    onRegistrationSuccess("registered_user")
                }
                is AuthNews.NavigateToAuth -> Unit
                is AuthNews.ShowSnackbar -> {
                    // Разовое уведомление об ошибке
                }
            }
        }
    }

    SignUpScreen(
        onRegisterKeycloakClick = {
            viewModel.dispatch(AuthEvent.Ui.StartLogin(prompt = "create"))
        },
        onNavigateBack = onNavigateBack,
        onGoogleSignUpClick = {
            viewModel.dispatch(AuthEvent.Ui.StartLogin(idpHint = "google"))
        },
        modifier = modifier,
    )
}
