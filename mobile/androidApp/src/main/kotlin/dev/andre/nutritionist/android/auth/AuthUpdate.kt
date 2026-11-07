package dev.andre.nutritionist.android.auth

import dev.andre.nutritionist.core.kotea.DslUpdate

/**
 * Чистый синхронный редьюсер логики авторизации (KoTEA Update).
 * Запрещены сайд-эффекты, вызовы корутин и ветка else в when.
 */
class AuthUpdate : DslUpdate<AuthState, AuthEvent, AuthCommand, AuthNews>() {

    override fun NextBuilder<AuthState, AuthCommand, AuthNews>.update(event: AuthEvent) {
        when (event) {
            is AuthEvent.Ui.StartLogin -> {
                state { copy(content = AuthState.Content.Loading) }
                commands(AuthCommand.LaunchAuthTab)
            }
            is AuthEvent.Ui.AuthResponseReceived -> {
                state { copy(content = AuthState.Content.Loading) }
                commands(AuthCommand.ProcessAuthResponse(event.intent))
            }
            is AuthEvent.Ui.LogoutClicked -> {
                state { copy(content = AuthState.Content.Loading) }
                commands(AuthCommand.Logout)
            }
            is AuthEvent.Internal.AuthIntentPrepared -> {
                news(AuthNews.LaunchAuthTab(event.intent))
            }
            is AuthEvent.Internal.AuthSuccess -> {
                state { copy(content = AuthState.Content.Authorized) }
                news(AuthNews.NavigateToMain)
            }
            is AuthEvent.Internal.AuthFailed -> {
                val message = event.error.message?.takeIf { it.isNotBlank() } ?: "Ошибка авторизации"
                state { copy(content = AuthState.Content.Error(message)) }
                news(AuthNews.ShowSnackbar(message))
            }
            is AuthEvent.Internal.LoggedOut -> {
                state { copy(content = AuthState.Content.Idle) }
            }
        }
    }
}
