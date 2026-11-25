package dev.andre.nutritionist.data.local

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import dev.andre.nutritionist.data.local.entity.LocalMealPlanEntity
import dev.andre.nutritionist.data.local.entity.LocalMealPlanItemEntity
import dev.andre.nutritionist.data.local.entity.LocalRecipeEntity
import dev.andre.nutritionist.data.local.entity.LocalRecipeIngredientEntity
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
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecipeDatabaseMigrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun migrate2To3_preservesExistingV2DataAndEnablesV3Tables() = runTest {
        val dbFile = tempFolder.newFile("migration_2_3_test.db")
        val driver = AndroidSQLiteDriver()

        // 1. Seed a genuine v2 SQLite database using exact v2 DDL from schema 2.json
        val v2Connection = driver.open(dbFile.absolutePath)
        try {
            v2Connection.execSQL(
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
            v2Connection.execSQL(
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
            v2Connection.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            )
            v2Connection.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ec599cc93998be12dd38642c4db69ade')",
            )
            v2Connection.execSQL(
                """
                INSERT INTO `profiles` (
                    `userId`, `profileId`, `heightCm`, `weightKg`, `gender`, `activityLevel`,
                    `goalType`, `targetCalories`, `targetProteinsGrams`, `targetFatsGrams`,
                    `targetCarbsGrams`, `targetWeightKg`, `weeklyBudget`, `updatedAtEpochMs`
                ) VALUES (
                    'user-v2', 'prof-v2', 180, 78.5, 'MALE', 'MODERATE',
                    'MAINTAIN', 2400, 150.0, 75.0, 280.0, 78.0, 5000.0, 1700000000000
                )
                """.trimIndent(),
            )
            v2Connection.execSQL(
                """
                INSERT INTO `meal_records` (
                    `id`, `idempotency_key`, `sync_status`, `meal_type`, `recorded_at_epoch`,
                    `source_type`, `source_id`, `name_snapshot`, `weight_grams`,
                    `calories`, `proteins`, `fats`, `carbs`, `cost_amount_minor`, `cost_currency`
                ) VALUES (
                    'meal-v2', 'idem-v2', 'SYNCED', 'BREAKFAST', 1700000000000,
                    'PRODUCT', 'prod-v2', 'Овсянка', 150.0,
                    210.0, 7.0, 4.0, 36.0, 3500, 'RUB'
                )
                """.trimIndent(),
            )
            v2Connection.execSQL("PRAGMA user_version = 2")
        } finally {
            v2Connection.close()
        }

        // 2. Open with Room v3 + explicit MIGRATION_2_3 (no destructive fallback)
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
            // 3. Assert v2 data survived intact (and Room schema validation passed!)
            val migratedProfile = migratedDb.profileDao().getProfile("user-v2")
            assertNotNull(migratedProfile)
            assertEquals("prof-v2", migratedProfile.profileId)
            assertEquals(2400, migratedProfile.targetCalories)

            val migratedMeal = migratedDb.mealRecordDao().getById("meal-v2")
            assertNotNull(migratedMeal)
            assertEquals("Овсянка", migratedMeal.nameSnapshot)
            assertEquals("SYNCED", migratedMeal.syncStatus)

            // 4. Assert new v3 tables accept inserts & relational queries cleanly
            val recipeDao = migratedDb.localRecipeDao()
            recipeDao.upsertRecipeWithIngredients(
                recipe = LocalRecipeEntity(
                    id = "rec-v3",
                    authorType = "USER",
                    authorId = "user-v2",
                    title = "Новый рецепт v3",
                    visibility = "PRIVATE",
                    idempotencyKey = "idem-rec-v3",
                    updatedAtEpochMs = 1700000001000L,
                ),
                ingredients = listOf(
                    LocalRecipeIngredientEntity(
                        id = "ing-v3",
                        recipeId = "rec-v3",
                        productId = "prod-1",
                        productName = "Творог",
                        weightGrams = 200.0,
                        calories = 120.0,
                        proteins = 18.0,
                        fats = 2.0,
                        carbs = 3.0,
                    ),
                ),
            )
            val loadedRecipe = recipeDao.getRecipeWithIngredients("rec-v3")
            assertNotNull(loadedRecipe)
            assertEquals(1, loadedRecipe.ingredients.size)

            val mealPlanDao = migratedDb.localMealPlanDao()
            mealPlanDao.upsertPlanWithItems(
                plan = LocalMealPlanEntity(
                    id = "plan-v3",
                    authorId = "coach-1",
                    authorType = "COACH",
                    title = "План v3",
                    visibility = "PUBLIC",
                    accessMode = "FREE",
                ),
                items = listOf(
                    LocalMealPlanItemEntity(
                        id = "item-v3",
                        planId = "plan-v3",
                        dayNumber = 1,
                        mealSlot = "BREAKFAST",
                        itemType = "RECIPE",
                        itemId = "rec-v3",
                        nameSnapshot = "Новый рецепт v3",
                        weightGrams = 200.0,
                        calories = 240.0,
                        proteins = 36.0,
                        fats = 4.0,
                        carbs = 6.0,
                    ),
                ),
            )
            val loadedPlan = mealPlanDao.getPlanWithItems("plan-v3")
            assertNotNull(loadedPlan)
            assertEquals(1, loadedPlan.items.size)
        } finally {
            migratedDb.close()
        }
    }
}
