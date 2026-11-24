package dev.andre.nutritionist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.andre.nutritionist.data.local.entity.LocalMealPlanEntity
import dev.andre.nutritionist.data.local.entity.LocalMealPlanItemEntity
import dev.andre.nutritionist.data.local.entity.LocalMealPlanWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalMealPlanDao {

    @Transaction
    @Query("SELECT * FROM local_meal_plans ORDER BY updated_at DESC")
    fun observeAllPlans(): Flow<List<LocalMealPlanWithItems>>

    @Transaction
    @Query("SELECT * FROM local_meal_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanWithItems(id: String): LocalMealPlanWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: LocalMealPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<LocalMealPlanItemEntity>)

    @Query("DELETE FROM local_meal_plan_items WHERE plan_id = :planId")
    suspend fun deleteItemsByPlanId(planId: String)

    @Transaction
    suspend fun upsertPlanWithItems(
        plan: LocalMealPlanEntity,
        items: List<LocalMealPlanItemEntity>,
    ) {
        upsertPlan(plan)
        deleteItemsByPlanId(plan.id)
        if (items.isNotEmpty()) {
            insertItems(items)
        }
    }

    @Query("UPDATE local_meal_plans SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM local_meal_plans WHERE id = :id")
    suspend fun deleteById(id: String)
}
