package dev.andre.nutritionist.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import dev.andre.nutritionist.data.local.dao.FavoriteProductDao
import dev.andre.nutritionist.data.local.dao.LocalMealPlanDao
import dev.andre.nutritionist.data.local.dao.LocalRecipeDao
import dev.andre.nutritionist.data.local.dao.MealRecordDao
import dev.andre.nutritionist.data.local.dao.ProfileDao
import dev.andre.nutritionist.data.local.dao.SavedRecipeDao
import dev.andre.nutritionist.data.local.entity.FavoriteProductEntity
import dev.andre.nutritionist.data.local.entity.LocalMealPlanEntity
import dev.andre.nutritionist.data.local.entity.LocalMealPlanItemEntity
import dev.andre.nutritionist.data.local.entity.LocalRecipeEntity
import dev.andre.nutritionist.data.local.entity.LocalRecipeIngredientEntity
import dev.andre.nutritionist.data.local.entity.MealRecordEntity
import dev.andre.nutritionist.data.local.entity.ProfileEntity
import dev.andre.nutritionist.data.local.entity.SavedRecipeEntity

@Database(
    entities = [
        ProfileEntity::class,
        MealRecordEntity::class,
        LocalRecipeEntity::class,
        LocalRecipeIngredientEntity::class,
        LocalMealPlanEntity::class,
        LocalMealPlanItemEntity::class,
        FavoriteProductEntity::class,
        SavedRecipeEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun mealRecordDao(): MealRecordDao
    abstract fun localRecipeDao(): LocalRecipeDao
    abstract fun localMealPlanDao(): LocalMealPlanDao
    abstract fun favoriteProductDao(): FavoriteProductDao
    abstract fun savedRecipeDao(): SavedRecipeDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
