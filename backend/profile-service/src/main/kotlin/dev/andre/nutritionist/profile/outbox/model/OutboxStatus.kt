package dev.andre.nutritionist.profile.outbox.model

/**
 * Жизненный цикл сообщений в Transactional Outbox (T-024).
 */
enum class OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
}
