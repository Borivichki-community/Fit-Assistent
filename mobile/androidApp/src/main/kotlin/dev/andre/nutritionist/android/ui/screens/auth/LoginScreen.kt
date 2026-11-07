package dev.andre.nutritionist.android.ui.screens.auth

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.AuthActionButton
import dev.andre.nutritionist.android.ui.components.AuthAvatarHeader
import dev.andre.nutritionist.android.ui.components.AuthButtonStyle
import dev.andre.nutritionist.android.ui.components.AuthInputField
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme

/**
 * Экран авторизации (в точности повторяет screenshots/экран авторизации.jpg).
 */
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onCreateAccountClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onGoogleClick: () -> Unit,
    modifier: Modifier = Modifier,
    onForgotPasswordClick: () -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val invalidEmailText = stringResource(R.string.validation_error_invalid_email)
    val passwordTooShortText = stringResource(R.string.validation_error_password_too_short)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.large)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // 1. Заголовок "Авторизация"
            Text(
                text = stringResource(R.string.auth_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Круглый аватар пользователя
            AuthAvatarHeader(size = 96.dp)

            Spacer(modifier = Modifier.height(36.dp))

            // 3. Поле Email
            AuthInputField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                placeholder = stringResource(R.string.auth_email_hint),
                leadingIcon = FitIcons.Mail,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                errorMessage = emailError,
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            // 4. Поле Пароль
            AuthInputField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                placeholder = stringResource(R.string.auth_password_hint),
                leadingIcon = FitIcons.Key,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                errorMessage = passwordError,
            )

            // 5. Ссылка "Забыл пароль"
            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier.padding(top = Spacing.small),
            ) {
                Text(
                    text = stringResource(R.string.auth_forgot_password),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // 6. Главная белая кнопка "Авторизация"
            AuthActionButton(
                text = stringResource(R.string.auth_login_button),
                onClick = {
                    var hasError = false
                    if (!email.contains("@") || !email.contains(".")) {
                        emailError = invalidEmailText
                        hasError = true
                    }
                    if (password.length < 6) {
                        passwordError = passwordTooShortText
                        hasError = true
                    }
                    if (!hasError) {
                        onLoginClick(email, password)
                    }
                },
                style = AuthButtonStyle.PRIMARY_WHITE,
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            // 7. Разделитель "Нет аккаунта?"
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

            // 8. Кнопка "Создать аккаунт"
            AuthActionButton(
                text = stringResource(R.string.auth_create_account),
                onClick = onCreateAccountClick,
                style = AuthButtonStyle.SECONDARY_DARK,
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            // 9. Кнопка "Через Telegram"
            AuthActionButton(
                text = stringResource(R.string.auth_via_telegram),
                onClick = onTelegramClick,
                style = AuthButtonStyle.SECONDARY_DARK,
                icon = FitIcons.Telegram,
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            // 10. Кнопка "Через Google"
            AuthActionButton(
                text = stringResource(R.string.auth_via_google),
                onClick = onGoogleClick,
                style = AuthButtonStyle.SECONDARY_DARK,
                icon = FitIcons.Google,
            )

            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}

@Preview(name = "LoginScreen - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "LoginScreen - Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun LoginScreenPreview() {
    FitAssistantTheme {
        LoginScreen(
            onLoginClick = { _, _ -> },
            onCreateAccountClick = {},
            onTelegramClick = {},
            onGoogleClick = {},
        )
    }
}
