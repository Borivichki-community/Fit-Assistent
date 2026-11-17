package dev.andre.nutritionist.profile.outbox

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProfileDeadLetterRecoverer(
    private val jdbc: JdbcTemplate,
    private val environment: Environment,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val MAX_TRACEPARENT_LENGTH = 256
    }

    @Transactional
    fun recover(
        record: ConsumerRecord<*, *>,
        exception: Exception,
    ) {
        val id =
            UUID.nameUUIDFromBytes(
                "${record.topic()}:${record.partition()}:${record.offset()}".toByteArray(Charsets.UTF_8),
            )
        val payload = DeadLetterEnvelope.encode(record, exception)
        jdbc.update(
            """
            INSERT INTO outbox_messages (id, aggregate_type, aggregate_id, partition_key, topic, payload, traceparent)
            VALUES (?, 'ProfileDeadLetter', ?, ?, ?, ?, ?)
            ON CONFLICT (aggregate_type, aggregate_id, topic) DO NOTHING
            """.trimIndent(),
            id,
            id.toString(),
            id.toString(),
            environment.getRequiredProperty("profile.calories.dead-letter-topic"),
            payload,
            record
                .headers()
                .lastHeader("traceparent")
                ?.value()
                ?.toString(Charsets.UTF_8)
                ?.take(MAX_TRACEPARENT_LENGTH),
        )
        logger.warn(
            "Calorie event queued for DLT: topic={}, partition={}, offset={}",
            record.topic(),
            record.partition(),
            record.offset(),
        )
    }
}
