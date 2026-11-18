package dev.andre.nutritionist.profile.config

import dev.andre.nutritionist.profile.consumer.CaloriesConsumedEventConsumer
import dev.andre.nutritionist.profile.consumer.CaloriesConsumedEventDecoder
import dev.andre.nutritionist.profile.outbox.ProfileDeadLetterRecoverer
import dev.andre.nutritionist.profile.service.CaloriesConsumptionService
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries

@Configuration(proxyBeanMethods = false)
@EnableKafka
@ConditionalOnProperty(prefix = "profile.calories", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ProfileKafkaConfiguration {
    @Bean
    fun profileCaloriesListenerFactory(
        environment: Environment,
        recoverer: ProfileDeadLetterRecoverer,
    ): ConcurrentKafkaListenerContainerFactory<ByteArray, ByteArray> {
        val attempts = environment.getRequiredProperty("profile.calories.max-attempts", Int::class.java)
        require(attempts in 1..MAX_ATTEMPTS) { "Invalid calorie consumer attempts" }
        val initialBackoff = environment.getRequiredProperty("profile.calories.initial-backoff-ms", Long::class.java)
        val backoff =
            ExponentialBackOffWithMaxRetries(attempts - 1).apply {
                initialInterval = initialBackoff
                multiplier = 2.0
                maxInterval = environment.getRequiredProperty("profile.calories.max-backoff-ms", Long::class.java)
            }
        require(backoff.initialInterval > 0 && backoff.maxInterval >= backoff.initialInterval)
        val handler = DefaultErrorHandler({ record, exception -> recoverer.recover(record, exception) }, backoff)
        handler.addNotRetryableExceptions(IllegalArgumentException::class.java)
        val properties =
            kafkaProperties(environment) +
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to
                        environment.getRequiredProperty("spring.kafka.bootstrap-servers"),
                    ConsumerConfig.GROUP_ID_CONFIG to environment.getRequiredProperty("profile.calories.group-id"),
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                    ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed",
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG to MAX_POLL_RECORDS,
                )
        return ConcurrentKafkaListenerContainerFactory<ByteArray, ByteArray>().apply {
            setConsumerFactory(
                DefaultKafkaConsumerFactory(properties, ByteArrayDeserializer(), ByteArrayDeserializer()),
            )
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD
            setCommonErrorHandler(handler)
        }
    }

    @Bean(destroyMethod = "close")
    fun caloriesConsumedEventDecoder(environment: Environment): CaloriesConsumedEventDecoder {
        val deserializer = KafkaAvroDeserializer()
        deserializer.configure(
            kafkaProperties(environment) + mapOf("specific.avro.reader" to true),
            false,
        )
        return CaloriesConsumedEventDecoder(deserializer)
    }

    @Bean
    fun caloriesConsumedEventConsumer(
        decoder: CaloriesConsumedEventDecoder,
        service: CaloriesConsumptionService,
    ): CaloriesConsumedEventConsumer = CaloriesConsumedEventConsumer(decoder, service)

    @Bean
    fun profileCaloriesDeadLetterTopic(environment: Environment) =
        TopicBuilder
            .name(environment.getRequiredProperty("profile.calories.dead-letter-topic"))
            .partitions(environment.getProperty("profile.calories.dlt-partitions", Int::class.java, 1))
            .replicas(environment.getProperty("profile.calories.dlt-replicas", Int::class.java, 1))
            .build()

    companion object {
        private const val MAX_ATTEMPTS = 100
        private const val MAX_POLL_RECORDS = 10

        fun kafkaProperties(environment: Environment): Map<String, Any> =
            Binder
                .get(environment)
                .bind("spring.kafka.properties", Bindable.mapOf(String::class.java, String::class.java))
                .orElse(emptyMap())
                .orEmpty()
    }
}
