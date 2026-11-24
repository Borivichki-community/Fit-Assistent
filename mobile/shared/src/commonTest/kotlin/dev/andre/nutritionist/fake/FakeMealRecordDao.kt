package dev.andre.nutritionist.fake

import dev.andre.nutritionist.data.local.dao.MealRecordDao
import dev.andre.nutritionist.data.local.entity.MealRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMealRecordDao : MealRecordDao {

    // mutable by design: test control
    val records = mutableMapOf<String, MealRecordEntity>()
    private val recordsFlow = MutableStateFlow<List<MealRecordEntity>>(emptyList())
    var shouldThrow: Throwable? = null

    override suspend fun insert(record: MealRecordEntity) {
        val error = shouldThrow
        if (error != null) throw error
        records[record.id] = record
        recordsFlow.value = records.values.toList()
    }

    override suspend fun getById(id: String): MealRecordEntity? {
        val error = shouldThrow
        if (error != null) throw error
        return records[id]
    }

    override suspend fun getPendingRecords(): List<MealRecordEntity> {
        val error = shouldThrow
        if (error != null) throw error
        return records.values.filter { it.syncStatus == "PENDING" }
    }

    override suspend fun updateSyncStatus(id: String, status: String) {
        val error = shouldThrow
        if (error != null) throw error
        val existing = records[id]
        if (existing != null) {
            records[id] = existing.copy(syncStatus = status)
            recordsFlow.value = records.values.toList()
        }
    }

    override fun getDailyRecordsFlow(fromEpoch: Long, toEpoch: Long): Flow<List<MealRecordEntity>> {
        return recordsFlow.map { list ->
            list.filter { it.recordedAtEpoch in fromEpoch..toEpoch }
                .sortedBy { it.recordedAtEpoch }
        }
    }

    override suspend fun getDailyRecords(fromEpoch: Long, toEpoch: Long): List<MealRecordEntity> {
        val error = shouldThrow
        if (error != null) throw error
        return records.values
            .filter { it.recordedAtEpoch in fromEpoch..toEpoch }
            .sortedBy { it.recordedAtEpoch }
    }

    override suspend fun deleteById(id: String) {
        val error = shouldThrow
        if (error != null) throw error
        records.remove(id)
        recordsFlow.value = records.values.toList()
    }

    fun reset() {
        records.clear()
        recordsFlow.value = emptyList()
        shouldThrow = null
    }
}
