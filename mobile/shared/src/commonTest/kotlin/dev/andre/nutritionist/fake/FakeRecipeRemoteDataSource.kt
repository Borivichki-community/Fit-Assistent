package dev.andre.nutritionist.fake

import com.budgetntr.proto.recipe.v1.WeekMenuDayProto
import dev.andre.nutritionist.data.remote.RecipeRemoteDataSource
import dev.andre.nutritionist.domain.model.AccessGrant
import dev.andre.nutritionist.domain.model.AccessGrantId
import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.CoachStudentLink
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.domain.model.GrantTargetType
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanSearchCriteria
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.PagedResult
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeSearchCriteria
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Instant

class FakeRecipeRemoteDataSource : RecipeRemoteDataSource {

    // mutable by design: test control and call capture
    var shouldFail: Throwable? = null
    var searchRecipesResult: PagedResult<Recipe> = PagedResult(emptyList(), null, 0)
    var recipeByIdMap: MutableMap<RecipeId, Recipe> = mutableMapOf()
    var createRecipeHandler: ((Recipe, String?) -> Recipe)? = null
    var updateRecipeHandler: ((Recipe, String?) -> Recipe)? = null
    var forkRecipeHandler: ((RecipeId, String?) -> Recipe)? = null

    val createRecipeKeys = mutableListOf<String?>()
    val updateRecipeKeys = mutableListOf<String?>()
    val deleteRecipeCalls = mutableListOf<Pair<RecipeId, String?>>()
    val forkRecipeCalls = mutableListOf<Pair<RecipeId, String?>>()

    override suspend fun generateMealPlan(params: GenerateBudgetMenuParams): Result<MealPlan> {
        shouldFail?.let { return Result.failure(it) }
        return Result.success(
            MealPlan(
                id = "generated-plan",
                period = TargetPeriod.DAY,
                meals = emptyList(),
                totalNutrition = NutritionFacts.ZERO,
                estimatedCost = Money.ZERO,
            ),
        )
    }

    override fun generateWeekMenu(
        dailyCalories: Int,
        weeklyBudgetMinor: Long,
        excludedProductIds: List<String>,
        preferredProductIds: List<String>,
    ): Flow<WeekMenuDayProto> = emptyFlow()

    override suspend fun searchRecipes(criteria: RecipeSearchCriteria): Result<PagedResult<Recipe>> {
        shouldFail?.let { return Result.failure(it) }
        return Result.success(searchRecipesResult)
    }

    override suspend fun getRecipe(id: RecipeId): Result<Recipe> {
        shouldFail?.let { return Result.failure(it) }
        val found = recipeByIdMap[id] ?: return Result.failure(NoSuchElementException("Not found: ${id.raw}"))
        return Result.success(found)
    }

    override suspend fun createRecipe(recipe: Recipe, idempotencyKey: String?): Result<Recipe> {
        createRecipeKeys.add(idempotencyKey)
        shouldFail?.let { return Result.failure(it) }
        val result = createRecipeHandler?.invoke(recipe, idempotencyKey) ?: recipe.copy(version = 1L)
        recipeByIdMap[result.id] = result
        return Result.success(result)
    }

    override suspend fun updateRecipe(recipe: Recipe, idempotencyKey: String?): Result<Recipe> {
        updateRecipeKeys.add(idempotencyKey)
        shouldFail?.let { return Result.failure(it) }
        val result = updateRecipeHandler?.invoke(recipe, idempotencyKey) ?: recipe.copy(version = recipe.version + 1L)
        recipeByIdMap[result.id] = result
        return Result.success(result)
    }

    override suspend fun deleteRecipe(id: RecipeId, idempotencyKey: String?): Result<Unit> {
        deleteRecipeCalls.add(id to idempotencyKey)
        shouldFail?.let { return Result.failure(it) }
        recipeByIdMap.remove(id)
        return Result.success(Unit)
    }

    override suspend fun forkRecipe(
        originId: RecipeId,
        newTitle: String?,
        idempotencyKey: String?,
    ): Result<Recipe> {
        forkRecipeCalls.add(originId to newTitle)
        shouldFail?.let { return Result.failure(it) }
        val result = forkRecipeHandler?.invoke(originId, newTitle)
            ?: return Result.failure(NoSuchElementException("Fork source not configured"))
        recipeByIdMap[result.id] = result
        return Result.success(result)
    }

    override suspend fun publishRecipe(
        id: RecipeId,
        visibility: ContentVisibility,
        idempotencyKey: String?,
    ): Result<Recipe> {
        shouldFail?.let { return Result.failure(it) }
        val existing = recipeByIdMap[id]
            ?: return Result.failure(NoSuchElementException("Publish target not found"))
        val updated = existing.copy(visibility = visibility, version = existing.version + 1L)
        recipeByIdMap[id] = updated
        return Result.success(updated)
    }

    override suspend fun searchMealPlans(criteria: MealPlanSearchCriteria): Result<PagedResult<MealPlanTemplate>> =
        Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun getMealPlan(id: MealPlanTemplateId): Result<MealPlanTemplate> =
        Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun createMealPlan(plan: MealPlanTemplate, idempotencyKey: String?): Result<MealPlanTemplate> =
        Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun updateMealPlan(plan: MealPlanTemplate, idempotencyKey: String?): Result<MealPlanTemplate> =
        Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun deleteMealPlan(id: MealPlanTemplateId, idempotencyKey: String?): Result<Unit> =
        Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun listCoachLinks(statusFilter: CoachLinkStatus?): Result<List<CoachStudentLink>> =
        Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun inviteStudent(
        studentId: UserId,
        canViewDiary: Boolean,
        idempotencyKey: String?,
    ): Result<CoachStudentLink> =
        Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun respondCoachInvite(
        linkId: CoachLinkId,
        accept: Boolean,
        idempotencyKey: String?,
    ): Result<CoachStudentLink> =
        Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun grantAccess(
        userId: UserId,
        targetType: GrantTargetType,
        targetId: String,
        grantSource: GrantSource,
        expiresAt: Instant?,
        idempotencyKey: String?,
    ): Result<AccessGrant> =
        Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun revokeAccess(grantId: AccessGrantId, idempotencyKey: String?): Result<Unit> =
        Result.failure(UnsupportedOperationException("Not configured"))
}
