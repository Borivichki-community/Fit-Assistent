package dev.andre.nutritionist.profile

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvroCompatibilityTest {
    @Test
    fun `serializer round trip preserves schema and data without external services`() {
        val schema =
            Schema.Parser().parse(
                """
                {"type":"record","name":"CompatibilityEvent","namespace":"verification",
                 "fields":[{"name":"message","type":"string"}]}
                """.trimIndent(),
            )
        val event = GenericData.Record(schema).apply { put("message", "compatible") }
        // Проверяем реальные serializer/deserializer с локальным хранилищем схем.
        // Сетевой доступ к Kafka и Schema Registry этот тест не проверяет.
        val registry = MockSchemaRegistryClient()
        val config = mapOf("schema.registry.url" to "mock://compatibility")

        KafkaAvroSerializer(registry).use { serializer ->
            KafkaAvroDeserializer(registry).use { deserializer ->
                serializer.configure(config, false)
                deserializer.configure(config, false)

                val bytes = serializer.serialize("compatibility", event)
                val decoded = deserializer.deserialize("compatibility", bytes) as GenericRecord

                assertEquals(schema, decoded.schema)
                assertEquals("compatible", decoded.get("message").toString())
            }
        }
    }
}
