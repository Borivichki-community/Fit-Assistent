package dev.andre.nutritionist.diary.grpc

import com.budgetntr.proto.diary.v1.DeleteMealRequest
import com.budgetntr.proto.diary.v1.DiaryServiceGrpc
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetDailyEntriesResponse
import dev.andre.nutritionist.diary.DiaryApplication
import dev.andre.nutritionist.diary.domain.model.MealType
import dev.andre.nutritionist.diary.domain.model.UserId
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

@SpringBootTest(
    classes = [DiaryApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.grpc.server.port=0",
        "diary.outbox.enabled=false",
        "diary.kafka.registration.enabled=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(30)
@DirtiesContext
class DiaryGetDayIntegrationTest
    @Autowired
    constructor(
        private val jdbc: JdbcTemplate,
        entityManagerFactory: EntityManagerFactory,
        grpc: GrpcServerLifecycle,
    ) {
        private val user = UserId("00000000-0000-0000-0000-000000000001")
        private val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        private val channel = ManagedChannelBuilder.forAddress("127.0.0.1", grpc.port).usePlaintext().build()

        @BeforeEach
        fun reset() {
            jdbc.execute("TRUNCATE outbox_messages, diary_entries, idempotency_keys")
        }

        @AfterAll
        fun close() {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }

        @Test
        fun `one query returns an empty day and four zero groups`() {
            val day = readDay()

            assertEquals(0, day.entriesCount)
            assertEquals(0, day.dailyTotal.calories)
            assertEquals(MealType.entries.map { it.name }, day.mealTotalsList.map { it.mealType })
            day.mealTotalsList.forEach {
                assertEquals(0, it.nutrition.calories)
                assertEquals(0.0, it.nutrition.proteins)
                assertEquals(0.0, it.nutrition.fats)
                assertEquals(0.0, it.nutrition.carbs)
            }
        }

        @Test
        fun `one and one hundred entries each require exactly one query`() {
            listOf(1, 100).forEach { count ->
                reset()
                seed(MealType.LUNCH, count)
                val day = readDay()

                assertEquals(count, day.entriesCount)
                assertEquals(150 * count, day.dailyTotal.calories)
                assertEquals(0.1 * count, day.dailyTotal.proteins, 0.000001)
                assertEquals(day.dailyTotal, day.mealTotalsList[1].nutrition)
                assertFalse(day.entriesList.first().hasCost())
                assertEquals(800.0, day.entriesList.first().quantityGrams)
                val ids = day.entriesList.map { it.id }
                assertEquals(ids.sorted(), ids)
            }
        }

        @Test
        fun `all meal totals and deletion reflect saved snapshots`() {
            seed(MealType.BREAKFAST)
            seed(MealType.LUNCH, 2)
            seed(MealType.DINNER)
            seed(MealType.SNACK)
            seed(MealType.LUNCH, userId = UserId("00000000-0000-0000-0000-000000000002"))
            val day = readDay()

            assertEquals(5, day.entriesCount)
            assertEquals(listOf(150, 300, 150, 150), day.mealTotalsList.map { it.nutrition.calories })
            assertEquals(750, day.dailyTotal.calories)
            assertEquals(0.5, day.dailyTotal.proteins)
            assertEquals(1.0, day.dailyTotal.fats)
            assertEquals(1.5, day.dailyTotal.carbs)
            val lunch = day.entriesList.first { it.mealType == "LUNCH" }
            stub(key = UUID.randomUUID().toString())
                .deleteMeal(DeleteMealRequest.newBuilder().setEntryId(lunch.id).build())

            val afterDeletion = readDay()
            assertEquals(4, afterDeletion.entriesCount)
            assertEquals(600, afterDeletion.dailyTotal.calories)
            assertEquals(150, afterDeletion.mealTotalsList[1].nutrition.calories)
        }

        @Test
        fun `spring and autumn local days include start and exclude next midnight`() {
            assertBoundaries("2026-03-29", "2026-03-28T23:00:00Z", "2026-03-29T22:00:00Z")
            reset()
            assertBoundaries("2026-10-25", "2026-10-24T22:00:00Z", "2026-10-25T23:00:00Z")
        }

        @Test
        fun `invalid dates zones and missing identity have grpc statuses`() {
            listOf(
                request("2026-02-30"),
                request(zone = "Invalid/Zone"),
            ).forEach { request ->
                val error = assertThrows(StatusRuntimeException::class.java) { stub().getDailyEntries(request) }
                assertEquals(Status.Code.INVALID_ARGUMENT, error.status.code)
            }
            val error =
                assertThrows(StatusRuntimeException::class.java) {
                    DiaryServiceGrpc
                        .newBlockingStub(channel)
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .getDailyEntries(request())
                }
            assertEquals(Status.Code.UNAUTHENTICATED, error.status.code)
        }

        private fun assertBoundaries(
            date: String,
            from: String,
            until: String,
        ) {
            seed(MealType.BREAKFAST, at = Instant.parse(from).minusSeconds(1).toString())
            seed(MealType.BREAKFAST, at = from)
            seed(MealType.DINNER, at = Instant.parse(until).minusSeconds(1).toString())
            seed(MealType.DINNER, at = until)
            val day = readDay(request(date, "Europe/Berlin"))

            assertEquals(2, day.entriesCount)
            assertEquals(300, day.dailyTotal.calories)
            assertEquals(listOf(150, 0, 150, 0), day.mealTotalsList.map { it.nutrition.calories })
        }

        private fun readDay(request: GetDailyEntriesRequest = request()): GetDailyEntriesResponse {
            statistics.clear()
            val day = stub().getDailyEntries(request)
            assertEquals(1L, statistics.queryExecutionCount)
            assertEquals(1L, statistics.prepareStatementCount)
            return day
        }

        private fun seed(
            mealType: MealType,
            count: Int = 1,
            userId: UserId = user,
            at: String = "2026-11-17T12:00:00Z",
        ) {
            jdbc.update(
                """
                INSERT INTO diary_entries
                    (id, user_id, idempotency_key, source_type, source_id, name_snapshot, weight_grams,
                     food_item_id, food_name, quantity_grams,
                     calories, proteins, fats, carbs, meal_type, recorded_at)
                SELECT gen_random_uuid(), ?, gen_random_uuid()::text, 'PRODUCT', gen_random_uuid(), 'Snapshot', 800,
                       gen_random_uuid(), 'Snapshot', 800,
                       150, 0.1, 0.2, 0.3, ?, CAST(? AS timestamptz)
                FROM generate_series(1, ?)
                """.trimIndent(),
                userId.raw,
                mealType.name,
                at,
                count,
            )
        }

        private fun request(
            date: String = "2026-11-17",
            zone: String = "UTC",
        ): GetDailyEntriesRequest =
            GetDailyEntriesRequest
                .newBuilder()
                .setDate(date)
                .setTimeZone(zone)
                .build()

        private fun stub(key: String? = null): DiaryServiceGrpc.DiaryServiceBlockingStub {
            val metadata =
                Metadata().apply {
                    put(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER), user.raw)
                    key?.let { put(Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER), it) }
                }
            return DiaryServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
        }

        companion object {
            private val postgres =
                PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                    .withStartupTimeout(Duration.ofSeconds(30))
                    .apply { start() }

            @JvmStatic
            @DynamicPropertySource
            fun databaseProperties(registry: DynamicPropertyRegistry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl)
                registry.add("spring.datasource.username", postgres::getUsername)
                registry.add("spring.datasource.password", postgres::getPassword)
            }
        }
    }
