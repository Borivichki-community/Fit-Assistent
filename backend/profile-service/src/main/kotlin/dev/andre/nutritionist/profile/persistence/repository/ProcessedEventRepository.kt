package dev.andre.nutritionist.profile.persistence.repository

import dev.andre.nutritionist.contracts.domain.Meal
import dev.andre.nutritionist.profile.domain.model.UserId
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class ProcessedEventRepository(
    private val jdbc: JdbcTemplate,
) {
    fun insert(
        meal: Meal,
        hash: String,
    ): Boolean =
        jdbc.update(
            """
            INSERT INTO processed_events (consumer_name, user_id, meal_id, calories, consumed_at, payload_hash)
            VALUES ('profile-calories', ?, ?, ?, ?, ?)
            ON CONFLICT (consumer_name, user_id, meal_id) DO NOTHING
            """.trimIndent(),
            UUID.fromString(meal.userId.raw),
            UUID.fromString(meal.mealId.raw),
            meal.calories,
            Timestamp.from(Instant.ofEpochMilli(meal.timestamp.toEpochMilliseconds())),
            hash,
        ) == 1

    fun matches(
        meal: Meal,
        hash: String,
    ): Boolean =
        jdbc.queryForObject(
            """
            SELECT payload_hash = ? FROM processed_events
            WHERE consumer_name = 'profile-calories' AND user_id = ? AND meal_id = ?
            """.trimIndent(),
            Boolean::class.java,
            hash,
            UUID.fromString(meal.userId.raw),
            UUID.fromString(meal.mealId.raw),
        ) == true

    fun sum(
        userId: UserId,
        from: Instant,
        until: Instant,
    ): Long =
        jdbc.queryForObject(
            """
            SELECT COALESCE(SUM(calories), 0) FROM processed_events
            WHERE consumer_name = 'profile-calories' AND user_id = ? AND consumed_at >= ? AND consumed_at < ?
            """.trimIndent(),
            Long::class.java,
            UUID.fromString(userId.raw),
            Timestamp.from(from),
            Timestamp.from(until),
        ) ?: 0L
}
