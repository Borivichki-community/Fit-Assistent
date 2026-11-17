package dev.andre.nutritionist.profile.config

import dev.andre.nutritionist.outbox.JdbcOutboxStore
import dev.andre.nutritionist.outbox.OutboxProcessor
import dev.andre.nutritionist.outbox.OutboxSettings
import dev.andre.nutritionist.profile.outbox.ProfileDeadLetterPublisher
import dev.andre.nutritionist.profile.outbox.ProfileDeadLetterScheduler
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Duration
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "profile.calories", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ProfileDeadLetterConfiguration {
    private companion object {
        const val REQUEST_TIMEOUT_MS = 5000
        const val DELIVERY_TIMEOUT_MS = 10000
        const val BATCH_SIZE = 10
        const val MAX_BACKOFF_MINUTES = 5L
    }

    @Bean
    fun profileDeadLetterProducerFactory(environment: Environment): DefaultKafkaProducerFactory<ByteArray, ByteArray> =
        DefaultKafkaProducerFactory(
            ProfileKafkaConfiguration.kafkaProperties(environment) +
                mapOf(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to
                        environment.getRequiredProperty("spring.kafka.bootstrap-servers"),
                    ProducerConfig.ACKS_CONFIG to "all",
                    ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                    ProducerConfig.MAX_BLOCK_MS_CONFIG to REQUEST_TIMEOUT_MS,
                    ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG to REQUEST_TIMEOUT_MS,
                    ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG to DELIVERY_TIMEOUT_MS,
                ),
            ByteArraySerializer(),
            ByteArraySerializer(),
        )

    @Bean
    fun profileDeadLetterKafkaTemplate(
        profileDeadLetterProducerFactory: DefaultKafkaProducerFactory<ByteArray, ByteArray>,
    ): KafkaTemplate<ByteArray, ByteArray> = KafkaTemplate(profileDeadLetterProducerFactory)

    @Bean
    fun profileDeadLetterStore(dataSource: DataSource): JdbcOutboxStore = JdbcOutboxStore(dataSource)

    @Bean
    fun profileDeadLetterScheduler(
        store: JdbcOutboxStore,
        kafka: KafkaTemplate<ByteArray, ByteArray>,
        environment: Environment,
    ): ProfileDeadLetterScheduler {
        val attempts = environment.getRequiredProperty("profile.calories.dlt-outbox.max-attempts", Int::class.java)
        val settings =
            OutboxSettings(
                batchSize = BATCH_SIZE,
                maxAttempts = attempts,
                initialBackoff = Duration.ofSeconds(1),
                maxBackoff = Duration.ofMinutes(MAX_BACKOFF_MINUTES),
            )
        return ProfileDeadLetterScheduler(OutboxProcessor(store, ProfileDeadLetterPublisher(kafka), settings))
    }
}
