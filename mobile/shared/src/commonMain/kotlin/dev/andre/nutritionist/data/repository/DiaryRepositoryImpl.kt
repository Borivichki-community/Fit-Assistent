package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.DailyDiary
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * Реализация [DiaryRepository] для мобильного приложения.
 * Обеспечивает потокобезопасное хранение записей дневника в памяти
 * с поддержкой подписки Flow.
 */
class DiaryRepositoryImpl(
    private val dispatchers: CoroutineDispatchers,
) : DiaryRepository {

    private val entriesFlow = MutableStateFlow<List<DiaryEntry>>(emptyList())

    override suspend fun getDailyDiary(userId: UserId, date: LocalDate): Result<DailyDiary> =
        withContext(dispatchers.io) {
            runCatching {
                val dayEntries = entriesFlow.value.filter { it.userId == userId && it.date == date }
                DailyDiary(
                    userId = userId,
                    date = date,
                    entries = dayEntries,
                    streakDays = 1,
                )
            }
        }

    override fun observeDailyDiary(userId: UserId, date: LocalDate): Flow<DailyDiary> =
        entriesFlow.map { allEntries ->
            val dayEntries = allEntries.filter { it.userId == userId && it.date == date }
            DailyDiary(
                userId = userId,
                date = date,
                entries = dayEntries,
                streakDays = 1,
            )
        }

    override suspend fun addEntry(entry: DiaryEntry): Result<DiaryEntry> =
        withContext(dispatchers.io) {
            runCatching {
                val current = entriesFlow.value.toMutableList()
                val index = current.indexOfFirst { it.id == entry.id }
                if (index >= 0) {
                    current[index] = entry
                } else {
                    current.add(entry)
                }
                entriesFlow.value = current
                entry
            }
        }

    override suspend fun deleteEntry(id: DiaryEntryId): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val current = entriesFlow.value.toMutableList()
                current.removeAll { it.id == id }
                entriesFlow.value = current
            }
        }
}
