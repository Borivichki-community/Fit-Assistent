package dev.andre.nutritionist.data.repository

import app.cash.turbine.test
import dev.andre.nutritionist.data.local.dao.ProfileDao
import dev.andre.nutritionist.data.local.entity.ProfileEntity
import dev.andre.nutritionist.data.mapper.ProfileEntityMapper.toEntity
import dev.andre.nutritionist.data.remote.ProfileRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = object : CoroutineDispatchers {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private val testUserId = UserId("user-123")
    private val testProfile = Profile(
        id = ProfileId("prof-1"),
        userId = testUserId,
        heightCm = 175,
        weightKg = 70.0,
        gender = Gender.MALE,
        activityLevel = ActivityLevel.MODERATE,
        goalType = GoalType.MAINTAIN,
        nutritionGoal = NutritionGoal(
            targetCalories = 2200,
            targetProteinsGrams = 140.0,
            targetFatsGrams = 70.0,
            targetCarbsGrams = 250.0,
            targetWeightKg = 70.0,
        ),
        weeklyBudget = 4500.0,
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    private lateinit var fakeDao: FakeProfileDao
    private lateinit var fakeRemoteDataSource: FakeProfileRemoteDataSource
    private lateinit var repository: ProfileRepositoryImpl

    @BeforeTest
    fun setUp() {
        fakeDao = FakeProfileDao()
        fakeRemoteDataSource = FakeProfileRemoteDataSource()
        repository = ProfileRepositoryImpl(
            dao = fakeDao,
            remoteDataSource = fakeRemoteDataSource,
            dispatchers = testDispatchers,
        )
    }

    @Test
    fun `observeProfile emits data from DAO when refreshProfile succeeds`() = runTest(testDispatcher) {
        fakeRemoteDataSource.profileToReturn = testProfile

        repository.observeProfile(testUserId).test {
            // Начальное состояние: в БД пусто
            assertNull(awaitItem())

            // Вызываем фоновое обновление из сети
            val refreshResult = repository.refreshProfile(testUserId)
            assertTrue(refreshResult.isSuccess)

            // Turbine фиксирует новую эмиссию, вызванную записью в DAO
            val updated = awaitItem()
            assertEquals(testProfile, updated)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshProfile failure preserves cached data in DAO`() = runTest(testDispatcher) {
        // Предзаполняем базу кэшированными данными
        fakeDao.insertOrReplace(testProfile.toEntity())
        fakeRemoteDataSource.errorToThrow = RuntimeException("Сетевой сбой 503")

        repository.observeProfile(testUserId).test {
            // Должен мгновенно вернуть существующий кэш
            assertEquals(testProfile, awaitItem())

            // Сетевой refresh завершается ошибкой
            val refreshResult = repository.refreshProfile(testUserId)
            assertTrue(refreshResult.isFailure)
            assertEquals("Сетевой сбой 503", refreshResult.exceptionOrNull()?.message)

            // Данные в DAO не пострадали
            assertEquals(testProfile, repository.getProfile(testUserId))

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateProfile updates both remote source and local DAO`() = runTest(testDispatcher) {
        fakeDao.insertOrReplace(testProfile.toEntity())
        val updatedProfile = testProfile.copy(heightCm = 180, weightKg = 74.0)
        fakeRemoteDataSource.profileToReturn = updatedProfile

        repository.observeProfile(testUserId).test {
            assertEquals(testProfile, awaitItem())

            val updateResult = repository.updateProfile(updatedProfile)
            assertTrue(updateResult.isSuccess)
            assertEquals(updatedProfile, updateResult.getOrNull())

            // Проверяем, что удалённый источник принял профиль
            assertEquals(updatedProfile, fakeRemoteDataSource.lastUpdatedProfile)

            // Проверяем, что DAO автоматически оповестил Flow о новом состоянии
            assertEquals(updatedProfile, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getProfile returns cached profile without network call`() = runTest(testDispatcher) {
        fakeDao.insertOrReplace(testProfile.toEntity())

        val result = repository.getProfile(testUserId)

        assertEquals(testProfile, result)
        assertNull(fakeRemoteDataSource.lastRequestedUserId)
    }

    @Test
    fun `getProfile falls back to remote and saves to DAO when cache is empty`() = runTest(testDispatcher) {
        fakeRemoteDataSource.profileToReturn = testProfile

        val result = repository.getProfile(testUserId)

        assertEquals(testProfile, result)
        assertEquals(testUserId, fakeRemoteDataSource.lastRequestedUserId)
        assertEquals(testProfile, repository.getProfile(testUserId))
    }

    private class FakeProfileDao : ProfileDao {
        val entities = MutableStateFlow<Map<String, ProfileEntity>>(emptyMap())

        override suspend fun getProfile(userId: String): ProfileEntity? =
            entities.value[userId]

        override fun observeProfile(userId: String): Flow<ProfileEntity?> =
            entities.map { it[userId] }

        override suspend fun insertOrReplace(entity: ProfileEntity) {
            entities.update { current ->
                current + (entity.userId to entity)
            }
        }

        override suspend fun clear(userId: String) {
            entities.update { current ->
                current - userId
            }
        }
    }

    private class FakeProfileRemoteDataSource : ProfileRemoteDataSource {
        var profileToReturn: Profile? = null
        var errorToThrow: Throwable? = null
        var lastRequestedUserId: UserId? = null
        var lastUpdatedProfile: Profile? = null

        override suspend fun getProfile(userId: UserId): Result<Profile> {
            lastRequestedUserId = userId
            errorToThrow?.let { return Result.failure(it) }
            val profile = profileToReturn ?: return Result.failure(NoSuchElementException("No profile"))
            return Result.success(profile)
        }

        override suspend fun updateProfile(profile: Profile): Result<Profile> {
            lastUpdatedProfile = profile
            errorToThrow?.let { return Result.failure(it) }
            val returnProfile = profileToReturn ?: profile
            return Result.success(returnProfile)
        }
    }
}