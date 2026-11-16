package dev.andre.nutritionist.outbox

import java.util.UUID

data class OutboxMessage(
    val id: OutboxMessageId,
    val topic: String,
    val partitionKey: String,
    val payload: ByteArray,
    val traceparent: String?,
) {
    init {
        require(topic.isNotBlank()) { "Outbox topic must not be blank" }
        require(partitionKey.isNotBlank()) { "Outbox partition key must not be blank" }
        require(payload.isNotEmpty()) { "Outbox payload must not be empty" }
    }
}

@JvmInline
value class OutboxMessageId(
    val raw: UUID,
)
