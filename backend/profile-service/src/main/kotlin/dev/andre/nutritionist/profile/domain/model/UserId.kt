package dev.andre.nutritionist.profile.domain.model

/**
 * Идентификатор пользователя в соответствии с AGENTS.md §1.3.
 */
@JvmInline
value class UserId(
    val raw: String,
)
