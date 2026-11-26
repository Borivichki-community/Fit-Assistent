package dev.andre.nutritionist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.andre.nutritionist.data.local.entity.MealRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MealRecordEntity)

    @Query("SELECT COUNT(*) FROM meal_records")
    suspend fun countAllIncludingDeleted(): Int

    @Query("SELECT * FROM meal_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MealRecordEntity?

    @Query("SELECT * FROM meal_records WHERE sync_status = 'PENDING' AND is_deleted = 0")
    suspend fun getPendingRecords(): List<MealRecordEntity>

    @Query("SELECT * FROM meal_records WHERE is_deleted = 1 AND sync_status = 'DELETED_PENDING'")
    suspend fun getPendingDeletedRecords(): List<MealRecordEntity>

    @Query("UPDATE meal_records SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE meal_records SET is_deleted = 1, sync_status = :status WHERE id = :id")
    suspend fun markAsDeleted(id: String, status: String = "DELETED_PENDING")

    @Query("SELECT * FROM meal_records WHERE is_deleted = 0 AND recorded_at_epoch BETWEEN :fromEpoch AND :toEpoch ORDER BY recorded_at_epoch ASC")
    fun getDailyRecordsFlow(fromEpoch: Long, toEpoch: Long): Flow<List<MealRecordEntity>>

    @Query("SELECT * FROM meal_records WHERE is_deleted = 0 AND recorded_at_epoch BETWEEN :fromEpoch AND :toEpoch ORDER BY recorded_at_epoch ASC")
    suspend fun getDailyRecords(fromEpoch: Long, toEpoch: Long): List<MealRecordEntity>

    @Query("DELETE FROM meal_records WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM meal_records WHERE id = :id")
    suspend fun deleteById(id: String)
}
