package dev.andre.nutritionist.profile.grpc

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.ProfileServiceGrpcKt
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal

@SpringBootTest
@Testcontainers
@ActiveProfiles("dev")
class ProfileGrpcServiceIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                .withDatabaseName("profile_grpc_test_db")
                .withUsername("test_user")
                .withPassword("test_pass")

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.liquibase.enabled") { "true" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
        }
    }

    @Autowired
    private lateinit var profileGrpcService: ProfileGrpcService

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
                .addService(profileGrpcService)
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
            assertTrue(
                exception.status.description?.contains("user_not_found_123") == true,
                "Error description must mention the queried userId",
            )
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
            assertEquals(0, BigDecimal("4500.00").compareTo(savedEntity?.weeklyBudget))

            // Проверка получения созданного профиля через getProfile
            val fetchedProfile =
                stub.getProfile(
                    GetProfileRequest.newBuilder().setUserId("user_alice_456").build(),
                )
            assertEquals(response.id, fetchedProfile.id)
            assertEquals("user_alice_456", fetchedProfile.userId)
            assertEquals(172, fetchedProfile.heightCm)
            assertEquals(2100, fetchedProfile.nutritionGoal.targetCalories)
        }

    // ── Тест-кейс 3: повторный updateProfile обновляет существующий (UPSERT) без дубликатов ──

    @Test
    fun `updateProfile updates existing profile idempotently without creating duplicate records`() =
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

    // ── Тест-кейс 4: валидация аргументов ──

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
