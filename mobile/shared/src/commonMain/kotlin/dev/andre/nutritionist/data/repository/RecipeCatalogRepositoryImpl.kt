package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.local.dao.LocalRecipeDao
import dev.andre.nutritionist.data.mapper.RecipeEntityMapper.toDomain
import dev.andre.nutritionist.data.mapper.RecipeEntityMapper.toEntity
import dev.andre.nutritionist.data.remote.RecipeRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.PagedResult
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeSearchCriteria
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.repository.RecipeCatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class RecipeCatalogRepositoryImpl(
    private val localRecipeDao: LocalRecipeDao,
    private val remoteDataSource: RecipeRemoteDataSource,
    private val dispatchers: CoroutineDispatchers,
) : RecipeCatalogRepository {

    override fun observeMyRecipes(): Flow<List<Recipe>> =
        localRecipeDao.observeMyRecipes()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override suspend fun searchPublicRecipes(criteria: RecipeSearchCriteria): Result<PagedResult<Recipe>> =
        withContext(dispatchers.io) {
            remoteDataSource.searchRecipes(criteria)
                .onSuccess { page ->
                    page.items.forEach { recipe ->
                        cacheSyncedRecipePreservingPending(recipe)
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                }
        }

    override suspend fun getRecipeById(id: RecipeId): Result<Recipe> =
        withContext(dispatchers.io) {
            val local = localRecipeDao.getRecipeWithIngredients(id.raw)
            if (local != null && local.recipe.syncStatus == SyncStatus.PENDING.name) {
                return@withContext Result.success(local.toDomain())
            }

            remoteDataSource.getRecipe(id)
                .onSuccess { remote ->
                    cacheSyncedRecipePreservingPending(remote)
                }
                .recoverCatching { error ->
                    if (error is CancellationException) throw error
                    if (local != null) {
                        local.toDomain()
                    } else {
                        throw error
                    }
                }
        }

    override suspend fun saveRecipe(recipe: Recipe): Result<Recipe> =
        withContext(dispatchers.io) {
            val now = Clock.System.now()
            val effectiveUpdatedAt = recipe.updatedAt ?: now
            val existing = localRecipeDao.getRawById(recipe.id.raw)
            val idempotencyKey = existing?.idempotencyKey ?: IdempotencyKey.generate().raw

            val pendingRecipe = recipe.copy(updatedAt = effectiveUpdatedAt)
            localRecipeDao.upsertRecipeWithIngredients(
                recipe = pendingRecipe.toEntity(
                    syncStatus = SyncStatus.PENDING,
                    idempotencyKey = idempotencyKey,
                    isDeleted = false,
                    now = effectiveUpdatedAt,
                ),
                ingredients = pendingRecipe.ingredients.map { it.toEntity(pendingRecipe.id.raw) },
            )

            val remoteResult = if (recipe.version == 0L) {
                remoteDataSource.createRecipe(pendingRecipe, idempotencyKey = idempotencyKey)
            } else {
                remoteDataSource.updateRecipe(pendingRecipe, idempotencyKey = idempotencyKey)
            }

            remoteResult.fold(
                onSuccess = { serverRecipe ->
                    if (serverRecipe.id != pendingRecipe.id) {
                        localRecipeDao.hardDeleteById(pendingRecipe.id.raw)
                    }
                    localRecipeDao.upsertRecipeWithIngredients(
                        recipe = serverRecipe.toEntity(
                            syncStatus = SyncStatus.SYNCED,
                            idempotencyKey = idempotencyKey,
                            isDeleted = false,
                            now = serverRecipe.updatedAt ?: now,
                        ),
                        ingredients = serverRecipe.ingredients.map { it.toEntity(serverRecipe.id.raw) },
                    )
                    Result.success(serverRecipe)
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    Result.success(pendingRecipe)
                },
            )
        }

    override suspend fun deleteRecipe(id: RecipeId): Result<Unit> =
        withContext(dispatchers.io) {
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val existing = localRecipeDao.getRawById(id.raw)
            val idempotencyKey = existing?.idempotencyKey ?: IdempotencyKey.generate().raw

            localRecipeDao.markDeleted(
                id = id.raw,
                status = SyncStatus.PENDING.name,
                updatedAt = nowMs,
            )

            remoteDataSource.deleteRecipe(id, idempotencyKey = idempotencyKey)
                .fold(
                    onSuccess = {
                        localRecipeDao.hardDeleteById(id.raw)
                        Result.success(Unit)
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        if (error is DomainError.NetworkUnavailable) {
                            Result.success(Unit)
                        } else {
                            Result.failure(error)
                        }
                    },
                )
        }

    override suspend fun forkRecipe(originId: RecipeId, newTitle: String?): Result<Recipe> =
        withContext(dispatchers.io) {
            val idempotencyKey = IdempotencyKey.generate().raw
            remoteDataSource.forkRecipe(originId, newTitle, idempotencyKey = idempotencyKey)
                .onSuccess { forked ->
                    localRecipeDao.upsertRecipeWithIngredients(
                        recipe = forked.toEntity(
                            syncStatus = SyncStatus.SYNCED,
                            idempotencyKey = idempotencyKey,
                            isDeleted = false,
                        ),
                        ingredients = forked.ingredients.map { it.toEntity(forked.id.raw) },
                    )
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                }
        }

    override suspend fun publishRecipe(id: RecipeId, visibility: ContentVisibility): Result<Recipe> =
        withContext(dispatchers.io) {
            val idempotencyKey = IdempotencyKey.generate().raw
            remoteDataSource.publishRecipe(id, visibility, idempotencyKey = idempotencyKey)
                .onSuccess { published ->
                    localRecipeDao.upsertRecipeWithIngredients(
                        recipe = published.toEntity(
                            syncStatus = SyncStatus.SYNCED,
                            idempotencyKey = idempotencyKey,
                            isDeleted = false,
                        ),
                        ingredients = published.ingredients.map { it.toEntity(published.id.raw) },
                    )
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                }
        }

    private suspend fun cacheSyncedRecipePreservingPending(recipe: Recipe) {
        val existing = localRecipeDao.getRawById(recipe.id.raw)
        if (existing?.syncStatus == SyncStatus.PENDING.name) {
            return
        }
        val key = existing?.idempotencyKey ?: IdempotencyKey.generate().raw
        localRecipeDao.upsertRecipeWithIngredients(
            recipe = recipe.toEntity(
                syncStatus = SyncStatus.SYNCED,
                idempotencyKey = key,
                isDeleted = false,
            ),
            ingredients = recipe.ingredients.map { it.toEntity(recipe.id.raw) },
        )
    }
}
