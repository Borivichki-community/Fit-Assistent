package dev.andre.nutritionist.android.work

/**
 * Пакет для фоновых воркеров WorkManager (SyncDiaryWorker).
 * Запуск фоновой синхронизации Room PENDING записей с gRPC-бэкендом
 * с сохранением idempotency-key и BackoffPolicy.EXPONENTIAL.
 */
object SyncWorkerConstants {
    const val SYNC_DIARY_WORK_NAME = "sync_diary_worker"
}
