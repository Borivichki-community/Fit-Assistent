package dev.andre.nutritionist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.andre.nutritionist.data.local.entity.DiaryEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для локального дневника питания (Room KMP).
 */
@Dao
interface DiaryDao {

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND dateEpochDay = :dateEpochDay ORDER BY recordedAtEpochMs ASC")
    suspend fun getDailyEntries(userId: String, dateEpochDay: Long): List<DiaryEntryEntity>

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND dateEpochDay = :dateEpochDay ORDER BY recordedAtEpochMs ASC")
    fun observeDailyEntries(userId: String, dateEpochDay: Long): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE syncStatus = 'SYNC_PENDING' OR syncStatus = 'PENDING' ORDER BY recordedAtEpochMs ASC")
    suspend fun getPendingEntries(): List<DiaryEntryEntity>

    @Query("SELECT * FROM diary_entries WHERE syncStatus = 'SYNC_PENDING' OR syncStatus = 'PENDING' ORDER BY recordedAtEpochMs ASC")
    fun observePending(): Flow<List<DiaryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entry: DiaryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(entries: List<DiaryEntryEntity>)

    @Query("UPDATE diary_entries SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: String)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM diary_entries WHERE userId = :userId")
    suspend fun clear(userId: String)
}
