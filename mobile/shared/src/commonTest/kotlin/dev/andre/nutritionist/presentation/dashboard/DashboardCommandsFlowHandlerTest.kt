package dev.andre.nutritionist.presentation.dashboard

import app.cash.turbine.test
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.DailyDiary
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.FoodItemId
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.ProfileRepository
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

class DashboardCommandsFlowHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = object : CoroutineDispatchers {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var diaryRepository: FakeDiaryRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var handler: DashboardCommandsFlowHandler

    private val testUserId = UserId("user-test-1")
    private val testDate = LocalDate(2026, 3, 15)

    private val sampleEntry = DiaryEntry(
        id = DiaryEntryId("entry-1"),
        userId = testUserId,
        foodItemId = FoodItemId("food-1"),
        foodName = "Гречка",
        portionGrams = 150.0,
        calories = 180,
        proteins = 6.0,
        fats = 2.0,
        carbs = 35.0,
        mealType = MealType.LUNCH,
        date = testDate,
        recordedAt = Instant.fromEpochMilliseconds(0),
        idempotencyKey = IdempotencyKey("idem-1"),
    )

    @BeforeTest
    fun setUp() {
        diaryRepository = FakeDiaryRepository()
        profileRepository = FakeProfileRepository()
        handler = DashboardCommandsFlowHandler(
            diaryRepository = diaryRepository,
            profileRepository = profileRepository,
            dispatchers = testDispatchers,
        )
    }

    @Test
    fun `Load command emits Loaded on repository success`() = runTest(testDispatcher) {
        val diary = DailyDiary(userId = testUserId, date = testDate, entries = listOf(sampleEntry))
        diaryRepository.diaryToReturn = diary

        handler.handle(flowOf(DashboardCommand.Load(testUserId, testDate))).test {
            val event = awaitItem()
            assertIs<DashboardEvent.Internal.Loaded>(event)
            assertEquals(diary, event.diary)
            awaitComplete()
        }
    }

    @Test
    fun `Load command emits LoadFailed when repository throws`() = runTest(testDispatcher) {
        diaryRepository.errorToThrow = RuntimeException("DB error")

        handler.handle(flowOf(DashboardCommand.Load(testUserId, testDate))).test {
            val event = awaitItem()
            assertIs<DashboardEvent.Internal.LoadFailed>(event)
            assertEquals("DB error", event.error.message)
            awaitComplete()
        }
    }

    @Test
    fun `AddMeal command emits MealAdded on success`() = runTest(testDispatcher) {
        handler.handle(flowOf(DashboardCommand.AddMeal(sampleEntry))).test {
            val event = awaitItem()
            assertIs<DashboardEvent.Internal.MealAdded>(event)
            assertEquals(sampleEntry, event.entry)
            awaitComplete()
        }
    }

    @Test
    fun `AddMeal command emits MutationFailed on repository failure`() = runTest(testDispatcher) {
        diaryRepository.errorToThrow = RuntimeException("Insert error")

        handler.handle(flowOf(DashboardCommand.AddMeal(sampleEntry))).test {
            val event = awaitItem()
            assertIs<DashboardEvent.Internal.MutationFailed>(event)
            assertEquals("Insert error", event.error.message)
            awaitComplete()
        }
    }

    @Test
    fun `DeleteMeal command emits MealDeleted on success`() = runTest(testDispatcher) {
        handler.handle(flowOf(DashboardCommand.DeleteMeal(sampleEntry.id, testUserId, testDate))).test {
            val event = awaitItem()
            assertIs<DashboardEvent.Internal.MealDeleted>(event)
            assertEquals(sampleEntry.id, event.entryId)
            awaitComplete()
        }
    }

    private class FakeDiaryRepository : DiaryRepository {
        var diaryToReturn: DailyDiary? = null
        var errorToThrow: Throwable? = null

        override suspend fun getDailyDiary(userId: UserId, date: LocalDate): Result<DailyDiary> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            val d = diaryToReturn ?: DailyDiary(userId = userId, date = date)
            return Result.success(d)
        }

        override fun observeDailyDiary(userId: UserId, date: LocalDate): Flow<DailyDiary> = flow {
            val err = errorToThrow
            if (err != null) throw err
            emit(diaryToReturn ?: DailyDiary(userId = userId, date = date))
        }

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

    private class FakeProfileRepository : ProfileRepository {
        var profileToReturn: Profile? = null

        override fun observeProfile(userId: UserId): Flow<Profile?> = flow {
            emit(profileToReturn)
        }

        override suspend fun getProfile(userId: UserId): Profile? = profileToReturn

        override suspend fun refreshProfile(userId: UserId): Result<Unit> = Result.success(Unit)

        override suspend fun updateProfile(profile: Profile): Result<Profile> = Result.success(profile)
    }
}
