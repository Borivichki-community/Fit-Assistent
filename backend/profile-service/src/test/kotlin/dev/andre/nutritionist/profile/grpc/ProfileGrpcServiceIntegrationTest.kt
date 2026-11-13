package dev.andre.nutritionist.profile.grpc

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.ProfileServiceGrpcKt
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.profile.grpc.interceptor.GrpcExceptionInterceptor
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.test.runTest
import dev.andre.nutritionist.profile.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

class ProfileGrpcServiceIntegrationTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var profileGrpcService: ProfileGrpcService

    @Autowired
    private lateinit var grpcExceptionInterceptor: GrpcExceptionInterceptor

    @Autowired
    private lateinit var profileRepository: ProfileJpaRepository

    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var stub: ProfileServiceGrpcKt.ProfileServiceCoroutineStub

    @BeforeEach
    fun setUp() {
        profileRepository.deleteAll()
        val serverName = InProcessServerBuilder.generateName()
        server =
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(profileGrpcService, grpcExceptionInterceptor))
                .build()
                .start()

        channel =
            InProcessChannelBuilder
                .forName(serverName)
                .directExecutor()
                .build()

        stub = ProfileServiceGrpcKt.ProfileServiceCoroutineStub(channel)
    }

    @AfterEach
    fun tearDown() {
        channel.shutdownNow()
        server.shutdownNow()
    }

    private suspend inline fun <reified T : Throwable> assertThrowsSuspend(crossinline block: suspend () -> Unit): T {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) return e
            throw AssertionError("Expected exception of type ${T::class.java.name}, but caught ${e::class.java.name}", e)
        }
        throw AssertionError("Expected exception of type ${T::class.java.name}, but no exception was thrown")
    }

    // ── Тест-кейс 1: getProfile для несуществующего пользователя -> NOT_FOUND ──

    @Test
    fun `getProfile for non-existent user returns NOT_FOUND status`() =
        runTest {
            val request =
                GetProfileRequest
                    .newBuilder()
                    .setUserId("user_not_found_123")
                    .build()

            val exception =
                assertThrowsSuspend<StatusException> {
                    stub.getProfile(request)
                }

            assertEquals(Status.Code.NOT_FOUND, exception.status.code)
            assertEquals("Profile not found", exception.status.description)
        }

    // ── Тест-кейс 2: updateProfile успешно создаёт профиль со всеми полями КБЖУ ──

    @Test
    fun `updateProfile creates new profile and saves all proto fields to postgres`() =
        runTest {
            val request =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId("user_alice_456")
                    .setHeightCm(172)
                    .setWeightKg(65.5)
                    .setGender(GenderProto.GENDER_FEMALE)
                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE)
                    .setGoalType(GoalTypeProto.GOAL_TYPE_MAINTAIN)
                    .setWeeklyBudget(4500.0)
                    .setNutritionGoal(
                        NutritionGoalProto
                            .newBuilder()
                            .setTargetCalories(2100)
                            .setTargetProteinsGrams(130.0)
                            .setTargetFatsGrams(65.0)
                            .setTargetCarbsGrams(250.0)
                            .setTargetWeightKg(64.0)
                            .build(),
                    ).build()

            val response = stub.updateProfile(request)

            // Проверка возвращённого ProfileProto
            assertEquals("user_alice_456", response.userId)
            assertEquals(172, response.heightCm)
            assertEquals(65.5, response.weightKg)
            assertEquals(GenderProto.GENDER_FEMALE, response.gender)
            assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE, response.activityLevel)
            assertEquals(GoalTypeProto.GOAL_TYPE_MAINTAIN, response.goalType)
            assertEquals(4500.0, response.weeklyBudget)
            assertEquals(2100, response.nutritionGoal.targetCalories)
            assertEquals(130.0, response.nutritionGoal.targetProteinsGrams)
            assertEquals(65.0, response.nutritionGoal.targetFatsGrams)
            assertEquals(250.0, response.nutritionGoal.targetCarbsGrams)
            assertEquals(64.0, response.nutritionGoal.targetWeightKg)
            assertTrue(response.id.isNotBlank())
            assertTrue(response.hasUpdatedAt())

            // Проверка сохранения в реальной PostgreSQL базе через JPA
            val savedEntity = profileRepository.findByUserId("user_alice_456")
            assertNotNull(savedEntity)
            assertEquals("user_alice_456", savedEntity?.userId)
            assertEquals(172, savedEntity?.heightCm)
            assertEquals(0, BigDecimal("65.50").compareTo(savedEntity?.weightKg))
            assertEquals(2100, savedEntity?.nutritionGoal?.targetCalories)
            assertEquals(0, BigDecimal("130.00").compareTo(savedEntity?.nutritionGoal?.targetProteinsGrams))
            assertEquals(0, BigDecimal("65.00").compareTo(savedEntity?.nutritionGoal?.targetFatsGrams))
            assertEquals(0, BigDecimal("250.00").compareTo(savedEntity?.nutritionGoal?.targetCarbsGrams))
            assertEquals(0, BigDecimal("64.00").compareTo(savedEntity?.nutritionGoal?.targetWeightKg))
            assertEquals(0, BigDecimal("4500.00").compareTo(savedEntity?.weeklyBudget))
        }

    // ── Тест-кейс 3: T-019 Авторасчёт КБЖУ при отсутствии nutrition_goal ───────

    @Test
    fun `updateProfile without nutrition_goal auto-calculates goals and saves to postgres`() =
        runTest {
            val request =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId("user_auto_calc_1")
                    .setHeightCm(180)
                    .setWeightKg(80.0)
                    .setGender(GenderProto.GENDER_MALE)
                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_SEDENTARY)
                    .setGoalType(GoalTypeProto.GOAL_TYPE_MAINTAIN)
                    .setWeeklyBudget(3500.0)
                    .build()

            val response = stub.updateProfile(request)

            assertEquals("user_auto_calc_1", response.userId)
            // При 180см, 80кг, 25 лет, MALE, SEDENTARY, MAINTAIN: 2166 ккал
            assertEquals(2166, response.nutritionGoal.targetCalories)
            assertEquals(120.0, response.nutritionGoal.targetProteinsGrams)
            assertEquals(80.0, response.nutritionGoal.targetFatsGrams)
            assertEquals(241.5, response.nutritionGoal.targetCarbsGrams)
            assertEquals(80.0, response.nutritionGoal.targetWeightKg)

            // Проверка в БД
            val saved = profileRepository.findByUserId("user_auto_calc_1")
            assertNotNull(saved)
            assertEquals(2166, saved?.nutritionGoal?.targetCalories)
            assertEquals(0, BigDecimal("120.00").compareTo(saved?.nutritionGoal?.targetProteinsGrams))
            assertEquals(0, BigDecimal("80.00").compareTo(saved?.nutritionGoal?.targetFatsGrams))
            assertEquals(0, BigDecimal("241.50").compareTo(saved?.nutritionGoal?.targetCarbsGrams))
        }

    @Test
    fun `updateProfile with zero target calories triggers auto-calculation`() =
        runTest {
            val request =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId("user_auto_calc_zero_calories")
                    .setHeightCm(165)
                    .setWeightKg(60.0)
                    .setGender(GenderProto.GENDER_FEMALE)
                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE)
                    .setGoalType(GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT)
                    .setWeeklyBudget(4000.0)
                    .setNutritionGoal(
                        NutritionGoalProto
                            .newBuilder()
                            .setTargetCalories(0)
                            .build(),
                    ).build()

            val response = stub.updateProfile(request)

            assertEquals("user_auto_calc_zero_calories", response.userId)
            // При 165см, 60кг, FEMALE, MODERATE, LOSE_WEIGHT: 1710 ккал
            assertEquals(1710, response.nutritionGoal.targetCalories)
            assertEquals(120.0, response.nutritionGoal.targetProteinsGrams)
            assertEquals(60.0, response.nutritionGoal.targetFatsGrams)
            assertEquals(172.5, response.nutritionGoal.targetCarbsGrams)
            assertEquals(54.0, response.nutritionGoal.targetWeightKg)
        }

    // ── Тест-кейс 4: UPSERT существующего профиля ──────────────────────────────

    @Test
    fun `updateProfile updates existing profile without creating duplicates`() =
        runTest {
            val initialRequest =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId("user_bob_789")
                    .setHeightCm(180)
                    .setWeightKg(82.0)
                    .setGender(GenderProto.GENDER_MALE)
                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_LIGHT)
                    .setGoalType(GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT)
                    .setWeeklyBudget(3000.0)
                    .setNutritionGoal(
                        NutritionGoalProto
                            .newBuilder()
                            .setTargetCalories(2400)
                            .setTargetProteinsGrams(160.0)
                            .setTargetFatsGrams(75.0)
                            .setTargetCarbsGrams(270.0)
                            .setTargetWeightKg(75.0)
                            .build(),
                    ).build()

            val initialResponse = stub.updateProfile(initialRequest)
            val initialId = initialResponse.id
            assertEquals(1, profileRepository.count())

            // Повторный вызов с обновлёнными данными (рост, вес, калории, бюджет)
            val updatedRequest =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId("user_bob_789")
                    .setHeightCm(181)
                    .setWeightKg(80.5)
                    .setGender(GenderProto.GENDER_MALE)
                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_VERY_ACTIVE)
                    .setGoalType(GoalTypeProto.GOAL_TYPE_MAINTAIN)
                    .setWeeklyBudget(5500.0)
                    .setNutritionGoal(
                        NutritionGoalProto
                            .newBuilder()
                            .setTargetCalories(2600)
                            .setTargetProteinsGrams(175.0)
                            .setTargetFatsGrams(80.0)
                            .setTargetCarbsGrams(290.0)
                            .setTargetWeightKg(80.0)
                            .build(),
                    ).build()

            val updatedResponse = stub.updateProfile(updatedRequest)

            // Проверка сохранения идентичности первичного ключа UUID
            assertEquals(initialId, updatedResponse.id)
            assertEquals("user_bob_789", updatedResponse.userId)
            assertEquals(181, updatedResponse.heightCm)
            assertEquals(80.5, updatedResponse.weightKg)
            assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_VERY_ACTIVE, updatedResponse.activityLevel)
            assertEquals(GoalTypeProto.GOAL_TYPE_MAINTAIN, updatedResponse.goalType)
            assertEquals(5500.0, updatedResponse.weeklyBudget)
            assertEquals(2600, updatedResponse.nutritionGoal.targetCalories)

            // Проверка отсутствия дубликатов в БД (строго 1 запись)
            assertEquals(1, profileRepository.count())
            val updatedEntity = profileRepository.findByUserId("user_bob_789")
            assertNotNull(updatedEntity)
            assertEquals(initialId, updatedEntity?.id.toString())
            assertEquals(181, updatedEntity?.heightCm)
            assertEquals(0, BigDecimal("80.50").compareTo(updatedEntity?.weightKg))
            assertEquals(0, BigDecimal("5500.00").compareTo(updatedEntity?.weeklyBudget))
            assertEquals(2600, updatedEntity?.nutritionGoal?.targetCalories)
        }

    // ── Тест-кейс 5: Валидация аргументов и перехват исключений ───────────────

    @Test
    fun `getProfile with blank userId returns INVALID_ARGUMENT`() =
        runTest {
            val exception =
                assertThrowsSuspend<StatusException> {
                    stub.getProfile(GetProfileRequest.newBuilder().setUserId("   ").build())
                }
            assertEquals(Status.Code.INVALID_ARGUMENT, exception.status.code)
        }

    @Test
    fun `updateProfile with non-positive heightCm returns INVALID_ARGUMENT`() =
        runTest {
            val request =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId("user_invalid")
                    .setHeightCm(0)
                    .setWeightKg(70.0)
                    .setWeeklyBudget(1000.0)
                    .build()

            val exception =
                assertThrowsSuspend<StatusException> {
                    stub.updateProfile(request)
                }
            assertEquals(Status.Code.INVALID_ARGUMENT, exception.status.code)
        }

    @Test
    fun `updateProfile with negative weeklyBudget returns INVALID_ARGUMENT`() =
        runTest {
            val request =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId("user_invalid")
                    .setHeightCm(175)
                    .setWeightKg(70.0)
                    .setWeeklyBudget(-500.0)
                    .build()

            val exception =
                assertThrowsSuspend<StatusException> {
                    stub.updateProfile(request)
                }
            assertEquals(Status.Code.INVALID_ARGUMENT, exception.status.code)
        }
}
