package dev.andre.nutritionist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.andre.nutritionist.data.local.entity.LocalRecipeEntity
import dev.andre.nutritionist.data.local.entity.LocalRecipeIngredientEntity
import dev.andre.nutritionist.data.local.entity.LocalRecipeWithIngredients
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalRecipeDao {

    @Transaction
    @Query("SELECT * FROM local_recipes WHERE is_deleted = 0 AND author_type != 'SYSTEM' ORDER BY updated_at DESC")
    fun observeMyRecipes(): Flow<List<LocalRecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM local_recipes WHERE is_deleted = 0 ORDER BY updated_at DESC")
    fun observeAllActive(): Flow<List<LocalRecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM local_recipes WHERE is_deleted = 0 AND sync_status = 'PENDING'")
    suspend fun getPendingRecipes(): List<LocalRecipeWithIngredients>

    @Transaction
    @Query("SELECT * FROM local_recipes WHERE id = :id AND is_deleted = 0 LIMIT 1")
    suspend fun getRecipeWithIngredients(id: String): LocalRecipeWithIngredients?

    @Query("SELECT * FROM local_recipes WHERE id = :id LIMIT 1")
    suspend fun getRawById(id: String): LocalRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipe(recipe: LocalRecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<LocalRecipeIngredientEntity>)

    @Query("DELETE FROM local_recipe_ingredients WHERE recipe_id = :recipeId")
    suspend fun deleteIngredientsByRecipeId(recipeId: String)

    @Transaction
    suspend fun upsertRecipeWithIngredients(
        recipe: LocalRecipeEntity,
        ingredients: List<LocalRecipeIngredientEntity>,
    ) {
        upsertRecipe(recipe)
        deleteIngredientsByRecipeId(recipe.id)
        if (ingredients.isNotEmpty()) {
            insertIngredients(ingredients)
        }
    }

    @Query("UPDATE local_recipes SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE local_recipes SET is_deleted = 1, sync_status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: String, status: String, updatedAt: Long)

    @Query("DELETE FROM local_recipes WHERE id = :id")
    suspend fun hardDeleteById(id: String)
}
