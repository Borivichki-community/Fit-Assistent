package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.local.dao.MealRecordDao
import dev.andre.nutritionist.data.mapper.MealRecordEntityMapper.toDomain
import dev.andre.nutritionist.data.mapper.MealRecordEntityMapper.toEntity
import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.sync.SyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

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
                    val error = remoteResult.exceptionOrNull()
                    if (error is CancellationException) throw error
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

    override suspend fun getRange(
        fromIso: String,
        untilIso: String,
        targetUserId: UserId?,
        timeZone: String,
    ): Result<List<MealRecord>> =
        withContext(dispatchers.io) {
            val tz = runCatching { TimeZone.of(timeZone) }.getOrElse { TimeZone.UTC }
            val fromDate = runCatching { LocalDate.parse(fromIso) }.getOrElse {
                return@withContext Result.failure(
                    DomainError.ValidationError(field = "fromIso", reason = "Invalid ISO date: $fromIso"),
                )
            }
            val untilDate = runCatching { LocalDate.parse(untilIso) }.getOrElse {
                return@withContext Result.failure(
                    DomainError.ValidationError(field = "untilIso", reason = "Invalid ISO date: $untilIso"),
                )
            }
            if (fromDate > untilDate) {
                return@withContext Result.failure(
                    DomainError.ValidationError(field = "dateRange", reason = "fromIso must be on or before untilIso"),
                )
            }

            val remoteResult = remoteDataSource.getRange(
                fromIso = fromIso,
                untilIso = untilIso,
                timeZone = tz.id,
                targetUserId = targetUserId,
            )

            if (targetUserId != null) {
                return@withContext remoteResult
            }

            val fromEpochMs = fromDate.atStartOfDayIn(tz).toEpochMilliseconds()
            val toEpochMs = untilDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds() - 1L

            remoteResult.fold(
                onSuccess = { remoteRecords ->
                    val deletedIds = dao.getPendingDeletedRecords().mapTo(HashSet()) { it.id }
                    val pendingLocal = dao.getDailyRecords(fromEpochMs, toEpochMs)
                        .filter { it.syncStatus == SyncStatus.PENDING.name && it.id !in deletedIds }
                        .map { it.toDomain() }
                    val filteredRemote = remoteRecords.filterNot { it.id.raw in deletedIds }
                    val mergedById = LinkedHashMap<String, MealRecord>(filteredRemote.size + pendingLocal.size)
                    for (record in filteredRemote) {
                        mergedById[record.id.raw] = record
                    }
                    for (record in pendingLocal) {
                        mergedById[record.id.raw] = record
                    }
                    Result.success(mergedById.values.sortedBy { it.recordedAt })
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    if (error is DomainError.Unauthorized || error is DomainError.Forbidden) {
                        Result.failure(error)
                    } else {
                        Result.success(dao.getDailyRecords(fromEpochMs, toEpochMs).map { it.toDomain() })
                    }
                },
            )
        }

    override suspend fun getPendingRecords(): List<MealRecord> =
        withContext(dispatchers.io) {
            dao.getPendingRecords().map { it.toDomain() }
        }

    override suspend fun syncPendingRecords(): Result<Int> =
        withContext(dispatchers.io) {
            runCatching {
                var syncedCount = 0

                val pending = dao.getPendingRecords()
                for (entity in pending) {
                    val domainRecord = entity.toDomain()
                    // Send to remote with the EXACT SAME idempotencyKey
                    val result = remoteDataSource.addEntry(domainRecord)
                    if (result.isSuccess) {
                        dao.updateSyncStatus(entity.id, SyncStatus.SYNCED.name)
                        syncedCount++
                    } else {
                        val error = result.exceptionOrNull()
                        if (error is CancellationException) throw error
                    }
                }

                val pendingDeletions = dao.getPendingDeletedRecords()
                for (entity in pendingDeletions) {
                    val result = remoteDataSource.deleteMeal(entity.id)
                    if (result.isSuccess) {
                        dao.deletePermanently(entity.id)
                        syncedCount++
                    } else {
                        val error = result.exceptionOrNull()
                        if (error is CancellationException) throw error
                    }
                }

                syncedCount
            }
        }

    override suspend fun deleteEntry(id: MealRecordId): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val existing = dao.getById(id.raw)
                // Immediately mark tombstone in Room so reactive Flow excludes it from UI
                dao.markAsDeleted(id.raw, SyncStatus.DELETED_PENDING.name)

                val remoteResult = remoteDataSource.deleteMeal(id.raw)
                if (remoteResult.isSuccess) {
                    dao.deletePermanently(id.raw)
                } else {
                    val error = remoteResult.exceptionOrNull()
                    if (error is CancellationException) throw error
                    if (existing != null) {
                        syncScheduler?.scheduleDiarySync()
                    } else {
                        throw error ?: DomainError.Unknown("Failed to delete entry '${id.raw}'")
                    }
                }
                Unit
            }.onFailure { if (it is CancellationException) throw it }
        }
}
