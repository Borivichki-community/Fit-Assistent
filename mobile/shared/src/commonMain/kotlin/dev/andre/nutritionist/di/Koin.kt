package dev.andre.nutritionist.di

import dev.andre.nutritionist.data.local.AppDatabase
import dev.andre.nutritionist.data.local.DatabaseBuilder
import dev.andre.nutritionist.data.local.dao.FavoriteProductDao
import dev.andre.nutritionist.data.local.dao.LocalMealPlanDao
import dev.andre.nutritionist.data.local.dao.LocalRecipeDao
import dev.andre.nutritionist.data.local.dao.MealRecordDao
import dev.andre.nutritionist.data.local.dao.ProfileDao
import dev.andre.nutritionist.data.local.dao.SavedRecipeDao
import dev.andre.nutritionist.data.repository.CatalogRepositoryImpl
import dev.andre.nutritionist.data.repository.CoachingRepositoryImpl
import dev.andre.nutritionist.data.repository.DiaryRepositoryImpl
import dev.andre.nutritionist.data.repository.MealPlanRepositoryImpl
import dev.andre.nutritionist.data.repository.ProfileRepositoryImpl
import dev.andre.nutritionist.data.repository.RecipeCatalogRepositoryImpl
import dev.andre.nutritionist.data.repository.RecipeRepositoryImpl
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.coroutines.DefaultCoroutineDispatchers
import dev.andre.nutritionist.domain.repository.CatalogRepository
import dev.andre.nutritionist.domain.repository.CoachingRepository
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import dev.andre.nutritionist.domain.repository.ProfileRepository
import dev.andre.nutritionist.domain.repository.RecipeCatalogRepository
import dev.andre.nutritionist.domain.repository.RecipeRepository
import dev.andre.nutritionist.domain.usecase.ApplyMealPlanDayToDiaryUseCase
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuUseCase
import dev.andre.nutritionist.domain.usecase.GetNutritionAnalyticsUseCase
import dev.andre.nutritionist.domain.usecase.LogRecipeToDiaryUseCase
import dev.andre.nutritionist.presentation.airecipe.MenuCommandsFlowHandler
import dev.andre.nutritionist.presentation.airecipe.MenuUiStateMapper
import dev.andre.nutritionist.presentation.airecipe.MenuUpdate
import dev.andre.nutritionist.presentation.airecipe.MenuViewModel
import dev.andre.nutritionist.presentation.catalog.CatalogSearchCommandsFlowHandler
import dev.andre.nutritionist.presentation.catalog.CatalogSearchUiStateMapper
import dev.andre.nutritionist.presentation.catalog.CatalogSearchUpdate
import dev.andre.nutritionist.presentation.catalog.CatalogSearchViewModel
import dev.andre.nutritionist.presentation.dashboard.DiaryCommandsFlowHandler
import dev.andre.nutritionist.presentation.dashboard.DiaryUiStateMapper
import dev.andre.nutritionist.presentation.dashboard.DiaryUpdate
import dev.andre.nutritionist.presentation.dashboard.DiaryViewModel
import dev.andre.nutritionist.presentation.profile.ProfileCommandsFlowHandler
import dev.andre.nutritionist.presentation.profile.ProfileUiStateMapper
import dev.andre.nutritionist.presentation.profile.ProfileUpdate
import dev.andre.nutritionist.presentation.profile.ProfileViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val domainModule: Module = module {
    factory { LogRecipeToDiaryUseCase(diaryRepository = get(), dispatchers = get()) }
    factory { ApplyMealPlanDayToDiaryUseCase(diaryRepository = get(), dispatchers = get()) }
    factory {
        GenerateBudgetMenuUseCase(
            recipeRepository = get(),
            catalogRepository = get(),
            dispatchers = get(),
        )
    }
    factory {
        GetNutritionAnalyticsUseCase(
            diaryRepository = get(),
            profileRepository = get(),
            dispatchers = get(),
        )
    }
}

val localDataModule: Module = module {
    single<AppDatabase> { get<DatabaseBuilder<AppDatabase>>().build() }
    single<ProfileDao> { get<AppDatabase>().profileDao() }
    single<MealRecordDao> { get<AppDatabase>().mealRecordDao() }
    single<LocalRecipeDao> { get<AppDatabase>().localRecipeDao() }
    single<LocalMealPlanDao> { get<AppDatabase>().localMealPlanDao() }
    single<FavoriteProductDao> { get<AppDatabase>().favoriteProductDao() }
    single<SavedRecipeDao> { get<AppDatabase>().savedRecipeDao() }
}

val dataModule: Module = module {
    single<ProfileRepository> {
        ProfileRepositoryImpl(
            dao = get(),
            remoteDataSource = get(),
            dispatchers = get(),
        )
    }
    single<DiaryRepository> {
        DiaryRepositoryImpl(
            dao = get(),
            remoteDataSource = get(),
            dispatchers = get(),
            syncScheduler = getOrNull(),
        )
    }
    single<CatalogRepository> {
        CatalogRepositoryImpl(
            remoteDataSource = get(),
            favoriteProductDao = get(),
            dispatchers = get(),
        )
    }
    single<RecipeRepository> {
        RecipeRepositoryImpl(
            remoteDataSource = get(),
            savedRecipeDao = get(),
            dispatchers = get(),
        )
    }
    single<RecipeCatalogRepository> {
        RecipeCatalogRepositoryImpl(
            localRecipeDao = get(),
            remoteDataSource = get(),
            dispatchers = get(),
        )
    }
    single<MealPlanRepository> {
        MealPlanRepositoryImpl(
            localMealPlanDao = get(),
            remoteDataSource = get(),
            dispatchers = get(),
        )
    }
    single<CoachingRepository> {
        CoachingRepositoryImpl(
            remoteDataSource = get(),
            dispatchers = get(),
        )
    }
}

val presentationModule: Module = module {
    factory { ProfileUpdate() }
    factory { ProfileCommandsFlowHandler(profileRepository = get(), dispatchers = get()) }
    factory { ProfileUiStateMapper() }
    factory { ProfileViewModel(update = get(), commandsFlowHandler = get(), uiStateMapper = get()) }

    factory { CatalogSearchUpdate() }
    factory { CatalogSearchCommandsFlowHandler(catalogRepository = get(), dispatchers = get()) }
    factory { CatalogSearchUiStateMapper() }
    factory {
        CatalogSearchViewModel(
            update = get(),
            commandsFlowHandler = get(),
            uiStateMapper = get(),
        )
    }

    factory { MenuUpdate() }
    factory { MenuCommandsFlowHandler(generateBudgetMenuUseCase = get(), dispatchers = get()) }
    factory { MenuUiStateMapper() }
    factory { MenuViewModel(update = get(), commandsFlowHandler = get(), uiStateMapper = get()) }

    factory { DiaryUpdate() }
    factory {
        DiaryCommandsFlowHandler(
            diaryRepository = get(),
            dispatchers = get(),
            profileRepository = getOrNull(),
        )
    }
    factory { DiaryUiStateMapper() }
    factory { DiaryViewModel(update = get(), commandsFlowHandler = get(), uiStateMapper = get()) }
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