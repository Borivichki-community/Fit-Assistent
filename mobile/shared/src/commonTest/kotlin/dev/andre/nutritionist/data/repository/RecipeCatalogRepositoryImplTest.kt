package dev.andre.nutritionist.data.repository

import app.cash.turbine.test
import dev.andre.nutritionist.data.mapper.RecipeEntityMapper.toEntity
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.PagedResult
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeAuthorType
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.model.RecipeIngredientId
import dev.andre.nutritionist.domain.model.RecipeSearchCriteria
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.fake.FakeLocalRecipeDao
import dev.andre.nutritionist.fake.FakeRecipeRemoteDataSource
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeCatalogRepositoryImplTest {

    private lateinit var localDao: FakeLocalRecipeDao
    private lateinit var remoteDataSource: FakeRecipeRemoteDataSource
    private lateinit var dispatchers: TestCoroutineDispatchers
    private lateinit var repository: RecipeCatalogRepositoryImpl

    @BeforeTest
    fun setUp() {
        localDao = FakeLocalRecipeDao()
        remoteDataSource = FakeRecipeRemoteDataSource()
        dispatchers = TestCoroutineDispatchers()
        repository = RecipeCatalogRepositoryImpl(
            localRecipeDao = localDao,
            remoteDataSource = remoteDataSource,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `observeMyRecipes emits Room updates reactively and filters out SYSTEM recipes`() = runTest {
        repository.observeMyRecipes().test {
            assertTrue(awaitItem().isEmpty())

            val systemRecipe = createRecipe(id = "sys-1", title = "Системный салат", authorType = RecipeAuthorType.SYSTEM)
            localDao.upsertRecipeWithIngredients(
                systemRecipe.toEntity(syncStatus = SyncStatus.SYNCED),
                systemRecipe.ingredients.map { it.toEntity("sys-1") },
            )
            // SYSTEM recipe is filtered out by observeMyRecipes query; Flow emits empty list or nothing new

            val userRecipe = createRecipe(id = "user-1", title = "Овсянка с ягодами", authorType = RecipeAuthorType.USER)
            localDao.upsertRecipeWithIngredients(
                userRecipe.toEntity(syncStatus = SyncStatus.PENDING),
                userRecipe.ingredients.map { it.toEntity("user-1") },
            )

            val latest = expectMostRecentItem()
            assertEquals(1, latest.size)
            assertEquals("user-1", latest.first().id.raw)
            assertEquals("Овсянка с ягодами", latest.first().title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveRecipe online inserts PENDING then updates Room to SYNCED and replaces temp id`() = runTest {
        val localRecipe = createRecipe(id = "local-temp-1", title = "Паста карбонара", version = 0L)
        val serverRecipe = localRecipe.copy(
            id = RecipeId("server-uuid-1"),
            version = 1L,
        )
        remoteDataSource.createRecipeHandler = { _, _ -> serverRecipe }

        val result = repository.saveRecipe(localRecipe)

        assertTrue(result.isSuccess)
        assertEquals(RecipeId("server-uuid-1"), result.getOrThrow().id)
        assertEquals(1, remoteDataSource.createRecipeKeys.size)

        assertNull(localDao.getRawById("local-temp-1"))
        val savedInRoom = localDao.getRawById("server-uuid-1")
        assertNotNull(savedInRoom)
        assertEquals("SYNCED", savedInRoom.syncStatus)
        assertEquals(remoteDataSource.createRecipeKeys.first(), savedInRoom.idempotencyKey)
    }

    @Test
    fun `saveRecipe offline preserves PENDING status and reuses idempotencyKey on subsequent sync`() = runTest {
        remoteDataSource.shouldFail = DomainError.NetworkUnavailable("No connection")
        val recipe = createRecipe(id = "offline-rec-1", title = "Омлет с сыром", version = 0L)

        val offlineResult = repository.saveRecipe(recipe)
        assertTrue(offlineResult.isSuccess)

        val pendingEntity = localDao.getRawById("offline-rec-1")
        assertNotNull(pendingEntity)
        assertEquals("PENDING", pendingEntity.syncStatus)
        val savedKey = pendingEntity.idempotencyKey

        // Retry after network comes back
        remoteDataSource.shouldFail = null
        remoteDataSource.createRecipeHandler = { r, _ -> r.copy(version = 1L) }

        val retryResult = repository.saveRecipe(recipe)
        assertTrue(retryResult.isSuccess)

        assertEquals(2, remoteDataSource.createRecipeKeys.size)
        assertEquals(savedKey, remoteDataSource.createRecipeKeys[0])
        assertEquals(savedKey, remoteDataSource.createRecipeKeys[1])

        val syncedEntity = localDao.getRawById("offline-rec-1")
        assertNotNull(syncedEntity)
        assertEquals("SYNCED", syncedEntity.syncStatus)
    }

    @Test
    fun `saveRecipe with positive version calls updateRecipe instead of createRecipe`() = runTest {
        val existing = createRecipe(id = "srv-rec-2", title = "Курица терияки", version = 3L)
        remoteDataSource.updateRecipeHandler = { r, _ -> r.copy(version = 4L) }

        val result = repository.saveRecipe(existing)

        assertTrue(result.isSuccess)
        assertEquals(4L, result.getOrThrow().version)
        assertTrue(remoteDataSource.createRecipeKeys.isEmpty())
        assertEquals(1, remoteDataSource.updateRecipeKeys.size)
        assertEquals("SYNCED", localDao.getRawById("srv-rec-2")?.syncStatus)
    }

    @Test
    fun `searchPublicRecipes caches public items without overwriting PENDING local edits`() = runTest {
        val pendingLocal = createRecipe(
            id = "rec-shared",
            title = "Локальная правка",
            authorType = RecipeAuthorType.USER,
            version = 1L,
        )
        localDao.upsertRecipeWithIngredients(
            pendingLocal.toEntity(syncStatus = SyncStatus.PENDING),
            pendingLocal.ingredients.map { it.toEntity("rec-shared") },
        )

        val publicCopy = pendingLocal.copy(title = "Версия с сервера")
        val freshPublic = createRecipe(
            id = "rec-public-2",
            title = "Борщ классический",
            authorType = RecipeAuthorType.SYSTEM,
            visibility = ContentVisibility.PUBLIC,
            version = 1L,
        )
        remoteDataSource.searchRecipesResult = PagedResult(
            items = listOf(publicCopy, freshPublic),
            nextPageToken = "cursor-2",
            totalCount = 2,
        )

        val result = repository.searchPublicRecipes(RecipeSearchCriteria(query = "борщ"))

        assertTrue(result.isSuccess)
        assertEquals("cursor-2", result.getOrThrow().nextPageToken)

        // PENDING edit preserved
        assertEquals("Локальная правка", localDao.getRawById("rec-shared")?.title)
        assertEquals("PENDING", localDao.getRawById("rec-shared")?.syncStatus)

        // New public recipe cached as SYNCED
        val cachedPublic = localDao.getRecipeWithIngredients("rec-public-2")
        assertNotNull(cachedPublic)
        assertEquals("SYNCED", cachedPublic.recipe.syncStatus)
        assertEquals("Борщ классический", cachedPublic.recipe.title)
    }

    @Test
    fun `forkRecipe persists forked recipe in Room as SYNCED`() = runTest {
        val forked = createRecipe(
            id = "fork-100",
            title = "Мой борщ",
            authorType = RecipeAuthorType.USER,
            originRecipeId = RecipeId("sys-borsch"),
            version = 1L,
        )
        remoteDataSource.forkRecipeHandler = { _, _ -> forked }

        val result = repository.forkRecipe(RecipeId("sys-borsch"), newTitle = "Мой борщ")

        assertTrue(result.isSuccess)
        assertEquals(forked, result.getOrThrow())

        val inRoom = localDao.getRecipeWithIngredients("fork-100")
        assertNotNull(inRoom)
        assertEquals("SYNCED", inRoom.recipe.syncStatus)
        assertEquals("sys-borsch", inRoom.recipe.originRecipeId)
    }

    @Test
    fun `deleteRecipe removes recipe from observeMyRecipes and deletes remotely`() = runTest {
        val recipe = createRecipe(id = "del-1", title = "Котлеты")
        localDao.upsertRecipeWithIngredients(
            recipe.toEntity(syncStatus = SyncStatus.SYNCED),
            recipe.ingredients.map { it.toEntity("del-1") },
        )

        val result = repository.deleteRecipe(RecipeId("del-1"))

        assertTrue(result.isSuccess)
        assertEquals(1, remoteDataSource.deleteRecipeCalls.size)
        assertNull(localDao.getRawById("del-1"))
    }

    private fun createRecipe(
        id: String,
        title: String,
        authorType: RecipeAuthorType = RecipeAuthorType.USER,
        visibility: ContentVisibility = ContentVisibility.PRIVATE,
        originRecipeId: RecipeId? = null,
        version: Long = 0L,
    ) = Recipe(
        id = RecipeId(id),
        title = title,
        authorType = authorType,
        authorId = UserId("user-42"),
        originRecipeId = originRecipeId,
        description = "Описание",
        instructions = "Приготовление",
        visibility = visibility,
        servingsCount = 2,
        totalWeightGrams = 300.0,
        nutrition = NutritionFacts(calories = 150.0, proteins = 12.0, fats = 5.0, carbs = 14.0),
        ingredients = listOf(
            RecipeIngredient(
                id = RecipeIngredientId("ing-$id-1"),
                productId = ProductId("prod-1"),
                name = "Ингредиент 1",
                weightGrams = 300.0,
                nutritionPer100g = NutritionFacts(150.0, 12.0, 5.0, 14.0),
            ),
        ),
        version = version,
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )
}
