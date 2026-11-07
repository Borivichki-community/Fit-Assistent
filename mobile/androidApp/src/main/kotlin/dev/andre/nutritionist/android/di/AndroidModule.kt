package dev.andre.nutritionist.android.di

import dev.andre.nutritionist.di.androidLocalDataModule
import dev.andre.nutritionist.di.androidRemoteDataModule
import org.koin.dsl.module

val androidAppModule = module {
    includes(androidLocalDataModule, androidRemoteDataModule)
}
