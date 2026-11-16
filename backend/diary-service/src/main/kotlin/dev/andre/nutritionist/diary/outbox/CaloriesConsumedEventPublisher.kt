package dev.andre.nutritionist.diary.outbox

import dev.andre.nutritionist.contracts.avro.CaloriesConsumedEvent
import dev.andre.nutritionist.outbox.OutboxMessage
import dev.andre.nutritionist.outbox.OutboxPublisher
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID
import java.util.concurrent.CompletableFuture

class CaloriesConsumedEventPublisher(
    private val kafka: KafkaTemplate<String, CaloriesConsumedEvent>,
) : OutboxPublisher {
    override fun startBatch() {
        check(!TransactionSynchronizationManager.isActualTransactionActive()) { "Outbox publication requires no database transaction" }
        kafka.producerFactory.reset()
    }

    override fun publish(
        message: OutboxMessage,
        verifyOwnership: () -> Unit,
    ): CompletableFuture<Unit> {
        check(!TransactionSynchronizationManager.isActualTransactionActive()) { "Outbox publication requires no database transaction" }
        require(message.topic == "diary.entry.created") { "Unsupported diary outbox topic" }
        val decoder = DecoderFactory.get().binaryDecoder(message.payload, null)
        val event = SpecificDatumReader<CaloriesConsumedEvent>(CaloriesConsumedEvent.getClassSchema()).read(null, decoder)
        require(decoder.isEnd) { "Unexpected bytes after CaloriesConsumedEvent" }
        require(UUID.fromString(message.partitionKey) == event.userId) { "Partition key must equal event userId" }
        val record = ProducerRecord(message.topic, event.userId.toString(), event)
        message.traceparent?.let { record.headers().add("traceparent", it.toByteArray(Charsets.UTF_8)) }
        return try {
            kafka.executeInTransaction { operations ->
                verifyOwnership()
                operations.send(record).join()
            }
            CompletableFuture.completedFuture(Unit)
        } catch (exception: Exception) {
            CompletableFuture.failedFuture(exception)
        }
    }
}
