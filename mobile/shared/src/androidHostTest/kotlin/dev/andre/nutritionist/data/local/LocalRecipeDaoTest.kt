package dev.andre.nutritionist.data.local

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import app.cash.turbine.test
import dev.andre.nutritionist.data.local.dao.LocalRecipeDao
import dev.andre.nutritionist.data.local.entity.LocalRecipeEntity
import dev.andre.nutritionist.data.local.entity.LocalRecipeIngredientEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocalRecipeDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: LocalRecipeDao

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder<AppDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dao = database.localRecipeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertRecipeWithIngredients_loadsRecipeAndAllIngredients() = runTest {
        val recipe = createRecipeEntity(id = "rec-1", title = "Гречка с курицей")
        val ingredients = listOf(
            createIngredientEntity(id = "ing-1", recipeId = "rec-1", name = "Гречка", weight = 100.0),
            createIngredientEntity(id = "ing-2", recipeId = "rec-1", name = "Куриное филе", weight = 200.0),
        )

        dao.upsertRecipeWithIngredients(recipe, ingredients)

        val loaded = dao.getRecipeWithIngredients("rec-1")
        assertNotNull(loaded)
        assertEquals("Гречка с курицей", loaded.recipe.title)
        assertEquals(2, loaded.ingredients.size)
        assertTrue(loaded.ingredients.any { it.productName == "Гречка" })
        assertTrue(loaded.ingredients.any { it.productName == "Куриное филе" })
    }

    @Test
    fun observeMyRecipes_filtersSystemAndDeletedAndEmitsReactively() = runTest {
        dao.observeMyRecipes().test {
            assertTrue(awaitItem().isEmpty())

            val systemRecipe = createRecipeEntity(
                id = "sys-1",
                title = "Системный рецепт",
                authorType = "SYSTEM",
                updatedAt = 1_000L,
            )
            dao.upsertRecipeWithIngredients(systemRecipe, emptyList())

            val userRecipe = createRecipeEntity(
                id = "user-1",
                title = "Мой завтрак",
                authorType = "USER",
                updatedAt = 2_000L,
            )
            dao.upsertRecipeWithIngredients(userRecipe, emptyList())

            // mutable by design: skipping empty emissions from filtered SYSTEM insert
            var items = awaitItem()
            while (items.isEmpty()) {
                items = awaitItem()
            }
            assertEquals(1, items.size)
            assertEquals("user-1", items.first().recipe.id)

            dao.markDeleted(id = "user-1", status = "PENDING", updatedAt = 3_000L)
            val afterDelete = awaitItem()
            assertTrue(afterDelete.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun hardDeleteById_cascadesDeleteToIngredients() = runTest {
        val recipe = createRecipeEntity(id = "rec-cascade", title = "Салат")
        val ingredient = createIngredientEntity(id = "ing-cascade", recipeId = "rec-cascade", name = "Огурец", weight = 150.0)
        dao.upsertRecipeWithIngredients(recipe, listOf(ingredient))

        assertNotNull(dao.getRecipeWithIngredients("rec-cascade"))

        dao.hardDeleteById("rec-cascade")

        assertNull(dao.getRecipeWithIngredients("rec-cascade"))
        assertNull(dao.getRawById("rec-cascade"))
    }

    @Test
    fun getPendingRecipes_returnsOnlyActivePendingRecipes() = runTest {
        val pending = createRecipeEntity(id = "p-1", title = "Pending", syncStatus = "PENDING")
        val synced = createRecipeEntity(id = "s-1", title = "Synced", syncStatus = "SYNCED")
        val deletedPending = createRecipeEntity(id = "d-1", title = "Deleted", syncStatus = "PENDING", isDeleted = true)

        dao.upsertRecipeWithIngredients(pending, emptyList())
        dao.upsertRecipeWithIngredients(synced, emptyList())
        dao.upsertRecipeWithIngredients(deletedPending, emptyList())

        val pendingList = dao.getPendingRecipes()
        assertEquals(1, pendingList.size)
        assertEquals("p-1", pendingList.first().recipe.id)
    }

    private fun createRecipeEntity(
        id: String,
        title: String,
        authorType: String = "USER",
        syncStatus: String = "PENDING",
        isDeleted: Boolean = false,
        updatedAt: Long = 1_700_000_000_000L,
    ) = LocalRecipeEntity(
        id = id,
        authorType = authorType,
        authorId = "user-1",
        originRecipeId = null,
        title = title,
        description = "Описание",
        instructions = "Шаги",
        visibility = "PRIVATE",
        servingsCount = 2,
        totalWeightGrams = 300.0,
        caloriesPer100g = 150.0,
        proteinsPer100g = 15.0,
        fatsPer100g = 5.0,
        carbsPer100g = 12.0,
        syncStatus = syncStatus,
        idempotencyKey = "idem-$id",
        isDeleted = isDeleted,
        updatedAtEpochMs = updatedAt,
    )

    private fun createIngredientEntity(
        id: String,
        recipeId: String,
        name: String,
        weight: Double,
    ) = LocalRecipeIngredientEntity(
        id = id,
        recipeId = recipeId,
        productId = "prod-$id",
        productName = name,
        weightGrams = weight,
        calories = 120.0,
        proteins = 10.0,
        fats = 2.0,
        carbs = 15.0,
    )
}
