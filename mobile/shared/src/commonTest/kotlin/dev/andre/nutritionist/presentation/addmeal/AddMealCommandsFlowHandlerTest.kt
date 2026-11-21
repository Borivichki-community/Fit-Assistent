package dev.andre.nutritionist.presentation.addmeal

import app.cash.turbine.test
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.DailyDiary
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.FoodItem
import dev.andre.nutritionist.domain.model.FoodItemId
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.FoodCatalogRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class AddMealCommandsFlowHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = object : CoroutineDispatchers {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var catalogRepository: FakeFoodCatalogRepository
    private lateinit var diaryRepository: FakeDiaryRepository
    private lateinit var handler: AddMealCommandsFlowHandler

    private val testUserId = UserId("user-test-1")
    private val testDate = LocalDate(2026, 3, 15)

    private val sampleFood = FoodItem(
        id = FoodItemId("food-chicken"),
        name = "Куриное филе",
        brand = "Петелинка",
        caloriesPer100g = 165,
        proteinsPer100g = 31.0,
        fatsPer100g = 3.6,
        carbsPer100g = 0.0,
        defaultPortionGrams = 150.0,
        category = "Обед",
    )

    private val sampleEntry = DiaryEntry(
        id = DiaryEntryId("entry-1"),
        userId = testUserId,
        foodItemId = sampleFood.id,
        foodName = "Куриное филе",
        portionGrams = 150.0,
        calories = 248,
        proteins = 46.5,
        fats = 5.4,
        carbs = 0.0,
        mealType = MealType.LUNCH,
        date = testDate,
        recordedAt = Instant.fromEpochMilliseconds(0),
        idempotencyKey = IdempotencyKey("idem-1"),
    )

    @BeforeTest
    fun setUp() {
        catalogRepository = FakeFoodCatalogRepository()
        diaryRepository = FakeDiaryRepository()
        handler = AddMealCommandsFlowHandler(
            foodCatalogRepository = catalogRepository,
            diaryRepository = diaryRepository,
            dispatchers = testDispatchers,
        )
    }

    @Test
    fun `SearchFoods command emits FoodsLoaded on success`() = runTest(testDispatcher) {
        catalogRepository.foodsToReturn = listOf(sampleFood)

        handler.handle(flowOf(AddMealCommand.SearchFoods(query = "курица", category = "Все"))).test {
            val event = awaitItem()
            assertIs<AddMealEvent.Internal.FoodsLoaded>(event)
            assertEquals(listOf(sampleFood), event.foods)
            awaitComplete()
        }
    }

    @Test
    fun `SearchFoods command emits SearchFailed on repository error`() = runTest(testDispatcher) {
        catalogRepository.errorToThrow = RuntimeException("Catalog error")

        handler.handle(flowOf(AddMealCommand.SearchFoods(query = "", category = "Все"))).test {
            val event = awaitItem()
            assertIs<AddMealEvent.Internal.SearchFailed>(event)
            assertEquals("Catalog error", event.error.message)
            awaitComplete()
        }
    }

    @Test
    fun `SaveDiaryEntry command emits EntrySaved on success`() = runTest(testDispatcher) {
        handler.handle(flowOf(AddMealCommand.SaveDiaryEntry(sampleEntry))).test {
            val event = awaitItem()
            assertIs<AddMealEvent.Internal.EntrySaved>(event)
            assertEquals(sampleEntry, event.entry)
            awaitComplete()
        }
    }

    @Test
    fun `SaveDiaryEntry command emits SaveFailed on repository error`() = runTest(testDispatcher) {
        diaryRepository.errorToThrow = RuntimeException("DB full")

        handler.handle(flowOf(AddMealCommand.SaveDiaryEntry(sampleEntry))).test {
            val event = awaitItem()
            assertIs<AddMealEvent.Internal.SaveFailed>(event)
            assertEquals("DB full", event.error.message)
            awaitComplete()
        }
    }

    private class FakeFoodCatalogRepository : FoodCatalogRepository {
        var foodsToReturn: List<FoodItem> = emptyList()
        var errorToThrow: Throwable? = null

        override suspend fun searchFood(query: String, category: String?): Result<List<FoodItem>> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            return Result.success(foodsToReturn)
        }

        override suspend fun getRecentFood(): Result<List<FoodItem>> = Result.success(foodsToReturn)
        override suspend fun getFavoriteFood(): Result<List<FoodItem>> = Result.success(foodsToReturn)
        override suspend fun getFoodById(id: FoodItemId): Result<FoodItem?> = Result.success(foodsToReturn.find { it.id == id })
    }

    private class FakeDiaryRepository : DiaryRepository {
        var errorToThrow: Throwable? = null

        override suspend fun getDailyDiary(userId: UserId, date: LocalDate): Result<DailyDiary> =
            Result.success(DailyDiary(userId = userId, date = date))

        override fun observeDailyDiary(userId: UserId, date: LocalDate): Flow<DailyDiary> =
            flow { emit(DailyDiary(userId = userId, date = date)) }

        override suspend fun addEntry(entry: DiaryEntry): Result<DiaryEntry> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            return Result.success(entry)
        }

        override suspend fun deleteEntry(id: DiaryEntryId): Result<Unit> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            return Result.success(Unit)
        }
    }
}
