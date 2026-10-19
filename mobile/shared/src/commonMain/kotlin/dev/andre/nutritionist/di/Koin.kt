package dev.andre.nutritionist.di

import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.coroutines.DefaultCoroutineDispatchers
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val domainModule: Module = module {
    // Регистрация UseCases
}

val dataModule: Module = module {
    // Регистрация Repositories и DataSources
}

val sharedModule: Module = module {
    single<CoroutineDispatchers> { DefaultCoroutineDispatchers() }
    includes(domainModule, dataModule)
}

/**
 * Инициализация Koin DI для KMP (Android / iOS).
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(sharedModule)
    }
