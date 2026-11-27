package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone

interface DiaryRepository {
    suspend fun addEntry(record: MealRecord): Result<MealRecord>
    fun observeDailyRecords(fromEpochMs: Long, toEpochMs: Long): Flow<List<MealRecord>>
    suspend fun getDailyRecords(fromEpochMs: Long, toEpochMs: Long): Result<List<MealRecord>>
    suspend fun getRange(
        fromIso: String,
        untilIso: String,
        targetUserId: UserId? = null,
        timeZone: String = TimeZone.currentSystemDefault().id,
    ): Result<List<MealRecord>>
    suspend fun getPendingRecords(): List<MealRecord>
    suspend fun syncPendingRecords(): Result<Int>
    suspend fun deleteEntry(id: MealRecordId): Result<Unit>
}
