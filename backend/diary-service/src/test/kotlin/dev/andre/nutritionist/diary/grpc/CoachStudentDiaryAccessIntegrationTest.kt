package dev.andre.nutritionist.diary.grpc

import com.budgetntr.proto.diary.v1.DeleteMealRequest
import com.budgetntr.proto.diary.v1.DiaryServiceGrpc
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetRangeRequest
import com.budgetntr.proto.recipe.v1.CheckCoachAccessRequest
import com.budgetntr.proto.recipe.v1.CheckCoachAccessResponse
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpcKt
import dev.andre.nutritionist.diary.DiaryApplication
import dev.andre.nutritionist.diary.config.DiaryClientConfiguration
import dev.andre.nutritionist.diary.domain.model.MealType
import dev.andre.nutritionist.diary.service.CoachAccessCacheService
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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
@Timeout(30)
@DirtiesContext
class CoachStudentDiaryAccessIntegrationTest
    @Autowired
    constructor(
        private val jdbc: JdbcTemplate,
        private val redisTemplate: StringRedisTemplate,
        private val circuitBreakerRegistry: CircuitBreakerRegistry,
        grpc: GrpcServerLifecycle,
    ) {
        private val channel = ManagedChannelBuilder.forAddress("127.0.0.1", grpc.port).usePlaintext().build()

        private val coachId = "00000000-0000-0000-0000-000000000101"
        private val studentId = "00000000-0000-0000-0000-000000000202"
        private val strangerId = "00000000-0000-0000-0000-000000000303"
        private val adminId = "00000000-0000-0000-0000-000000000999"

        @BeforeEach
        fun resetState() {
            jdbc.execute("TRUNCATE outbox_messages, diary_entries, idempotency_keys")
            redisTemplate.delete(
                listOf(
                    CoachAccessCacheService.formatKey(coachId, studentId),
                    CoachAccessCacheService.formatKey(strangerId, studentId),
                ),
            )
            fakeRecipeService.reset()
            circuitBreakerRegistry.circuitBreaker(DiaryClientConfiguration.RECIPE_ACCESS_CIRCUIT_BREAKER).reset()
        }

        @AfterAll
        fun shutdown() {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            recipeServer.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }

        @Test
        fun `reading own diary with empty or self target_user_id skips recipe-ai-service`() {
            seedDiaryEntry(coachId, "Coach Oatmeal", 320, MealType.BREAKFAST)
            seedDiaryEntry(studentId, "Student Salad", 450, MealType.LUNCH)

            val ownDefault = diaryStub(coachId).getDailyEntries(dayRequest())
            val ownExplicit = diaryStub(coachId).getDailyEntries(dayRequest(targetUserId = coachId))
            val ownRange = diaryStub(coachId).getRange(rangeRequest(targetUserId = coachId))

            assertEquals(1, ownDefault.entriesCount)
            assertEquals("Coach Oatmeal", ownDefault.entriesList.single().nameSnapshot)
            assertEquals(1, ownExplicit.entriesCount)
            assertEquals("Coach Oatmeal", ownExplicit.entriesList.single().nameSnapshot)
            assertEquals(1, ownRange.entriesCount)
            assertEquals(0, fakeRecipeService.callCount.get())
        }

        @Test
        fun `reading another user diary without coach link returns PERMISSION_DENIED and caches denial`() {
            seedDiaryEntry(studentId, "Student Salad", 450, MealType.LUNCH)
            fakeRecipeService.responder.set { accessResponse(hasAccess = false, canViewDiary = false) }

            val dayError =
                assertThrows(StatusRuntimeException::class.java) {
                    diaryStub(strangerId).getDailyEntries(dayRequest(targetUserId = studentId))
                }
            assertEquals(Status.Code.PERMISSION_DENIED, dayError.status.code)
            assertEquals(1, fakeRecipeService.callCount.get())

            val cacheKey = CoachAccessCacheService.formatKey(strangerId, studentId)
            assertEquals("0", redisTemplate.opsForValue().get(cacheKey))

            val rangeError =
                assertThrows(StatusRuntimeException::class.java) {
                    diaryStub(strangerId).getRange(rangeRequest(targetUserId = studentId))
                }
            assertEquals(Status.Code.PERMISSION_DENIED, rangeError.status.code)
            assertEquals(1, fakeRecipeService.callCount.get())
        }

        @Test
        fun `coach with link but can_view_diary false is denied`() {
            seedDiaryEntry(studentId, "Student Salad", 450, MealType.LUNCH)
            fakeRecipeService.responder.set { accessResponse(hasAccess = true, canViewDiary = false) }

            val error =
                assertThrows(StatusRuntimeException::class.java) {
                    diaryStub(coachId, roles = "ROLE_COACH").getDailyEntries(dayRequest(targetUserId = studentId))
                }
            assertEquals(Status.Code.PERMISSION_DENIED, error.status.code)
            assertEquals(1, fakeRecipeService.callCount.get())
        }

        @Test
        fun `active coach reads student diary, propagates traceparent, caches 1 with 60s TTL and hits cache on subsequent calls`() {
            seedDiaryEntry(studentId, "Student Chicken", 520, MealType.DINNER)
            fakeRecipeService.responder.set { accessResponse(hasAccess = true, canViewDiary = true) }
            val traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"

            val firstDay =
                diaryStub(coachId, roles = "ROLE_COACH", traceparent = traceparent)
                    .getDailyEntries(dayRequest(targetUserId = studentId))

            assertEquals(1, firstDay.entriesCount)
            assertEquals("Student Chicken", firstDay.entriesList.single().nameSnapshot)
            assertEquals(520, firstDay.dailyTotal.calories)
            assertEquals(1, fakeRecipeService.callCount.get())
            assertEquals(coachId, fakeRecipeService.lastUserId.get())
            assertEquals("true", fakeRecipeService.lastDevBypass.get())
            assertEquals(traceparent, fakeRecipeService.lastTraceparent.get())

            val cacheKey = CoachAccessCacheService.formatKey(coachId, studentId)
            assertEquals("1", redisTemplate.opsForValue().get(cacheKey))
            val ttlSeconds = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS)
            assertTrue(ttlSeconds in 1..60, "Expected TTL in 1..60 seconds, got $ttlSeconds")

            val secondDay = diaryStub(coachId, roles = "ROLE_COACH").getDailyEntries(dayRequest(targetUserId = studentId))
            val range = diaryStub(coachId, roles = "ROLE_COACH").getRange(rangeRequest(targetUserId = studentId))

            assertEquals(1, secondDay.entriesCount)
            assertEquals(1, range.entriesCount)
            assertEquals("Student Chicken", range.entriesList.single().nameSnapshot)
            assertEquals(1, fakeRecipeService.callCount.get())
        }

        @Test
        fun `fail-closed returns UNAVAILABLE when recipe-ai-service fails and trips circuit breaker to OPEN without caching`() {
            seedDiaryEntry(studentId, "Student Secret Meal", 610, MealType.LUNCH)
            fakeRecipeService.responder.set {
                throw Status.UNAVAILABLE.withDescription("Simulated recipe outage").asRuntimeException()
            }

            repeat(3) {
                val error =
                    assertThrows(StatusRuntimeException::class.java) {
                        diaryStub(coachId, roles = "ROLE_COACH").getDailyEntries(dayRequest(targetUserId = studentId))
                    }
                assertEquals(Status.Code.UNAVAILABLE, error.status.code)
            }

            val breaker = circuitBreakerRegistry.circuitBreaker(DiaryClientConfiguration.RECIPE_ACCESS_CIRCUIT_BREAKER)
            assertEquals(CircuitBreaker.State.OPEN, breaker.state)
            assertEquals(3, fakeRecipeService.callCount.get())

            val openDayError =
                assertThrows(StatusRuntimeException::class.java) {
                    diaryStub(coachId, roles = "ROLE_COACH").getDailyEntries(dayRequest(targetUserId = studentId))
                }
            val openRangeError =
                assertThrows(StatusRuntimeException::class.java) {
                    diaryStub(coachId, roles = "ROLE_COACH").getRange(rangeRequest(targetUserId = studentId))
                }
            assertEquals(Status.Code.UNAVAILABLE, openDayError.status.code)
            assertEquals(Status.Code.UNAVAILABLE, openRangeError.status.code)
            assertEquals(3, fakeRecipeService.callCount.get())
            assertNull(redisTemplate.opsForValue().get(CoachAccessCacheService.formatKey(coachId, studentId)))
        }

        @Test
        fun `admin role reads student diary and mutations remain strictly owner-only`() {
            val studentEntryId = seedDiaryEntry(studentId, "Student Oats", 290, MealType.BREAKFAST)

            val adminView = diaryStub(adminId, roles = "ROLE_ADMIN").getDailyEntries(dayRequest(targetUserId = studentId))
            assertEquals(1, adminView.entriesCount)
            assertEquals(0, fakeRecipeService.callCount.get())

            fakeRecipeService.responder.set { accessResponse(hasAccess = true, canViewDiary = true) }
            val deleteError =
                assertThrows(StatusRuntimeException::class.java) {
                    diaryStub(coachId, roles = "ROLE_COACH", idempotencyKey = UUID.randomUUID().toString())
                        .deleteMeal(DeleteMealRequest.newBuilder().setEntryId(studentEntryId).build())
                }
            assertEquals(Status.Code.NOT_FOUND, deleteError.status.code)
        }

        private fun seedDiaryEntry(
            ownerId: String,
            name: String,
            calories: Int,
            mealType: MealType,
        ): String {
            val entryId = UUID.randomUUID().toString()
            jdbc.update(
                """
                INSERT INTO diary_entries
                    (id, user_id, idempotency_key, source_type, source_id, name_snapshot, weight_grams,
                     food_item_id, food_name, quantity_grams,
                     calories, proteins, fats, carbs, meal_type, recorded_at)
                VALUES
                    (CAST(? AS uuid), ?, ?, 'PRODUCT', gen_random_uuid(), ?, 250,
                     gen_random_uuid(), ?, 250,
                     ?, 20.0, 5.0, 30.0, ?, CAST('2026-11-17T12:00:00Z' AS timestamptz))
                """.trimIndent(),
                entryId,
                ownerId,
                UUID.randomUUID().toString(),
                name,
                name,
                calories,
                mealType.name,
            )
            return entryId
        }

        private fun dayRequest(targetUserId: String? = null): GetDailyEntriesRequest =
            GetDailyEntriesRequest
                .newBuilder()
                .setDate("2026-11-17")
                .setTimeZone("UTC")
                .apply {
                    if (targetUserId != null) {
                        setTargetUserId(targetUserId)
                    }
                }.build()

        private fun rangeRequest(targetUserId: String? = null): GetRangeRequest =
            GetRangeRequest
                .newBuilder()
                .setStartDate("2026-11-17")
                .setEndDate("2026-11-18")
                .setTimeZone("UTC")
                .apply {
                    if (targetUserId != null) {
                        setTargetUserId(targetUserId)
                    }
                }.build()

        private fun diaryStub(
            userId: String,
            roles: String? = null,
            traceparent: String? = null,
            idempotencyKey: String? = null,
        ): DiaryServiceGrpc.DiaryServiceBlockingStub {
            val headers =
                Metadata().apply {
                    put(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER), userId)
                    roles?.let { put(Metadata.Key.of("x-user-roles", Metadata.ASCII_STRING_MARSHALLER), it) }
                    traceparent?.let { put(Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER), it) }
                    idempotencyKey?.let { put(Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER), it) }
                }
            return DiaryServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
        }

        private fun accessResponse(
            hasAccess: Boolean,
            canViewDiary: Boolean,
        ): CheckCoachAccessResponse =
            CheckCoachAccessResponse
                .newBuilder()
                .setHasAccess(hasAccess)
                .setCanViewDiary(canViewDiary)
                .build()

        class FakeRecipeAiService : RecipeAiServiceGrpcKt.RecipeAiServiceCoroutineImplBase() {
            val callCount = AtomicInteger(0)
            val lastUserId = AtomicReference<String?>(null)
            val lastDevBypass = AtomicReference<String?>(null)
            val lastTraceparent = AtomicReference<String?>(null)
            val responder =
                AtomicReference<(CheckCoachAccessRequest) -> CheckCoachAccessResponse> {
                    CheckCoachAccessResponse
                        .newBuilder()
                        .setHasAccess(false)
                        .setCanViewDiary(false)
                        .build()
                }

            fun reset() {
                callCount.set(0)
                lastUserId.set(null)
                lastDevBypass.set(null)
                lastTraceparent.set(null)
                responder.set {
                    CheckCoachAccessResponse
                        .newBuilder()
                        .setHasAccess(false)
                        .setCanViewDiary(false)
                        .build()
                }
            }

            override suspend fun checkCoachAccess(request: CheckCoachAccessRequest): CheckCoachAccessResponse {
                callCount.incrementAndGet()
                return responder.get().invoke(request)
            }
        }

        companion object {
            private val postgres =
                PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                    .withStartupTimeout(Duration.ofSeconds(30))
                    .apply { start() }

            private val redis =
                GenericContainer(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withStartupTimeout(Duration.ofSeconds(30))
                    .apply { start() }

            private val fakeRecipeService = FakeRecipeAiService()

            private val headerCaptureInterceptor =
                object : ServerInterceptor {
                    private val userKey = Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER)
                    private val bypassKey = Metadata.Key.of("x-dev-bypass", Metadata.ASCII_STRING_MARSHALLER)
                    private val traceKey = Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER)

                    override fun <ReqT, RespT> interceptCall(
                        call: ServerCall<ReqT, RespT>,
                        headers: Metadata,
                        next: ServerCallHandler<ReqT, RespT>,
                    ): ServerCall.Listener<ReqT> {
                        fakeRecipeService.lastUserId.set(headers.get(userKey))
                        fakeRecipeService.lastDevBypass.set(headers.get(bypassKey))
                        fakeRecipeService.lastTraceparent.set(headers.get(traceKey))
                        return next.startCall(call, headers)
                    }
                }

            private val recipeServer: Server =
                Grpc
                    .newServerBuilderForPort(0, InsecureServerCredentials.create())
                    .addService(ServerInterceptors.intercept(fakeRecipeService, headerCaptureInterceptor))
                    .build()
                    .start()

            @JvmStatic
            @DynamicPropertySource
            fun registerProperties(registry: DynamicPropertyRegistry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl)
                registry.add("spring.datasource.username", postgres::getUsername)
                registry.add("spring.datasource.password", postgres::getPassword)
                registry.add("spring.data.redis.host", redis::getHost)
                registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
                registry.add("diary.clients.recipe.address") { "127.0.0.1:${recipeServer.port}" }
                registry.add("diary.clients.recipe.plaintext") { "true" }
                registry.add("diary.clients.recipe.breaker-window") { "4" }
                registry.add("diary.clients.recipe.breaker-min-calls") { "3" }
                registry.add("diary.clients.recipe.breaker-open-for") { "30s" }
            }
        }
    }
