package dev.andre.nutritionist.android.auth

import android.content.Intent
import dev.andre.nutritionist.core.kotea.UiStateMapper

/**
 * Неделимый снимок состояния фичи авторизации (KoTEA State).
 */
data class AuthState(
    val content: Content = Content.Idle,
) {
    sealed interface Content {
        data object Idle : Content
        data object Loading : Content
        data object Authorized : Content
        data class Error(val message: String) : Content
    }
}

/**
 * Контракт событий авторизации:
 * - [Ui]: намерения пользователя из интерфейса;
 * - [Internal]: результаты асинхронных операций из CommandsFlowHandler.
 */
sealed interface AuthEvent {

    sealed interface Ui : AuthEvent {
        data object StartLogin : Ui
        data class AuthResponseReceived(val intent: Intent) : Ui
        data object LogoutClicked : Ui
    }

    sealed interface Internal : AuthEvent {
        data class AuthIntentPrepared(val intent: Intent) : Internal
        data object AuthSuccess : Internal
        data class AuthFailed(val error: Throwable) : Internal
        data object LoggedOut : Internal
    }
}

/**
 * Асинхронные команды авторизации (сайд-эффекты).
 */
sealed interface AuthCommand {
    data object LaunchAuthTab : AuthCommand
    data class ProcessAuthResponse(val intent: Intent) : AuthCommand
    data object Logout : AuthCommand
}

/**
 * Разовые уведомления для UI (эффекты навигации и snackbar).
 */
sealed interface AuthNews {
    data class LaunchAuthTab(val intent: Intent) : AuthNews
    data object NavigateToMain : AuthNews
    data class ShowSnackbar(val message: String) : AuthNews
}

/**
 * Тонкое состояние для Compose UI.
 */
data class AuthUiState(
    val content: Content = Content.Idle,
) {
    sealed interface Content {
        data object Idle : Content
        data object Loading : Content
        data object Authorized : Content
        data class Error(val message: String) : Content
    }
}

/**
 * Чистый маппер из [AuthState] в [AuthUiState].
 */
class AuthUiStateMapper : UiStateMapper<AuthState, AuthUiState> {
    override fun map(state: AuthState): AuthUiState {
        val uiContent = when (val c = state.content) {
            is AuthState.Content.Idle -> AuthUiState.Content.Idle
            is AuthState.Content.Loading -> AuthUiState.Content.Loading
            is AuthState.Content.Authorized -> AuthUiState.Content.Authorized
            is AuthState.Content.Error -> AuthUiState.Content.Error(c.message)
        }
        return AuthUiState(content = uiContent)
    }
}
