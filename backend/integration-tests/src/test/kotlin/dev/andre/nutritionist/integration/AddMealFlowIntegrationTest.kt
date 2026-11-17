package dev.andre.nutritionist.integration

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.DiaryServiceGrpc
import com.budgetntr.proto.diary.v1.NutritionSummary
import com.budgetntr.proto.profile.v1.GetDailyCaloriesBalanceRequest
import com.budgetntr.proto.profile.v1.ProfileServiceGrpc
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.contracts.avro.CaloriesConsumedEvent
import dev.andre.nutritionist.integration.support.IntegrationEnvironment
import dev.andre.nutritionist.integration.support.IntegrationEnvironment.Companion.TOPIC
import dev.andre.nutritionist.integration.support.TestUserId
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.Connection
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@JvmInline
value class RequestKey(
    val raw: String,
)

@Timeout(60)
class AddMealFlowIntegrationTest {
    @Test
    fun `AddEntry updates Profile balance and repeated request has no duplicate effects`() {
        val user = newUser()
        val request = request()
        val key = newKey()
        assertBalance(user, 0, 2000)
        val first = diary(user, key).addEntry(request)

        awaitBalance(user, 350, 1650)
        assertPersisted(user, 1)
        val event = eventFor(user)
        KafkaAvroDeserializer().use { decoder ->
            decoder.configure(mapOf("schema.registry.url" to environment.registryUrl, "specific.avro.reader" to true), false)
            val decoded = decoder.deserialize(TOPIC, event.value()) as CaloriesConsumedEvent
            assertEquals(user.raw, decoded.userId)
            assertEquals(UUID.fromString(request.entryId), decoded.mealId)
            assertEquals(350, decoded.calories)
            assertEquals(Instant.ofEpochSecond(request.recordedAt.seconds), decoded.timestamp)
            assertEquals("LUNCH", decoded.mealType.toString())
        }
        val subject =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build()
                .send(
                    HttpRequest
                        .newBuilder(
                            URI.create("${environment.registryUrl}/subjects/$TOPIC-value/versions/latest"),
                        ).timeout(Duration.ofSeconds(3))
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.ofString(),
                )
        assertEquals(200, subject.statusCode())

        assertEquals(first, diary(user, key).addEntry(request))
        assertPersisted(user, 1)
        assertBalance(user, 350, 1650)
    }

    @Test
    fun `redelivery of the actual Diary event is committed without counting calories twice`() {
        val user = newUser()
        diary(user, newKey()).addEntry(request())
        awaitBalance(user, 350, 1650)
        val original = eventFor(user)
        val replay = ProducerRecord(TOPIC, original.partition(), original.key(), original.value())
        original.headers().forEach { replay.headers().add(it) }
        val metadata =
            KafkaProducer(
                mapOf(
                    "bootstrap.servers" to environment.kafka.bootstrapServers,
                    "max.block.ms" to 5000,
                    "delivery.timeout.ms" to 10000,
                    "request.timeout.ms" to 5000,
                ),
                ByteArraySerializer(),
                ByteArraySerializer(),
            ).use { producer -> producer.send(replay).get(10, TimeUnit.SECONDS) }

        environment.awaitCommitted(metadata.partition(), metadata.offset())
        assertPersisted(user, 1)
        assertBalance(user, 350, 1650)
    }

    @Test
    fun `concurrent retries return the same result and create one entry and one event`() {
        val user = newUser()
        val request = request()
        val key = newKey()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(4)
        try {
            val results =
                (1..4).map {
                    executor.submit<com.budgetntr.proto.diary.v1.AddEntryResponse> {
                        check(start.await(5, TimeUnit.SECONDS))
                        diary(user, key).addEntry(request)
                    }
                }
            start.countDown()
            val responses = results.map { it.get(15, TimeUnit.SECONDS) }
            assertEquals(1, responses.distinct().size)
            assertEquals(request.entryId, responses.first().entry.id)
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(3, TimeUnit.SECONDS)
        }
        awaitBalance(user, 350, 1650)
        assertPersisted(user, 1)
    }

    @Test
    fun `conflicting key has no effects while a new meal is counted independently`() {
        val user = newUser()
        val key = newKey()
        val first = request()
        diary(user, key).addEntry(first)
        awaitBalance(user, 350, 1650)
        val conflict =
            assertThrows(StatusRuntimeException::class.java) {
                diary(user, key).addEntry(first.toBuilder().setFoodName("Different request").build())
            }
        assertEquals(Status.Code.ALREADY_EXISTS, conflict.status.code)
        assertPersisted(user, 1)
        assertBalance(user, 350, 1650)

        diary(user, newKey()).addEntry(request(150))
        awaitBalance(user, 500, 1500)
        assertPersisted(user, 2)
    }

    private fun newUser(): TestUserId = TestUserId(UUID.randomUUID()).also(environment::seedProfile)

    private fun newKey(): RequestKey = RequestKey(UUID.randomUUID().toString())

    private fun request(calories: Int = 350): AddEntryRequest =
        AddEntryRequest
            .newBuilder()
            .setEntryId(UUID.randomUUID().toString())
            .setFoodItemId(UUID.randomUUID().toString())
            .setFoodName("Rice")
            .setQuantityGrams(100.0)
            .setMealType("LUNCH")
            .setRecordedAt(Timestamp.newBuilder().setSeconds(Instant.parse("2026-11-17T10:00:00Z").epochSecond))
            .setNutrition(
                NutritionSummary
                    .newBuilder()
                    .setCalories(calories)
                    .setProteins(5.0)
                    .setFats(2.0)
                    .setCarbs(60.0),
            ).build()

    private fun headers(user: TestUserId): Metadata =
        Metadata().apply {
            put(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER), user.raw.toString())
        }

    private fun diary(
        user: TestUserId,
        key: RequestKey,
    ): DiaryServiceGrpc.DiaryServiceBlockingStub =
        DiaryServiceGrpc
            .newBlockingStub(environment.diary.channel)
            .withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(
                    headers(user).apply {
                        put(Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER), key.raw)
                    },
                ),
            ).withDeadlineAfter(10, TimeUnit.SECONDS)

    private fun assertBalance(
        user: TestUserId,
        consumed: Long,
        remaining: Long,
    ) {
        val balance =
            ProfileServiceGrpc
                .newBlockingStub(environment.profile.channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers(user)))
                .withDeadlineAfter(3, TimeUnit.SECONDS)
                .getDailyCaloriesBalance(
                    GetDailyCaloriesBalanceRequest
                        .newBuilder()
                        .setUserId(user.raw.toString())
                        .setDate("2026-11-17")
                        .setTimeZone("Europe/Moscow")
                        .build(),
                )
        assertEquals(2000, balance.targetCalories)
        assertEquals(consumed, balance.consumedCalories)
        assertEquals(remaining, balance.remainingCalories)
    }

    private fun awaitBalance(
        user: TestUserId,
        consumed: Long,
        remaining: Long,
    ) {
        await().atMost(Duration.ofSeconds(20)).untilAsserted { assertBalance(user, consumed, remaining) }
    }

    private fun assertPersisted(
        user: TestUserId,
        expected: Int,
    ) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            environment.diaryConnection().use { connection ->
                assertEquals(expected, count(connection, "SELECT COUNT(*) FROM diary_entries WHERE user_id = ?", user))
                assertEquals(expected, count(connection, "SELECT COUNT(*) FROM idempotency_keys WHERE user_id = ?", user))
                assertEquals(
                    expected,
                    count(
                        connection,
                        "SELECT COUNT(*) FROM outbox_messages WHERE partition_key = ? AND status = 'PUBLISHED'",
                        user,
                    ),
                )
                assertEquals(expected, count(connection, "SELECT COUNT(*) FROM outbox_messages WHERE partition_key = ?", user))
            }
            environment.profileConnection().use { connection ->
                assertEquals(expected, count(connection, "SELECT COUNT(*) FROM processed_events WHERE user_id::text = ?", user))
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*) FROM outbox_messages").use { result ->
                        check(result.next())
                        assertEquals(0, result.getInt(1), "Unexpected Profile DLT outbox entry")
                    }
                }
            }
        }
    }

    private fun count(
        connection: Connection,
        sql: String,
        user: TestUserId,
    ): Int =
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, user.raw.toString())
            statement.executeQuery().use { result ->
                check(result.next())
                result.getInt(1)
            }
        }

    private fun eventFor(user: TestUserId): ConsumerRecord<ByteArray, ByteArray> =
        KafkaConsumer(
            mapOf(
                "bootstrap.servers" to environment.kafka.bootstrapServers,
                "group.id" to "t057-observer-${UUID.randomUUID()}",
                "enable.auto.commit" to false,
                "isolation.level" to "read_committed",
                "default.api.timeout.ms" to 5000,
            ),
            ByteArrayDeserializer(),
            ByteArrayDeserializer(),
        ).use { consumer ->
            val partitions = (0..2).map { TopicPartition(TOPIC, it) }
            consumer.assign(partitions)
            consumer.seekToBeginning(partitions)
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            val record =
                generateSequence { consumer.poll(Duration.ofMillis(200)) }
                    .takeWhile { System.nanoTime() < deadline }
                    .flatMap { it.asSequence() }
                    .firstOrNull { it.key().toString(Charsets.UTF_8) == user.raw.toString() }
            checkNotNull(record) { "Diary event not found for ${user.raw}" }
        }

    companion object {
        @JvmField
        @RegisterExtension
        val environment = IntegrationEnvironment()
    }
}
