package dev.andre.nutritionist.contracts.mapper

import dev.andre.nutritionist.contracts.avro.CaloriesConsumedEvent
import dev.andre.nutritionist.contracts.avro.UserRegisteredEvent
import dev.andre.nutritionist.contracts.domain.Meal
import dev.andre.nutritionist.contracts.domain.MealId
import dev.andre.nutritionist.contracts.domain.User
import dev.andre.nutritionist.contracts.domain.UserId
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * Изолирующий слой маппинга между транспортными Avro-контрактами и Domain-сущностями.
 * Согласно AGENTS.md §2.2, сгенерированные DTO/Avro классы не должны попадать в Domain.
 */
object UserRegisteredEventMapper {
    fun toDomain(event: UserRegisteredEvent): User {
        val rawId = requireNotNull(event.id) { "UserRegisteredEvent.id must not be null" }.toString()
        val email = requireNotNull(event.email) { "UserRegisteredEvent.email must not be null" }
        val instant =
            requireNotNull(event.registrationTimestamp) {
                "UserRegisteredEvent.registrationTimestamp must not be null"
            }

        return User(
            id = UserId(rawId),
            email = email,
            registeredAt = Instant.fromEpochMilliseconds(instant.toEpochMilli()),
            source = event.source,
        )
    }

    fun toAvro(domain: User): UserRegisteredEvent {
        val uuid =
            runCatching { UUID.fromString(domain.id.raw) }
                .getOrElse { UUID.nameUUIDFromBytes(domain.id.raw.toByteArray()) }

        return UserRegisteredEvent
            .newBuilder()
            .setId(uuid)
            .setEmail(domain.email)
            .setRegistrationTimestamp(java.time.Instant.ofEpochMilli(domain.registeredAt.toEpochMilliseconds()))
            .setSource(domain.source)
            .build()
    }
}

object CaloriesConsumedEventMapper {
    fun toDomain(event: CaloriesConsumedEvent): Meal {
        val userRawId = requireNotNull(event.userId) { "CaloriesConsumedEvent.userId must not be null" }.toString()
        val mealRawId = requireNotNull(event.mealId) { "CaloriesConsumedEvent.mealId must not be null" }.toString()
        val instant = requireNotNull(event.timestamp) { "CaloriesConsumedEvent.timestamp must not be null" }

        return Meal(
            userId = UserId(userRawId),
            mealId = MealId(mealRawId),
            calories = event.calories,
            timestamp = Instant.fromEpochMilliseconds(instant.toEpochMilli()),
            mealType = event.mealType,
            notes = event.notes,
        )
    }

    fun toAvro(domain: Meal): CaloriesConsumedEvent {
        val userUuid =
            runCatching { UUID.fromString(domain.userId.raw) }
                .getOrElse { UUID.nameUUIDFromBytes(domain.userId.raw.toByteArray()) }
        val mealUuid =
            runCatching { UUID.fromString(domain.mealId.raw) }
                .getOrElse { UUID.nameUUIDFromBytes(domain.mealId.raw.toByteArray()) }

        return CaloriesConsumedEvent
            .newBuilder()
            .setUserId(userUuid)
            .setMealId(mealUuid)
            .setCalories(domain.calories)
            .setTimestamp(java.time.Instant.ofEpochMilli(domain.timestamp.toEpochMilliseconds()))
            .setMealType(domain.mealType)
            .setNotes(domain.notes)
            .build()
    }
}
