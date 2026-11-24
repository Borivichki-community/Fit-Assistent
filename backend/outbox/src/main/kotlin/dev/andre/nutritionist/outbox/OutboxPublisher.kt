package dev.andre.nutritionist.outbox

import java.util.concurrent.CompletableFuture

fun interface OutboxPublisher {
    fun publish(
        message: OutboxMessage,
        verifyOwnership: () -> Unit,
    ): CompletableFuture<Unit>

    fun publish(message: OutboxMessage): CompletableFuture<Unit> = publish(message) { }

    fun startBatch() = Unit
}
