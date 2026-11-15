package dev.andre.nutritionist.diary.messaging

import dev.andre.nutritionist.contracts.avro.CaloriesConsumedEvent
import dev.andre.nutritionist.contracts.avro.UserRegisteredEvent
import dev.andre.nutritionist.diary.DiaryApplication
import dev.andre.nutritionist.diary.domain.model.UserId
import dev.andre.nutritionist.diary.service.DiaryInitializationService
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import io.grpc.ManagedChannelBuilder
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@SpringBootTest(
    classes = [DiaryApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.grpc.server.port=0",
        "diary.kafka.registration.retry-interval=PT0.1S",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
class UserRegisteredEventConsumerIntegrationTest
    @Autowired
    constructor(
        private val jdbc: JdbcTemplate,
        private val initialization: DiaryInitializationService,
        private val listeners: KafkaListenerEndpointRegistry,
        private val grpcServer: GrpcServerLifecycle,
    ) {
        private val admin = AdminClient.create(mapOf(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers))
        private val producer = KafkaProducer<String, UserRegisteredEvent>(sourceProducerProperties())

        @AfterAll
        fun closeResources() {
            listeners.stop()
            producer.close()
            admin.close()
            schemaRegistry.stop()
            kafka.stop()
            postgres.stop()
            registrationNetwork.close()
        }

        @Test
        fun `grpc health is serving while registration consumer is enabled`() {
            val channel =
                ManagedChannelBuilder
                    .forAddress("127.0.0.1", grpcServer.port)
                    .usePlaintext()
                    .build()
            try {
                val response =
                    HealthGrpc
                        .newBlockingStub(channel)
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .check(HealthCheckRequest.getDefaultInstance())
                assertEquals(HealthCheckResponse.ServingStatus.SERVING, response.status)
            } finally {
                channel.shutdownNow()
            }
        }

        @Test
        fun `unexpected Avro record goes to DLT without blocking the partition`() {
            val user = user()
            val unexpected =
                CaloriesConsumedEvent
                    .newBuilder()
                    .setUserId(UUID.fromString(user.raw))
                    .setMealId(UUID.randomUUID())
                    .setCalories(150)
                    .setTimestamp(Instant.parse("2026-11-15T00:00:00Z"))
                    .build()
            val metadata =
                KafkaProducer<String, Any>(
                    sourceProducerProperties() + ("value.subject.name.strategy" to RecordNameStrategy::class.java),
                ).use {
                    it
                        .send(ProducerRecord(TOPIC, 0, user.raw, unexpected))
                        .get(20, TimeUnit.SECONDS)
                }

            assertNotNull(deadLetter(user).headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN))
            awaitCommitted(metadata)
            assertEquals(0, userCount(user))
            assertEquals(0, processedCount(user))
            val valid = user()
            awaitCommitted(send(valid, event(valid)))
            assertEquals(1, userCount(valid))
        }

        @Test
        fun `registration is initialized before its offset is committed`() {
            val user = user()
            val event = event(user)
            val metadata = send(user, event)

            awaitCommitted(metadata)

            assertEquals(1, userCount(user))
            assertEquals(1, processedCount(user))
            assertEquals(
                event.registrationTimestamp,
                jdbc
                    .queryForObject(
                        "SELECT registered_at FROM diary_users WHERE user_id = ?",
                        Timestamp::class.java,
                        user.raw,
                    )?.toInstant(),
            )
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM diary_entries WHERE user_id = ?", Int::class.java, user.raw))
        }

        @Test
        fun `duplicate registration on another offset preserves one initialized diary`() {
            val user = user()
            val first = event(user)
            awaitCommitted(send(user, first))
            val repeated =
                UserRegisteredEvent
                    .newBuilder(first)
                    .setRegistrationTimestamp(first.registrationTimestamp.plusSeconds(60))
                    .build()

            awaitCommitted(send(user, repeated))

            assertEquals(1, userCount(user))
            assertEquals(1, processedCount(user))
            assertEquals(
                first.registrationTimestamp,
                jdbc
                    .queryForObject(
                        "SELECT registered_at FROM diary_users WHERE user_id = ?",
                        Timestamp::class.java,
                        user.raw,
                    )?.toInstant(),
            )
        }

        @Test
        fun `invalid event goes to DLT and does not block the next registration`() {
            val invalidUser = user()
            val invalid =
                UserRegisteredEvent
                    .newBuilder(event(invalidUser))
                    .setRegistrationTimestamp(Instant.ofEpochSecond(253_402_300_800L))
                    .build()
            val badMetadata = send(invalidUser, invalid)

            val deadLetter = deadLetter(invalidUser)
            awaitCommitted(badMetadata)

            assertNotNull(deadLetter.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN))
            assertEquals(0, userCount(invalidUser))
            assertEquals(0, processedCount(invalidUser))
            val validUser = user()
            awaitCommitted(send(validUser, event(validUser)))
            assertEquals(1, userCount(validUser))
        }

        @Test
        fun `malformed Avro is preserved as raw bytes in DLT`() {
            val user = user()
            val payload = byteArrayOf(1, 2, 3)
            val metadata =
                KafkaProducer<String, ByteArray>(
                    mapOf(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers),
                    StringSerializer(),
                    ByteArraySerializer(),
                ).use {
                    it.send(ProducerRecord(TOPIC, 0, user.raw, payload)).get(20, TimeUnit.SECONDS)
                }

            assertArrayEquals(payload, deadLetter(user).value())
            awaitCommitted(metadata)
            assertEquals(0, userCount(user))
            assertEquals(0, processedCount(user))
        }

        @Test
        fun `database error retries then goes to DLT and rolls back processed marker`() {
            val user = user()
            jdbc.execute("ALTER TABLE diary_users ADD CONSTRAINT registration_test_failure CHECK (user_id <> '${user.raw}')")
            try {
                val metadata = send(user, event(user))
                val deadLetter = deadLetter(user)
                awaitCommitted(metadata)

                assertEquals(0, userCount(user))
                assertEquals(0, processedCount(user))
                val attempt = checkNotNull(deadLetter.headers().lastHeader(KafkaHeaders.DELIVERY_ATTEMPT))
                assertEquals(3, ByteBuffer.wrap(attempt.value()).int)
            } finally {
                jdbc.execute("ALTER TABLE diary_users DROP CONSTRAINT registration_test_failure")
            }
        }

        @Test
        fun `concurrent deliveries initialize one diary`() {
            val user = user()
            val timestamp = kotlin.time.Instant.fromEpochSeconds(1_700_000_000)
            val workers = 8
            val ready = CountDownLatch(workers)
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val futures =
                    List(workers) {
                        executor.submit<Boolean> {
                            ready.countDown()
                            check(start.await(10, TimeUnit.SECONDS))
                            initialization.initialize(user, timestamp)
                        }
                    }
                assertTrue(ready.await(10, TimeUnit.SECONDS))
                start.countDown()

                assertEquals(1, futures.map { it.get(20, TimeUnit.SECONDS) }.count { it })
                assertEquals(1, userCount(user))
                assertEquals(1, processedCount(user))
            } finally {
                start.countDown()
                executor.shutdownNow()
            }
        }

        @Test
        fun `late registration preserves existing diary entries`() {
            val user = user()
            jdbc.update(
                """
                INSERT INTO diary_entries (
                    id, user_id, idempotency_key, food_item_id, food_name, quantity_grams,
                    calories, proteins, fats, carbs, meal_type, recorded_at
                )
                VALUES (gen_random_uuid(), ?, 'before-registration', gen_random_uuid(), 'Oatmeal', 200,
                    150, 5, 3, 25, 'BREAKFAST', NOW())
                """.trimIndent(),
                user.raw,
            )

            awaitCommitted(send(user, event(user)))

            assertEquals(1, userCount(user))
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM diary_entries WHERE user_id = ?", Int::class.java, user.raw))
        }

        private fun user(): UserId = UserId(UUID.randomUUID().toString())

        private fun event(user: UserId): UserRegisteredEvent =
            UserRegisteredEvent
                .newBuilder()
                .setId(UUID.fromString(user.raw))
                .setEmail("registration@example.test")
                .setRegistrationTimestamp(Instant.parse("2026-11-15T00:00:00Z"))
                .setSource("profile-service")
                .build()

        private fun send(
            user: UserId,
            event: UserRegisteredEvent,
        ): RecordMetadata = producer.send(ProducerRecord(TOPIC, 0, user.raw, event)).get(20, TimeUnit.SECONDS)

        private fun sourceProducerProperties(): Map<String, Any> =
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                "schema.registry.url" to "http://${schemaRegistry.host}:${schemaRegistry.getMappedPort(8081)}",
            )

        private fun awaitCommitted(metadata: RecordMetadata) {
            await().atMost(Duration.ofSeconds(30)).ignoreExceptions().until {
                val offsets =
                    admin
                        .listConsumerGroupOffsets(GROUP)
                        .partitionsToOffsetAndMetadata()
                        .get(5, TimeUnit.SECONDS)
                (offsets[TopicPartition(metadata.topic(), metadata.partition())]?.offset() ?: 0) > metadata.offset()
            }
        }

        private fun deadLetter(user: UserId): ConsumerRecord<String, ByteArray> =
            KafkaConsumer(
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ConsumerConfig.GROUP_ID_CONFIG to "dlt-check-${UUID.randomUUID()}",
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                ),
                StringDeserializer(),
                ByteArrayDeserializer(),
            ).use { consumer ->
                consumer.assign(listOf(TopicPartition(TOPIC + ".DLT", 0)))
                consumer.seekToBeginning(consumer.assignment())
                val result = AtomicReference<ConsumerRecord<String, ByteArray>>()
                await().atMost(Duration.ofSeconds(30)).until {
                    val record = consumer.poll(Duration.ofMillis(250)).firstOrNull { it.key() == user.raw }
                    record?.let(result::set)
                    result.get() != null
                }
                checkNotNull(result.get())
            }

        private fun userCount(user: UserId): Int =
            checkNotNull(jdbc.queryForObject("SELECT COUNT(*) FROM diary_users WHERE user_id = ?", Int::class.java, user.raw))

        private fun processedCount(user: UserId): Int =
            checkNotNull(
                jdbc.queryForObject(
                    "SELECT COUNT(*) FROM processed_events WHERE consumer_name = 'diary-user-registered' AND event_key = ?",
                    Int::class.java,
                    "UserRegisteredEvent:${user.raw}",
                ),
            )

        companion object {
            private const val TOPIC = "user.registered"
            private const val GROUP = "diary-registration-test"
            private val registrationNetwork = Network.newNetwork()
            private val kafka =
                KafkaContainer(
                    DockerImageName
                        .parse("apache/kafka:4.2.2@sha256:1213eb3943d551e5ed1fca7a4e109001cee35770b66a02a0c37a8964efe09b69")
                        .asCompatibleSubstituteFor("apache/kafka"),
                ).withNetwork(registrationNetwork)
                    .withNetworkAliases("kafka")
                    .withListener("kafka:19092")
                    .apply { start() }
            private val schemaRegistry =
                GenericContainer<Nothing>(
                    DockerImageName.parse(
                        "confluentinc/cp-schema-registry:8.2.4@sha256:b5a110851999118dd2ebd154bba8406271606d6ebf958bc83c56dabcd68664c2",
                    ),
                ).apply {
                    withNetwork(registrationNetwork)
                    withExposedPorts(8081)
                    withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                    withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                    withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "kafka:19092")
                    withEnv("SCHEMA_REGISTRY_KAFKASTORE_TOPIC_REPLICATION_FACTOR", "1")
                    withEnv("SCHEMA_REGISTRY_SCHEMA_COMPATIBILITY_LEVEL", "BACKWARD")
                    waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
                    start()
                }
            private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).apply { start() }

            @JvmStatic
            @DynamicPropertySource
            fun infrastructureProperties(registry: DynamicPropertyRegistry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl)
                registry.add("spring.datasource.username", postgres::getUsername)
                registry.add("spring.datasource.password", postgres::getPassword)
                registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
                registry.add("spring.kafka.consumer.group-id") { GROUP }
                registry.add("spring.kafka.properties.[schema.registry.url]") {
                    "http://${schemaRegistry.host}:${schemaRegistry.getMappedPort(8081)}"
                }
            }
        }
    }
