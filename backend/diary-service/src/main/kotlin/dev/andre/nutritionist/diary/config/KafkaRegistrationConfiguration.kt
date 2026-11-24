package dev.andre.nutritionist.diary.config

import dev.andre.nutritionist.contracts.avro.UserRegisteredEvent
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.util.backoff.FixedBackOff
import java.time.Duration

@Configuration(proxyBeanMethods = false)
@EnableKafka
@EnableConfigurationProperties(KafkaRegistrationConfiguration.Settings::class)
@ConditionalOnProperty(prefix = "diary.kafka.registration", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class KafkaRegistrationConfiguration(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
    @Value("\${spring.kafka.properties.[schema.registry.url]}") private val schemaRegistryUrl: String,
    @Value("\${spring.kafka.consumer.group-id:diary-service}") private val groupId: String,
) {
    @Bean
    fun registrationKafkaAdmin(): KafkaAdmin = KafkaAdmin(mapOf(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers))

    @Bean
    fun userRegisteredTopics(settings: Settings): KafkaAdmin.NewTopics =
        KafkaAdmin.NewTopics(
            TopicBuilder
                .name(settings.topic)
                .partitions(settings.partitions)
                .replicas(settings.replicationFactor)
                .build(),
            TopicBuilder
                .name(settings.topic + ".DLT")
                .partitions(settings.partitions)
                .replicas(settings.replicationFactor)
                .build(),
        )

    @Bean
    fun userRegisteredDltProducerFactory(): ProducerFactory<String, Any> {
        val serializers =
            mapOf<Class<*>, Serializer<*>>(
                ByteArray::class.java to ByteArraySerializer(),
                SpecificRecord::class.java to KafkaAvroSerializer(),
            )
        val factory =
            DefaultKafkaProducerFactory<String, Any>(
                mapOf(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                    ProducerConfig.ACKS_CONFIG to "all",
                    ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                    "schema.registry.url" to schemaRegistryUrl,
                    "value.subject.name.strategy" to RecordNameStrategy::class.java,
                ),
                StringSerializer(),
                DelegatingByTypeSerializer(serializers, true),
            )
        return factory
    }

    @Bean
    fun userRegisteredDltTemplate(
        @Qualifier("userRegisteredDltProducerFactory") factory: ProducerFactory<String, Any>,
    ): KafkaTemplate<String, Any> = KafkaTemplate(factory)

    @Bean
    fun userRegisteredKafkaListenerContainerFactory(
        settings: Settings,
        @Qualifier("userRegisteredDltTemplate") template: KafkaTemplate<String, Any>,
    ): ConcurrentKafkaListenerContainerFactory<String, UserRegisteredEvent> {
        val recoverer =
            DeadLetterPublishingRecoverer(template) { record, _ ->
                TopicPartition(settings.topic + ".DLT", record.partition())
            }
        recoverer.setFailIfSendResultIsError(true)
        recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(10))
        val errorHandler =
            DefaultErrorHandler(recoverer, FixedBackOff(settings.retryInterval.toMillis(), settings.retries)).apply {
                setCommitRecovered(true)
                addNotRetryableExceptions(IllegalArgumentException::class.java)
            }
        return ConcurrentKafkaListenerContainerFactory<String, UserRegisteredEvent>().apply {
            setConsumerFactory(
                DefaultKafkaConsumerFactory<String, UserRegisteredEvent>(
                    mapOf(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG to groupId,
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 50,
                        ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to 300000,
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
                        ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS to StringDeserializer::class.java,
                        ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to KafkaAvroDeserializer::class.java,
                        "schema.registry.url" to schemaRegistryUrl,
                        "specific.avro.reader" to true,
                    ),
                ),
            )
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            containerProperties.setDeliveryAttemptHeader(true)
            setCommonErrorHandler(errorHandler)
        }
    }

    @ConfigurationProperties("diary.kafka.registration")
    data class Settings(
        val topic: String = "user.registered",
        val partitions: Int = 3,
        val replicationFactor: Int = 1,
        val retryInterval: Duration = Duration.ofSeconds(1),
        val retries: Long = 2,
    ) {
        init {
            require(topic.isNotBlank() && partitions > 0 && replicationFactor > 0)
            require(!retryInterval.isNegative && retries >= 0)
        }
    }
}
