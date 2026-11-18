package dev.andre.nutritionist.profile.consumer

import dev.andre.nutritionist.contracts.avro.CaloriesConsumedEvent
import dev.andre.nutritionist.contracts.domain.Meal
import dev.andre.nutritionist.contracts.mapper.CaloriesConsumedEventMapper
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.util.UUID

class CaloriesConsumedEventDecoder(
    private val deserializer: KafkaAvroDeserializer,
) : AutoCloseable {
    fun decode(record: ConsumerRecord<ByteArray, ByteArray>): Meal {
        val payload = requireNotNull(record.value()) { "Null calorie event" }
        require(payload.size >= CONFLUENT_HEADER_SIZE && payload[0] == 0.toByte()) { "Invalid Confluent Avro frame" }
        val event = deserializer.deserialize(record.topic(), record.headers(), payload)
        require(event is CaloriesConsumedEvent) { "Unexpected event type" }
        val meal = CaloriesConsumedEventMapper.toDomain(event)
        val key = requireNotNull(record.key()) { "Missing user partition key" }.toString(Charsets.UTF_8)
        require(UUID.fromString(key) == event.userId) { "Partition key must match userId" }
        require(meal.calories >= 0) { "Calories must be non-negative" }
        return meal
    }

    override fun close() = deserializer.close()

    private companion object {
        const val CONFLUENT_HEADER_SIZE = 5
    }
}
