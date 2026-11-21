package dev.andre.nutritionist.domain.model

/**
 * Статус синхронизации локальных записей с бэкендом.
 */
enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED;

    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SYNC_PENDING = "SYNC_PENDING"
        const val STATUS_SYNCED = "SYNCED"
        const val STATUS_FAILED = "FAILED"
    }
}
