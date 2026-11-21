package dev.andre.nutritionist.di

import dev.andre.nutritionist.data.local.AppDatabase
import dev.andre.nutritionist.data.local.DatabaseBuilder
import dev.andre.nutritionist.data.local.dao.ProfileDao
import dev.andre.nutritionist.data.repository.DiaryRepositoryImpl
import dev.andre.nutritionist.data.repository.FoodCatalogRepositoryImpl
import dev.andre.nutritionist.data.repository.MealPlanRepositoryImpl
import dev.andre.nutritionist.data.repository.ProfileRepositoryImpl
import dev.andre.nutritionist.data.repository.RecipeRepositoryImpl
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.coroutines.DefaultCoroutineDispatchers
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.FoodCatalogRepository
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import dev.andre.nutritionist.domain.repository.ProfileRepository
import dev.andre.nutritionist.domain.repository.RecipeRepository
import dev.andre.nutritionist.presentation.addmeal.AddMealCommandsFlowHandler
import dev.andre.nutritionist.presentation.addmeal.AddMealUiStateMapper
import dev.andre.nutritionist.presentation.addmeal.AddMealUpdate
import dev.andre.nutritionist.presentation.addmeal.AddMealViewModel
import dev.andre.nutritionist.presentation.airecipe.AiRecipeCommandsFlowHandler
import dev.andre.nutritionist.presentation.airecipe.AiRecipeUiStateMapper
import dev.andre.nutritionist.presentation.airecipe.AiRecipeUpdate
import dev.andre.nutritionist.presentation.airecipe.AiRecipeViewModel
import dev.andre.nutritionist.presentation.catalog.CatalogCommandsFlowHandler
import dev.andre.nutritionist.presentation.catalog.CatalogUiStateMapper
import dev.andre.nutritionist.presentation.catalog.CatalogUpdate
import dev.andre.nutritionist.presentation.catalog.CatalogViewModel
import dev.andre.nutritionist.presentation.dashboard.DashboardCommandsFlowHandler
import dev.andre.nutritionist.presentation.dashboard.DashboardUiStateMapper
import dev.andre.nutritionist.presentation.dashboard.DashboardUpdate
import dev.andre.nutritionist.presentation.dashboard.DashboardViewModel
import dev.andre.nutritionist.presentation.profile.ProfileCommandsFlowHandler
import dev.andre.nutritionist.presentation.profile.ProfileUiStateMapper
import dev.andre.nutritionist.presentation.profile.ProfileUpdate
import dev.andre.nutritionist.presentation.profile.ProfileViewModel
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipesCommandsFlowHandler
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipesUiStateMapper
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipesUpdate
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipesViewModel
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
    single<ProfileRepository> {
        ProfileRepositoryImpl(
            dao = get(),
            remoteDataSource = get(),
            dispatchers = get(),
        )
    }
    single<DiaryRepository> {
        DiaryRepositoryImpl(
            dispatchers = get(),
        )
    }
    single<FoodCatalogRepository> {
        FoodCatalogRepositoryImpl(
            dispatchers = get(),
        )
    }
    single<MealPlanRepository> {
        MealPlanRepositoryImpl(
            dispatchers = get(),
        )
    }
    single<RecipeRepository> {
        RecipeRepositoryImpl()
    }
}

val presentationModule: Module = module {
    factory { ProfileUpdate() }
    factory { ProfileCommandsFlowHandler(profileRepository = get(), dispatchers = get()) }
    factory { ProfileUiStateMapper() }
    factory { ProfileViewModel(update = get(), commandsFlowHandler = get(), uiStateMapper = get()) }

    factory { DashboardUpdate() }
    factory {
        DashboardCommandsFlowHandler(
            diaryRepository = get(),
            profileRepository = get(),
            dispatchers = get(),
        )
    }
    factory { DashboardUiStateMapper() }
    factory {
        DashboardViewModel(
            update = get(),
            commandsFlowHandler = get(),
            uiStateMapper = get(),
        )
    }

    factory { AddMealUpdate() }
    factory {
        AddMealCommandsFlowHandler(
            foodCatalogRepository = get(),
            diaryRepository = get(),
            dispatchers = get(),
        )
    }
    factory { AddMealUiStateMapper() }
    factory {
        AddMealViewModel(
            update = get(),
            commandsFlowHandler = get(),
            uiStateMapper = get(),
        )
    }

    factory { CatalogUpdate() }
    factory {
        CatalogCommandsFlowHandler(
            mealPlanRepository = get(),
            dispatchers = get(),
        )
    }
    factory { CatalogUiStateMapper() }
    factory {
        CatalogViewModel(
            update = get(),
            commandsFlowHandler = get(),
            uiStateMapper = get(),
        )
    }

    factory { AiRecipeUpdate() }
    factory {
        AiRecipeCommandsFlowHandler(
            recipeRepository = get(),
            diaryRepository = get(),
            dispatchers = get(),
        )
    }
    factory { AiRecipeUiStateMapper() }
    factory {
        AiRecipeViewModel(
            update = get(),
            commandsFlowHandler = get(),
            uiStateMapper = get(),
        )
    }

    factory { SavedRecipesUpdate() }
    factory {
        SavedRecipesCommandsFlowHandler(
            recipeRepository = get(),
            diaryRepository = get(),
            dispatchers = get(),
        )
    }
    factory { SavedRecipesUiStateMapper() }
    factory {
        SavedRecipesViewModel(
            update = get(),
            commandsFlowHandler = get(),
            uiStateMapper = get(),
        )
    }
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
