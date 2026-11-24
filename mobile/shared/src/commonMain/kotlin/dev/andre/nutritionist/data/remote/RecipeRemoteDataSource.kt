package dev.andre.nutritionist.data.remote

import com.budgetntr.proto.recipe.v1.WeekMenuDayProto
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
import dev.andre.nutritionist.domain.model.PagedResult
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeSearchCriteria
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface RecipeRemoteDataSource {
    suspend fun generateMealPlan(params: GenerateBudgetMenuParams): Result<MealPlan>
    fun generateWeekMenu(
        dailyCalories: Int,
        weeklyBudgetMinor: Long,
        excludedProductIds: List<String> = emptyList(),
        preferredProductIds: List<String> = emptyList(),
    ): Flow<WeekMenuDayProto>

    suspend fun searchRecipes(criteria: RecipeSearchCriteria): Result<PagedResult<Recipe>>
    suspend fun getRecipe(id: RecipeId): Result<Recipe>
    suspend fun createRecipe(recipe: Recipe, idempotencyKey: String? = null): Result<Recipe>
    suspend fun updateRecipe(recipe: Recipe, idempotencyKey: String? = null): Result<Recipe>
    suspend fun deleteRecipe(id: RecipeId, idempotencyKey: String? = null): Result<Unit>
    suspend fun forkRecipe(originId: RecipeId, newTitle: String? = null, idempotencyKey: String? = null): Result<Recipe>
    suspend fun publishRecipe(id: RecipeId, visibility: ContentVisibility, idempotencyKey: String? = null): Result<Recipe>

    suspend fun searchMealPlans(criteria: MealPlanSearchCriteria): Result<PagedResult<MealPlanTemplate>>
    suspend fun getMealPlan(id: MealPlanTemplateId): Result<MealPlanTemplate>
    suspend fun createMealPlan(plan: MealPlanTemplate, idempotencyKey: String? = null): Result<MealPlanTemplate>
    suspend fun updateMealPlan(plan: MealPlanTemplate, idempotencyKey: String? = null): Result<MealPlanTemplate>
    suspend fun deleteMealPlan(id: MealPlanTemplateId, idempotencyKey: String? = null): Result<Unit>

    suspend fun listCoachLinks(statusFilter: CoachLinkStatus? = null): Result<List<CoachStudentLink>>
    suspend fun inviteStudent(studentId: UserId, canViewDiary: Boolean = false, idempotencyKey: String? = null): Result<CoachStudentLink>
    suspend fun respondCoachInvite(linkId: CoachLinkId, accept: Boolean, idempotencyKey: String? = null): Result<CoachStudentLink>
    suspend fun grantAccess(
        userId: UserId,
        targetType: GrantTargetType,
        targetId: String,
        grantSource: GrantSource = GrantSource.COACH_GRANT,
        expiresAt: Instant? = null,
        idempotencyKey: String? = null,
    ): Result<AccessGrant>
    suspend fun revokeAccess(grantId: AccessGrantId, idempotencyKey: String? = null): Result<Unit>
}
