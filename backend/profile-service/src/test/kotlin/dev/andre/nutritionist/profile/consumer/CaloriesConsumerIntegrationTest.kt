package dev.andre.nutritionist.profile.consumer

import com.budgetntr.proto.profile.v1.GetDailyCaloriesBalanceRequest
import com.budgetntr.proto.profile.v1.ProfileServiceGrpcKt
import dev.andre.nutritionist.contracts.avro.CaloriesConsumedEvent
import dev.andre.nutritionist.contracts.mapper.CaloriesConsumedEventMapper
import dev.andre.nutritionist.profile.domain.model.UserId
import dev.andre.nutritionist.profile.outbox.DeadLetterEnvelope
import dev.andre.nutritionist.profile.outbox.ProfileDeadLetterScheduler
import dev.andre.nutritionist.profile.service.CaloriesConsumptionService
import dev.andre.nutritionist.profile.service.DailyCaloriesService
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.grpc.server.port=0",
        "profile.calories.initial-backoff-ms=10",
        "profile.calories.max-backoff-ms=20",
        "profile.calories.max-attempts=2",
        "profile.calories.dlt-outbox.poll-interval=PT1H",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
class CaloriesConsumerIntegrationTest
    @Autowired
    constructor(
        private val jdbc: JdbcTemplate,
        private val service: CaloriesConsumptionService,
        private val daily: DailyCaloriesService,
        private val scheduler: ProfileDeadLetterScheduler,
        private val transactions: PlatformTransactionManager,
        private val grpc: GrpcServerLifecycle,
    ) {
        @AfterAll
        fun stopContainers() {
            registry.stop()
            kafka.stop()
            postgres.stop()
            network.close()
        }

        @Test
        fun `Kafka event and redelivery are counted once and readable through authenticated RPC`() {
            val user = UUID.randomUUID()
            profile(user)
            val event = event(user, 350)
            val bytes = serialize(event)
            send(user, bytes)
            send(user, bytes)
            await().atMost(Duration.ofSeconds(15)).untilAsserted {
                assertEquals(350, balance(user).consumedCalories)
            }
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM processed_events WHERE user_id = ?", Int::class.java, user))
            val channel = ManagedChannelBuilder.forAddress("127.0.0.1", grpc.port).usePlaintext().build()
            try {
                val headers =
                    Metadata().apply {
                        put(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER), user.toString())
                        put(Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER), "must-not-cache-read")
                    }
                val stub =
                    ProfileServiceGrpcKt
                        .ProfileServiceCoroutineStub(channel)
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                val request =
                    GetDailyCaloriesBalanceRequest
                        .newBuilder()
                        .setUserId(user.toString())
                        .setDate("2026-11-17")
                        .setTimeZone("Europe/Moscow")
                        .build()
                val result =
                    runBlocking {
                        stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers)).getDailyCaloriesBalance(request)
                    }
                assertEquals(1650, result.remainingCalories)
                val failure =
                    assertThrows(StatusException::class.java) {
                        runBlocking { stub.getDailyCaloriesBalance(request) }
                    }
                assertEquals(Status.Code.UNAUTHENTICATED, failure.status.code)
                val other = request.toBuilder().setUserId(UUID.randomUUID().toString()).build()
                val denied =
                    assertThrows(StatusException::class.java) {
                        runBlocking {
                            stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers)).getDailyCaloriesBalance(other)
                        }
                    }
                assertEquals(Status.Code.PERMISSION_DENIED, denied.status.code)
            } finally {
                channel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS)
            }
        }

        @Test
        fun `concurrent duplicates rollback and conflicting payload preserve one fact`() {
            val user = UUID.randomUUID()
            val meal = CaloriesConsumedEventMapper.toDomain(event(user, 250))
            val executor = Executors.newFixedThreadPool(4)
            try {
                val futures = (1..8).map { executor.submit { service.consume(meal) } }
                futures.forEach { it.get(5, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }
            assertThrows(IllegalArgumentException::class.java) { service.consume(meal.copy(calories = 251)) }
            val rolledBack = CaloriesConsumedEventMapper.toDomain(event(user, 100))
            assertThrows(IllegalStateException::class.java) {
                TransactionTemplate(transactions).executeWithoutResult {
                    service.consume(rolledBack)
                    error("rollback")
                }
            }
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM processed_events WHERE user_id = ?", Int::class.java, user))
            service.consume(rolledBack)
            profile(user)
            assertEquals(350, balance(user).consumedCalories)
            jdbc.update("UPDATE profiles SET target_calories = 300 WHERE user_id = ?", user.toString())
            assertEquals(-50, balance(user).remainingCalories)
        }

        @Test
        fun `day boundaries use meal time and DST aware timezone`() {
            val user = UUID.randomUUID()
            profile(user)
            listOf(
                "2026-03-28T22:59:59Z" to 10,
                "2026-03-28T23:00:00Z" to 100,
                "2026-03-29T21:59:59Z" to 200,
                "2026-03-29T22:00:00Z" to 20,
            ).forEach { (timestamp, calories) ->
                service.consume(CaloriesConsumedEventMapper.toDomain(event(user, calories, Instant.parse(timestamp))))
            }
            val result = daily.getBalance(UserId(user.toString()), LocalDate.parse("2026-03-29"), ZoneId.of("Europe/Berlin"))
            assertEquals(300, result.consumedCalories)
            assertEquals(1700, result.remainingCalories)
        }

        @Test
        fun `database failure retries then DLT preserves bytes and can be replayed`() {
            val user = UUID.randomUUID()
            val bytes = serialize(event(user, 420))
            jdbc.execute("CREATE SEQUENCE test_calories_attempts")
            jdbc.execute(
                """
                CREATE FUNCTION test_reject_calories() RETURNS trigger LANGUAGE plpgsql AS
                'BEGIN
                    IF NEW.user_id = ''$user''::uuid THEN
                        PERFORM nextval(''test_calories_attempts'');
                        RAISE EXCEPTION ''temporary database error'';
                    END IF;
                    RETURN NEW;
                END'
                """.trimIndent(),
            )
            jdbc.execute(
                "CREATE TRIGGER test_reject BEFORE INSERT ON processed_events FOR EACH ROW " +
                    "EXECUTE FUNCTION test_reject_calories()",
            )
            try {
                send(user, bytes)
                await().atMost(Duration.ofSeconds(15)).untilAsserted {
                    assertTrue(jdbc.queryForObject("SELECT last_value FROM test_calories_attempts", Long::class.java) == 2L)
                    assertTrue((jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages", Int::class.java) ?: 0) >= 1)
                }
            } finally {
                jdbc.execute("DROP TRIGGER test_reject ON processed_events")
                jdbc.execute("DROP FUNCTION test_reject_calories()")
                jdbc.execute("DROP SEQUENCE test_calories_attempts")
            }
            scheduler.publish()
            await().atMost(Duration.ofSeconds(10)).untilAsserted {
                assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages WHERE status <> 'PUBLISHED'", Int::class.java))
            }
            KafkaConsumer<ByteArray, ByteArray>(
                mapOf(
                    "bootstrap.servers" to kafka.bootstrapServers,
                    "group.id" to UUID.randomUUID().toString(),
                    "enable.auto.commit" to false,
                ),
                ByteArrayDeserializer(),
                ByteArrayDeserializer(),
            ).use { consumer ->
                val partition = TopicPartition(DLT, 0)
                consumer.assign(listOf(partition))
                consumer.seekToBeginning(listOf(partition))
                val records =
                    generateSequence { consumer.poll(Duration.ofMillis(500)).toList() }
                        .take(20)
                        .flatten()
                        .first { it.key()?.toString(Charsets.UTF_8) == user.toString() }
                assertArrayEquals(bytes, records.value())
                assertTrue(records.headers().lastHeader("profile-original-offset") != null)
                send(user, records.value())
                send(user, records.value())
            }
            await().atMost(Duration.ofSeconds(10)).untilAsserted {
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM processed_events WHERE user_id = ?", Int::class.java, user))
            }
        }

        @Test
        fun `DLT envelope preserves tombstone empty payload null key and binary headers`() {
            listOf(null, byteArrayOf(), byteArrayOf(1, 2, 3)).forEach { value ->
                val record = ConsumerRecord<ByteArray, ByteArray>(TOPIC, 0, 12, null, value)
                record.headers().add("traceparent", byteArrayOf(1, 2))
                record.headers().add("nullable", null)
                val decoded = DeadLetterEnvelope.decode(DLT, DeadLetterEnvelope.encode(record, IllegalArgumentException()))
                assertArrayEquals(value, decoded.value())
                assertEquals(null, decoded.key())
                assertArrayEquals(byteArrayOf(1, 2), decoded.headers().lastHeader("traceparent").value())
            }
        }

        private fun balance(user: UUID) =
            daily.getBalance(
                UserId(user.toString()),
                LocalDate.parse("2026-11-17"),
                ZoneId.of("Europe/Moscow"),
            )

        private fun profile(user: UUID) {
            jdbc.update(
                """
                INSERT INTO profiles (
                    id, user_id, height_cm, weight_kg, gender, activity_level, goal_type, target_calories,
                    target_proteins_grams, target_fats_grams, target_carbs_grams, target_weight_kg, weekly_budget, version
                ) VALUES (?, ?, 180, 80, 'MALE', 'MODERATE', 'MAINTAIN', 2000, 100, 60, 250, 80, 5000, 0)
                """.trimIndent(),
                UUID.randomUUID(),
                user.toString(),
            )
        }

        private fun event(
            user: UUID,
            calories: Int,
            instant: Instant = Instant.parse("2026-11-17T10:00:00Z"),
        ) = CaloriesConsumedEvent
            .newBuilder()
            .setUserId(user)
            .setMealId(UUID.randomUUID())
            .setCalories(calories)
            .setTimestamp(instant)
            .build()

        private fun serialize(event: CaloriesConsumedEvent): ByteArray =
            KafkaAvroSerializer().use { serializer ->
                serializer.configure(mapOf("schema.registry.url" to registryUrl()), false)
                serializer.serialize(TOPIC, event)
            }

        private fun send(
            user: UUID,
            payload: ByteArray,
        ) {
            KafkaProducer(
                mapOf("bootstrap.servers" to kafka.bootstrapServers, "max.block.ms" to 5000),
                ByteArraySerializer(),
                ByteArraySerializer(),
            ).use { producer ->
                producer.send(ProducerRecord(TOPIC, user.toString().toByteArray(Charsets.UTF_8), payload)).get(5, TimeUnit.SECONDS)
            }
        }

        companion object {
            private const val TOPIC = "diary.entry.created"
            private const val DLT = "diary.entry.created.profile.dlt"
            private val network = Network.newNetwork()
            private val postgres =
                PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                    .withStartupTimeout(Duration.ofSeconds(45))
                    .apply { start() }
            private val kafka =
                KafkaContainer(
                    DockerImageName.parse("apache/kafka@sha256:1213eb3943d551e5ed1fca7a4e109001cee35770b66a02a0c37a8964efe09b69"),
                ).withNetwork(network)
                    .withNetworkAliases("kafka")
                    .withListener("kafka:19092")
                    .withStartupTimeout(Duration.ofSeconds(45))
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
                    .waitingFor(Wait.forHttp("/subjects").forPort(8081))
                    .withStartupTimeout(Duration.ofSeconds(45))
                    .apply { start() }

            private fun registryUrl() = "http://${registry.host}:${registry.getMappedPort(8081)}"

            @JvmStatic
            @DynamicPropertySource
            fun properties(properties: DynamicPropertyRegistry) {
                Admin.create(mapOf("bootstrap.servers" to kafka.bootstrapServers)).use { admin ->
                    admin.createTopics(listOf(NewTopic(TOPIC, 1, 1.toShort()))).all().get(10, TimeUnit.SECONDS)
                }
                properties.add("spring.datasource.url", postgres::getJdbcUrl)
                properties.add("spring.datasource.username", postgres::getUsername)
                properties.add("spring.datasource.password", postgres::getPassword)
                properties.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
                properties.add("spring.kafka.properties.[schema.registry.url]", ::registryUrl)
            }
        }
    }
