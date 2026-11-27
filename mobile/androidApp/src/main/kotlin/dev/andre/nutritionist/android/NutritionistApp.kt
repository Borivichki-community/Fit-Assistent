package dev.andre.nutritionist.android

import android.app.Application
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.di.androidAppModule
import dev.andre.nutritionist.di.initKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

class NutritionistApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            initKoin {
                androidContext(this@NutritionistApp)
                modules(androidAppModule)
            }
        }
        // Фоновый прогрев EncryptedSharedPreferences / Android Keystore до обращения в MainActivity
        appScope.launch {
            runCatching { GlobalContext.getOrNull()?.get<AuthRepository>()?.isAuthenticated() }
        }
    }
}
