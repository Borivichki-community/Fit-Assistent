package dev.andre.nutritionist.android

import android.app.Application
import dev.andre.nutritionist.android.di.androidAppModule
import dev.andre.nutritionist.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

class NutritionistApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            initKoin {
                androidContext(this@NutritionistApp)
                modules(androidAppModule)
            }
        }
    }
}
