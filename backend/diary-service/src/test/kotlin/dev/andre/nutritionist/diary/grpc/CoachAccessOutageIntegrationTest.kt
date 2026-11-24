package dev.andre.nutritionist.diary.grpc

import com.budgetntr.proto.diary.v1.DiaryServiceGrpc
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetRangeRequest
import com.budgetntr.proto.recipe.v1.CheckCoachAccessRequest
import com.budgetntr.proto.recipe.v1.CheckCoachAccessResponse
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpcKt
import dev.andre.nutritionist.diary.DiaryApplication
import dev.andre.nutritionist.diary.domain.model.MealType
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.net.ServerSocket
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(
    classes = [DiaryApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.grpc.server.port=0",
        "diary.outbox.enabled=false",
        "diary.kafka.registration.enabled=false",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@Timeout(30)
@DirtiesContext
class CoachAccessOutageIntegrationTest
    @Autowired
    constructor(
        private val jdbc: JdbcTemplate,
        grpc: GrpcServerLifecycle,
    ) {
        private val channel = ManagedChannelBuilder.forAddress("127.0.0.1", grpc.port).usePlaintext().build()

        private val coachId = "00000000-0000-0000-0000-000000000101"
        private val studentId = "00000000-0000-0000-0000-000000000202"

        @BeforeEach
        fun reset() {
            jdbc.execute("TRUNCATE outbox_messages, diary_entries, idempotency_keys")
            fakeRecipeService.callCount.set(0)
        }

        @AfterAll
        fun shutdown() {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }

        @Test
        @Order(1)
        fun `when redis is down coach access transparently falls back to recipe-ai-service`() {
            seedDiaryEntry(studentId, "Student Fallback Meal", 410)

            val day =
                diaryStub(coachId).getDailyEntries(
                    GetDailyEntriesRequest
                        .newBuilder()
                        .setDate("2026-11-17")
                        .setTimeZone("UTC")
                        .setTargetUserId(studentId)
                        .build(),
                )
            val range =
                diaryStub(coachId).getRange(
                    GetRangeRequest
                        .newBuilder()
                        .setStartDate("2026-11-17")
                        .setEndDate("2026-11-18")
                        .setTimeZone("UTC")
                        .setTargetUserId(studentId)
                        .build(),
                )

            assertEquals(1, day.entriesCount)
            assertEquals("Student Fallback Meal", day.entriesList.single().nameSnapshot)
            assertEquals(1, range.entriesCount)
            assertEquals(2, fakeRecipeService.callCount.get())
        }

        @Test
        @Order(2)
        fun `when recipe-ai-service server is unreachable diary fails closed with UNAVAILABLE`() {
            seedDiaryEntry(studentId, "Protected Student Meal", 500)
            recipeServer.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)

            val dayError =
                assertThrows(StatusRuntimeException::class.java) {
                    diaryStub(coachId).getDailyEntries(
                        GetDailyEntriesRequest
                            .newBuilder()
                            .setDate("2026-11-17")
                            .setTimeZone("UTC")
                            .setTargetUserId(studentId)
                            .build(),
                    )
                }
            val rangeError =
                assertThrows(StatusRuntimeException::class.java) {
                    diaryStub(coachId).getRange(
                        GetRangeRequest
                            .newBuilder()
                            .setStartDate("2026-11-17")
                            .setEndDate("2026-11-18")
                            .setTimeZone("UTC")
                            .setTargetUserId(studentId)
                            .build(),
                    )
                }

            assertEquals(Status.Code.UNAVAILABLE, dayError.status.code)
            assertEquals(Status.Code.UNAVAILABLE, rangeError.status.code)
        }

        private fun seedDiaryEntry(
            ownerId: String,
            name: String,
            calories: Int,
        ) {
            jdbc.update(
                """
                INSERT INTO diary_entries
                    (id, user_id, idempotency_key, source_type, source_id, name_snapshot, weight_grams,
                     food_item_id, food_name, quantity_grams,
                     calories, proteins, fats, carbs, meal_type, recorded_at)
                VALUES
                    (gen_random_uuid(), ?, ?, 'PRODUCT', gen_random_uuid(), ?, 200,
                     gen_random_uuid(), ?, 200,
                     ?, 15.0, 4.0, 20.0, ?, CAST('2026-11-17T12:00:00Z' AS timestamptz))
                """.trimIndent(),
                ownerId,
                UUID.randomUUID().toString(),
                name,
                name,
                calories,
                MealType.LUNCH.name,
            )
        }

        private fun diaryStub(userId: String): DiaryServiceGrpc.DiaryServiceBlockingStub {
            val headers =
                Metadata().apply {
                    put(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER), userId)
                    put(Metadata.Key.of("x-user-roles", Metadata.ASCII_STRING_MARSHALLER), "ROLE_COACH")
                }
            return DiaryServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
        }

        class AllowingRecipeAiService : RecipeAiServiceGrpcKt.RecipeAiServiceCoroutineImplBase() {
            val callCount = AtomicInteger(0)

            override suspend fun checkCoachAccess(request: CheckCoachAccessRequest): CheckCoachAccessResponse {
                callCount.incrementAndGet()
                return CheckCoachAccessResponse
                    .newBuilder()
                    .setHasAccess(true)
                    .setCanViewDiary(true)
                    .build()
            }
        }

        companion object {
            private val postgres =
                PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                    .withStartupTimeout(Duration.ofSeconds(30))
                    .apply { start() }

            private val unusedRedisPort: Int = ServerSocket(0).use { it.localPort }

            private val fakeRecipeService = AllowingRecipeAiService()

            private val recipeServer: Server =
                Grpc
                    .newServerBuilderForPort(0, InsecureServerCredentials.create())
                    .addService(fakeRecipeService)
                    .build()
                    .start()

            @JvmStatic
            @DynamicPropertySource
            fun registerProperties(registry: DynamicPropertyRegistry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl)
                registry.add("spring.datasource.username", postgres::getUsername)
                registry.add("spring.datasource.password", postgres::getPassword)
                registry.add("spring.data.redis.host") { "127.0.0.1" }
                registry.add("spring.data.redis.port") { unusedRedisPort }
                registry.add("spring.data.redis.connect-timeout") { "300ms" }
                registry.add("spring.data.redis.timeout") { "300ms" }
                registry.add("diary.clients.recipe.address") { "127.0.0.1:${recipeServer.port}" }
                registry.add("diary.clients.recipe.plaintext") { "true" }
            }
        }
    }
