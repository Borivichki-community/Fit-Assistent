package dev.andre.nutritionist.android

import android.app.Application
import dev.andre.nutritionist.android.di.androidAppModule
import dev.andre.nutritionist.android.network.AndroidDiaryRemoteDataSource
import dev.andre.nutritionist.data.remote.DefaultDiaryRemoteDataSource
import dev.andre.nutritionist.di.initKoin
import dev.andre.nutritionist.domain.coroutines.DefaultCoroutineDispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

class NutritionistApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val diaryRemote = AndroidDiaryRemoteDataSource(DefaultCoroutineDispatchers())
        DefaultDiaryRemoteDataSource.companionSyncHandler = { entry ->
            diaryRemote.syncEntry(entry)
        }
        if (GlobalContext.getOrNull() == null) {
            initKoin {
                androidContext(this@NutritionistApp)
                modules(androidAppModule)
            }
        }
    }
}
