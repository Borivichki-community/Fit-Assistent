package dev.andre.nutritionist.android.di

import dev.andre.nutritionist.android.auth.AppAuthIntentProvider
import dev.andre.nutritionist.android.auth.AppAuthTokenExchanger
import dev.andre.nutritionist.android.auth.AuthConfig
import dev.andre.nutritionist.android.auth.AuthCommandsFlowHandler
import dev.andre.nutritionist.android.auth.AuthIntentProvider
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.auth.AuthRepositoryImpl
import dev.andre.nutritionist.android.auth.AuthStorage
import dev.andre.nutritionist.android.auth.AuthUiStateMapper
import dev.andre.nutritionist.android.auth.AuthUpdate
import dev.andre.nutritionist.android.auth.AuthViewModel
import dev.andre.nutritionist.android.auth.EncryptedPrefsAuthStorage
import dev.andre.nutritionist.android.auth.TokenExchanger
import dev.andre.nutritionist.di.androidLocalDataModule
import dev.andre.nutritionist.di.androidRemoteDataModule
import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import net.openid.appauth.AuthorizationService
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val authModule = module {
    single { AuthConfig() }
    single { AuthorizationService(get()) }
    single<AuthStorage> { EncryptedPrefsAuthStorage(context = get(), dispatchers = get()) }
    single<TokenExchanger> { AppAuthTokenExchanger(authService = get()) }
    single<AuthIntentProvider> {
        AppAuthIntentProvider(
            authConfig = get(),
            authService = get(),
            dispatchers = get(),
        )
    }
    single {
        AuthRepositoryImpl(
            authStorage = get(),
            tokenExchanger = get(),
            authService = get(),
            dispatchers = get(),
        )
    } bind AuthRepository::class bind AuthTokenProvider::class

    single { AuthUpdate() }
    single { AuthUiStateMapper() }
    single {
        AuthCommandsFlowHandler(
            authRepository = get(),
            authIntentProvider = get(),
            dispatchers = get(),
        )
    }
    viewModel {
        AuthViewModel(
            update = get(),
            commandsFlowHandler = get(),
            uiStateMapper = get(),
        )
    }
}

val androidAppModule = module {
    includes(androidLocalDataModule, authModule, androidRemoteDataModule)
}
