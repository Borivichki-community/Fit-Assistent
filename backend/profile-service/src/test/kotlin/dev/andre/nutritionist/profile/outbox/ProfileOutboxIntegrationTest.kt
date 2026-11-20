package dev.andre.nutritionist.profile.outbox

import dev.andre.nutritionist.contracts.avro.UserRegisteredEvent
import dev.andre.nutritionist.profile.domain.model.ActivityLevel
import dev.andre.nutritionist.profile.domain.model.Gender
import dev.andre.nutritionist.profile.domain.model.GoalType
import dev.andre.nutritionist.profile.domain.model.UpsertProfileCommand
import dev.andre.nutritionist.profile.domain.model.UserId
import dev.andre.nutritionist.profile.outbox.model.OutboxStatus
import dev.andre.nutritionist.profile.outbox.processor.OutboxProcessor.Companion.USER_REGISTERED_TOPIC
import dev.andre.nutritionist.profile.outbox.repository.OutboxJpaRepository
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import dev.andre.nutritionist.profile.service.ProfileService
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * Сквозной интеграционный тест Transactional Outbox (T-025).
 *
 * Стек:
 * - PostgreSQL 17 (Testcontainers)
 * - Redis 7 (Testcontainers)
 * - Apache Kafka 3.9 / Confluent Kafka 7.7 (Testcontainers)
 * - Confluent Schema Registry 7.7 (Testcontainers)
 *
 * Проверяемый сценарий:
 * 1. Регистрация нового профиля через [ProfileService.upsertProfile].
 * 2. Атомарное сохранение ProfileEntity и OutboxMessageEntity (PENDING) в одной транзакции.
 * 3. Асинхронная доставка OutboxProcessor: перевод статуса в PUBLISHED.
 * 4. Чтение из Kafka Consumer: проверка партиционирования по userId и валидности Avro-схемы.
 * 5. Повторный upsertProfile существующего профиля НЕ порождает нового события.
 */
@SpringBootTest
@ActiveProfiles("dev")
class ProfileOutboxIntegrationTest {
    @Autowired
    private lateinit var profileService: ProfileService

    @Autowired
    private lateinit var profileRepository: ProfileJpaRepository

    @Autowired
    private lateinit var outboxRepository: OutboxJpaRepository

    @BeforeEach
    fun cleanDatabase() {
        outboxRepository.deleteAll()
        profileRepository.deleteAll()
    }

    @Test
    fun `creating new profile saves outbox message and processor publishes avro event to kafka`() {
        val userId = "usr_outbox_integration_test_1"
        val testEmail = "nutritionist.test@example.com"

        val command =
            UpsertProfileCommand(
                userId = UserId(userId),
                heightCm = 182,
                weightKg = BigDecimal("78.50"),
                gender = Gender.MALE,
                activityLevel = ActivityLevel.MODERATE,
                goalType = GoalType.LOSE_WEIGHT,
                nutritionGoal = null,
                weeklyBudget = BigDecimal("4500.00"),
                email = testEmail,
            )

        // 1. Создание профиля
        val savedProfile = profileService.upsertProfile(command)
        assertNotNull(savedProfile)
        assertEquals(userId, savedProfile.userId)

        // 2. Ожидаем успешную публикацию через OutboxProcessor в Kafka
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted {
                val messages = outboxRepository.findAll()
                assertEquals(1, messages.size)
                val outbox = messages[0]
                assertEquals(userId, outbox.aggregateId)
                assertEquals(USER_REGISTERED_TOPIC, outbox.topic)
                assertEquals(OutboxStatus.PUBLISHED, outbox.status)
                assertNotNull(outbox.publishedAt)
            }

        // 3. Вычитываем событие из Kafka реальным Consumer с десериализацией Avro
        createTestKafkaConsumer().use { consumer ->
            consumer.subscribe(listOf(USER_REGISTERED_TOPIC))

            val records =
                await()
                    .atMost(15, TimeUnit.SECONDS)
                    .pollInterval(200, TimeUnit.MILLISECONDS)
                    .until({
                        val polled = consumer.poll(Duration.ofMillis(500))
                        if (polled.isEmpty) null else polled
                    }) { it != null }

            requireNotNull(records)
            val record = records.first()

            // Проверка строгой партиционированности по userId (FIFO в рамках одной партиции)
            assertEquals(userId, record.key())

            val event = record.value() as? UserRegisteredEvent
            assertNotNull(event, "Record value must be instance of UserRegisteredEvent")
            requireNotNull(event)

            assertEquals(testEmail, event.email.toString())
            assertEquals("profile-service", event.source?.toString())
            assertNotNull(event.id)
            assertNotNull(event.registrationTimestamp)
        }

        // 4. Повторный upsertProfile для существующего пользователя НЕ должен создавать повторный UserRegisteredEvent
        val updateCommand = command.copy(heightCm = 185)
        profileService.upsertProfile(updateCommand)

        // Даем воркеру время на возможную ошибочную публикацию
        Thread.sleep(500)
        val messagesAfterUpdate = outboxRepository.findAll()
        assertEquals(1, messagesAfterUpdate.size, "UpdateProfile must NOT publish duplicate UserRegisteredEvent")
    }

    private fun createTestKafkaConsumer(): KafkaConsumer<String, Any> {
        val props =
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, "outbox-test-group-${System.currentTimeMillis()}")
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java.name)
                put(
                    KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                    "http://${schemaRegistry.host}:${schemaRegistry.getMappedPort(8081)}",
                )
                put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true")
            }
        return KafkaConsumer(props)
    }

    companion object {
        private val network: Network = Network.newNetwork()

        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                .withDatabaseName("profile_outbox_test_db")
                .withUsername("test_user")
                .withPassword("test_pass")
                .withReuse(true)
                .apply { start() }

        @JvmStatic
        val redis: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true)
                .apply { start() }

        @JvmStatic
        val kafka: KafkaContainer =
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.0"))
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .apply { start() }

        @JvmStatic
        val schemaRegistry: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("confluentinc/cp-schema-registry:7.7.0"))
                .withNetwork(network)
                .withExposedPorts(8081)
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9092")
                .dependsOn(kafka)
                .apply { start() }

        @AfterAll
        @JvmStatic
        fun stopContainers() {
            schemaRegistry.stop()
            kafka.stop()
            network.close()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.liquibase.enabled") { "true" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }

            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
            registry.add("spring.kafka.properties.[schema.registry.url]") {
                "http://${schemaRegistry.host}:${schemaRegistry.getMappedPort(8081)}"
            }

            registry.add("outbox.enabled") { "true" }
            registry.add("outbox.polling-interval-ms") { "200" }
            registry.add("outbox.batch-size") { "50" }
        }
    }
}
