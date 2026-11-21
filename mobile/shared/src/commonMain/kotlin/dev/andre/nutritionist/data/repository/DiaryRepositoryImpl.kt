package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.local.dao.DiaryDao
import dev.andre.nutritionist.data.mapper.toDomain
import dev.andre.nutritionist.data.mapper.toEntity
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.DailyDiary
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * Реализация [DiaryRepository] для мобильного приложения.
 * Обеспечивает Room KMP хранение записей дневника питания (Local-First)
 * с поддержкой подписки Flow и fallback в память.
 */
class DiaryRepositoryImpl(
    private val dispatchers: CoroutineDispatchers,
    private val diaryDao: DiaryDao? = null,
) : DiaryRepository {

    private val inMemoryEntriesFlow = MutableStateFlow<List<DiaryEntry>>(emptyList())

    override suspend fun getDailyDiary(userId: UserId, date: LocalDate): Result<DailyDiary> =
        withContext(dispatchers.io) {
            runCatching {
                val dayEntries = if (diaryDao != null) {
                    diaryDao.getDailyEntries(userId.raw, date.toEpochDays().toLong()).map { it.toDomain() }
                } else {
                    inMemoryEntriesFlow.value.filter { it.userId == userId && it.date == date }
                }
                DailyDiary(
                    userId = userId,
                    date = date,
                    entries = dayEntries,
                    streakDays = 1,
                )
            }
        }

    override fun observeDailyDiary(userId: UserId, date: LocalDate): Flow<DailyDiary> =
        if (diaryDao != null) {
            diaryDao.observeDailyEntries(userId.raw, date.toEpochDays().toLong()).map { entities ->
                DailyDiary(
                    userId = userId,
                    date = date,
                    entries = entities.map { it.toDomain() },
                    streakDays = 1,
                )
            }
        } else {
            inMemoryEntriesFlow.map { allEntries ->
                val dayEntries = allEntries.filter { it.userId == userId && it.date == date }
                DailyDiary(
                    userId = userId,
                    date = date,
                    entries = dayEntries,
                    streakDays = 1,
                )
            }
        }

    override suspend fun addEntry(entry: DiaryEntry): Result<DiaryEntry> =
        withContext(dispatchers.io) {
            runCatching {
                val entryToSave = if (entry.syncStatus == SyncStatus.SYNCED) {
                    entry
                } else {
                    entry.copy(syncStatus = SyncStatus.PENDING)
                }

                if (diaryDao != null) {
                    diaryDao.insertOrReplace(entryToSave.toEntity(SyncStatus.STATUS_SYNC_PENDING))
                } else {
                    val current = inMemoryEntriesFlow.value.toMutableList()
                    val index = current.indexOfFirst { it.id == entry.id }
                    if (index >= 0) {
                        current[index] = entryToSave
                    } else {
                        current.add(entryToSave)
                    }
                    inMemoryEntriesFlow.value = current
                }
                entryToSave
            }
        }

    override suspend fun deleteEntry(id: DiaryEntryId): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                if (diaryDao != null) {
                    diaryDao.deleteById(id.raw)
                } else {
                    val current = inMemoryEntriesFlow.value.toMutableList()
                    current.removeAll { it.id == id }
                    inMemoryEntriesFlow.value = current
                }
            }
        }
}
