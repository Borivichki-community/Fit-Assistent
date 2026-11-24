package dev.andre.nutritionist.outbox

import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.concurrent.CompletionException

class OutboxProcessor(
    private val store: JdbcOutboxStore,
    private val publisher: OutboxPublisher,
    private val settings: OutboxSettings = OutboxSettings(),
    private val clock: Clock = Clock.systemUTC(),
) {
    private val logger = LoggerFactory.getLogger(OutboxProcessor::class.java)

    fun processBatch(): Int =
        store.withExclusiveLock { session ->
            val first = session.nextDue() ?: return@withExclusiveLock 0
            publisher.startBatch()
            generateSequence(first) { if (Thread.currentThread().isInterrupted) null else session.nextDue() }
                .take(settings.batchSize)
                .onEach { pending -> deliver(session, pending) }
                .count()
        }

    private fun deliver(
        session: JdbcOutboxStore.Session,
        pending: JdbcOutboxStore.PendingMessage,
    ) {
        try {
            publisher.publish(pending.message, session::verifyOwnership).join()
        } catch (exception: Exception) {
            val cause = if (exception is CompletionException) exception.cause ?: exception else exception
            session.markFailed(
                pending,
                settings,
                clock.instant().plus(settings.backoff(pending.attempts + 1)),
                cause.javaClass.simpleName,
            )
            logger.warn(
                "Outbox publication failed: id={}, topic={}, key={}, attempt={}, error={}",
                pending.message.id.raw,
                pending.message.topic,
                pending.message.partitionKey,
                pending.attempts + 1,
                cause.javaClass.simpleName,
            )
            return
        }
        session.markPublished(pending.message.id)
        logger.info(
            "Outbox published: id={}, topic={}, key={}",
            pending.message.id.raw,
            pending.message.topic,
            pending.message.partitionKey,
        )
    }
}
