/**
 * Эталонный пример: офлайн-сохранение записи дневника питания в Room
 * с генерацией idempotencyKey ОДИН РАЗ и планированием синхронизации.
 *
 * Файл: app/shared/src/commonMain/kotlin/com/budgetntr/shared/...
 *
 * ПРАВИЛА (из AGENTS.md):
 * - idempotencyKey генерируется при создании записи, НИКОГДА при retry
 * - SyncStatus.PENDING устанавливается при создании
 * - SyncScheduler — интерфейс, реализация в androidMain (WorkManager)
 * - Нет java.*, android.*, Context в commonMain
 * - Нет !! операторов
 * - Диспетчеры через конструктор
 */

// ─── Domain Layer (commonMain) ────────────────────────────────────────────────

// app/shared/src/commonMain/kotlin/com/budgetntr/shared/domain/model/ids.kt
package com.budgetntr.shared.domain.model

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@JvmInline value class DiaryEntryId(val raw: String)
@JvmInline value class FoodItemId(val raw: String)

/** Ключ идемпотентности — генерируется ОДИН РАЗ при создании записи. */
@JvmInline value class IdempotencyKey(val raw: String) {
    companion object {
        fun generate(): IdempotencyKey = IdempotencyKey(Uuid.random().toString())
    }
}

// ─── Domain Model ─────────────────────────────────────────────────────────────

// app/shared/src/commonMain/kotlin/com/budgetntr/shared/domain/model/DiaryEntry.kt
package com.budgetntr.shared.domain.model

import kotlinx.datetime.Instant

enum class SyncStatus { PENDING, SYNCED, FAILED }

data class DiaryEntry(
    val id: DiaryEntryId,
    /** Генерируется при создании и больше не меняется. */
    val idempotencyKey: IdempotencyKey,
    val foodItemId: FoodItemId,
    val quantityGrams: Double,
    val recordedAt: Instant,           // kotlinx-datetime — НЕ java.time
    val syncStatus: SyncStatus,
    val caloriesLabel: String,         // "320 ккал" — форматируется в :shared
    val priceLabel: String,            // "85 ₽"    — форматируется в :shared
)

// ─── Repository Interface (domain) ───────────────────────────────────────────

// app/shared/src/commonMain/kotlin/com/budgetntr/shared/domain/repository/DiaryRepository.kt
package com.budgetntr.shared.domain.repository

import com.budgetntr.shared.domain.model.DiaryEntry
import com.budgetntr.shared.domain.model.DiaryEntryId
import com.budgetntr.shared.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface DiaryRepository {
    suspend fun save(entry: DiaryEntry)
    suspend fun getById(id: DiaryEntryId): DiaryEntry
    fun observePendingEntries(): Flow<List<DiaryEntry>>
    suspend fun updateSyncStatus(id: DiaryEntryId, status: SyncStatus)
}

// ─── SyncScheduler Interface (domain/платформенный код за интерфейсом) ────────

// app/shared/src/commonMain/kotlin/com/budgetntr/shared/domain/sync/SyncScheduler.kt
package com.budgetntr.shared.domain.sync

interface SyncScheduler {
    /** Планирует фоновую синхронизацию PENDING-записей. */
    fun scheduleSync()
    fun cancelSync()
}

// ─── UseCase (domain) ────────────────────────────────────────────────────────

// app/shared/src/commonMain/kotlin/com/budgetntr/shared/domain/usecase/diary/AddDiaryEntryUseCase.kt
package com.budgetntr.shared.domain.usecase.diary

import com.budgetntr.shared.domain.coroutines.CoroutineDispatchers
import com.budgetntr.shared.domain.model.*
import com.budgetntr.shared.domain.repository.DiaryRepository
import com.budgetntr.shared.domain.sync.SyncScheduler
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class AddDiaryEntryUseCase(
    private val diaryRepository: DiaryRepository,
    private val syncScheduler: SyncScheduler,
    private val dispatchers: CoroutineDispatchers,
) {
    /**
     * Создаёт запись дневника с уникальным idempotencyKey.
     *
     * idempotencyKey генерируется ЗДЕСЬ — один раз при создании.
     * При повторной синхронизации (retry в SyncWorker) используется
     * тот же ключ, сохранённый в Room.
     */
    suspend operator fun invoke(
        foodItemId: FoodItemId,
        quantityGrams: Double,
        recordedAt: Instant,
    ): Result<DiaryEntry> = withContext(dispatchers.io) {
        runCatching {
            val entry = DiaryEntry(
                id = DiaryEntryId(Uuid.random().toString()),
                // idempotencyKey генерируется ОДИН РАЗ — здесь, до записи в Room
                idempotencyKey = IdempotencyKey.generate(),
                foodItemId = foodItemId,
                quantityGrams = quantityGrams,
                recordedAt = recordedAt,
                syncStatus = SyncStatus.PENDING,
                caloriesLabel = "",  // заполняется после загрузки данных о продукте
                priceLabel = "",
            )
            diaryRepository.save(entry)
            syncScheduler.scheduleSync()
            entry
        }
    }
}

// ─── Room Entity (data/local) ─────────────────────────────────────────────────

// app/shared/src/commonMain/kotlin/com/budgetntr/shared/data/local/entity/DiaryEntryEntity.kt
package com.budgetntr.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey val id: String,
    /** Сохраняется вместе с записью и не меняется при retry. */
    val idempotencyKey: String,
    val foodItemId: String,
    val quantityGrams: Double,
    val recordedAt: Long,         // epochMillis → Instant через mapper
    val syncStatus: String,       // "PENDING" | "SYNCED" | "FAILED"
    val caloriesLabel: String,
    val priceLabel: String,
)

// ─── DAO ──────────────────────────────────────────────────────────────────────

// app/shared/src/commonMain/kotlin/com/budgetntr/shared/data/local/dao/DiaryDao.kt
package com.budgetntr.shared.data.local.dao

import androidx.room.*
import com.budgetntr.shared.data.local.entity.DiaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entry: DiaryEntryEntity)

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: String): DiaryEntryEntity?

    @Query("SELECT * FROM diary_entries WHERE syncStatus = 'PENDING' ORDER BY recordedAt ASC")
    fun observePending(): Flow<List<DiaryEntryEntity>>

    @Query("UPDATE diary_entries SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("SELECT * FROM diary_entries ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<DiaryEntryEntity>>
}

// ─── Mapper (data/mapper) ─────────────────────────────────────────────────────

// app/shared/src/commonMain/kotlin/com/budgetntr/shared/data/mapper/DiaryMapper.kt
package com.budgetntr.shared.data.mapper

import com.budgetntr.shared.data.local.entity.DiaryEntryEntity
import com.budgetntr.shared.domain.model.*
import kotlinx.datetime.Instant

object DiaryMapper {

    fun DiaryEntryEntity.toDomain(): DiaryEntry = DiaryEntry(
        id = DiaryEntryId(id),
        idempotencyKey = IdempotencyKey(idempotencyKey),
        foodItemId = FoodItemId(foodItemId),
        quantityGrams = quantityGrams,
        recordedAt = Instant.fromEpochMilliseconds(recordedAt),
        syncStatus = when (syncStatus) {
            "PENDING" -> SyncStatus.PENDING
            "SYNCED"  -> SyncStatus.SYNCED
            "FAILED"  -> SyncStatus.FAILED
            // Нет else — если добавится новый статус, компилятор укажет
            else      -> error("Unknown syncStatus: $syncStatus")
        },
        caloriesLabel = caloriesLabel,
        priceLabel = priceLabel,
    )

    fun DiaryEntry.toEntity(): DiaryEntryEntity = DiaryEntryEntity(
        id = id.raw,
        idempotencyKey = idempotencyKey.raw,
        foodItemId = foodItemId.raw,
        quantityGrams = quantityGrams,
        recordedAt = recordedAt.toEpochMilliseconds(),
        syncStatus = syncStatus.name,
        caloriesLabel = caloriesLabel,
        priceLabel = priceLabel,
    )
}

// ─── Repository Implementation (data/repository) ─────────────────────────────

// app/shared/src/commonMain/kotlin/com/budgetntr/shared/data/repository/DiaryRepositoryImpl.kt
package com.budgetntr.shared.data.repository

import com.budgetntr.shared.data.local.dao.DiaryDao
import com.budgetntr.shared.data.mapper.DiaryMapper.toDomain
import com.budgetntr.shared.data.mapper.DiaryMapper.toEntity
import com.budgetntr.shared.domain.coroutines.CoroutineDispatchers
import com.budgetntr.shared.domain.model.*
import com.budgetntr.shared.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DiaryRepositoryImpl(
    private val dao: DiaryDao,
    private val dispatchers: CoroutineDispatchers,
) : DiaryRepository {

    override suspend fun save(entry: DiaryEntry): Unit =
        withContext(dispatchers.io) {
            dao.insertOrReplace(entry.toEntity())
        }

    override suspend fun getById(id: DiaryEntryId): DiaryEntry =
        withContext(dispatchers.io) {
            dao.getById(id.raw)
                ?.toDomain()
                ?: error("DiaryEntry not found: ${id.raw}")
        }

    override fun observePendingEntries(): Flow<List<DiaryEntry>> =
        dao.observePending()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override suspend fun updateSyncStatus(id: DiaryEntryId, status: SyncStatus): Unit =
        withContext(dispatchers.io) {
            dao.updateSyncStatus(id.raw, status.name)
        }
}

// ─── Android: WorkManagerSyncScheduler (androidMain) ─────────────────────────

// app/shared/src/androidMain/kotlin/com/budgetntr/shared/sync/WorkManagerSyncScheduler.kt
// NOTE: этот файл — в androidMain, не в commonMain!
package com.budgetntr.shared.sync

import android.content.Context
import androidx.work.*
import com.budgetntr.shared.domain.sync.SyncScheduler
import java.util.concurrent.TimeUnit

class WorkManagerSyncScheduler(
    private val context: Context,
) : SyncScheduler {

    override fun scheduleSync() {
        val request = PeriodicWorkRequestBuilder<SyncDiaryWorkerShim>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            // BackoffPolicy.EXPONENTIAL — ОБЯЗАТЕЛЬНО для idempotency retry
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,   // не заменять уже запланированную
            request,
        )
    }

    override fun cancelSync() {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
    }

    companion object {
        const val SYNC_WORK_NAME = "sync_diary_worker"
    }
}

// ─── Android: SyncDiaryWorker (androidApp) ────────────────────────────────────

// app/androidApp/src/main/kotlin/com/budgetntr/android/sync/SyncDiaryWorker.kt
package com.budgetntr.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.budgetntr.shared.domain.model.SyncStatus
import com.budgetntr.shared.domain.repository.DiaryRepository
import com.budgetntr.shared.data.remote.grpc.DiaryGrpcDataSource
import io.grpc.Metadata

class SyncDiaryWorker(
    context: Context,
    params: WorkerParameters,
    private val diaryRepository: DiaryRepository,
    private val diaryGrpcDataSource: DiaryGrpcDataSource,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Читаем PENDING записи — idempotencyKey уже сохранён в Room
        val pendingEntries = diaryRepository.observePendingEntries()
            // Берём snapshot (не стрим) через первое значение
            .let { flow ->
                var snapshot: List<com.budgetntr.shared.domain.model.DiaryEntry> = emptyList()
                flow.collect { snapshot = it; return@collect }
                snapshot
            }

        if (pendingEntries.isEmpty()) return Result.success()

        var hasFailure = false
        pendingEntries.forEach { entry ->
            runCatching {
                val metadata = Metadata().apply {
                    // ВСЕГДА берём idempotencyKey из Room — НИКОГДА не генерируем новый
                    put(IDEMPOTENCY_KEY_HEADER, entry.idempotencyKey.raw)
                }
                diaryGrpcDataSource.addEntry(entry, metadata)
                diaryRepository.updateSyncStatus(entry.id, SyncStatus.SYNCED)
            }.onFailure {
                hasFailure = true
                diaryRepository.updateSyncStatus(entry.id, SyncStatus.FAILED)
            }
        }

        return if (hasFailure && runAttemptCount < MAX_RETRY_ATTEMPTS) {
            Result.retry()  // WorkManager применит BackoffPolicy.EXPONENTIAL
        } else {
            Result.success()
        }
    }

    companion object {
        val IDEMPOTENCY_KEY_HEADER: Metadata.Key<String> =
            Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
        const val MAX_RETRY_ATTEMPTS = 5
    }
}
