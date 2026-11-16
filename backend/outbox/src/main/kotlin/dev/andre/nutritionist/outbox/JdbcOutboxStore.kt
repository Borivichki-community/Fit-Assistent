package dev.andre.nutritionist.outbox

import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class JdbcOutboxStore(
    private val dataSource: DataSource,
) {
    fun withExclusiveLock(action: (Session) -> Int): Int =
        dataSource.connection.use { connection ->
            check(connection.autoCommit) { "Outbox delivery requires an autocommit connection" }
            if (!lockQuery(connection, "pg_try_advisory_lock")) {
                return@use 0
            }
            try {
                action(Session(connection))
            } finally {
                try {
                    check(lockQuery(connection, "pg_advisory_unlock")) { "Outbox publisher lock was lost" }
                } catch (exception: Exception) {
                    connection.abort { it.run() }
                    throw exception
                }
            }
        }

    fun retryFailed(id: OutboxMessageId): Boolean =
        withExclusiveLock { session ->
            session.connection
                .prepareStatement(
                    """
                    UPDATE outbox_messages
                    SET status = 'PENDING', attempts = 0, next_attempt_at = CURRENT_TIMESTAMP, last_error = NULL
                    WHERE id = ? AND status = 'FAILED'
                    """.trimIndent(),
                ).use { statement ->
                    statement.setObject(1, id.raw)
                    statement.executeUpdate()
                }
        } == 1

    private fun lockQuery(
        connection: Connection,
        function: String,
    ): Boolean =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT $function(hashtextextended('nutritionist.outbox.publisher', 0))").use { result ->
                check(result.next())
                result.getBoolean(1)
            }
        }

    data class PendingMessage(
        val message: OutboxMessage,
        val attempts: Int,
    )

    class Session internal constructor(
        internal val connection: Connection,
    ) {
        fun verifyOwnership() {
            connection.createStatement().use { statement ->
                statement
                    .executeQuery(
                        """
                        SELECT EXISTS (
                            SELECT 1 FROM pg_locks
                            WHERE locktype = 'advisory' AND pid = pg_backend_pid() AND granted AND objsubid = 1
                              AND classid::bigint = ((hashtextextended('nutritionist.outbox.publisher', 0) >> 32) & 4294967295)
                              AND objid::bigint = (hashtextextended('nutritionist.outbox.publisher', 0) & 4294967295)
                        )
                        """.trimIndent(),
                    ).use { result ->
                        check(result.next() && result.getBoolean(1)) { "Outbox publisher lock was lost" }
                    }
            }
        }

        fun nextDue(): PendingMessage? =
            connection.createStatement().use { statement ->
                statement
                    .executeQuery(
                        """
                        SELECT m.id, m.topic, m.partition_key, m.payload, m.traceparent, m.attempts
                        FROM outbox_messages m
                        WHERE m.status = 'PENDING' AND m.next_attempt_at <= CURRENT_TIMESTAMP
                          AND NOT EXISTS (
                              SELECT 1 FROM outbox_messages earlier
                              WHERE earlier.topic = m.topic AND earlier.partition_key = m.partition_key
                                AND earlier.sequence_number < m.sequence_number AND earlier.status <> 'PUBLISHED'
                          )
                        ORDER BY m.sequence_number
                        LIMIT 1
                        """.trimIndent(),
                    ).use { row ->
                        if (!row.next()) {
                            return@use null
                        }
                        PendingMessage(
                            OutboxMessage(
                                OutboxMessageId(row.getObject("id", UUID::class.java)),
                                row.getString("topic"),
                                row.getString("partition_key"),
                                row.getBytes("payload"),
                                row.getString("traceparent"),
                            ),
                            row.getInt("attempts"),
                        )
                    }
            }

        fun markPublished(id: OutboxMessageId) {
            connection
                .prepareStatement(
                    """
                    UPDATE outbox_messages
                    SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP, attempts = attempts + 1, last_error = NULL
                    WHERE id = ? AND status = 'PENDING'
                    """.trimIndent(),
                ).use { statement ->
                    statement.setObject(1, id.raw)
                    check(statement.executeUpdate() == 1) { "Pending outbox message disappeared" }
                }
        }

        fun markFailed(
            pending: PendingMessage,
            settings: OutboxSettings,
            retryAt: Instant,
            errorType: String,
        ) {
            val attempts = pending.attempts + 1
            connection
                .prepareStatement(
                    """
                    UPDATE outbox_messages
                    SET status = ?, attempts = ?, next_attempt_at = ?, last_error = ?
                    WHERE id = ? AND status = 'PENDING'
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, if (attempts >= settings.maxAttempts) "FAILED" else "PENDING")
                    statement.setInt(2, attempts)
                    statement.setTimestamp(3, Timestamp.from(retryAt))
                    statement.setString(4, errorType.take(256))
                    statement.setObject(5, pending.message.id.raw)
                    check(statement.executeUpdate() == 1) { "Pending outbox message disappeared" }
                }
        }
    }
}
