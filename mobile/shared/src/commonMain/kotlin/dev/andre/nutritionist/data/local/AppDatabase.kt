package dev.andre.nutritionist.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import dev.andre.nutritionist.data.local.dao.DiaryDao
import dev.andre.nutritionist.data.local.dao.ProfileDao
import dev.andre.nutritionist.data.local.entity.DiaryEntryEntity
import dev.andre.nutritionist.data.local.entity.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        DiaryEntryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun diaryDao(): DiaryDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
