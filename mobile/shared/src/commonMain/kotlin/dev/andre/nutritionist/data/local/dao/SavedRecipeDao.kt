package dev.andre.nutritionist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.andre.nutritionist.data.local.entity.SavedRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedRecipeEntity)

    @Query("DELETE FROM saved_recipes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM saved_recipes ORDER BY saved_at DESC")
    fun observeAll(): Flow<List<SavedRecipeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_recipes WHERE id = :id)")
    fun isFavorite(id: String): Flow<Boolean>
}
