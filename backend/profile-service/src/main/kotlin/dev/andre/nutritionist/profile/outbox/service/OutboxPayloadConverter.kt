package dev.andre.nutritionist.profile.outbox.service

import org.apache.avro.Schema
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.avro.specific.SpecificRecord
import java.io.ByteArrayOutputStream

/**
 * Локальная сериализация и десериализация Avro SpecificRecord в бинарный формат bytea.
 * Согласно AGENTS.md §4.1, выполняется строго без сетевых обращений к Schema Registry внутри транзакции.
 */
object OutboxPayloadConverter {
    fun <T : SpecificRecord> serialize(record: T): ByteArray {
        val out = ByteArrayOutputStream()
        val writer = SpecificDatumWriter<T>(record.schema)
        val encoder = EncoderFactory.get().binaryEncoder(out, null)
        writer.write(record, encoder)
        encoder.flush()
        return out.toByteArray()
    }

    fun <T : SpecificRecord> deserialize(
        bytes: ByteArray,
        schema: Schema,
    ): T {
        val reader = SpecificDatumReader<T>(schema)
        val decoder = DecoderFactory.get().binaryDecoder(bytes, null)
        return reader.read(null, decoder)
    }
}
