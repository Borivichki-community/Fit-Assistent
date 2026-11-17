package dev.andre.nutritionist.profile.outbox

import dev.andre.nutritionist.outbox.OutboxMessage
import dev.andre.nutritionist.outbox.OutboxPublisher
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.CompletableFuture

class ProfileDeadLetterPublisher(
    private val kafka: KafkaTemplate<ByteArray, ByteArray>,
) : OutboxPublisher {
    override fun publish(
        message: OutboxMessage,
        verifyOwnership: () -> Unit,
    ): CompletableFuture<Unit> {
        check(!TransactionSynchronizationManager.isActualTransactionActive()) {
            "DLT publication requires no DB transaction"
        }
        verifyOwnership()
        return kafka.send(DeadLetterEnvelope.decode(message.topic, message.payload)).thenApply { }
    }
}
