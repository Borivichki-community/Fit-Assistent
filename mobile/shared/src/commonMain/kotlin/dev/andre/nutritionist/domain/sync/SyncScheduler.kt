package dev.andre.nutritionist.domain.sync

/**
 * Интерфейс планирования фоновой синхронизации данных.
 */
interface SyncScheduler {
    fun scheduleDiarySync()
}
