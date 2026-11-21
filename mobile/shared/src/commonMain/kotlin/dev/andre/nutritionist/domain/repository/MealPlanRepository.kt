package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.GroceryItem
import dev.andre.nutritionist.domain.model.GroceryItemId
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanDay
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.domain.model.UserId

/**
 * Интерфейс репозитория готовых и персональных рационов питания.
 */
interface MealPlanRepository {
    suspend fun getMealPlans(query: String = "", category: String = "Все"): Result<List<MealPlan>>
    suspend fun getMealPlanById(id: MealPlanId): Result<MealPlan?>
    suspend fun toggleFavorite(id: MealPlanId): Result<Boolean>
    suspend fun getActiveMealPlan(userId: UserId): Result<MealPlan?>
    suspend fun setActiveMealPlan(userId: UserId, planId: MealPlanId): Result<Unit>
    suspend fun deactivateMealPlan(userId: UserId): Result<Unit>
    suspend fun getMealPlanDays(planId: MealPlanId): Result<List<MealPlanDay>>
    suspend fun getGroceryList(planId: MealPlanId): Result<List<GroceryItem>>
    suspend fun toggleGroceryItem(planId: MealPlanId, itemId: GroceryItemId): Result<Boolean>
}

