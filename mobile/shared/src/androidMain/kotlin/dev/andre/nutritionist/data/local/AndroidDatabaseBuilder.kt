package dev.andre.nutritionist.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

class AndroidDatabaseBuilder(
    private val context: Context,
    private val databaseName: String = "fit_assistant.db",
) : DatabaseBuilder<AppDatabase> {

    override fun build(): AppDatabase {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(databaseName)
        return Room.databaseBuilder<AppDatabase>(
            context = appContext,
            name = dbFile.absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
