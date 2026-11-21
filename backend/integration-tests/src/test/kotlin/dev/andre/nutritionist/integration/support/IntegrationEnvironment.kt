package dev.andre.nutritionist.integration.support

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.GroupIdNotFoundException
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@JvmInline
value class TestUserId(
    val raw: UUID,
)

class IntegrationEnvironment :
    BeforeAllCallback,
    AfterAllCallback {
    private val resources = ConcurrentLinkedDeque<AutoCloseable>()
    private val diaryProcess = AtomicReference<ServiceProcess>()
    private val profileProcess = AtomicReference<ServiceProcess>()
    private val network = Network.newNetwork()
    private val postgres =
        PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("diary_db")
            .withStartupTimeout(Duration.ofSeconds(45))
    val kafka: KafkaContainer =
        KafkaContainer(
            DockerImageName.parse(
                "apache/kafka@sha256:1213eb3943d551e5ed1fca7a4e109001cee35770b66a02a0c37a8964efe09b69",
            ),
        ).withNetwork(network)
            .withNetworkAliases("kafka")
            .withListener("kafka:19092")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
            .withStartupTimeout(Duration.ofSeconds(45))
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
            .withStartupTimeout(Duration.ofSeconds(60))

    val diary: ServiceProcess get() = checkNotNull(diaryProcess.get())
    val profile: ServiceProcess get() = checkNotNull(profileProcess.get())
    val registryUrl: String get() = "http://${registry.host}:${registry.getMappedPort(8081)}"
    private val profileUrl: String get() = postgres.jdbcUrl.substringBeforeLast("/") + "/profile_db"

    override fun beforeAll(context: ExtensionContext) {
        try {
            resources.addFirst(network)
            resources.addFirst(postgres)
            postgres.start()
            profileDatabase()
            resources.addFirst(kafka)
            kafka.start()
            resources.addFirst(registry)
            registry.start()
            Admin.create(mapOf("bootstrap.servers" to kafka.bootstrapServers)).use { admin ->
                admin.createTopics(listOf(NewTopic(TOPIC, 3, 1.toShort()))).all().get(10, TimeUnit.SECONDS)
            }
            val common =
                mapOf(
                    "spring.datasource.username" to postgres.username,
                    "spring.datasource.password" to postgres.password,
                    "spring.kafka.bootstrap-servers" to kafka.bootstrapServers,
                    "spring.kafka.properties.[schema.registry.url]" to registryUrl,
                )
            val diary =
                ServiceProcess(
                    "diary",
                    common +
                        mapOf(
                            "spring.datasource.url" to postgres.jdbcUrl,
                            "diary.kafka.registration.enabled" to "false",
                            "diary.outbox.enabled" to "true",
                            "diary.outbox.poll-interval" to "PT0.2S",
                            "diary.outbox.transaction-id-prefix" to "t057-${UUID.randomUUID()}-",
                        ),
                )
            resources.addFirst(diary)
            diaryProcess.set(diary)
            diary.awaitReady()
            val profile =
                ServiceProcess(
                    "profile",
                    common +
                        mapOf(
                            "spring.datasource.url" to profileUrl,
                            "profile.calories.enabled" to "true",
                            "profile.calories.group-id" to GROUP,
                        ),
                )
            resources.addFirst(profile)
            profileProcess.set(profile)
            profile.awaitReady()
            Admin.create(mapOf("bootstrap.servers" to kafka.bootstrapServers)).use { admin ->
                await()
                    .atMost(Duration.ofSeconds(15))
                    .ignoreExceptionsMatching { it is ExecutionException && it.cause is GroupIdNotFoundException }
                    .untilAsserted {
                        val members = admin.describeConsumerGroups(listOf(GROUP)).all().get(3, TimeUnit.SECONDS)
                        assertTrue(members.getValue(GROUP).members().any { it.assignment().topicPartitions().isNotEmpty() })
                    }
            }
        } catch (failure: Throwable) {
            runCatching { closeResources() }.exceptionOrNull()?.let(failure::addSuppressed)
            throw failure
        }
    }

    private fun profileDatabase() {
        diaryConnection().use { connection ->
            connection.createStatement().use { it.execute("CREATE DATABASE profile_db") }
        }
    }

    fun diaryConnection(): Connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)

    fun profileConnection(): Connection = DriverManager.getConnection(profileUrl, postgres.username, postgres.password)

    fun seedProfile(user: TestUserId) {
        profileConnection().use { connection ->
            connection
                .prepareStatement(
                    """
                    INSERT INTO profiles (
                        id, user_id, height_cm, weight_kg, gender, activity_level, goal_type, target_calories,
                        target_proteins_grams, target_fats_grams, target_carbs_grams, target_weight_kg, weekly_budget, version
                    ) VALUES (?, ?, 180, 80, 'MALE', 'MODERATE', 'MAINTAIN', 2000, 100, 60, 250, 80, 5000, 0)
                    """.trimIndent(),
                ).use { statement ->
                    statement.setObject(1, UUID.randomUUID())
                    statement.setString(2, user.raw.toString())
                    statement.executeUpdate()
                }
        }
    }

    fun awaitCommitted(
        partition: Int,
        offset: Long,
    ) {
        Admin.create(mapOf("bootstrap.servers" to kafka.bootstrapServers)).use { admin ->
            await().atMost(Duration.ofSeconds(15)).untilAsserted {
                val offsets = admin.listConsumerGroupOffsets(GROUP).partitionsToOffsetAndMetadata().get(3, TimeUnit.SECONDS)
                assertTrue((offsets[TopicPartition(TOPIC, partition)]?.offset() ?: -1) > offset)
            }
        }
    }

    override fun afterAll(context: ExtensionContext) {
        closeResources()
    }

    private fun closeResources() {
        val failures =
            generateSequence { resources.pollFirst() }
                .mapNotNull { resource -> runCatching { resource.close() }.exceptionOrNull() }
                .toList()
        if (failures.isNotEmpty()) {
            val log = Path.of(System.getProperty("integration.logs"), "cleanup.log")
            Files.createDirectories(log.parent)
            Files.writeString(log, failures.joinToString("\n") { it.stackTraceToString() })
            throw IllegalStateException("Test environment cleanup failed: $log", failures.first())
        }
    }

    companion object {
        const val TOPIC = "diary.entry.created"
        const val GROUP = "t057-profile-calories"
    }
}
