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
import dev.andre.nutritionist.diary.service.DiaryService
import dev.andre.nutritionist.outbox.JdbcOutboxStore
import dev.andre.nutritionist.outbox.OutboxMessage
import dev.andre.nutritionist.outbox.OutboxMessageId
import dev.andre.nutritionist.outbox.OutboxProcessor
import dev.andre.nutritionist.outbox.OutboxPublisher
import dev.andre.nutritionist.outbox.OutboxSettings
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.ProducerFencedException
import org.apache.kafka.common.serialization.StringDeserializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.MapPropertySource
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.DefaultTransactionIdSuffixStrategy
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.io.EOFException
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import javax.sql.DataSource
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
class DiaryOutboxPublishingIntegrationTest
    @Autowired
    constructor(
        private val jdbc: JdbcTemplate,
        private val publisher: OutboxPublisher,
        private val processor: OutboxProcessor,
        private val store: JdbcOutboxStore,
        private val diary: DiaryService,
        private val dataSource: DataSource,
        private val producerFactory: DefaultKafkaProducerFactory<String, CaloriesConsumedEvent>,
        private val transactionManager: PlatformTransactionManager,
        grpc: GrpcServerLifecycle,
    ) {
        private val userId = UUID.randomUUID()
        private val channel =
            ManagedChannelBuilder
                .forAddress("127.0.0.1", grpc.port)
                .usePlaintext()
                .build()

        @BeforeAll
        fun createTopic() {
            Admin.create(mapOf("bootstrap.servers" to kafka.bootstrapServers)).use { admin ->
                admin.createTopics(listOf(NewTopic(TOPIC, 3, 1.toShort()))).all().get(20, TimeUnit.SECONDS)
            }
        }

        @BeforeEach
        fun reset() {
            jdbc.execute("TRUNCATE outbox_messages, diary_entries, idempotency_keys")
        }

        @AfterAll
        fun close() {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            registry.stop()
            kafka.stop()
            postgres.stop()
            network.close()
        }

        @Test
        fun `RPC snapshot publishes as specific Avro with user key and traceparent`() {
            val request = request()
            val client = stub(TRACEPARENT)
            val response = client.addEntry(request)
            assertEquals(response, client.addEntry(request))
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages", Int::class.java))
            val message = pendingMessage()
            jdbc.update("DELETE FROM diary_entries WHERE id = ?", UUID.fromString(request.entryId))

            consumer().use { consumer ->
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive())
                publisher.publish(message).get(20, TimeUnit.SECONDS)
                val record = receive(consumer)
                val event = record.value()
                assertEquals(userId.toString(), record.key())
                assertEquals(userId, event.userId)
                assertEquals(UUID.fromString(request.entryId), event.mealId)
                assertEquals(300, event.calories)
                assertEquals(1735689600000L, event.timestamp.toEpochMilli())
                assertEquals("LUNCH", event.mealType.toString())
                assertNull(event.notes)
                assertEquals(
                    TRACEPARENT,
                    record
                        .headers()
                        .lastHeader("traceparent")
                        .value()
                        .toString(Charsets.UTF_8),
                )
            }
            assertPending(message)
        }

        @Test
        fun `publication without traceparent does not invent a header`() {
            stub(null).addEntry(request())
            val message = pendingMessage()
            consumer().use { consumer ->
                publisher.publish(message).get(20, TimeUnit.SECONDS)
                val record = receive(consumer)
                assertEquals(userId.toString(), record.key())
                assertNull(record.headers().lastHeader("traceparent"))
            }
            assertPending(message)
        }

        @Test
        fun `uppercase UUID produces the same canonical partition key`() {
            val originalKey = userId.toString().uppercase()
            stub(TRACEPARENT, UserId(originalKey)).addEntry(request())
            val message = pendingMessage()
            assertEquals(userId.toString(), message.partitionKey)
            consumer().use { consumer ->
                publisher.publish(message).get(20, TimeUnit.SECONDS)
                val record = receive(consumer)
                assertEquals(userId.toString(), record.key())
                assertEquals(userId, record.value().userId)
            }
            assertPending(message)
        }

        @Test
        fun `publication in database transaction is rejected before sending`() {
            stub(TRACEPARENT).addEntry(request())
            val message = pendingMessage()
            consumer().use { consumer ->
                TransactionTemplate(transactionManager).executeWithoutResult {
                    assertThrows(IllegalStateException::class.java) { publisher.publish(message) }
                }
                assertTrue(consumer.poll(Duration.ofMillis(500)).isEmpty)
            }
            assertPending(message)
        }

        @Test
        fun `invalid topic key and payload are rejected without sending`() {
            stub(TRACEPARENT).addEntry(request())
            val message = pendingMessage()
            consumer().use { consumer ->
                assertThrows(IllegalArgumentException::class.java) {
                    publisher.publish(message.copy(topic = "wrong.topic"))
                }
                assertThrows(IllegalArgumentException::class.java) {
                    publisher.publish(message.copy(partitionKey = UUID.randomUUID().toString()))
                }
                assertThrows(EOFException::class.java) {
                    publisher.publish(message.copy(payload = message.payload.copyOf(message.payload.size - 1)))
                }
                assertThrows(IllegalArgumentException::class.java) {
                    publisher.publish(message.copy(payload = message.payload + byteArrayOf(0)))
                }
                assertTrue(consumer.poll(Duration.ofMillis(500)).isEmpty)
            }
            assertPending(message)
        }

        @Test
        fun `processor publishes FIFO with canonical user key regardless of recorded time`() {
            val first = request()
            val second = request().toBuilder().setRecordedAt(Timestamp.newBuilder().setSeconds(1600000000).build()).build()
            stub(TRACEPARENT, UserId(userId.toString().uppercase())).addEntry(first)
            stub(TRACEPARENT).addEntry(second)
            consumer().use { consumer ->
                assertEquals(2, processor.processBatch())
                val records = receiveMany(consumer, 2)
                assertEquals(listOf(first.entryId, second.entryId), records.map { it.value().mealId.toString() })
                assertEquals(listOf(userId.toString(), userId.toString()), records.map { it.key() })
                assertEquals(1, records.map { it.partition() }.distinct().size)
            }
            assertEquals(2, publishedCount())
            assertEquals(0, processor.processBatch())
        }

        @Test
        fun `ACK gate prevents PUBLISHED and a competing processor until completion`() {
            val first = request()
            val second = request()
            stub(TRACEPARENT).addEntry(first)
            stub(TRACEPARENT).addEntry(second)
            val gate = CompletableFuture<Unit>()
            val gatedPublisher =
                OutboxPublisher { message, verifyOwnership ->
                    publisher.publish(message, verifyOwnership).thenCompose { gate }
                }
            val primary = OutboxProcessor(store, gatedPublisher)
            val competing = OutboxProcessor(JdbcOutboxStore(dataSource), publisher)
            consumer().use { consumer ->
                Executors.newSingleThreadExecutor().use { executor ->
                    val task = executor.submit<Int> { primary.processBatch() }
                    try {
                        assertEquals(first.entryId, receive(consumer).value().mealId.toString())
                        assertEquals(0, publishedCount())
                        assertEquals(0, competing.processBatch())
                        assertTrue(consumer.poll(Duration.ofMillis(200)).isEmpty)
                    } finally {
                        gate.complete(Unit)
                    }
                    assertEquals(2, task.get(20, TimeUnit.SECONDS))
                    assertEquals(second.entryId, receive(consumer).value().mealId.toString())
                }
            }
            assertEquals(0, competing.processBatch())
        }

        @Test
        fun `failed head blocks its user but not others and can be retried`() {
            val first = request()
            val second = request()
            val other = request()
            stub(TRACEPARENT).addEntry(first)
            stub(TRACEPARENT).addEntry(second)
            stub(TRACEPARENT, UserId(UUID.randomUUID().toString())).addEntry(other)
            val fail = AtomicBoolean(true)
            val unreliable =
                OutboxPublisher { message, verifyOwnership ->
                    if (message.partitionKey == userId.toString() && fail.get()) {
                        CompletableFuture.failedFuture(IllegalStateException("Injected failure"))
                    } else {
                        publisher.publish(message, verifyOwnership)
                    }
                }
            val settings = OutboxSettings(100, 2, Duration.ofHours(1), Duration.ofHours(1))
            val relay = OutboxProcessor(store, unreliable, settings)
            consumer().use { consumer ->
                assertEquals(2, relay.processBatch())
                assertEquals(other.entryId, receive(consumer).value().mealId.toString())
                assertEquals(0, relay.processBatch())
                assertEquals("PENDING", status(first))
                assertEquals("PENDING", status(second))
                jdbc.update("UPDATE outbox_messages SET next_attempt_at = CURRENT_TIMESTAMP WHERE aggregate_id = ?", first.entryId)
                assertEquals(1, relay.processBatch())
                assertEquals("FAILED", status(first))
                assertEquals("PENDING", status(second))
                assertEquals(0, relay.processBatch())
                val id =
                    OutboxMessageId(
                        checkNotNull(
                            jdbc.queryForObject("SELECT id FROM outbox_messages WHERE aggregate_id = ?", UUID::class.java, first.entryId),
                        ),
                    )
                assertTrue(store.retryFailed(id))
                fail.set(false)
                assertEquals(2, relay.processBatch())
                assertEquals(listOf(first.entryId, second.entryId), receiveMany(consumer, 2).map { it.value().mealId.toString() })
                assertFalse(store.retryFailed(id))
            }
        }

        @Test
        fun `failure to persist ACK replays the same event before the next after relay restart`() {
            val first = request()
            val second = request()
            stub(TRACEPARENT).addEntry(first)
            stub(TRACEPARENT).addEntry(second)
            val crashAfterAck =
                OutboxPublisher { message, verifyOwnership ->
                    publisher.publish(message, verifyOwnership).thenApply {
                        jdbc.execute("ALTER TABLE outbox_messages ADD CONSTRAINT test_reject_ack CHECK (status <> 'PUBLISHED')")
                    }
                }
            consumer().use { consumer ->
                try {
                    assertThrows(SQLException::class.java) { OutboxProcessor(store, crashAfterAck).processBatch() }
                    assertEquals(first.entryId, receive(consumer).value().mealId.toString())
                    assertEquals("PENDING", status(first))
                    assertEquals("PENDING", status(second))
                } finally {
                    jdbc.execute("ALTER TABLE outbox_messages DROP CONSTRAINT IF EXISTS test_reject_ack")
                }
                val restarted = OutboxProcessor(JdbcOutboxStore(dataSource), publisher)
                assertEquals(2, restarted.processBatch())
                assertEquals(listOf(first.entryId, second.entryId), receiveMany(consumer, 2).map { it.value().mealId.toString() })
            }
        }

        @Test
        fun `broker outage preserves pending payload and recovers automatically on next batch`() {
            stub(TRACEPARENT).addEntry(request())
            val message = pendingMessage()
            consumer().use { consumer ->
                kafka.dockerClient.pauseContainerCmd(kafka.containerId).exec()
                try {
                    assertEquals(1, processor.processBatch())
                    assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM outbox_messages", String::class.java))
                    assertEquals(1, jdbc.queryForObject("SELECT attempts FROM outbox_messages", Int::class.java))
                    assertPayloadUnchanged(message)
                } finally {
                    kafka.dockerClient.unpauseContainerCmd(kafka.containerId).exec()
                }
                jdbc.execute("UPDATE outbox_messages SET next_attempt_at = CURRENT_TIMESTAMP")
                assertEquals(1, processor.processBatch())
                assertEquals(userId, receive(consumer).value().userId)
                assertEquals("PUBLISHED", jdbc.queryForObject("SELECT status FROM outbox_messages", String::class.java))
            }
        }

        @Test
        fun `Schema Registry outage retries stored Avro without changing the snapshot`() {
            stub(TRACEPARENT).addEntry(request())
            val message = pendingMessage()
            producerFactory.reset()
            consumer().use { consumer ->
                registry.dockerClient.pauseContainerCmd(registry.containerId).exec()
                try {
                    assertEquals(1, processor.processBatch())
                    assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM outbox_messages", String::class.java))
                    assertEquals(1, jdbc.queryForObject("SELECT attempts FROM outbox_messages", Int::class.java))
                } finally {
                    registry.dockerClient.unpauseContainerCmd(registry.containerId).exec()
                }
                jdbc.execute("UPDATE outbox_messages SET next_attempt_at = CURRENT_TIMESTAMP")
                assertEquals(1, processor.processBatch())
                assertEquals(userId, receive(consumer).value().userId)
                assertPayloadUnchanged(message)
            }
        }

        @Test
        fun `Spring scheduled relay automatically publishes committed RPC events`() {
            consumer().use { consumer ->
                AnnotationConfigApplicationContext().use { context ->
                    context.environment.propertySources.addFirst(
                        MapPropertySource(
                            "outbox-test",
                            mapOf("diary.outbox.enabled" to "true", "diary.outbox.poll-interval" to "PT0.05S"),
                        ),
                    )
                    context.registerBean(OutboxProcessor::class.java, Supplier { processor })
                    context.register(SchedulingConfiguration::class.java, DiaryOutboxScheduler::class.java)
                    context.refresh()
                    val entry = request()
                    stub(TRACEPARENT).addEntry(entry)
                    assertEquals(entry.entryId, receive(consumer).value().mealId.toString())
                    await().atMost(Duration.ofSeconds(10)).untilAsserted { assertEquals("PUBLISHED", status(entry)) }
                }
            }
        }

        @Test
        fun `concurrent appends serialize user commit order and sequence beats timestamps`() {
            val first = command()
            val second = command()
            val inserted = CountDownLatch(1)
            val release = CountDownLatch(1)
            Executors.newFixedThreadPool(2).use { executor ->
                val one =
                    executor.submit {
                        TransactionTemplate(transactionManager).executeWithoutResult {
                            diary.addEntry(first)
                            inserted.countDown()
                            check(release.await(20, TimeUnit.SECONDS))
                        }
                    }
                try {
                    check(inserted.await(20, TimeUnit.SECONDS))
                    val two =
                        executor.submit {
                            TransactionTemplate(transactionManager).executeWithoutResult { diary.addEntry(second) }
                        }
                    await().atMost(Duration.ofSeconds(10)).until {
                        val waiting =
                            jdbc.queryForObject(
                                "SELECT COUNT(*) FROM pg_locks WHERE locktype = 'advisory' AND NOT granted",
                                Int::class.java,
                            )
                        (waiting ?: 0) > 0
                    }
                    assertFalse(two.isDone)
                    release.countDown()
                    one.get(20, TimeUnit.SECONDS)
                    two.get(20, TimeUnit.SECONDS)
                } finally {
                    release.countDown()
                }
            }
            jdbc.update(
                "UPDATE outbox_messages SET created_at = CURRENT_TIMESTAMP + INTERVAL '1 day' WHERE aggregate_id = ?",
                first.entryId.raw.toString(),
            )
            consumer().use { consumer ->
                assertEquals(2, processor.processBatch())
                assertEquals(listOf(first.entryId.raw, second.entryId.raw), receiveMany(consumer, 2).map { it.value().mealId })
            }
        }

        @Test
        fun `takeover after database disconnect fences the old Kafka producer`() {
            stub(TRACEPARENT).addEntry(request())
            val message = pendingMessage()
            val event =
                SpecificDatumReader<CaloriesConsumedEvent>(CaloriesConsumedEvent.getClassSchema()).read(
                    null,
                    DecoderFactory.get().binaryDecoder(message.payload, null),
                )
            val staleFactory =
                DefaultKafkaProducerFactory<String, CaloriesConsumedEvent>(producerFactory.configurationProperties).apply {
                    setTransactionIdPrefix(checkNotNull(producerFactory.transactionIdPrefix))
                    setTransactionIdSuffixStrategy(DefaultTransactionIdSuffixStrategy(1))
                }
            consumer().use { consumer ->
                try {
                    staleFactory.createProducer().use { stale ->
                        stale.beginTransaction()
                        stale.send(ProducerRecord(TOPIC, userId.toString(), event)).get(20, TimeUnit.SECONDS)
                        assertTrue(consumer.poll(Duration.ofMillis(200)).isEmpty)
                        dataSource.connection.use { abandoned ->
                            abandoned.createStatement().use { statement ->
                                statement.execute("SELECT pg_advisory_lock(hashtextextended('nutritionist.outbox.publisher', 0))")
                                val pid =
                                    statement.executeQuery("SELECT pg_backend_pid()").use { result ->
                                        check(result.next())
                                        result.getInt(1)
                                    }
                                assertEquals(0, processor.processBatch())
                                assertEquals(true, jdbc.queryForObject("SELECT pg_terminate_backend(?, 5000)", Boolean::class.java, pid))
                                assertEquals(1, OutboxProcessor(JdbcOutboxStore(dataSource), publisher).processBatch())
                            }
                        }
                        assertEquals(event.mealId, receive(consumer).value().mealId)
                        assertThrows(ProducerFencedException::class.java) { stale.commitTransaction() }
                    }
                } finally {
                    staleFactory.destroy()
                }
            }
            assertEquals("PUBLISHED", jdbc.queryForObject("SELECT status FROM outbox_messages", String::class.java))
        }

        @Test
        fun `owner delayed before Kafka init cannot send after takeover published later events`() {
            val first = request()
            val second = request()
            stub(TRACEPARENT).addEntry(first)
            stub(TRACEPARENT).addEntry(second)
            val selected = CountDownLatch(1)
            val resume = CountDownLatch(1)
            val oldFactory =
                DefaultKafkaProducerFactory<String, CaloriesConsumedEvent>(producerFactory.configurationProperties).apply {
                    setTransactionIdPrefix(checkNotNull(producerFactory.transactionIdPrefix))
                    setTransactionIdSuffixStrategy(DefaultTransactionIdSuffixStrategy(1))
                }
            val oldPublisher = CaloriesConsumedEventPublisher(KafkaTemplate(oldFactory))
            val delayed =
                OutboxPublisher { message, verifyOwnership ->
                    selected.countDown()
                    check(resume.await(20, TimeUnit.SECONDS))
                    oldPublisher.publish(message, verifyOwnership)
                }
            try {
                consumer().use { consumer ->
                    Executors.newSingleThreadExecutor().use { executor ->
                        val oldTask =
                            executor.submit<Int> { OutboxProcessor(store, delayed).processBatch() }
                        try {
                            check(selected.await(20, TimeUnit.SECONDS))
                            val pid =
                                checkNotNull(
                                    jdbc.queryForObject(
                                        "SELECT pid FROM pg_locks WHERE locktype = 'advisory' AND granted AND pid <> pg_backend_pid()",
                                        Int::class.java,
                                    ),
                                )
                            assertEquals(true, jdbc.queryForObject("SELECT pg_terminate_backend(?, 5000)", Boolean::class.java, pid))
                            assertEquals(2, processor.processBatch())
                            assertEquals(
                                listOf(first.entryId, second.entryId),
                                receiveMany(consumer, 2).map { it.value().mealId.toString() },
                            )
                        } finally {
                            resume.countDown()
                        }
                        assertThrows(ExecutionException::class.java) { oldTask.get(20, TimeUnit.SECONDS) }
                        assertTrue(consumer.poll(Duration.ofMillis(500)).isEmpty)
                    }
                }
            } finally {
                oldFactory.destroy()
            }
            assertEquals("PUBLISHED", status(first))
            assertEquals("PUBLISHED", status(second))
        }

        @Test
        fun `delivery migration upgrades existing pending rows and sequence defaults`() {
            stub(TRACEPARENT).addEntry(request())
            val message = pendingMessage()
            val schema = "outbox_upgrade"
            jdbc.execute("CREATE SCHEMA $schema")
            try {
                migrate("db/changelog/v1/004-diary-outbox.yaml", schema)
                repeat(2) { index ->
                    jdbc.update(
                        """
                        INSERT INTO outbox_upgrade.outbox_messages
                            (id, aggregate_type, aggregate_id, partition_key, topic, payload, created_at)
                        VALUES (?, 'DiaryEntry', ?, ?, ?, ?, CURRENT_TIMESTAMP + (? * INTERVAL '1 second'))
                        """.trimIndent(),
                        UUID.randomUUID(),
                        UUID.randomUUID().toString(),
                        userId.toString().uppercase(),
                        TOPIC,
                        message.payload,
                        index,
                    )
                }
                migrate("db/changelog/v1/005-diary-outbox-delivery.yaml", schema)
                val rows = jdbc.queryForList("SELECT * FROM outbox_upgrade.outbox_messages ORDER BY sequence_number")
                assertEquals(listOf(1L, 2L), rows.map { (it["sequence_number"] as Number).toLong() })
                assertTrue(rows.all { it["partition_key"] == userId.toString() && it["next_attempt_at"] != null })
                jdbc.update(
                    """
                    INSERT INTO outbox_upgrade.outbox_messages
                        (id, aggregate_type, aggregate_id, partition_key, topic, payload)
                    VALUES (?, 'DiaryEntry', ?, ?, ?, ?)
                    """.trimIndent(),
                    UUID.randomUUID(),
                    UUID.randomUUID().toString(),
                    userId.toString(),
                    TOPIC,
                    message.payload,
                )
                assertEquals(3L, jdbc.queryForObject("SELECT MAX(sequence_number) FROM outbox_upgrade.outbox_messages", Long::class.java))
            } finally {
                jdbc.execute("DROP SCHEMA $schema CASCADE")
            }
        }

        private fun migrate(
            changelog: String,
            schema: String,
        ) {
            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
                connection.schema = schema
                val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
                database.defaultSchemaName = schema
                Liquibase(changelog, ClassLoaderResourceAccessor(), database).use { it.update(Contexts(), LabelExpression()) }
            }
        }

        @Test
        fun `backoff grows exponentially and is capped`() {
            val settings = OutboxSettings(initialBackoff = Duration.ofSeconds(1), maxBackoff = Duration.ofSeconds(4))
            assertEquals(listOf(1L, 2L, 4L, 4L, 4L), listOf(1, 2, 3, 4, 1_000).map { settings.backoff(it).seconds })
        }

        @Configuration(proxyBeanMethods = false)
        @EnableScheduling
        class SchedulingConfiguration

        private fun publishedCount(): Int? =
            jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages WHERE status = 'PUBLISHED'", Int::class.java)

        private fun assertPayloadUnchanged(message: OutboxMessage) {
            val payload =
                jdbc.queryForObject("SELECT payload FROM outbox_messages WHERE id = ?", ByteArray::class.java, message.id.raw)
            assertTrue(message.payload.contentEquals(payload))
        }

        private fun status(entry: AddEntryRequest): String? =
            jdbc.queryForObject("SELECT status FROM outbox_messages WHERE aggregate_id = ?", String::class.java, entry.entryId)

        private fun command(): AddEntryCommand =
            AddEntryCommand(
                DiaryEntryId(UUID.randomUUID()),
                UserId(userId.toString()),
                IdempotencyKey(UUID.randomUUID().toString()),
                FoodItemId(UUID.randomUUID()),
                "Rice",
                100.0,
                DomainNutritionSummary(300, 5.0, 2.0, 60.0),
                MealType.LUNCH,
                kotlin.time.Instant.parse("2025-01-01T00:00:00Z"),
            )

        private fun pendingMessage(): OutboxMessage =
            jdbc
                .query("SELECT id, topic, partition_key, payload, traceparent FROM outbox_messages WHERE status = 'PENDING'") { row, _ ->
                    OutboxMessage(
                        OutboxMessageId(row.getObject("id", UUID::class.java)),
                        row.getString("topic"),
                        row.getString("partition_key"),
                        row.getBytes("payload"),
                        row.getString("traceparent"),
                    )
                }.single()

        private fun assertPending(message: OutboxMessage) {
            val row = jdbc.queryForMap("SELECT status, attempts, published_at, payload FROM outbox_messages WHERE id = ?", message.id.raw)
            assertEquals("PENDING", row["status"])
            assertEquals(0, row["attempts"])
            assertNull(row["published_at"])
            assertTrue(message.payload.contentEquals(row["payload"] as ByteArray))
        }

        private fun consumer(): KafkaConsumer<String, CaloriesConsumedEvent> {
            val consumer =
                KafkaConsumer<String, CaloriesConsumedEvent>(
                    mapOf(
                        "bootstrap.servers" to kafka.bootstrapServers,
                        "group.id" to UUID.randomUUID().toString(),
                        "enable.auto.commit" to false,
                        "key.deserializer" to StringDeserializer::class.java,
                        "value.deserializer" to KafkaAvroDeserializer::class.java,
                        "schema.registry.url" to registryUrl(),
                        "specific.avro.reader" to true,
                        "isolation.level" to "read_committed",
                    ),
                )
            val partitions = consumer.partitionsFor(TOPIC).map { TopicPartition(TOPIC, it.partition()) }
            consumer.assign(partitions)
            consumer.seekToEnd(partitions)
            partitions.forEach { consumer.position(it) }
            return consumer
        }

        private fun receive(consumer: KafkaConsumer<String, CaloriesConsumedEvent>): ConsumerRecord<String, CaloriesConsumedEvent> =
            receiveMany(consumer, 1).single()

        private fun receiveMany(
            consumer: KafkaConsumer<String, CaloriesConsumedEvent>,
            count: Int,
        ): List<ConsumerRecord<String, CaloriesConsumedEvent>> {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
            val records =
                generateSequence { consumer.poll(Duration.ofMillis(500)) }
                    .takeWhile { System.nanoTime() < deadline }
                    .flatMap { it.asSequence() }
                    .take(count)
                    .toList()
            assertEquals(count, records.size, "Expected CaloriesConsumedEvent records")
            return records
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

        private fun stub(
            traceparent: String?,
            user: UserId = UserId(userId.toString()),
        ): DiaryServiceGrpc.DiaryServiceBlockingStub {
            val metadata =
                Metadata().apply {
                    put(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER), user.raw)
                    put(Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER), UUID.randomUUID().toString())
                    traceparent?.let { put(Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER), it) }
                }
            return DiaryServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(20, TimeUnit.SECONDS)
        }

        companion object {
            private const val TOPIC = "diary.entry.created"
            private const val TRACEPARENT = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
            private val network = Network.newNetwork()
            private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).apply { start() }
            private val kafka =
                KafkaContainer(
                    DockerImageName.parse(
                        "apache/kafka@sha256:1213eb3943d551e5ed1fca7a4e109001cee35770b66a02a0c37a8964efe09b69",
                    ),
                ).withNetwork(network)
                    .withNetworkAliases("kafka")
                    .withListener("kafka:19092")
                    .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                    .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
                    .apply { start() }
            private val registry =
                GenericContainer(
                    DockerImageName.parse(
                        "confluentinc/cp-schema-registry@sha256:b5a110851999118dd2ebd154bba8406271606d6ebf958bc83c56dabcd68664c2",
                    ),
                ).withNetwork(network)
                    .withExposedPorts(8081)
                    .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                    .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:19092")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_TOPIC_REPLICATION_FACTOR", "1")
                    .withEnv("SCHEMA_REGISTRY_SCHEMA_COMPATIBILITY_LEVEL", "BACKWARD")
                    .waitingFor(Wait.forHttp("/subjects").forPort(8081))
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .apply { start() }

            private fun registryUrl(): String = "http://${registry.host}:${registry.getMappedPort(8081)}"

            @JvmStatic
            @DynamicPropertySource
            fun properties(properties: DynamicPropertyRegistry) {
                properties.add("spring.datasource.url", postgres::getJdbcUrl)
                properties.add("spring.datasource.username", postgres::getUsername)
                properties.add("spring.datasource.password", postgres::getPassword)
                properties.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
                properties.add("spring.kafka.producer.properties.[delivery.timeout.ms]") { "3000" }
                properties.add("spring.kafka.producer.properties.[request.timeout.ms]") { "1000" }
                properties.add("spring.kafka.producer.properties.[max.block.ms]") { "2000" }
                properties.add("spring.kafka.properties.[http.connect.timeout.ms]") { "500" }
                properties.add("spring.kafka.properties.[http.read.timeout.ms]") { "500" }
                properties.add("spring.kafka.properties.[max.retries]") { "0" }
                properties.add("spring.kafka.properties.[schema.registry.url]", ::registryUrl)
            }
        }
    }
