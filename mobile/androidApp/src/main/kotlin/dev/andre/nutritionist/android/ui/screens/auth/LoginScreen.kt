package dev.andre.nutritionist.android.ui.screens.auth

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.auth.AuthUiState
import dev.andre.nutritionist.android.ui.components.AuthActionButton
import dev.andre.nutritionist.android.ui.components.AuthAvatarHeader
import dev.andre.nutritionist.android.ui.components.AuthButtonStyle
import dev.andre.nutritionist.android.ui.components.ErrorContent
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme

/**
 * Чистый экран авторизации (The Elm Architecture / MVI, stateless).
 * Интегрирован с OAuth 2.1 PKCE через Keycloak и исключает устаревшие mock-поля ввода паролей.
 */
@Composable
fun LoginScreen(
    state: AuthUiState,
    onLoginClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
    onTelegramClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        when (val content = state.content) {
            is AuthUiState.Content.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    Text(
                        text = stringResource(R.string.auth_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            is AuthUiState.Content.Error -> {
                ErrorContent(
                    message = content.message,
                    onRetry = onLoginClick,
                    modifier = Modifier.padding(paddingValues),
                )
            }
            is AuthUiState.Content.Idle,
            is AuthUiState.Content.Authorized,
            -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = Spacing.large)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(48.dp))

                    // 1. Заголовок "Авторизация"
                    Text(
                        text = stringResource(R.string.auth_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    Spacer(modifier = Modifier.height(Spacing.small))

                    // 2. Подзаголовок
                    Text(
                        text = stringResource(R.string.auth_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Spacing.medium),
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // 3. Круглый аватар пользователя
                    AuthAvatarHeader(size = 96.dp)

                    Spacer(modifier = Modifier.height(48.dp))

                    // 4. Главная кнопка "Войти через Keycloak / Аккаунт"
                    AuthActionButton(
                        text = stringResource(R.string.auth_login_keycloak),
                        onClick = onLoginClick,
                        style = AuthButtonStyle.PRIMARY_WHITE,
                        icon = FitIcons.Key,
                    )

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // 5. Кнопка "Войти через Google" (kc_idp_hint=google)
                    AuthActionButton(
                        text = stringResource(R.string.auth_via_google),
                        onClick = onGoogleClick,
                        style = AuthButtonStyle.SECONDARY_DARK,
                        icon = FitIcons.Google,
                    )

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // 6. Разделитель "Нет аккаунта?"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        )
                        Text(
                            text = stringResource(R.string.auth_no_account),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.medium),
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.small))

                    // 7. Кнопка быстрой регистрации Keycloak
                    AuthActionButton(
                        text = stringResource(R.string.auth_create_account),
                        onClick = onCreateAccountClick,
                        style = AuthButtonStyle.SECONDARY_DARK,
                    )

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // 8. Кнопка "Через Telegram"
                    AuthActionButton(
                        text = stringResource(R.string.auth_via_telegram),
                        onClick = onTelegramClick,
                        style = AuthButtonStyle.SECONDARY_DARK,
                        icon = FitIcons.Telegram,
                    )

                    Spacer(modifier = Modifier.height(Spacing.large))
                }
            }
        }
    }
}

@Preview(name = "LoginScreen - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "LoginScreen - Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun LoginScreenPreview() {
    FitAssistantTheme {
        LoginScreen(
            state = AuthUiState(content = AuthUiState.Content.Idle),
            onLoginClick = {},
            onGoogleClick = {},
            onCreateAccountClick = {},
        )
    }
}
