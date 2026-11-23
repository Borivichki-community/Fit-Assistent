package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.DailyDiary
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Контракт репозитория дневника питания.
 */
interface DiaryRepository {
    suspend fun getDailyDiary(userId: UserId, date: LocalDate): Result<DailyDiary>
    fun observeDailyDiary(userId: UserId, date: LocalDate): Flow<DailyDiary>
    suspend fun addEntry(entry: DiaryEntry): Result<DiaryEntry>
    suspend fun deleteEntry(id: DiaryEntryId): Result<Unit>
}
