package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.local.dao.MealRecordDao
import dev.andre.nutritionist.data.mapper.MealRecordEntityMapper.toDomain
import dev.andre.nutritionist.data.mapper.MealRecordEntityMapper.toEntity
import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DiaryRepositoryImpl(
    private val dao: MealRecordDao,
    private val remoteDataSource: DiaryRemoteDataSource,
    private val dispatchers: CoroutineDispatchers,
    private val syncScheduler: SyncScheduler? = null,
) : DiaryRepository {

    override suspend fun addEntry(record: MealRecord): Result<MealRecord> =
        withContext(dispatchers.io) {
            runCatching {
                // a. Create entity with PENDING status
                val pendingRecord = record.copy(syncStatus = SyncStatus.PENDING)
                // b. Save immediately to Room local DB -> UI updates instantly via Flow
                dao.insert(pendingRecord.toEntity())

                // c. Attempt to send to remote diary-service
                val remoteResult = remoteDataSource.addEntry(pendingRecord)

                // d. If remote call succeeded -> update Room status to SYNCED
                if (remoteResult.isSuccess) {
                    val syncedRecord = pendingRecord.copy(syncStatus = SyncStatus.SYNCED)
                    dao.updateSyncStatus(pendingRecord.id.raw, SyncStatus.SYNCED.name)
                    syncedRecord
                } else {
                    // e. If network failed -> record stays PENDING and schedule background sync
                    syncScheduler?.scheduleDiarySync()
                    pendingRecord
                }
            }
        }

    override fun observeDailyRecords(fromEpochMs: Long, toEpochMs: Long): Flow<List<MealRecord>> =
        dao.getDailyRecordsFlow(fromEpochMs, toEpochMs)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override suspend fun getDailyRecords(fromEpochMs: Long, toEpochMs: Long): Result<List<MealRecord>> =
        withContext(dispatchers.io) {
            runCatching {
                dao.getDailyRecords(fromEpochMs, toEpochMs).map { it.toDomain() }
            }
        }

    override suspend fun getPendingRecords(): List<MealRecord> =
        withContext(dispatchers.io) {
            dao.getPendingRecords().map { it.toDomain() }
        }

    override suspend fun syncPendingRecords(): Result<Int> =
        withContext(dispatchers.io) {
            runCatching {
                val pending = dao.getPendingRecords()
                var syncedCount = 0
                for (entity in pending) {
                    val domainRecord = entity.toDomain()
                    // Send to remote with the EXACT SAME idempotencyKey
                    val result = remoteDataSource.addEntry(domainRecord)
                    if (result.isSuccess) {
                        dao.updateSyncStatus(entity.id, SyncStatus.SYNCED.name)
                        syncedCount++
                    }
                }
                syncedCount
            }
        }

    override suspend fun deleteEntry(id: MealRecordId): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                dao.deleteById(id.raw)
                remoteDataSource.deleteMeal(id.raw)
                Unit
            }
        }
}
