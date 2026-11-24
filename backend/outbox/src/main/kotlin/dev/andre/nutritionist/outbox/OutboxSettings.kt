package dev.andre.nutritionist.outbox

import java.time.Duration

data class OutboxSettings(
    val batchSize: Int = 100,
    val maxAttempts: Int = 10,
    val initialBackoff: Duration = Duration.ofSeconds(1),
    val maxBackoff: Duration = Duration.ofMinutes(5),
) {
    init {
        require(batchSize in 1..10_000) { "Invalid outbox batch size" }
        require(maxAttempts in 1..1_000) { "Invalid outbox max attempts" }
        require(initialBackoff.toMillis() > 0) { "Outbox backoff must be positive" }
        require(maxBackoff >= initialBackoff && maxBackoff <= Duration.ofDays(1)) { "Invalid outbox max backoff" }
    }

    fun backoff(attempt: Int): Duration {
        require(attempt > 0) { "Attempt must be positive" }
        val multiplier = 1L shl (attempt - 1).coerceAtMost(30)
        val millis = initialBackoff.toMillis()
        val cap = maxBackoff.toMillis()
        return Duration.ofMillis(if (multiplier > cap / millis) cap else millis * multiplier)
    }
}
