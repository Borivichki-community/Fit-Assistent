package dev.andre.nutritionist.data.remote

import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.UserId

interface DiaryRemoteDataSource {
    suspend fun addEntry(record: MealRecord): Result<MealRecord>
    suspend fun getDailyEntries(
        date: String,
        timeZone: String = "UTC",
        targetUserId: UserId? = null,
    ): Result<List<MealRecord>>
    suspend fun getRange(
        fromIso: String,
        untilIso: String,
        timeZone: String = "UTC",
        targetUserId: UserId? = null,
    ): Result<List<MealRecord>>
    suspend fun deleteMeal(entryId: String): Result<Unit>
}
