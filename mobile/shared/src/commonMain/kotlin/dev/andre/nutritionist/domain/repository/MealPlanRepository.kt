package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.MealPlanSearchCriteria
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.PagedResult

interface MealPlanRepository {
    suspend fun searchMealPlans(
        criteria: MealPlanSearchCriteria = MealPlanSearchCriteria(),
    ): Result<PagedResult<MealPlanTemplate>>

    suspend fun getMealPlanById(id: MealPlanTemplateId): Result<MealPlanTemplate>

    suspend fun saveMealPlan(plan: MealPlanTemplate): Result<MealPlanTemplate>

    suspend fun deleteMealPlan(id: MealPlanTemplateId): Result<Unit>
}
