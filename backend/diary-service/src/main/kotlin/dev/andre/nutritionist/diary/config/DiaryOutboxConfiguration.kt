package dev.andre.nutritionist.diary.config

import dev.andre.nutritionist.contracts.avro.CaloriesConsumedEvent
import dev.andre.nutritionist.diary.outbox.CaloriesConsumedEventPublisher
import dev.andre.nutritionist.outbox.JdbcOutboxStore
import dev.andre.nutritionist.outbox.OutboxProcessor
import dev.andre.nutritionist.outbox.OutboxPublisher
import dev.andre.nutritionist.outbox.OutboxSettings
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.DefaultTransactionIdSuffixStrategy
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
class DiaryOutboxConfiguration {
    @Bean
    fun outboxSettings(environment: Environment): OutboxSettings =
        OutboxSettings(
            environment.getProperty("diary.outbox.batch-size", Int::class.java, 100),
            environment.getProperty("diary.outbox.max-attempts", Int::class.java, 10),
            Duration.parse(environment.getProperty("diary.outbox.initial-backoff", "PT1S")),
            Duration.parse(environment.getProperty("diary.outbox.max-backoff", "PT5M")),
        )

    @Bean
    fun jdbcOutboxStore(dataSource: DataSource): JdbcOutboxStore = JdbcOutboxStore(dataSource)

    @Bean
    fun outboxProcessor(
        store: JdbcOutboxStore,
        publisher: OutboxPublisher,
        settings: OutboxSettings,
    ): OutboxProcessor = OutboxProcessor(store, publisher, settings)

    @Bean
    fun diaryOutboxProducerFactory(environment: Environment): DefaultKafkaProducerFactory<String, CaloriesConsumedEvent> {
        val binder = Binder.get(environment)
        val properties =
            binder
                .bind("spring.kafka.properties", Bindable.mapOf(String::class.java, String::class.java))
                .orElse(null)
                .orEmpty() +
                binder
                    .bind("spring.kafka.producer.properties", Bindable.mapOf(String::class.java, String::class.java))
                    .orElse(null)
                    .orEmpty() +
                mapOf(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to environment.getRequiredProperty("spring.kafka.bootstrap-servers"),
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to
                        environment.getRequiredProperty("spring.kafka.producer.key-serializer"),
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to
                        environment.getRequiredProperty("spring.kafka.producer.value-serializer"),
                    ProducerConfig.ACKS_CONFIG to environment.getProperty("spring.kafka.producer.acks", "all"),
                )
        return DefaultKafkaProducerFactory<String, CaloriesConsumedEvent>(properties).apply {
            setTransactionIdPrefix(environment.getRequiredProperty("diary.outbox.transaction-id-prefix"))
            setTransactionIdSuffixStrategy(DefaultTransactionIdSuffixStrategy(1))
        }
    }

    @Bean
    fun diaryOutboxKafkaTemplate(
        diaryOutboxProducerFactory: DefaultKafkaProducerFactory<String, CaloriesConsumedEvent>,
    ): KafkaTemplate<String, CaloriesConsumedEvent> = KafkaTemplate(diaryOutboxProducerFactory)

    @Bean
    fun caloriesConsumedEventPublisher(diaryOutboxKafkaTemplate: KafkaTemplate<String, CaloriesConsumedEvent>): OutboxPublisher =
        CaloriesConsumedEventPublisher(diaryOutboxKafkaTemplate)
}
