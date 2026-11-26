package dev.andre.nutritionist.fake

import dev.andre.nutritionist.domain.model.MealPlanSearchCriteria
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.PagedResult
import dev.andre.nutritionist.domain.repository.MealPlanRepository

class FakeMealPlanRepository : MealPlanRepository {
    val plans = mutableListOf<MealPlanTemplate>()
    var errorToThrow: Throwable? = null

    override suspend fun searchMealPlans(criteria: MealPlanSearchCriteria): Result<PagedResult<MealPlanTemplate>> {
        errorToThrow?.let { return Result.failure(it) }
        val filtered = plans.filter { plan ->
            val matchesQuery = criteria.query.isBlank() ||
                plan.title.contains(criteria.query, ignoreCase = true)
            val matchesVisibility = criteria.visibility == null || plan.visibility == criteria.visibility
            matchesQuery && matchesVisibility
        }
        return Result.success(PagedResult(items = filtered, totalCount = filtered.size.toLong()))
    }

    override suspend fun getMealPlanById(id: MealPlanTemplateId): Result<MealPlanTemplate> {
        errorToThrow?.let { return Result.failure(it) }
        val plan = plans.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException("Plan $id not found"))
        return Result.success(plan)
    }

    override suspend fun saveMealPlan(plan: MealPlanTemplate): Result<MealPlanTemplate> {
        errorToThrow?.let { return Result.failure(it) }
        val saved = plan.copy(version = plan.version + 1L)
        plans.removeAll { it.id == saved.id }
        plans.add(saved)
        return Result.success(saved)
    }

    override suspend fun deleteMealPlan(id: MealPlanTemplateId): Result<Unit> {
        errorToThrow?.let { return Result.failure(it) }
        plans.removeAll { it.id == id }
        return Result.success(Unit)
    }
}
