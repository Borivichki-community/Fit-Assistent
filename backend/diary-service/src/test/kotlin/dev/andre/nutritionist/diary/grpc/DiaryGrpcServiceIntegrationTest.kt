package dev.andre.nutritionist.diary.grpc

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.AddEntryResponse
import com.budgetntr.proto.diary.v1.DeleteMealRequest
import com.budgetntr.proto.diary.v1.DiaryServiceGrpc
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetRangeRequest
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.diary.DiaryApplication
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.budgetntr.proto.diary.v1.CostSnapshot as CostSnapshotProto
import com.budgetntr.proto.diary.v1.Money as MoneyProto
import com.budgetntr.proto.diary.v1.NutritionSummary as NutritionSummaryProto

@SpringBootTest(
    classes = [DiaryApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = ["spring.grpc.server.port=0", "diary.kafka.registration.enabled=false"],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
class DiaryGrpcServiceIntegrationTest
    @Autowired
    constructor(
        lifecycle: GrpcServerLifecycle,
        private val jdbc: JdbcTemplate,
        private val scheduler: ScheduledAnnotationBeanPostProcessor,
    ) {
        private val channel =
            ManagedChannelBuilder
                .forAddress("127.0.0.1", lifecycle.port)
                .usePlaintext()
                .build()

        @BeforeEach
        fun clearDatabase() {
            jdbc.update("DELETE FROM idempotency_keys")
            jdbc.update("DELETE FROM diary_entries")
        }

        @AfterAll
        fun closeResources() {
            channel.shutdownNow()
            postgres.stop()
        }

        @Test
        fun `cleanup task is registered by the existing configuration`() {
            client("alice", "expired").addEntry(entry())
            jdbc.update("UPDATE idempotency_keys SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'")
            val task = scheduler.scheduledTasks.single { it.task.toString().endsWith("IdempotencyCleanup.cleanUp") }

            task.task.runnable.run()

            assertEquals(0, count("idempotency_keys"))
            assertEquals(1, count("diary_entries"))
        }

        @Test
        fun `add repeat and daily totals work through grpc and jpa`() {
            val request = entry()
            val cost =
                CostSnapshotProto
                    .newBuilder()
                    .setTotal(MoneyProto.newBuilder().setAmountMinor(12345).setCurrencyCode("RUB"))
                    .setUnitPrice(MoneyProto.newBuilder().setAmountMinor(8230).setCurrencyCode("RUB"))
                    .setPriceBasisMilligrams(100000)
            val withCost = request.toBuilder().setCost(cost).build()
            val first = client("alice", "add-key").addEntry(withCost)
            val repeated = client("alice", "add-key").addEntry(withCost)
            val day = client("alice").getDailyEntries(day("2026-11-15", "UTC"))

            assertEquals(first, repeated)
            assertEquals(request.entryId, first.entry.id)
            assertEquals(12345L, first.entry.cost.total.amountMinor)
            assertEquals(8230L, first.entry.cost.unitPrice.amountMinor)
            assertEquals(1, day.entriesCount)
            assertEquals(150, day.dailyTotal.calories)
            assertEquals(5.0, day.dailyTotal.proteins)
            assertEquals(1, count("diary_entries"))
            assertEquals(1, count("idempotency_keys"))

            val unknown = client("alice", "unknown-cost").addEntry(entry())
            val unknownCost =
                client("alice").getDailyEntries(day("2026-11-15", "UTC")).entriesList.single { it.id == unknown.entry.id }
            assertFalse(unknownCost.hasCost())
        }

        @Test
        fun `same key with another body returns already exists`() {
            val request = entry()
            client("alice", "same-key").addEntry(request)

            assertStatus(Status.Code.ALREADY_EXISTS) {
                client("alice", "same-key").addEntry(request.toBuilder().setQuantityGrams(300.0).build())
            }
            assertEquals(1, count("diary_entries"))
            assertEquals(1, count("idempotency_keys"))
        }

        @Test
        fun `delete is idempotent and isolated by user`() {
            val added = client("alice", "add").addEntry(entry())
            val deletion = DeleteMealRequest.newBuilder().setEntryId(added.entry.id).build()

            assertTrue(client("bob").getDailyEntries(day("2026-11-15", "UTC")).entriesList.isEmpty())
            assertStatus(Status.Code.NOT_FOUND) { client("bob", "delete").deleteMeal(deletion) }
            assertEquals(1, count("diary_entries"))
            assertEquals(1, count("idempotency_keys"))

            val first = client("alice", "delete").deleteMeal(deletion)
            assertEquals(first, client("alice", "delete").deleteMeal(deletion))
            assertEquals(0, count("diary_entries"))
            assertEquals(2, count("idempotency_keys"))
        }

        @Test
        fun `range includes both dates and respects daylight saving boundaries`() {
            listOf(
                "2026-03-28T22:59:59Z",
                "2026-03-28T23:00:00Z",
                "2026-03-29T21:59:59Z",
                "2026-03-29T22:00:00Z",
            ).forEachIndexed { index, timestamp ->
                client("alice", "add-$index").addEntry(entry(timestamp))
            }

            val day = client("alice").getDailyEntries(day("2026-03-29", "Europe/Berlin"))
            assertEquals(2, day.entriesCount)
            assertEquals(300, day.dailyTotal.calories)

            val range =
                GetRangeRequest
                    .newBuilder()
                    .setStartDate("2026-03-28")
                    .setEndDate("2026-03-29")
                    .setTimeZone("Europe/Berlin")
                    .build()
            assertEquals(3, client("alice").getRange(range).entriesCount)
            assertEquals(0, client("bob").getRange(range).entriesCount)
        }

        @Test
        fun `user context and mutation key are required`() {
            assertStatus(Status.Code.UNAUTHENTICATED) {
                DiaryServiceGrpc.newBlockingStub(channel).getDailyEntries(day("2026-11-15", "UTC"))
            }
            assertStatus(Status.Code.INVALID_ARGUMENT) { client("alice").addEntry(entry()) }
            assertStatus(Status.Code.INVALID_ARGUMENT) {
                client("alice").deleteMeal(DeleteMealRequest.newBuilder().setEntryId(UUID.randomUUID().toString()).build())
            }
            assertEquals(0, client("alice").getDailyEntries(day("2026-11-15", "UTC")).entriesCount)
            assertEquals(0, count("idempotency_keys"))
        }

        @Test
        fun `invalid requests return invalid argument and do not reserve keys`() {
            val request = entry()
            assertStatus(Status.Code.INVALID_ARGUMENT) {
                client("alice", "negative").addEntry(request.toBuilder().setQuantityGrams(-1.0).build())
            }
            assertStatus(Status.Code.INVALID_ARGUMENT) {
                client("alice", "nan").addEntry(request.toBuilder().setQuantityGrams(Double.NaN).build())
            }
            assertStatus(Status.Code.INVALID_ARGUMENT) {
                client("alice", "uuid").addEntry(request.toBuilder().setEntryId("invalid").build())
            }
            assertStatus(Status.Code.INVALID_ARGUMENT) {
                client("alice", "meal").addEntry(request.toBuilder().setMealType("INVALID").build())
            }
            assertStatus(Status.Code.INVALID_ARGUMENT) {
                client("alice").getDailyEntries(day("2026-11-15", "Invalid/Zone"))
            }
            assertStatus(Status.Code.INVALID_ARGUMENT) {
                client("alice").getRange(
                    GetRangeRequest
                        .newBuilder()
                        .setStartDate("2026-11-16")
                        .setEndDate("2026-11-15")
                        .setTimeZone("UTC")
                        .build(),
                )
            }
            assertEquals(0, count("diary_entries"))
            assertEquals(0, count("idempotency_keys"))
        }

        @Test
        fun `database failure rolls back the new key`() {
            val request = entry()
            val original = client("alice", "original").addEntry(request)

            assertStatus(Status.Code.ALREADY_EXISTS) { client("alice", "another-key").addEntry(request) }

            assertEquals(original, client("alice", "original").addEntry(request))
            assertEquals(1, count("diary_entries"))
            assertEquals(1, count("idempotency_keys"))
        }

        @Test
        fun `parallel grpc requests return one stored response`() {
            val request = entry()
            val workers = 8
            val ready = CountDownLatch(workers)
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val futures =
                    List(workers) {
                        executor.submit<AddEntryResponse> {
                            ready.countDown()
                            check(start.await(10, TimeUnit.SECONDS))
                            client("alice", "parallel").addEntry(request)
                        }
                    }
                assertTrue(ready.await(10, TimeUnit.SECONDS))
                start.countDown()
                val responses = futures.map { it.get(30, TimeUnit.SECONDS) }

                responses.forEach { assertEquals(responses.first(), it) }
                assertEquals(1, count("diary_entries"))
                assertEquals(1, count("idempotency_keys"))
            } finally {
                start.countDown()
                executor.shutdownNow()
            }
        }

        private fun client(
            userId: String,
            key: String? = null,
        ): DiaryServiceGrpc.DiaryServiceBlockingStub {
            val resolvedUserId =
                runCatching { UUID.fromString(userId).toString() }.getOrElse {
                    UUID.nameUUIDFromBytes(userId.toByteArray()).toString()
                }
            val headers =
                Metadata().apply {
                    put(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER), resolvedUserId)
                    key?.let { put(Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER), it) }
                }
            return DiaryServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(20, TimeUnit.SECONDS)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
        }

        private fun entry(timestamp: String = "2026-11-15T12:00:00Z"): AddEntryRequest {
            val instant = Instant.parse(timestamp)
            return AddEntryRequest
                .newBuilder()
                .setEntryId(UUID.randomUUID().toString())
                .setFoodItemId(UUID.randomUUID().toString())
                .setFoodName("Oatmeal")
                .setQuantityGrams(200.0)
                .setMealType("BREAKFAST")
                .setRecordedAt(Timestamp.newBuilder().setSeconds(instant.epochSecond).setNanos(instant.nano))
                .setNutrition(
                    NutritionSummaryProto
                        .newBuilder()
                        .setCalories(150)
                        .setProteins(5.0)
                        .setFats(3.0)
                        .setCarbs(25.0),
                ).build()
        }

        private fun day(
            date: String,
            timeZone: String,
        ): GetDailyEntriesRequest =
            GetDailyEntriesRequest
                .newBuilder()
                .setDate(date)
                .setTimeZone(timeZone)
                .build()

        private fun count(table: String): Int = checkNotNull(jdbc.queryForObject("SELECT COUNT(*) FROM $table", Int::class.java))

        private fun assertStatus(
            expected: Status.Code,
            action: () -> Unit,
        ) {
            assertEquals(expected, assertThrows(StatusRuntimeException::class.java, action).status.code)
        }

        companion object {
            private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).apply { start() }

            @JvmStatic
            @DynamicPropertySource
            fun databaseProperties(registry: DynamicPropertyRegistry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl)
                registry.add("spring.datasource.username", postgres::getUsername)
                registry.add("spring.datasource.password", postgres::getPassword)
            }
        }
    }
