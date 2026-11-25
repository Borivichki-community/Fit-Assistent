package dev.andre.nutritionist.domain.usecase

import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.ProfileRepository
import dev.andre.nutritionist.fake.FakeDiaryRepository
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class GetNutritionAnalyticsUseCaseTest {

    private lateinit var diaryRepository: FakeDiaryRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var useCase: GetNutritionAnalyticsUseCase

    private val currentUserId = UserId("user-self")

    @BeforeTest
    fun setUp() {
        diaryRepository = FakeDiaryRepository()
        profileRepository = FakeProfileRepository()
        useCase = GetNutritionAnalyticsUseCase(
            diaryRepository = diaryRepository,
            profileRepository = profileRepository,
            dispatchers = TestCoroutineDispatchers(),
            currentUserIdProvider = { currentUserId },
        )
    }

    @Test
    fun `calculates totals macro averages budget adherence and daily breakdown including empty days`() = runTest {
        profileRepository.profiles[currentUserId.raw] = createProfile(
            userId = currentUserId,
            targetCalories = 2000,
            targetProteins = 150.0,
            targetFats = 70.0,
            targetCarbs = 200.0,
            weeklyBudget = 7000.0, // 1000 RUB/day -> 3000 RUB for 3-day period
        )

        // Day 1: 2026-11-01 -> 1950 kcal (within +-10% of 2000 -> goal met), spent 1200 RUB
        diaryRepository.addEntry(
            createRecord(
                id = "d1-1",
                recordedAt = Instant.parse("2026-11-01T08:00:00Z"),
                calories = 800.0,
                proteins = 60.0,
                fats = 25.0,
                carbs = 80.0,
                costRubles = 500.0,
            ),
        )
        diaryRepository.addEntry(
            createRecord(
                id = "d1-2",
                recordedAt = Instant.parse("2026-11-01T14:00:00Z"),
                calories = 1150.0,
                proteins = 80.0,
                fats = 35.0,
                carbs = 120.0,
                costRubles = 700.0,
            ),
        )

        // Day 2: 2026-11-02 -> no records

        // Day 3: 2026-11-03 -> 2450 kcal (outside +-10% of 2000 -> goal NOT met), spent 2100 RUB
        diaryRepository.addEntry(
            createRecord(
                id = "d3-1",
                recordedAt = Instant.parse("2026-11-03T18:00:00Z"),
                calories = 2450.0,
                proteins = 160.0,
                fats = 80.0,
                carbs = 260.0,
                costRubles = 2100.0,
            ),
        )

        val result = useCase(
            fromDate = LocalDate(2026, 11, 1),
            toDate = LocalDate(2026, 11, 3),
            timeZone = TimeZone.UTC,
        )

        assertTrue(result.isSuccess)
        val analytics = result.getOrThrow()

        assertEquals(3, analytics.daysCount)
        assertEquals(2, analytics.activeDaysCount)
        assertEquals(4400, analytics.totalCalories)
        assertEquals(2200, analytics.averageCalories)
        assertEquals(150.0, analytics.averageProteinsGrams)
        assertEquals(70.0, analytics.averageFatsGrams)
        assertEquals(230.0, analytics.averageCarbsGrams)
        assertEquals(2000, analytics.targetCaloriesPerDay)
        assertEquals(Money.fromRubles(3300.0), analytics.totalSpent)
        assertEquals(Money.fromRubles(3000.0), analytics.budgetLimit)
        assertEquals(110, analytics.budgetUsagePercent)
        assertTrue(analytics.isBudgetExceeded)
        // Average 2200 vs target 2000 => 10% deviation => 90% adherence
        assertEquals(90, analytics.calorieAdherencePercent)

        assertEquals(3, analytics.dailyBreakdown.size)
        val day1 = analytics.dailyBreakdown[0]
        val day2 = analytics.dailyBreakdown[1]
        val day3 = analytics.dailyBreakdown[2]

        assertEquals(LocalDate(2026, 11, 1), day1.date)
        assertEquals(1950, day1.calories)
        assertEquals(Money.fromRubles(1200.0), day1.spent)
        assertTrue(day1.isCalorieGoalMet)

        assertEquals(LocalDate(2026, 11, 2), day2.date)
        assertEquals(0, day2.calories)
        assertEquals(Money.ZERO, day2.spent)
        assertFalse(day2.isCalorieGoalMet)

        assertEquals(LocalDate(2026, 11, 3), day3.date)
        assertEquals(2450, day3.calories)
        assertEquals(Money.fromRubles(2100.0), day3.spent)
        assertFalse(day3.isCalorieGoalMet)
    }

    @Test
    fun `supports coach targetUserId by querying student diary range and student profile`() = runTest {
        val studentId = UserId("student-77")
        profileRepository.profiles[currentUserId.raw] = createProfile(
            userId = currentUserId,
            targetCalories = 3000,
            weeklyBudget = 14000.0,
        )
        profileRepository.profiles[studentId.raw] = createProfile(
            userId = studentId,
            targetCalories = 1800,
            weeklyBudget = 3500.0, // 500/day -> 1000 for 2 days
        )
        diaryRepository.recordsByUser[studentId.raw] = listOf(
            createRecord(
                id = "st-1",
                recordedAt = Instant.parse("2026-11-10T12:00:00Z"),
                calories = 1800.0,
                proteins = 120.0,
                fats = 60.0,
                carbs = 190.0,
                costRubles = 400.0,
            ),
        )

        val result = useCase(
            fromDate = LocalDate(2026, 11, 10),
            toDate = LocalDate(2026, 11, 11),
            targetUserId = studentId,
            timeZone = TimeZone.UTC,
        )

        assertTrue(result.isSuccess)
        val analytics = result.getOrThrow()
        assertEquals(studentId, diaryRepository.lastGetRangeTargetUserId)
        assertEquals(studentId, profileRepository.lastRequestedUserId)
        assertEquals(1800, analytics.targetCaloriesPerDay)
        assertEquals(Money.fromRubles(1000.0), analytics.budgetLimit)
        assertEquals(Money.fromRubles(400.0), analytics.totalSpent)
        assertFalse(analytics.isBudgetExceeded)
        assertEquals(100, analytics.calorieAdherencePercent)
    }

    @Test
    fun `returns ValidationError when fromDate is after toDate`() = runTest {
        val result = useCase(
            fromDate = LocalDate(2026, 11, 10),
            toDate = LocalDate(2026, 11, 5),
            timeZone = TimeZone.UTC,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.ValidationError)
        assertEquals(0, diaryRepository.getRangeCallCount)
    }

    @Test
    fun `propagates DiaryRepository getRange failure`() = runTest {
        diaryRepository.shouldFailGetRange = DomainError.Forbidden("Access denied")

        val result = useCase(
            fromDate = LocalDate(2026, 11, 1),
            toDate = LocalDate(2026, 11, 7),
            timeZone = TimeZone.UTC,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
    }

    private fun createProfile(
        userId: UserId,
        targetCalories: Int,
        targetProteins: Double = 140.0,
        targetFats: Double = 60.0,
        targetCarbs: Double = 220.0,
        weeklyBudget: Double,
    ) = Profile(
        id = ProfileId("prof-${userId.raw}"),
        userId = userId,
        heightCm = 175,
        weightKg = 70.0,
        gender = Gender.MALE,
        activityLevel = ActivityLevel.MODERATE,
        goalType = GoalType.MAINTAIN,
        nutritionGoal = NutritionGoal(
            targetCalories = targetCalories,
            targetProteinsGrams = targetProteins,
            targetFatsGrams = targetFats,
            targetCarbsGrams = targetCarbs,
            targetWeightKg = 70.0,
        ),
        weeklyBudget = weeklyBudget,
        updatedAt = Instant.fromEpochMilliseconds(1700000000000L),
    )

    private fun createRecord(
        id: String,
        recordedAt: Instant,
        calories: Double,
        proteins: Double,
        fats: Double,
        carbs: Double,
        costRubles: Double,
    ) = MealRecord(
        id = MealRecordId(id),
        mealType = MealType.LUNCH,
        recordedAt = recordedAt,
        sourceType = EntrySourceType.PRODUCT,
        nameSnapshot = "Блюдо",
        weightGrams = 250.0,
        nutrition = NutritionFacts(
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
        ),
        cost = Money.fromRubles(costRubles),
        idempotencyKey = IdempotencyKey("idem-$id"),
        syncStatus = SyncStatus.SYNCED,
    )

    private class FakeProfileRepository : ProfileRepository {
        val profiles = mutableMapOf<String, Profile>()
        private val profilesFlow = MutableStateFlow<Map<String, Profile>>(emptyMap())
        var lastRequestedUserId: UserId? = null

        override fun observeProfile(userId: UserId): Flow<Profile?> =
            profilesFlow.map { it[userId.raw] ?: profiles[userId.raw] }

        override suspend fun getProfile(userId: UserId): Profile? {
            lastRequestedUserId = userId
            return profiles[userId.raw]
        }

        override suspend fun refreshProfile(userId: UserId): Result<Unit> = Result.success(Unit)

        override suspend fun updateProfile(profile: Profile): Result<Profile> {
            profiles[profile.userId.raw] = profile
            profilesFlow.value = profiles.toMap()
            return Result.success(profile)
        }
    }
}
