package dev.andre.nutritionist.android

import android.app.Application
import dev.andre.nutritionist.android.di.androidAppModule
import dev.andre.nutritionist.di.initKoin
import org.koin.android.ext.koin.androidContext

class NutritionistApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@NutritionistApp)
            modules(androidAppModule)
        }
    }
}
