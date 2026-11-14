package dev.andre.nutritionist.profile.grpc

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.ProfileServiceGrpcKt
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.profile.AbstractPostgresIntegrationTest
import dev.andre.nutritionist.profile.grpc.idempotency.IdempotencyServerInterceptor
import dev.andre.nutritionist.profile.grpc.idempotency.IdempotencyStore
import dev.andre.nutritionist.profile.grpc.interceptor.GrpcExceptionInterceptor
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.test.runTest
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
    private lateinit var idempotencyServerInterceptor: IdempotencyServerInterceptor

    @Autowired
    private lateinit var idempotencyStore: IdempotencyStore

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
                .addService(
                    ServerInterceptors.intercept(
                        profileGrpcService,
                        grpcExceptionInterceptor,
                        idempotencyServerInterceptor,
                    ),
                ).build()
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

    private fun stubWithIdempotency(idempotencyKey: String? = null): ProfileServiceGrpcKt.ProfileServiceCoroutineStub {
        if (idempotencyKey == null) return stub
        val headers = Metadata()
        headers.put(IdempotencyServerInterceptor.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
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
            assertTrue(response.hasUpdatedAt())

            val savedEntity = profileRepository.findByUserId("user_alice_456")
            assertNotNull(savedEntity)
            assertEquals(172, savedEntity?.heightCm)
            assertEquals(0, BigDecimal("65.50").compareTo(savedEntity?.weightKg))
            assertEquals(0, BigDecimal("4500.00").compareTo(savedEntity?.weeklyBudget))
            assertEquals(2100, savedEntity?.nutritionGoal?.targetCalories)
            assertEquals(0, BigDecimal("130.00").compareTo(savedEntity?.nutritionGoal?.targetProteinsGrams))
            assertEquals(0, BigDecimal("65.00").compareTo(savedEntity?.nutritionGoal?.targetFatsGrams))
            assertEquals(0, BigDecimal("250.00").compareTo(savedEntity?.nutritionGoal?.targetCarbsGrams))
            assertEquals(0, BigDecimal("64.00").compareTo(savedEntity?.nutritionGoal?.targetWeightKg))
        }

    // ── Тест-кейс 3: Автоматический расчёт КБЖУ через NutritionGoalCalculator ──

    @Test
    fun `updateProfile without nutrition_goal auto-calculates КБЖУ via NutritionGoalCalculator`() =
        runTest {
            val request =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId("user_calc_1")
                    .setHeightCm(180)
                    .setWeightKg(80.0)
                    .setGender(GenderProto.GENDER_MALE)
                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE)
                    .setGoalType(GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT)
                    .setWeeklyBudget(3000.0)
                    .build()

            val response = stub.updateProfile(request)

            assertEquals("user_calc_1", response.userId)
            val goal = response.nutritionGoal
            assertNotNull(goal)
            assertTrue(goal.targetCalories > 0, "Калории должны быть рассчитаны автоматически")
            assertTrue(goal.targetProteinsGrams > 0.0, "Белки должны быть рассчитаны автоматически")
            assertTrue(goal.targetFatsGrams > 0.0, "Жиры должны быть рассчитаны автоматически")
            assertTrue(goal.targetCarbsGrams > 0.0, "Углеводы должны быть рассчитаны автоматически")

            val expectedCalories = (goal.targetProteinsGrams * 4) + (goal.targetFatsGrams * 9) + (goal.targetCarbsGrams * 4)
            assertTrue(
                kotlin.math.abs(goal.targetCalories - expectedCalories) <= 15,
                "Баланс калорий КБЖУ должен сходиться: рассчитано=${goal.targetCalories}, сумма макросов=$expectedCalories",
            )
        }

    @Test
    fun `updateProfile with zero target_calories auto-calculates КБЖУ via NutritionGoalCalculator`() =
        runTest {
            val request =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId("user_calc_zero")
                    .setHeightCm(165)
                    .setWeightKg(55.0)
                    .setGender(GenderProto.GENDER_FEMALE)
                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_LIGHT)
                    .setGoalType(GoalTypeProto.GOAL_TYPE_MAINTAIN)
                    .setWeeklyBudget(2500.0)
                    .setNutritionGoal(
                        NutritionGoalProto.newBuilder().setTargetCalories(0).build(),
                    ).build()

            val response = stub.updateProfile(request)

            assertTrue(response.nutritionGoal.targetCalories > 0)
            assertTrue(response.nutritionGoal.targetProteinsGrams > 0.0)
        }

    // ── Тест-кейс 4: updateProfile обновляет существующий профиль (UPSERT) ──────

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
                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_SEDENTARY)
                    .setGoalType(GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT)
                    .setWeeklyBudget(5000.0)
                    .setNutritionGoal(
                        NutritionGoalProto
                            .newBuilder()
                            .setTargetCalories(2000)
                            .setTargetProteinsGrams(140.0)
                            .setTargetFatsGrams(60.0)
                            .setTargetCarbsGrams(225.0)
                            .setTargetWeightKg(75.0)
                            .build(),
                    ).build()

            val initialResponse = stub.updateProfile(initialRequest)
            val initialId = initialResponse.id
            assertEquals(1, profileRepository.count())

            val updateRequest =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId("user_bob_789")
                    .setHeightCm(181)
                    .setWeightKg(80.5)
                    .setGender(GenderProto.GENDER_MALE)
                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE)
                    .setGoalType(GoalTypeProto.GOAL_TYPE_MAINTAIN)
                    .setWeeklyBudget(5500.0)
                    .setNutritionGoal(
                        NutritionGoalProto
                            .newBuilder()
                            .setTargetCalories(2600)
                            .setTargetProteinsGrams(160.0)
                            .setTargetFatsGrams(75.0)
                            .setTargetCarbsGrams(320.0)
                            .setTargetWeightKg(80.0)
                            .build(),
                    ).build()

            val updatedResponse = stub.updateProfile(updateRequest)

            assertEquals(initialId, updatedResponse.id)
            assertEquals(181, updatedResponse.heightCm)
            assertEquals(80.5, updatedResponse.weightKg)
            assertEquals(5500.0, updatedResponse.weeklyBudget)
            assertEquals(2600, updatedResponse.nutritionGoal.targetCalories)

            assertEquals(1, profileRepository.count())
            val updatedEntity = profileRepository.findByUserId("user_bob_789")
            assertNotNull(updatedEntity)
            assertEquals(initialId, updatedEntity?.id.toString())
            assertEquals(181, updatedEntity?.heightCm)
            assertEquals(0, BigDecimal("80.50").compareTo(updatedEntity?.weightKg))
            assertEquals(0, BigDecimal("5500.00").compareTo(updatedEntity?.weeklyBudget))
            assertEquals(2600, updatedEntity?.nutritionGoal?.targetCalories)
        }

    // ── Тест-кейс 5: Валидация аргументов ──────────────────────────────────────

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

    // ── Тест-кейс 6: Идемпотентность по заголовку idempotency-key ─────────────

    @Test
    fun `updateProfile with idempotency key returns cached response on second call`() =
        runTest {
            val request =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId("user_idemp_1")
                    .setHeightCm(180)
                    .setWeightKg(80.0)
                    .setGender(GenderProto.GENDER_MALE)
                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_SEDENTARY)
                    .setGoalType(GoalTypeProto.GOAL_TYPE_MAINTAIN)
                    .setWeeklyBudget(3000.0)
                    .build()

            val userStub = stubWithIdempotency("idemp-key-test-123")

            val firstResponse = userStub.updateProfile(request)
            val secondResponse = userStub.updateProfile(request)

            assertEquals(firstResponse.id, secondResponse.id)
            assertEquals(firstResponse.updatedAt.seconds, secondResponse.updatedAt.seconds)
            assertEquals(firstResponse.updatedAt.nanos, secondResponse.updatedAt.nanos)

            // Проверяем, что ответ физически лежит в Redis
            val cachedBytes = idempotencyStore.get("idemp-key-test-123")
            assertNotNull(cachedBytes, "Ответ должен быть сохранён в RedisIdempotencyStore")
        }
}
