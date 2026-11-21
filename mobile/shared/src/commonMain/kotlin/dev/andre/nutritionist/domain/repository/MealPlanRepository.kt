package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanId

/**
 * Интерфейс репозитория готовых и персональных рационов питания.
 */
interface MealPlanRepository {
    suspend fun getMealPlans(query: String = "", category: String = "Все"): Result<List<MealPlan>>
    suspend fun getMealPlanById(id: MealPlanId): Result<MealPlan?>
    suspend fun toggleFavorite(id: MealPlanId): Result<Boolean>
}
