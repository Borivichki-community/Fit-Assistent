package dev.andre.nutritionist.android.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import dev.andre.nutritionist.domain.sync.SyncScheduler

/**
 * Android WorkManager реализация [SyncScheduler].
 */
class WorkManagerSyncScheduler(
    context: Context,
) : SyncScheduler {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun scheduleDiarySync() {
        workManager.enqueueUniqueWork(
            SyncDiaryWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            SyncDiaryWorker.buildWorkRequest(),
        )
    }
}
