package dev.andre.nutritionist.android.auth

import android.net.Uri

/**
 * Конфигурация клиента OAuth 2.0 / OpenID Connect для взаимодействия с Keycloak.
 *
 * Все параметры имеют безопасные значения по умолчанию для локального окружения,
 * но могут переопределяться через Koin DI или BuildConfig без хардкода секретов.
 * Для публичного клиента (public client) с PKCE client_secret не используется.
 */
data class AuthConfig(
    val clientId: String = DEFAULT_CLIENT_ID,
    val redirectUri: Uri = Uri.parse(DEFAULT_REDIRECT_URI),
    val discoveryUri: Uri = Uri.parse(DEFAULT_DISCOVERY_URI),
    val scope: String = DEFAULT_SCOPE,
) {
    companion object {
        const val DEFAULT_CLIENT_ID: String = "fitassistant-mobile"
        const val DEFAULT_REDIRECT_URI: String = "com.budgetntr.fitassistant://oauth2redirect"
        const val DEFAULT_DISCOVERY_URI: String =
            "http://10.0.2.2:8080/realms/FitAssistant/.well-known/openid-configuration"
        const val DEFAULT_SCOPE: String = "openid profile email"
    }
}
