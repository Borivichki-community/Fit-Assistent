package dev.andre.nutritionist.diary.outbox

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.DiaryServiceGrpc
import com.budgetntr.proto.diary.v1.NutritionSummary
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.contracts.avro.CaloriesConsumedEvent
import dev.andre.nutritionist.diary.DiaryApplication
import dev.andre.nutritionist.diary.domain.model.AddEntryCommand
import dev.andre.nutritionist.diary.domain.model.DiaryEntryId
import dev.andre.nutritionist.diary.domain.model.FoodItemId
import dev.andre.nutritionist.diary.domain.model.IdempotencyKey
import dev.andre.nutritionist.diary.domain.model.MealType
import dev.andre.nutritionist.diary.domain.model.UserId
import dev.andre.nutritionist.diary.grpc.idempotency.IdempotencyService
import dev.andre.nutritionist.diary.grpc.idempotency.IdempotencyStore.IdempotencyScope
import dev.andre.nutritionist.diary.service.DiaryService
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.IllegalTransactionStateException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Instant
import dev.andre.nutritionist.diary.domain.model.NutritionSummary as DomainNutritionSummary

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
@DirtiesContext
class DiaryOutboxIntegrationTest
    @Autowired
    constructor(
        private val jdbc: JdbcTemplate,
        private val diary: DiaryService,
        private val idempotency: IdempotencyService,
        grpc: GrpcServerLifecycle,
    ) {
        private val user = UserId(UUID.randomUUID().toString())
        private val channel =
            ManagedChannelBuilder
                .forAddress("127.0.0.1", grpc.port)
                .usePlaintext()
                .build()

        @BeforeEach
        fun reset() {
            jdbc.execute("TRUNCATE outbox_messages, diary_entries, idempotency_keys")
        }

        @AfterAll
        fun close() {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            postgres.stop()
        }

        @Test
        fun `RPC stores meal event response and trace together without Kafka`() {
            val key = UUID.randomUUID().toString()
            val request = request()
            val response = stub(key).addEntry(request)
            assertEquals(response, stub(key).addEntry(request))
            assertCounts(1)

            val row = jdbc.queryForMap("SELECT * FROM outbox_messages")
            val event =
                SpecificDatumReader<CaloriesConsumedEvent>(CaloriesConsumedEvent.getClassSchema()).read(
                    null,
                    DecoderFactory.get().binaryDecoder(row["payload"] as ByteArray, null),
                )
            assertEquals(UUID.fromString(user.raw), event.userId)
            assertEquals(UUID.fromString(request.entryId), event.mealId)
            assertEquals(300, event.calories)
            assertEquals(1735689600000L, event.timestamp.toEpochMilli())
            assertEquals("LUNCH", event.mealType.toString())
            assertEquals("DiaryEntry", row["aggregate_type"])
            assertEquals(request.entryId, row["aggregate_id"])
            assertEquals(user.raw, row["partition_key"])
            assertEquals("diary.entry.created", row["topic"])
            assertEquals("PENDING", row["status"])
            assertEquals(0, row["attempts"])
            assertEquals(TRACEPARENT, row["traceparent"])
            assertNotNull(row["created_at"])
            assertEquals(null, row["published_at"])
        }

        @Test
        fun `outbox failure rolls back meal and response then retry succeeds`() {
            val request = request()
            val key = UUID.randomUUID().toString()
            jdbc.execute("ALTER TABLE outbox_messages ADD CONSTRAINT test_reject_event CHECK (status <> 'PENDING')")
            try {
                assertThrows(StatusRuntimeException::class.java) { stub(key).addEntry(request) }
                assertCounts(0)
            } finally {
                jdbc.execute("ALTER TABLE outbox_messages DROP CONSTRAINT test_reject_event")
            }
            stub(key).addEntry(request)
            assertCounts(1)
        }

        @Test
        fun `failure after both inserts rolls back the complete operation`() {
            val entryId = DiaryEntryId(UUID.randomUUID())
            val key = IdempotencyKey(UUID.randomUUID().toString())
            val scope = IdempotencyScope(user, "test-add-entry", key)
            val command =
                AddEntryCommand(
                    entryId,
                    user,
                    key,
                    FoodItemId(UUID.randomUUID()),
                    "Rice",
                    100.0,
                    DomainNutritionSummary(300, 5.0, 2.0, 60.0),
                    MealType.LUNCH,
                    Instant.parse("2025-01-01T00:00:00Z"),
                )
            assertThrows(IllegalStateException::class.java) {
                idempotency.execute(scope, byteArrayOf(1)) {
                    diary.addEntry(command)
                    error("Response construction failed")
                }
            }
            assertCounts(0)
            idempotency.execute(scope, byteArrayOf(1)) {
                diary.addEntry(command)
                byteArrayOf(2)
            }
            assertCounts(1)
        }

        @Test
        fun `concurrent RPC retries create one event`() {
            val request = request()
            val client = stub(UUID.randomUUID().toString())
            val start = CountDownLatch(1)
            Executors.newFixedThreadPool(8).use { executor ->
                val futures =
                    List(8) {
                        executor.submit<com.budgetntr.proto.diary.v1.AddEntryResponse> {
                            check(start.await(10, TimeUnit.SECONDS))
                            client.addEntry(request)
                        }
                    }
                start.countDown()
                val responses = futures.map { it.get(30, TimeUnit.SECONDS) }
                assertEquals(1, responses.distinct().size)
            }
            assertCounts(1)
        }

        @Test
        fun `conflicting body and new key for existing meal do not add events`() {
            val request = request()
            val client = stub(UUID.randomUUID().toString())
            client.addEntry(request)
            assertEquals(
                Status.Code.ALREADY_EXISTS,
                assertThrows(StatusRuntimeException::class.java) {
                    client.addEntry(request.toBuilder().setFoodName("Changed").build())
                }.status.code,
            )
            assertThrows(StatusRuntimeException::class.java) {
                stub(UUID.randomUUID().toString()).addEntry(request)
            }
            assertCounts(1)
        }

        @Test
        fun `invalid meal and invocation outside a transaction create nothing`() {
            assertEquals(
                Status.Code.INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException::class.java) {
                    stub(UUID.randomUUID().toString()).addEntry(request().toBuilder().setQuantityGrams(-1.0).build())
                }.status.code,
            )
            assertThrows(IllegalTransactionStateException::class.java) {
                diary.deleteMeal(user, DiaryEntryId(UUID.randomUUID()))
            }
            assertCounts(0)
        }

        private fun assertCounts(expected: Int) {
            listOf("diary_entries", "outbox_messages", "idempotency_keys").forEach { table ->
                assertEquals(expected, jdbc.queryForObject("SELECT COUNT(*) FROM $table", Int::class.java), table)
            }
        }

        private fun request(): AddEntryRequest =
            AddEntryRequest
                .newBuilder()
                .setEntryId(UUID.randomUUID().toString())
                .setFoodItemId(UUID.randomUUID().toString())
                .setFoodName("Rice")
                .setQuantityGrams(100.0)
                .setMealType("LUNCH")
                .setRecordedAt(Timestamp.newBuilder().setSeconds(1735689600).build())
                .setNutrition(
                    NutritionSummary
                        .newBuilder()
                        .setCalories(300)
                        .setProteins(5.0)
                        .setFats(2.0)
                        .setCarbs(60.0)
                        .build(),
                ).build()

        private fun stub(key: String): DiaryServiceGrpc.DiaryServiceBlockingStub {
            val headers =
                Metadata().apply {
                    put(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER), user.raw)
                    put(Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER), key)
                    put(Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER), TRACEPARENT)
                }
            return DiaryServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .withDeadlineAfter(20, TimeUnit.SECONDS)
        }

        companion object {
            private const val TRACEPARENT = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
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
