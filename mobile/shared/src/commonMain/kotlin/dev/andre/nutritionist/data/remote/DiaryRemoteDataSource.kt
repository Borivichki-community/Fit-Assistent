package dev.andre.nutritionist.data.remote

import dev.andre.nutritionist.domain.model.MealRecord

interface DiaryRemoteDataSource {
    suspend fun addEntry(record: MealRecord): Result<MealRecord>
    suspend fun getDailyEntries(date: String, timeZone: String = "UTC"): Result<List<MealRecord>>
    suspend fun deleteMeal(entryId: String): Result<Unit>
}
