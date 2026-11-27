package dev.andre.nutritionist.android.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import dev.andre.nutritionist.domain.sync.SyncScheduler

/**
 * Android-реализация [SyncScheduler] на базе WorkManager.
 * Использует ExistingWorkPolicy.KEEP для предотвращения дублирования фоновых задач.
 */
class WorkManagerSyncScheduler(
    private val context: Context,
) : SyncScheduler {

    override fun scheduleDiarySync() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorkerConstants.SYNC_DIARY_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            SyncDiaryWorker.buildWorkRequest(),
        )
    }
}
