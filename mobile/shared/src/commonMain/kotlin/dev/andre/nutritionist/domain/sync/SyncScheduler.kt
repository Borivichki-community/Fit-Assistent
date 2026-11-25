package dev.andre.nutritionist.domain.sync

/**
 * Контракт планирования фоновой синхронизации отложенных (PENDING) записей дневника.
 * Платформенная реализация для Android использует WorkManager.
 */
interface SyncScheduler {
    fun scheduleDiarySync()
}
