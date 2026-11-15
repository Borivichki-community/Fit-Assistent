package dev.andre.nutritionist.diary.persistence.repository

import dev.andre.nutritionist.diary.domain.model.UserId
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionSynchronizationManager
import kotlin.time.Instant

@Repository
class DiaryInitializationRepository(
    private val jdbc: JdbcTemplate,
) {
    fun markProcessed(userId: UserId): Boolean {
        check(TransactionSynchronizationManager.isActualTransactionActive())
        return jdbc.update(
            """
            INSERT INTO processed_events (consumer_name, event_key)
            VALUES ('diary-user-registered', ?)
            ON CONFLICT (consumer_name, event_key) DO NOTHING
            """.trimIndent(),
            "UserRegisteredEvent:${userId.raw}",
        ) == 1
    }

    fun initialize(
        userId: UserId,
        registeredAt: Instant,
    ) {
        check(TransactionSynchronizationManager.isActualTransactionActive())
        jdbc.update(
            """
            INSERT INTO diary_users (user_id, registered_at)
            VALUES (?, ?)
            ON CONFLICT (user_id) DO NOTHING
            """.trimIndent(),
            userId.raw,
            java.sql.Timestamp.from(
                java.time.Instant.ofEpochSecond(registeredAt.epochSeconds, registeredAt.nanosecondsOfSecond.toLong()),
            ),
        )
    }
}
