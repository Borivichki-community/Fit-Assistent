package dev.andre.nutritionist.android.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import dev.andre.nutritionist.data.local.dao.MealRecordDao
import dev.andre.nutritionist.data.local.entity.MealRecordEntity
import dev.andre.nutritionist.data.mapper.MealRecordEntityMapper.toDomain
import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.SyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Воркер фоновой синхронизации записей дневника (PENDING -> SYNCED / FAILED).
 * Повторно отправляет PENDING-записи в diary-service, сохраняя исходный idempotencyKey.
 */
class SyncDiaryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val mealRecordDao: MealRecordDao by inject()
    private val diaryRemoteDataSource: DiaryRemoteDataSource by inject()
    private val dispatchers: CoroutineDispatchers by inject()

    override suspend fun doWork(): Result =
        syncPendingEntries(
            mealRecordDao = mealRecordDao,
            diaryRemoteDataSource = diaryRemoteDataSource,
            dispatchers = dispatchers,
            runAttemptCount = runAttemptCount,
        )

    companion object {
        const val MAX_RETRIES = 5
        const val BACKOFF_DELAY_SECONDS = 10L

        fun buildWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<SyncDiaryWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_SECONDS,
                    TimeUnit.SECONDS,
                )
                .build()
        }

        internal suspend fun syncPendingEntries(
            mealRecordDao: MealRecordDao,
            diaryRemoteDataSource: DiaryRemoteDataSource,
            dispatchers: CoroutineDispatchers,
            runAttemptCount: Int,
        ): Result = withContext(dispatchers.io) {
            try {
                val pendingRecords = mealRecordDao.getPendingRecords()
                val pendingDeletions = mealRecordDao.getPendingDeletedRecords()
                if (pendingRecords.isEmpty() && pendingDeletions.isEmpty()) {
                    return@withContext Result.success()
                }

                val failedAdditions = mutableListOf<MealRecordEntity>()
                for (entity in pendingRecords) {
                    val domainRecord = entity.toDomain()
                    val remoteResult = diaryRemoteDataSource.addEntry(domainRecord)
                    if (remoteResult.isSuccess) {
                        mealRecordDao.updateSyncStatus(entity.id, SyncStatus.SYNCED.name)
                    } else {
                        val error = remoteResult.exceptionOrNull()
                        if (error is CancellationException) throw error
                        failedAdditions.add(entity)
                    }
                }

                val failedDeletions = mutableListOf<MealRecordEntity>()
                for (entity in pendingDeletions) {
                    val remoteResult = diaryRemoteDataSource.deleteMeal(entity.id)
                    if (remoteResult.isSuccess) {
                        mealRecordDao.deletePermanently(entity.id)
                    } else {
                        val error = remoteResult.exceptionOrNull()
                        if (error is CancellationException) throw error
                        failedDeletions.add(entity)
                    }
                }

                if (failedAdditions.isEmpty() && failedDeletions.isEmpty()) {
                    return@withContext Result.success()
                }

                if (runAttemptCount >= MAX_RETRIES) {
                    for (failed in failedAdditions) {
                        mealRecordDao.updateSyncStatus(failed.id, SyncStatus.FAILED.name)
                    }
                    for (failedDel in failedDeletions) {
                        mealRecordDao.updateSyncStatus(failedDel.id, SyncStatus.FAILED.name)
                    }
                    Result.failure()
                } else {
                    Result.retry()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (runAttemptCount >= MAX_RETRIES) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        }
    }
}
