package dev.andre.nutritionist.android.ui.screens.auth

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.andre.nutritionist.android.ui.components.AuthActionButton
import dev.andre.nutritionist.android.ui.components.AuthAvatarHeader
import dev.andre.nutritionist.android.ui.components.AuthButtonStyle
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme

/**
 * Чистый экран регистрации (stateless Compose UI).
 * Делегирует регистрационный поток и верификацию email системе Keycloak через OAuth 2.1 PKCE.
 */
@Composable
fun SignUpScreen(
    onRegisterKeycloakClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onGoogleSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Spacer(modifier = Modifier.height(48.dp))

            // 1. Заголовок "Регистрация"
            Text(
                text = stringResource(R.string.signup_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            // 2. Подзаголовок
            Text(
                text = stringResource(R.string.signup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.medium),
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 3. Круглый аватар пользователя
            AuthAvatarHeader(size = 96.dp)

            Spacer(modifier = Modifier.height(48.dp))

            // 4. Главная кнопка "Зарегистрироваться через Keycloak"
            AuthActionButton(
                text = stringResource(R.string.signup_register_keycloak),
                onClick = onRegisterKeycloakClick,
                style = AuthButtonStyle.PRIMARY_WHITE,
                icon = FitIcons.Key,
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            // 5. Быстрая регистрация через Google
            AuthActionButton(
                text = stringResource(R.string.signup_via_google),
                onClick = onGoogleSignUpClick,
                style = AuthButtonStyle.SECONDARY_DARK,
                icon = FitIcons.Google,
            )

            Spacer(modifier = Modifier.height(Spacing.large))

            // 6. Кнопка "Назад"
            AuthActionButton(
                text = stringResource(R.string.signup_back_button),
                onClick = onNavigateBack,
                style = AuthButtonStyle.SECONDARY_DARK,
            )

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
            onRegisterKeycloakClick = {},
            onNavigateBack = {},
            onGoogleSignUpClick = {},
        )
    }
}
