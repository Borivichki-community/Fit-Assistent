package dev.andre.nutritionist.profile.config

import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

/**
 * Конфигурация Kafka Producer для Transactional Outbox (T-024).
 *
 * Гарантирует настройки надежности:
 * - acks = all
 * - enable.idempotence = true
 * - StringSerializer для ключа партиции (userId)
 * - KafkaAvroSerializer с подключением к Schema Registry
 */
@Configuration
class KafkaProducerConfig {
    @Bean
    fun producerFactory(
        @Value("\${spring.kafka.bootstrap-servers:localhost:9092}") bootstrapServers: String,
        @Value("\${spring.kafka.properties.[schema.registry.url]:http://localhost:8081}") schemaRegistryUrl: String,
        @Value("\${spring.kafka.producer.acks:all}") acks: String,
    ): ProducerFactory<String, Any> {
        val configProps =
            mutableMapOf<String, Any>(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to acks,
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                "schema.registry.url" to schemaRegistryUrl,
            )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(factory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> = KafkaTemplate(factory)
}
