package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.recipe.v1.CoachLinkStatusProto
import com.budgetntr.proto.recipe.v1.CreateMealPlanRequest
import com.budgetntr.proto.recipe.v1.CreateMealPlanResponse
import com.budgetntr.proto.recipe.v1.CreateRecipeRequest
import com.budgetntr.proto.recipe.v1.CreateRecipeResponse
import com.budgetntr.proto.recipe.v1.DeleteMealPlanRequest
import com.budgetntr.proto.recipe.v1.DeleteMealPlanResponse
import com.budgetntr.proto.recipe.v1.DeleteRecipeRequest
import com.budgetntr.proto.recipe.v1.DeleteRecipeResponse
import com.budgetntr.proto.recipe.v1.ForkRecipeRequest
import com.budgetntr.proto.recipe.v1.ForkRecipeResponse
import com.budgetntr.proto.recipe.v1.GenerateMealPlanRequest
import com.budgetntr.proto.recipe.v1.GenerateMealPlanResponse
import com.budgetntr.proto.recipe.v1.GenerateWeekMenuRequest
import com.budgetntr.proto.recipe.v1.GenerateWeekMenuResponse
import com.budgetntr.proto.recipe.v1.GetMealPlanRequest
import com.budgetntr.proto.recipe.v1.GetMealPlanResponse
import com.budgetntr.proto.recipe.v1.GetRecipeRequest
import com.budgetntr.proto.recipe.v1.GetRecipeResponse
import com.budgetntr.proto.recipe.v1.GrantAccessRequest
import com.budgetntr.proto.recipe.v1.GrantAccessResponse
import com.budgetntr.proto.recipe.v1.InviteStudentRequest
import com.budgetntr.proto.recipe.v1.InviteStudentResponse
import com.budgetntr.proto.recipe.v1.ListCoachLinksRequest
import com.budgetntr.proto.recipe.v1.ListCoachLinksResponse
import com.budgetntr.proto.recipe.v1.PublishRecipeRequest
import com.budgetntr.proto.recipe.v1.PublishRecipeResponse
import com.budgetntr.proto.recipe.v1.RespondCoachInviteRequest
import com.budgetntr.proto.recipe.v1.RespondCoachInviteResponse
import com.budgetntr.proto.recipe.v1.RevokeAccessRequest
import com.budgetntr.proto.recipe.v1.RevokeAccessResponse
import com.budgetntr.proto.recipe.v1.SearchMealPlansRequest
import com.budgetntr.proto.recipe.v1.SearchMealPlansResponse
import com.budgetntr.proto.recipe.v1.SearchRecipesRequest
import com.budgetntr.proto.recipe.v1.SearchRecipesResponse
import com.budgetntr.proto.recipe.v1.UpdateMealPlanRequest
import com.budgetntr.proto.recipe.v1.UpdateMealPlanResponse
import com.budgetntr.proto.recipe.v1.UpdateRecipeRequest
import com.budgetntr.proto.recipe.v1.UpdateRecipeResponse
import com.budgetntr.proto.recipe.v1.WeekMenuDayProto
import dev.andre.nutritionist.data.mapper.RecipeCatalogDtoMapper.toCreateRequest
import dev.andre.nutritionist.data.mapper.RecipeCatalogDtoMapper.toDomain
import dev.andre.nutritionist.data.mapper.RecipeCatalogDtoMapper.toProto
import dev.andre.nutritionist.data.mapper.RecipeCatalogDtoMapper.toProtoRequest
import dev.andre.nutritionist.data.mapper.RecipeCatalogDtoMapper.toProtoString
import dev.andre.nutritionist.data.mapper.RecipeCatalogDtoMapper.toTimestampProto
import dev.andre.nutritionist.data.mapper.RecipeCatalogDtoMapper.toUpdateRequest
import dev.andre.nutritionist.data.mapper.RecipeDtoMapper.toDomain
import dev.andre.nutritionist.data.mapper.RecipeDtoMapper.toRequest
import dev.andre.nutritionist.data.remote.RecipeRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.error.DomainError
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlin.time.Instant

interface RecipeGrpcServiceClient {
    suspend fun generateMealPlan(request: GenerateMealPlanRequest): GenerateMealPlanResponse
    fun generateWeekMenu(request: GenerateWeekMenuRequest): Flow<GenerateWeekMenuResponse>

    suspend fun searchRecipes(request: SearchRecipesRequest): SearchRecipesResponse
    suspend fun getRecipe(request: GetRecipeRequest): GetRecipeResponse
    suspend fun createRecipe(request: CreateRecipeRequest, idempotencyKey: String? = null): CreateRecipeResponse
    suspend fun updateRecipe(request: UpdateRecipeRequest, idempotencyKey: String? = null): UpdateRecipeResponse
    suspend fun deleteRecipe(request: DeleteRecipeRequest, idempotencyKey: String? = null): DeleteRecipeResponse
    suspend fun forkRecipe(request: ForkRecipeRequest, idempotencyKey: String? = null): ForkRecipeResponse
    suspend fun publishRecipe(request: PublishRecipeRequest, idempotencyKey: String? = null): PublishRecipeResponse

    suspend fun searchMealPlans(request: SearchMealPlansRequest): SearchMealPlansResponse
    suspend fun getMealPlan(request: GetMealPlanRequest): GetMealPlanResponse
    suspend fun createMealPlan(request: CreateMealPlanRequest, idempotencyKey: String? = null): CreateMealPlanResponse
    suspend fun updateMealPlan(request: UpdateMealPlanRequest, idempotencyKey: String? = null): UpdateMealPlanResponse
    suspend fun deleteMealPlan(request: DeleteMealPlanRequest, idempotencyKey: String? = null): DeleteMealPlanResponse

    suspend fun listCoachLinks(request: ListCoachLinksRequest): ListCoachLinksResponse
    suspend fun inviteStudent(request: InviteStudentRequest, idempotencyKey: String? = null): InviteStudentResponse
    suspend fun respondCoachInvite(request: RespondCoachInviteRequest, idempotencyKey: String? = null): RespondCoachInviteResponse
    suspend fun grantAccess(request: GrantAccessRequest, idempotencyKey: String? = null): GrantAccessResponse
    suspend fun revokeAccess(request: RevokeAccessRequest, idempotencyKey: String? = null): RevokeAccessResponse
}

class RecipeGrpcRemoteDataSource(
    private val client: RecipeGrpcServiceClient,
    private val dispatchers: CoroutineDispatchers,
    private val errorMapper: (Throwable) -> DomainError = { throwable ->
        throwable as? DomainError ?: DomainError.Unknown(throwable.message ?: "Unknown error", throwable)
    },
) : RecipeRemoteDataSource {

    override suspend fun generateMealPlan(params: GenerateBudgetMenuParams): Result<MealPlan> =
        executeMapped {
            val request = params.toRequest()
            client.generateMealPlan(request).toDomain()
        }

    override fun generateWeekMenu(
        dailyCalories: Int,
        weeklyBudgetMinor: Long,
        excludedProductIds: List<String>,
        preferredProductIds: List<String>,
    ): Flow<WeekMenuDayProto> {
        val request = GenerateWeekMenuRequest(
            dailyCalories = dailyCalories,
            weeklyBudgetMinor = weeklyBudgetMinor,
            excludedProductIds = excludedProductIds,
            preferredProductIds = preferredProductIds,
        )
        return client.generateWeekMenu(request)
            .mapNotNull { it.dayReady }
            .flowOn(dispatchers.io)
    }

    override suspend fun searchRecipes(criteria: RecipeSearchCriteria): Result<PagedResult<Recipe>> =
        executeMapped {
            client.searchRecipes(criteria.toProtoRequest()).toDomain()
        }

    override suspend fun getRecipe(id: RecipeId): Result<Recipe> =
        executeMapped {
            client.getRecipe(GetRecipeRequest(recipeId = id.raw)).recipe.toDomain()
        }

    override suspend fun createRecipe(recipe: Recipe, idempotencyKey: String?): Result<Recipe> =
        executeMapped {
            client.createRecipe(recipe.toCreateRequest(), idempotencyKey).recipe.toDomain()
        }

    override suspend fun updateRecipe(recipe: Recipe, idempotencyKey: String?): Result<Recipe> =
        executeMapped {
            client.updateRecipe(recipe.toUpdateRequest(), idempotencyKey).recipe.toDomain()
        }

    override suspend fun deleteRecipe(id: RecipeId, idempotencyKey: String?): Result<Unit> =
        executeMapped {
            client.deleteRecipe(DeleteRecipeRequest(recipeId = id.raw), idempotencyKey)
            Unit
        }

    override suspend fun forkRecipe(
        originId: RecipeId,
        newTitle: String?,
        idempotencyKey: String?,
    ): Result<Recipe> = executeMapped {
        val request = ForkRecipeRequest(
            sourceRecipeId = originId.raw,
            newTitle = newTitle.orEmpty(),
        )
        client.forkRecipe(request, idempotencyKey).recipe.toDomain()
    }

    override suspend fun publishRecipe(
        id: RecipeId,
        visibility: ContentVisibility,
        idempotencyKey: String?,
    ): Result<Recipe> = executeMapped {
        val request = PublishRecipeRequest(
            recipeId = id.raw,
            visibility = visibility.toProto(),
        )
        client.publishRecipe(request, idempotencyKey).recipe.toDomain()
    }

    override suspend fun searchMealPlans(criteria: MealPlanSearchCriteria): Result<PagedResult<MealPlanTemplate>> =
        executeMapped {
            client.searchMealPlans(criteria.toProtoRequest()).toDomain()
        }

    override suspend fun getMealPlan(id: MealPlanTemplateId): Result<MealPlanTemplate> =
        executeMapped {
            client.getMealPlan(GetMealPlanRequest(planId = id.raw)).mealPlan.toDomain()
        }

    override suspend fun createMealPlan(
        plan: MealPlanTemplate,
        idempotencyKey: String?,
    ): Result<MealPlanTemplate> = executeMapped {
        client.createMealPlan(plan.toCreateRequest(), idempotencyKey).mealPlan.toDomain()
    }

    override suspend fun updateMealPlan(
        plan: MealPlanTemplate,
        idempotencyKey: String?,
    ): Result<MealPlanTemplate> = executeMapped {
        client.updateMealPlan(plan.toUpdateRequest(), idempotencyKey).mealPlan.toDomain()
    }

    override suspend fun deleteMealPlan(id: MealPlanTemplateId, idempotencyKey: String?): Result<Unit> =
        executeMapped {
            client.deleteMealPlan(DeleteMealPlanRequest(planId = id.raw), idempotencyKey)
            Unit
        }

    override suspend fun listCoachLinks(statusFilter: CoachLinkStatus?): Result<List<CoachStudentLink>> =
        executeMapped {
            val request = ListCoachLinksRequest(
                statusFilter = statusFilter?.toProto() ?: CoachLinkStatusProto.COACH_LINK_STATUS_UNSPECIFIED,
            )
            client.listCoachLinks(request).links.map { it.toDomain() }
        }

    override suspend fun inviteStudent(
        studentId: UserId,
        canViewDiary: Boolean,
        idempotencyKey: String?,
    ): Result<CoachStudentLink> = executeMapped {
        val request = InviteStudentRequest(
            studentId = studentId.raw,
            canViewDiary = canViewDiary,
        )
        client.inviteStudent(request, idempotencyKey).link.toDomain()
    }

    override suspend fun respondCoachInvite(
        linkId: CoachLinkId,
        accept: Boolean,
        idempotencyKey: String?,
    ): Result<CoachStudentLink> = executeMapped {
        val request = RespondCoachInviteRequest(
            linkId = linkId.raw,
            accept = accept,
        )
        client.respondCoachInvite(request, idempotencyKey).link.toDomain()
    }

    override suspend fun grantAccess(
        userId: UserId,
        targetType: GrantTargetType,
        targetId: String,
        grantSource: GrantSource,
        expiresAt: Instant?,
        idempotencyKey: String?,
    ): Result<AccessGrant> = executeMapped {
        val request = GrantAccessRequest(
            userId = userId.raw,
            targetType = targetType.toProtoString(),
            targetId = targetId,
            grantSource = grantSource.toProto(),
            expiresAt = expiresAt?.toTimestampProto(),
        )
        client.grantAccess(request, idempotencyKey).grant.toDomain()
    }

    override suspend fun revokeAccess(grantId: AccessGrantId, idempotencyKey: String?): Result<Unit> =
        executeMapped {
            client.revokeAccess(RevokeAccessRequest(grantId = grantId.raw), idempotencyKey)
            Unit
        }

    private suspend inline fun <T> executeMapped(crossinline block: suspend () -> T): Result<T> =
        withContext(dispatchers.io) {
            try {
                Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Result.failure(errorMapper(t))
            }
        }
}
