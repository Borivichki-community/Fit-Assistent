package dev.andre.nutritionist.diary.outbox

import dev.andre.nutritionist.contracts.avro.CaloriesConsumedEvent
import dev.andre.nutritionist.diary.domain.model.DiaryEntry
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID

@Repository
class DiaryOutboxRepository(
    private val jdbc: JdbcTemplate,
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun append(
        entry: DiaryEntry,
        traceparent: String?,
    ) {
        val event =
            CaloriesConsumedEvent
                .newBuilder()
                .setUserId(UUID.fromString(entry.userId.raw))
                .setMealId(entry.id.raw)
                .setCalories(entry.nutrition.calories)
                .setTimestamp(Instant.ofEpochSecond(entry.recordedAt.epochSeconds, entry.recordedAt.nanosecondsOfSecond.toLong()))
                .setMealType(entry.mealType.name)
                .build()
        val payload =
            ByteArrayOutputStream().use { output ->
                val encoder = EncoderFactory.get().binaryEncoder(output, null)
                SpecificDatumWriter<CaloriesConsumedEvent>(CaloriesConsumedEvent.getClassSchema()).write(event, encoder)
                encoder.flush()
                output.toByteArray()
            }
        jdbc.update(
            """
            INSERT INTO outbox_messages
                (id, aggregate_type, aggregate_id, partition_key, topic, payload, traceparent)
            VALUES (?, 'DiaryEntry', ?, ?, 'diary.entry.created', ?, ?)
            """.trimIndent(),
            UUID.randomUUID(),
            entry.id.raw.toString(),
            entry.userId.raw,
            payload,
            traceparent,
        )
    }
}
