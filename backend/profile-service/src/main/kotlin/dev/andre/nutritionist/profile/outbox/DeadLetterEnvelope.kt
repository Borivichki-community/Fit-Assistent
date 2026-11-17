package dev.andre.nutritionist.profile.outbox

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object DeadLetterEnvelope {
    private const val DIAGNOSTIC_HEADER_COUNT = 4

    fun encode(
        record: ConsumerRecord<*, *>,
        exception: Exception,
    ): ByteArray {
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { output ->
            writeBytes(output, record.key() as ByteArray?)
            writeBytes(output, record.value() as ByteArray?)
            val headers = record.headers().toList()
            output.writeInt(headers.size + DIAGNOSTIC_HEADER_COUNT)
            headers.forEach { header ->
                output.writeUTF(header.key())
                writeBytes(output, header.value())
            }
            mapOf(
                "profile-original-topic" to record.topic(),
                "profile-original-partition" to record.partition().toString(),
                "profile-original-offset" to record.offset().toString(),
                "profile-failure-type" to exception.javaClass.name,
            ).forEach { (key, value) ->
                output.writeUTF(key)
                writeBytes(output, value.toByteArray(Charsets.UTF_8))
            }
        }
        return buffer.toByteArray()
    }

    fun decode(
        topic: String,
        payload: ByteArray,
    ): ProducerRecord<ByteArray, ByteArray> =
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            val key = readBytes(input)
            val value = readBytes(input)
            val headers = RecordHeaders()
            val count = input.readInt()
            require(count in 0..payload.size) { "Invalid DLT header count" }
            repeat(count) {
                headers.add(input.readUTF(), readBytes(input))
            }
            require(input.available() == 0) { "Trailing DLT envelope bytes" }
            ProducerRecord(topic, null, key, value, headers)
        }

    private fun writeBytes(
        output: DataOutputStream,
        value: ByteArray?,
    ) {
        output.writeInt(value?.size ?: -1)
        value?.let(output::write)
    }

    private fun readBytes(input: DataInputStream): ByteArray? {
        val size = input.readInt()
        if (size == -1) return null
        require(size in 0..input.available()) { "Invalid DLT field size" }
        return ByteArray(size).also(input::readFully)
    }
}
