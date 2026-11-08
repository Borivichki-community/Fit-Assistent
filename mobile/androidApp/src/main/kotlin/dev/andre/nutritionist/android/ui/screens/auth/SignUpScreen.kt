package dev.andre.nutritionist.android.ui.screens.auth

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
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
 * Экран регистрации (в точности повторяет screenshots/экран регистрации 1.jpg
 * с шагом подтверждения почты кодом и быстрой регистрацией через Google).
 */
@Composable
fun SignUpScreen(
    onRegistrationSuccess: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onGoogleSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }

    val invalidEmailText = stringResource(R.string.validation_error_invalid_email)
    val passwordTooShortText = stringResource(R.string.validation_error_password_too_short)
    val invalidCodeText = stringResource(R.string.validation_error_code_invalid)

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

            // 1. Заголовок "Регистрация"
            Text(
                text = stringResource(R.string.signup_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Круглый аватар пользователя
            AuthAvatarHeader(size = 96.dp)

            Spacer(modifier = Modifier.height(36.dp))

            if (!isCodeSent) {
                // Шаг 1: Ввод почты и пароля
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

                Spacer(modifier = Modifier.height(Spacing.large))

                // Главная белая кнопка "Регистрация" (отправляет код на email)
                AuthActionButton(
                    text = stringResource(R.string.signup_register_button),
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
                            isCodeSent = true
                        }
                    },
                    style = AuthButtonStyle.PRIMARY_WHITE,
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Быстрая регистрация через Google
                AuthActionButton(
                    text = stringResource(R.string.signup_via_google),
                    onClick = onGoogleSignUpClick,
                    style = AuthButtonStyle.SECONDARY_DARK,
                    icon = FitIcons.Google,
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Кнопка "Назад"
                AuthActionButton(
                    text = stringResource(R.string.signup_back_button),
                    onClick = onNavigateBack,
                    style = AuthButtonStyle.SECONDARY_DARK,
                )
            } else {
                // Шаг 2: Ввод кода подтверждения из почты
                Text(
                    text = stringResource(R.string.signup_code_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                Text(
                    text = stringResource(R.string.signup_code_subtitle, email),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(Spacing.large))

                AuthInputField(
                    value = verificationCode,
                    onValueChange = {
                        verificationCode = it
                        if (codeError != null) codeError = null
                    },
                    placeholder = stringResource(R.string.signup_code_hint),
                    leadingIcon = FitIcons.Key,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    errorMessage = codeError,
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                TextButton(
                    onClick = { /* повторная отправка кода */ },
                ) {
                    Text(
                        text = stringResource(R.string.signup_resend_code),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Кнопка "Подтвердить код"
                AuthActionButton(
                    text = stringResource(R.string.signup_confirm_code),
                    onClick = {
                        if (verificationCode.trim().length < 4) {
                            codeError = invalidCodeText
                        } else {
                            onRegistrationSuccess(email)
                        }
                    },
                    style = AuthButtonStyle.PRIMARY_WHITE,
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Кнопка "Назад" (возврат к вводу почты)
                AuthActionButton(
                    text = stringResource(R.string.signup_back_button),
                    onClick = { isCodeSent = false },
                    style = AuthButtonStyle.SECONDARY_DARK,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}

@Preview(name = "SignUpScreen - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "SignUpScreen - Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SignUpScreenPreview() {
    FitAssistantTheme {
        SignUpScreen(
            onRegistrationSuccess = {},
            onNavigateBack = {},
            onGoogleSignUpClick = {},
        )
    }
}
