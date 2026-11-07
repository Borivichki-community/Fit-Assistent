package dev.andre.nutritionist.di

import dev.andre.nutritionist.data.local.AppDatabase
import dev.andre.nutritionist.data.local.DatabaseBuilder
import dev.andre.nutritionist.data.local.dao.ProfileDao
import dev.andre.nutritionist.data.repository.ProfileRepositoryImpl
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.coroutines.DefaultCoroutineDispatchers
import dev.andre.nutritionist.domain.repository.ProfileRepository
import dev.andre.nutritionist.presentation.profile.ProfileCommandsFlowHandler
import dev.andre.nutritionist.presentation.profile.ProfileUiStateMapper
import dev.andre.nutritionist.presentation.profile.ProfileUpdate
import dev.andre.nutritionist.presentation.profile.ProfileViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val domainModule: Module = module {
    // Регистрация UseCases
}

val localDataModule: Module = module {
    single<AppDatabase> { get<DatabaseBuilder<AppDatabase>>().build() }
    single<ProfileDao> { get<AppDatabase>().profileDao() }
}

val dataModule: Module = module {
    single<ProfileRepository> { ProfileRepositoryImpl(remoteDataSource = get()) }
}

val presentationModule: Module = module {
    factory { ProfileUpdate() }
    factory { ProfileCommandsFlowHandler(profileRepository = get(), dispatchers = get()) }
    factory { ProfileUiStateMapper() }
    factory { ProfileViewModel(update = get(), commandsFlowHandler = get(), uiStateMapper = get()) }
}

val sharedModule: Module = module {
    single<CoroutineDispatchers> { DefaultCoroutineDispatchers() }
    includes(domainModule, localDataModule, dataModule, presentationModule)
}

/**
 * Инициализация Koin DI для KMP (Android / iOS).
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(sharedModule)
    }
