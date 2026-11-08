package dev.andre.nutritionist.contracts.domain

import kotlinx.datetime.Instant
import kotlin.jvm.JvmInline

/**
 * Строго типизированные идентификаторы сущностей системы.
 * Согласно AGENTS.md §1.3, использование сырых String/Int запрещено.
 */
@JvmInline
value class UserId(
    val raw: String,
) {
    init {
        require(raw.isNotBlank()) { "UserId cannot be blank" }
    }
}

@JvmInline
value class MealId(
    val raw: String,
) {
    init {
        require(raw.isNotBlank()) { "MealId cannot be blank" }
    }
}

/**
 * Доменная сущность пользователя системы.
 * Полностью изолирована от транспортных форматов (Avro, Protobuf, JSON).
 */
data class User(
    val id: UserId,
    val email: String,
    val registeredAt: Instant,
    val source: String? = null,
)

/**
 * Доменная сущность фиксации приёма пищи и потреблённых калорий.
 */
data class Meal(
    val userId: UserId,
    val mealId: MealId,
    val calories: Int,
    val timestamp: Instant,
    val mealType: String? = null,
    val notes: String? = null,
)
