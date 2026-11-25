package dev.andre.nutritionist.data.local

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import app.cash.turbine.test
import dev.andre.nutritionist.data.local.entity.FavoriteProductEntity
import dev.andre.nutritionist.data.local.entity.SavedRecipeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppDatabaseMigrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun migrate3To4_preservesV3DataAndEnablesFavoritesAndSavedRecipes() = runTest {
        val dbFile = tempFolder.newFile("migration_3_4_test.db")
        val driver = AndroidSQLiteDriver()

        val v3Connection = driver.open(dbFile.absolutePath)
        try {
            seedV2Tables(v3Connection)
            DatabaseMigrations.MIGRATION_2_3.migrate(v3Connection)
            v3Connection.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            )
            v3Connection.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '29d9001fc9c6ed98ae5128a4c8628a6a')",
            )
            v3Connection.execSQL(
                """
                INSERT INTO `local_recipes` (
                    `id`, `author_type`, `author_id`, `origin_recipe_id`, `title`,
                    `description`, `instructions`, `visibility`, `servings_count`,
                    `total_weight_g`, `calories_per_100g`, `proteins_per_100g`,
                    `fats_per_100g`, `carbs_per_100g`, `sync_status`, `idempotency_key`,
                    `is_deleted`, `updated_at`
                ) VALUES (
                    'rec-v3-keep', 'USER', 'u-1', NULL, 'Сохранённый v3 рецепт',
                    NULL, NULL, 'PRIVATE', 1, 200.0, 150.0, 10.0, 5.0, 16.0,
                    'SYNCED', 'idem-v3', 0, 1700000000000
                )
                """.trimIndent(),
            )
            v3Connection.execSQL("PRAGMA user_version = 3")
        } finally {
            v3Connection.close()
        }

        val context = RuntimeEnvironment.getApplication()
        val migratedDb = Room.databaseBuilder<AppDatabase>(
            context = context,
            name = dbFile.absolutePath,
        )
            .setDriver(driver)
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4,
            )
            .build()

        try {
            // Existing v3 data survived
            val existingRecipe = migratedDb.localRecipeDao().getRecipeWithIngredients("rec-v3-keep")
            assertNotNull(existingRecipe)
            assertEquals("Сохранённый v3 рецепт", existingRecipe.recipe.title)

            // New v4 tables support CRUD and reactive Flow queries
            val favoriteProductDao = migratedDb.favoriteProductDao()
            favoriteProductDao.upsert(
                FavoriteProductEntity(
                    id = "prod-1",
                    productId = "prod-1",
                    name = "Гречка ядрица",
                    category = "Крупы",
                    calories = 313.0,
                    proteins = 12.6,
                    fats = 3.3,
                    carbs = 57.1,
                    priceAmountMinor = 11_900L,
                    savedAt = 1700000005000L,
                ),
            )
            assertTrue(favoriteProductDao.existsById("prod-1"))
            favoriteProductDao.isFavorite("prod-1").test {
                assertTrue(awaitItem())
                favoriteProductDao.deleteById("prod-1")
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            val savedRecipeDao = migratedDb.savedRecipeDao()
            savedRecipeDao.upsert(
                SavedRecipeEntity(
                    id = "saved-1",
                    planId = "plan-77",
                    title = "Белковый завтрак",
                    mealType = "BREAKFAST",
                    calories = 420.0,
                    proteins = 32.0,
                    fats = 14.0,
                    carbs = 40.0,
                    costMinor = 14_500L,
                    ingredientsJson = "[]",
                    instructions = "Смешать и запечь.",
                    savedAt = 1700000010000L,
                ),
            )
            savedRecipeDao.observeAll().test {
                val list = awaitItem()
                assertEquals(1, list.size)
                assertEquals("Белковый завтрак", list.first().title)
                assertEquals("plan-77", list.first().planId)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            migratedDb.close()
        }
    }

    @Test
    fun migrate2To4_runsSequentialMigrationsWithoutLoss() = runTest {
        val dbFile = tempFolder.newFile("migration_2_4_test.db")
        val driver = AndroidSQLiteDriver()

        val v2Connection = driver.open(dbFile.absolutePath)
        try {
            seedV2Tables(v2Connection)
            v2Connection.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            )
            v2Connection.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ec599cc93998be12dd38642c4db69ade')",
            )
            v2Connection.execSQL(
                """
                INSERT INTO `meal_records` (
                    `id`, `idempotency_key`, `sync_status`, `meal_type`, `recorded_at_epoch`,
                    `source_type`, `source_id`, `name_snapshot`, `weight_grams`,
                    `calories`, `proteins`, `fats`, `carbs`, `cost_amount_minor`, `cost_currency`
                ) VALUES (
                    'meal-v2-keep', 'idem-v2-keep', 'PENDING', 'DINNER', 1700000000000,
                    'PRODUCT', 'prod-v2', 'Творог', 180.0,
                    218.0, 31.0, 9.0, 3.0, 7500, 'RUB'
                )
                """.trimIndent(),
            )
            v2Connection.execSQL("PRAGMA user_version = 2")
        } finally {
            v2Connection.close()
        }

        val context = RuntimeEnvironment.getApplication()
        val migratedDb = Room.databaseBuilder<AppDatabase>(
            context = context,
            name = dbFile.absolutePath,
        )
            .setDriver(driver)
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4,
            )
            .build()

        try {
            val meal = migratedDb.mealRecordDao().getById("meal-v2-keep")
            assertNotNull(meal)
            assertEquals("PENDING", meal.syncStatus)
            assertEquals("idem-v2-keep", meal.idempotencyKey)

            migratedDb.favoriteProductDao().upsert(
                FavoriteProductEntity(
                    id = "fav-v4",
                    productId = "fav-v4",
                    name = "Кефир",
                    category = "Молочные",
                    calories = 40.0,
                    proteins = 3.0,
                    fats = 1.0,
                    carbs = 4.0,
                    priceAmountMinor = null,
                    savedAt = 1700000020000L,
                ),
            )
            assertTrue(migratedDb.favoriteProductDao().existsById("fav-v4"))
        } finally {
            migratedDb.close()
        }
    }

    private fun seedV2Tables(connection: androidx.sqlite.SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `profiles` (
                `userId` TEXT NOT NULL,
                `profileId` TEXT NOT NULL,
                `heightCm` INTEGER NOT NULL,
                `weightKg` REAL NOT NULL,
                `gender` TEXT NOT NULL,
                `activityLevel` TEXT NOT NULL,
                `goalType` TEXT NOT NULL,
                `targetCalories` INTEGER NOT NULL,
                `targetProteinsGrams` REAL NOT NULL,
                `targetFatsGrams` REAL NOT NULL,
                `targetCarbsGrams` REAL NOT NULL,
                `targetWeightKg` REAL NOT NULL,
                `weeklyBudget` REAL NOT NULL,
                `updatedAtEpochMs` INTEGER NOT NULL,
                PRIMARY KEY(`userId`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `meal_records` (
                `id` TEXT NOT NULL,
                `idempotency_key` TEXT NOT NULL,
                `sync_status` TEXT NOT NULL,
                `meal_type` TEXT NOT NULL,
                `recorded_at_epoch` INTEGER NOT NULL,
                `source_type` TEXT NOT NULL,
                `source_id` TEXT,
                `name_snapshot` TEXT NOT NULL,
                `weight_grams` REAL NOT NULL,
                `calories` REAL NOT NULL,
                `proteins` REAL NOT NULL,
                `fats` REAL NOT NULL,
                `carbs` REAL NOT NULL,
                `cost_amount_minor` INTEGER,
                `cost_currency` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}
