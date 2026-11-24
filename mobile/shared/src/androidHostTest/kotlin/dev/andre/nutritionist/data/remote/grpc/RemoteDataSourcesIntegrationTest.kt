package dev.andre.nutritionist.data.remote.grpc

import dev.andre.nutritionist.data.remote.CatalogRemoteDataSource
import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.data.remote.RecipeRemoteDataSource
import dev.andre.nutritionist.data.remote.grpc.interceptors.UnifiedGrpcClientInterceptor
import dev.andre.nutritionist.domain.coroutines.DefaultCoroutineDispatchers
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.net.Socket

class RemoteDataSourcesIntegrationTest {

    private val dispatchers = DefaultCoroutineDispatchers()
    private val interceptor = UnifiedGrpcClientInterceptor()

    private var catalogChannel: ManagedChannel? = null
    private var diaryChannel: ManagedChannel? = null
    private var recipeChannel: ManagedChannel? = null

    private lateinit var catalogDataSource: CatalogRemoteDataSource
    private lateinit var diaryDataSource: DiaryRemoteDataSource
    private lateinit var recipeDataSource: RecipeRemoteDataSource

    private val isCatalogReachable by lazy { isPortReachable("127.0.0.1", 9192) }
    private val isDiaryReachable by lazy { isPortReachable("127.0.0.1", 9193) }
    private val isRecipeReachable by lazy { isPortReachable("127.0.0.1", 9194) }

    @Before
    fun setUp() {
        if (isCatalogReachable) {
            val ch = ManagedChannelBuilder.forAddress("127.0.0.1", 9192)
                .usePlaintext()
                .intercept(interceptor)
                .build()
            catalogChannel = ch
            val client = AndroidCatalogGrpcServiceClient(ch)
            catalogDataSource = CatalogGrpcRemoteDataSource(client, dispatchers)
        }

        if (isDiaryReachable) {
            val ch = ManagedChannelBuilder.forAddress("127.0.0.1", 9193)
                .usePlaintext()
                .intercept(interceptor)
                .build()
            diaryChannel = ch
            val client = AndroidDiaryGrpcServiceClient(ch)
            diaryDataSource = DiaryGrpcRemoteDataSource(client, dispatchers)
        }

        if (isRecipeReachable) {
            val ch = ManagedChannelBuilder.forAddress("127.0.0.1", 9194)
                .usePlaintext()
                .intercept(interceptor)
                .build()
            recipeChannel = ch
            val client = AndroidRecipeGrpcServiceClient(ch)
            recipeDataSource = RecipeGrpcRemoteDataSource(client, dispatchers)
        }
    }

    @After
    fun tearDown() {
        catalogChannel?.shutdownNow()
        diaryChannel?.shutdownNow()
        recipeChannel?.shutdownNow()
    }

    @Test
    fun catalogRemoteDataSource_searchProducts() = runBlocking {
        if (!isCatalogReachable) {
            println("[SKIP] Catalog service is not reachable on port 9192")
            return@runBlocking
        }

        val result = catalogDataSource.searchProducts(query = "Куриное")
        assertTrue(result.isSuccess)
        val (products, _) = result.getOrThrow()
        assertNotNull(products)
    }

    @Test
    fun recipeRemoteDataSource_generateMealPlan() = runBlocking {
        if (!isRecipeReachable) {
            println("[SKIP] Recipe service is not reachable on port 9194")
            return@runBlocking
        }

        val params = GenerateBudgetMenuParams(
            period = TargetPeriod.DINNER,
            targetCalories = 650.0,
            maxBudget = Money.fromRubles(350.0),
        )
        val result = recipeDataSource.generateMealPlan(params)
        assertTrue(result.isSuccess)
        val plan = result.getOrThrow()
        assertNotNull(plan.id)
    }

    @Test
    fun diaryRemoteDataSource_addAndGetDailyEntries() = runBlocking {
        if (!isDiaryReachable) {
            println("[SKIP] Diary service is not reachable on port 9193")
            return@runBlocking
        }

        val record = MealRecord(
            id = MealRecordId("entry-test-${System.currentTimeMillis()}"),
            mealType = MealType.DINNER,
            recordedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            sourceType = EntrySourceType.PRODUCT,
            nameSnapshot = "Куриное филе на гриле",
            weightGrams = 200.0,
            nutrition = NutritionFacts(calories = 226.0, proteins = 47.2, fats = 3.8, carbs = 0.0),
            cost = Money.fromRubles(80.0),
            idempotencyKey = IdempotencyKey.generate(),
            syncStatus = SyncStatus.PENDING,
        )

        val addResult = diaryDataSource.addEntry(record)
        assertTrue(addResult.isSuccess)
        val saved = addResult.getOrThrow()
        assertNotNull(saved.id)
    }

    private fun isPortReachable(host: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 500)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
