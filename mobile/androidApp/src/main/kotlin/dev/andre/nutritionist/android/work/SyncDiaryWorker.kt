package dev.andre.nutritionist.android.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import dev.andre.nutritionist.data.local.dao.DiaryDao
import dev.andre.nutritionist.data.mapper.toDomain
import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.domain.model.SyncStatus
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Фоновый воркер [SyncDiaryWorker] для надежной синхронизации локальных записей дневника
 * со статусом SYNC_PENDING с бэкендом (diary-service).
 *
 * Гарантии:
 * - Запуск строго при наличии сетевого соединения (NetworkType.CONNECTED).
 * - Сохранение оригинального idempotencyKey из Room (запрет генерации нового ключа при повторе).
 * - Экспоненциальный откат (BackoffPolicy.EXPONENTIAL).
 * - Обновление статуса на SYNCED при успехе.
 */
class SyncDiaryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val diaryDao: DiaryDao by inject()
    private val diaryRemoteDataSource: DiaryRemoteDataSource by inject()

    override suspend fun doWork(): Result {
        return runCatching {
            val pendingEntities = diaryDao.getPendingEntries()
            if (pendingEntities.isEmpty()) {
                return Result.success()
            }

            for (entity in pendingEntities) {
                val domainEntry = entity.toDomain()
                val syncResult = diaryRemoteDataSource.syncEntry(domainEntry)
                if (syncResult.isSuccess) {
                    diaryDao.updateSyncStatus(entity.id, SyncStatus.STATUS_SYNCED)
                } else {
                    if (runAttemptCount < MAX_RETRIES) {
                        return Result.retry()
                    } else {
                        diaryDao.updateSyncStatus(entity.id, SyncStatus.STATUS_FAILED)
                    }
                }
            }
            Result.success()
        }.getOrElse {
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val MAX_RETRIES = 5
        const val WORK_NAME = "sync_diary_worker"

        fun buildWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<SyncDiaryWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.SECONDS,
                )
                .build()
        }
    }
}
