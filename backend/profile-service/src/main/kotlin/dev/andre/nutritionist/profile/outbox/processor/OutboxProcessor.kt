package dev.andre.nutritionist.profile.outbox.processor

import dev.andre.nutritionist.contracts.avro.UserRegisteredEvent
import dev.andre.nutritionist.profile.outbox.entity.OutboxMessageEntity
import dev.andre.nutritionist.profile.outbox.model.OutboxStatus
import dev.andre.nutritionist.profile.outbox.repository.OutboxJpaRepository
import dev.andre.nutritionist.profile.outbox.service.OutboxPayloadConverter
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Фоновый публикатор Transactional Outbox (T-024).
 *
 * Требования архитектуры (AGENTS.md §4.1, §4.3):
 * 1. Запуск по расписанию вне бизнес-транзакции.
 * 2. Выборка батча PENDING сообщений с блокировкой (FOR UPDATE SKIP LOCKED).
 * 3. Отправка в Kafka через [KafkaTemplate]:
 *    - Ключ партиции: строго aggregate_id (userId), чтобы гарантировать строгий FIFO порядок внутри партиции!
 *    - acks = all, enable.idempotence = true.
 * 4. Публикация в Kafka выполняется СТРОГО ВНЕ транзакции базы данных.
 * 5. После успешного ACK статус обновляется на PUBLISHED.
 * 6. При ошибке инкрементируются attempts, при превышении maxAttempts статус переводится в FAILED.
 * 7. Метрики Micrometer: Counter outbox.messages.published, Gauge outbox.pending.count.
 */
@Suppress("LongParameterList", "TooGenericExceptionCaught")
@Component
@ConditionalOnProperty(name = ["outbox.enabled"], havingValue = "true", matchIfMissing = true)
class OutboxProcessor(
    private val outboxRepository: OutboxJpaRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val meterRegistry: MeterRegistry,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${outbox.batch-size:50}") private val batchSize: Int = 50,
    @Value("\${outbox.max-attempts:5}") private val maxAttempts: Int = 5,
    @Value("\${outbox.send-timeout-seconds:10}") private val sendTimeoutSeconds: Long = 10,
) {
    private val logger = LoggerFactory.getLogger(OutboxProcessor::class.java)

    private val publishedCounter: Counter =
        Counter
            .builder("outbox.messages.published")
            .description("Count of outbox messages successfully published to Kafka")
            .register(meterRegistry)

    @PostConstruct
    fun registerGauge() {
        Gauge
            .builder("outbox.pending.count") {
                outboxRepository.countByStatus(OutboxStatus.PENDING).toDouble()
            }.description("Number of pending outbox messages waiting to be published")
            .register(meterRegistry)
    }

    /**
     * Основной цикл опроса. Намеренно НЕ @Transactional, чтобы избежать блокировок
     * соединений БД во время сетевых вызовов в Kafka.
     */
    @Scheduled(fixedDelayString = "\${outbox.polling-interval-ms:500}")
    fun processOutbox() {
        try {
            val messages = fetchPendingBatch()
            if (messages.isEmpty()) {
                return
            }

            for (message in messages) {
                processSingleMessage(message)
            }
        } catch (e: Exception) {
            logger.error("Error during outbox processing iteration", e)
        }
    }

    fun fetchPendingBatch(): List<OutboxMessageEntity> =
        transactionTemplate.execute {
            outboxRepository.findByStatusWithLock(
                status = OutboxStatus.PENDING,
                pageable = PageRequest.of(0, batchSize),
            )
        }

    private fun processSingleMessage(message: OutboxMessageEntity) {
        MDC.put("userId", message.aggregateId)
        MDC.put("outboxId", message.id.toString())
        try {
            val payloadObject = deserializePayload(message)

            logger.info(
                "Publishing outbox message: id={}, aggregateId={}, topic={}",
                message.id,
                message.aggregateId,
                message.topic,
            )

            // Отправка в Kafka: ключ партиции строго aggregateId (userId) для гарантии FIFO
            kafkaTemplate
                .send(message.topic, message.aggregateId, payloadObject)
                .get(sendTimeoutSeconds, TimeUnit.SECONDS)

            outboxRepository.markPublished(
                id = message.id,
                status = OutboxStatus.PUBLISHED,
                publishedAt = Instant.now(),
            )
            publishedCounter.increment()

            logger.info("Successfully published outbox message: id={}, topic={}", message.id, message.topic)
        } catch (ex: Exception) {
            logger.error(
                "Failed to publish outbox message: id={}, topic={}, attempts={}",
                message.id,
                message.topic,
                message.attempts + 1,
                ex,
            )
            outboxRepository.recordFailure(
                id = message.id,
                maxAttempts = maxAttempts,
                failedStatus = OutboxStatus.FAILED,
            )
        } finally {
            MDC.remove("userId")
            MDC.remove("outboxId")
        }
    }

    private fun deserializePayload(message: OutboxMessageEntity): Any =
        when (message.topic) {
            USER_REGISTERED_TOPIC -> {
                OutboxPayloadConverter.deserialize<UserRegisteredEvent>(
                    bytes = message.payload,
                    schema = UserRegisteredEvent.getClassSchema(),
                )
            }
            else -> {
                // Для бинарных топиков или других полезных нагрузок
                message.payload
            }
        }

    companion object {
        const val USER_REGISTERED_TOPIC = "user.registered"
    }
}
