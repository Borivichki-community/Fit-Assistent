package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.local.dao.LocalMealPlanDao
import dev.andre.nutritionist.data.mapper.MealPlanEntityMapper.toDomain
import dev.andre.nutritionist.data.mapper.MealPlanEntityMapper.toEntity
import dev.andre.nutritionist.data.mapper.MealPlanEntityMapper.toItemEntities
import dev.andre.nutritionist.data.remote.RecipeRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealPlanSearchCriteria
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.PagedResult
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

class MealPlanRepositoryImpl(
    private val localMealPlanDao: LocalMealPlanDao,
    private val remoteDataSource: RecipeRemoteDataSource,
    private val dispatchers: CoroutineDispatchers,
) : MealPlanRepository {

    override suspend fun searchMealPlans(
        criteria: MealPlanSearchCriteria,
    ): Result<PagedResult<MealPlanTemplate>> =
        withContext(dispatchers.io) {
            remoteDataSource.searchMealPlans(criteria)
                .onSuccess { page ->
                    page.items.forEach { plan ->
                        cachePlan(plan)
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                }
        }

    override suspend fun getMealPlanById(id: MealPlanTemplateId): Result<MealPlanTemplate> =
        withContext(dispatchers.io) {
            remoteDataSource.getMealPlan(id)
                .onSuccess { plan ->
                    cachePlan(plan)
                }
                .recoverCatching { error ->
                    if (error is CancellationException) throw error
                    val cached = localMealPlanDao.getPlanWithItems(id.raw)
                    if (cached != null) {
                        cached.toDomain()
                    } else {
                        throw error
                    }
                }
        }

    override suspend fun saveMealPlan(plan: MealPlanTemplate): Result<MealPlanTemplate> =
        withContext(dispatchers.io) {
            val idempotencyKey = IdempotencyKey.generate().raw
            val remoteResult = if (plan.version == 0L) {
                remoteDataSource.createMealPlan(plan, idempotencyKey = idempotencyKey)
            } else {
                remoteDataSource.updateMealPlan(plan, idempotencyKey = idempotencyKey)
            }

            remoteResult
                .onSuccess { savedPlan ->
                    if (savedPlan.id != plan.id) {
                        localMealPlanDao.deleteById(plan.id.raw)
                    }
                    cachePlan(savedPlan)
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                }
        }

    override suspend fun deleteMealPlan(id: MealPlanTemplateId): Result<Unit> =
        withContext(dispatchers.io) {
            val idempotencyKey = IdempotencyKey.generate().raw
            remoteDataSource.deleteMealPlan(id, idempotencyKey = idempotencyKey)
                .onSuccess {
                    localMealPlanDao.deleteById(id.raw)
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                }
        }

    private suspend fun cachePlan(plan: MealPlanTemplate) {
        localMealPlanDao.upsertPlanWithItems(
            plan = plan.toEntity(syncStatus = SyncStatus.SYNCED),
            items = plan.toItemEntities(),
        )
    }
}
