package dev.andre.nutritionist.recipe.infrastructure.grpc

import com.budgetntr.proto.recipe.v1.CheckCoachAccessRequest
import com.budgetntr.proto.recipe.v1.CheckCoachAccessResponse
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
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpc
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpcKt
import com.budgetntr.proto.recipe.v1.RecipeProto
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
import dev.andre.nutritionist.recipe.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.recipe.domain.model.MenuRequest
import dev.andre.nutritionist.recipe.domain.model.ProductId
import dev.andre.nutritionist.recipe.domain.model.RequestContext
import dev.andre.nutritionist.recipe.domain.model.UserId
import dev.andre.nutritionist.recipe.domain.service.CoachingAccessService
import dev.andre.nutritionist.recipe.domain.service.MealPlanService
import dev.andre.nutritionist.recipe.domain.service.RecipeCatalogService
import dev.andre.nutritionist.recipe.domain.service.RecipeMenuService
import dev.andre.nutritionist.recipe.infrastructure.grpc.client.CatalogClient
import dev.andre.nutritionist.recipe.infrastructure.grpc.mapper.toDomain
import dev.andre.nutritionist.recipe.infrastructure.grpc.mapper.toInstant
import dev.andre.nutritionist.recipe.infrastructure.grpc.mapper.toProto
import dev.andre.nutritionist.recipe.infrastructure.llm.MenuUnavailable
import dev.andre.nutritionist.recipe.infrastructure.llm.QuotaExceeded
import dev.andre.nutritionist.recipe.infrastructure.redis.RedisMenuStore
import dev.andre.nutritionist.recipe.infrastructure.security.AuthContext
import dev.andre.nutritionist.recipe.infrastructure.security.currentAuthContext
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Grpc
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.kotlin.GrpcContextElement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.grpc.server.service.GrpcService
import org.springframework.stereotype.Component
import java.util.UUID
import javax.net.ssl.SSLPeerUnverifiedException
import javax.security.auth.x500.X500Principal

@GrpcService
class RecipeGrpcService(
    private val recipe: RecipeMenuService,
    private val catalogService: RecipeCatalogService? = null,
    private val mealPlanService: MealPlanService? = null,
    private val coachingAccessService: CoachingAccessService? = null,
    private val redisStore: RedisMenuStore? = null,
    private val dispatchers: CoroutineDispatchers =
        dev.andre.nutritionist.recipe.coroutines
            .DefaultCoroutineDispatchers(),
) : RecipeAiServiceGrpcKt.RecipeAiServiceCoroutineImplBase() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun requireCatalog(): RecipeCatalogService = checkNotNull(catalogService) { "RecipeCatalogService is not configured" }

    private fun requireMealPlan(): MealPlanService = checkNotNull(mealPlanService) { "MealPlanService is not configured" }

    private fun requireCoaching(): CoachingAccessService = checkNotNull(coachingAccessService) { "CoachingAccessService is not configured" }

    // =========================================================================
    // Существующие RPC генерации меню
    // =========================================================================

    override fun generateWeekMenu(request: GenerateWeekMenuRequest): Flow<GenerateWeekMenuResponse> {
        val auth = currentAuthContext()
        val userId =
            auth.userId ?: RecipeIdentityInterceptor.requestContext
                .get()
                ?.user
                ?.raw
                ?: throw Status.UNAUTHENTICATED.asException()
        val context = RecipeIdentityInterceptor.requestContext.get() ?: RequestContext(UserId(userId))
        val logging = mapOf("userId" to userId)

        return flow {
            val input =
                MenuRequest(
                    request.dailyCalories,
                    request.weeklyBudgetMinor,
                    productIds(request.excludedProductIdsList).sortedBy { it.raw },
                    productIds(request.preferredProductIdsList),
                )
            val menu = recipe.generate(input, context)
            menu.daysList.forEach { emit(GenerateWeekMenuResponse.newBuilder().setDayReady(it).build()) }
            emit(GenerateWeekMenuResponse.newBuilder().setFinal(menu.summary).build())
        }.catch { failure ->
            val status = mapFailure(failure)
            throw status.asException()
        }.flowOn(dispatchers.io + MDCContext(logging))
    }

    override suspend fun generateMealPlan(request: GenerateMealPlanRequest): GenerateMealPlanResponse {
        val auth = currentAuthContext()
        val userId =
            auth.userId ?: RecipeIdentityInterceptor.requestContext
                .get()
                ?.user
                ?.raw
                ?: "usr_test_flow"
        val context = RecipeIdentityInterceptor.requestContext.get() ?: RequestContext(UserId(userId))
        val logging = mapOf("userId" to userId)

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val domainReq = request.toDomain()
                val plan = recipe.generateMealPlan(domainReq, context)
                plan.toProto()
            } catch (failure: Exception) {
                val status = mapFailure(failure)
                throw status.asException()
            }
        }
    }

    // =========================================================================
    // Управление рецептами
    // =========================================================================

    override suspend fun searchRecipes(request: SearchRecipesRequest): SearchRecipesResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val result =
                    requireCatalog().searchRecipes(
                        query = request.query.takeIf { it.isNotBlank() },
                        authorType = request.authorType.name,
                        visibility = request.visibility.name,
                        pageSize = request.pageSize,
                        pageToken = request.pageToken.takeIf { it.isNotBlank() },
                        authContext = auth,
                    )
                SearchRecipesResponse
                    .newBuilder()
                    .addAllRecipes(result.items.map { it.toProto(includeIngredients = false) })
                    .setNextPageToken(result.nextPageToken)
                    .setTotalCount(result.totalCount)
                    .build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun getRecipe(request: GetRecipeRequest): GetRecipeResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))
        val recipeId =
            runCatching { UUID.fromString(request.recipeId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid recipe_id format").asException() }

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val cacheKey = "recipe:public:$recipeId"
                // Проверяем Redis-кэш для публичных рецептов
                val cachedBytes = redisStore?.readCachedRecipe(cacheKey)
                if (cachedBytes != null) {
                    val proto = runCatching { RecipeProto.parseFrom(cachedBytes) }.getOrNull()
                    if (proto != null && proto.visibility.name.contains("PUBLIC")) {
                        return@withContext GetRecipeResponse.newBuilder().setRecipe(proto).build()
                    }
                }

                val recipeEntity = requireCatalog().getRecipe(recipeId, auth)
                val recipeProto = recipeEntity.toProto(includeIngredients = true)

                // Если рецепт публичный, сохраняем в Redis-кэш
                if (recipeEntity.visibility.equals("PUBLIC", ignoreCase = true)) {
                    runCatching {
                        redisStore?.writeCachedRecipe(cacheKey, recipeProto.toByteArray())
                    }
                }

                GetRecipeResponse.newBuilder().setRecipe(recipeProto).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun createRecipe(request: CreateRecipeRequest): CreateRecipeResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val created =
                    requireCatalog().createRecipe(
                        title = request.title,
                        description = request.description.takeIf { it.isNotBlank() },
                        instructions = request.instructions.takeIf { it.isNotBlank() },
                        visibility = request.visibility.name,
                        servingsCount = request.servingsCount,
                        ingredients = request.ingredientsList,
                        authContext = auth,
                    )
                CreateRecipeResponse.newBuilder().setRecipe(created.toProto()).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun updateRecipe(request: UpdateRecipeRequest): UpdateRecipeResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))
        val recipeId =
            runCatching { UUID.fromString(request.recipeId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid recipe_id format").asException() }

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val updated =
                    requireCatalog().updateRecipe(
                        recipeId = recipeId,
                        title = request.title,
                        description = request.description.takeIf { it.isNotBlank() },
                        instructions = request.instructions.takeIf { it.isNotBlank() },
                        visibility = request.visibility.name,
                        servingsCount = request.servingsCount,
                        ingredients = request.ingredientsList,
                        authContext = auth,
                    )
                // Инвалидация Redis-кэша
                redisStore?.evictCachedRecipe("recipe:public:$recipeId")

                UpdateRecipeResponse.newBuilder().setRecipe(updated.toProto()).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun deleteRecipe(request: DeleteRecipeRequest): DeleteRecipeResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))
        val recipeId =
            runCatching { UUID.fromString(request.recipeId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid recipe_id format").asException() }

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val success = requireCatalog().deleteRecipe(recipeId, auth)
                redisStore?.evictCachedRecipe("recipe:public:$recipeId")
                DeleteRecipeResponse.newBuilder().setSuccess(success).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun forkRecipe(request: ForkRecipeRequest): ForkRecipeResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))
        val originId =
            runCatching { UUID.fromString(request.sourceRecipeId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid source_recipe_id format").asException() }

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val forked =
                    requireCatalog().forkRecipe(
                        originRecipeId = originId,
                        newTitle = request.newTitle.takeIf { it.isNotBlank() },
                        authContext = auth,
                    )
                ForkRecipeResponse.newBuilder().setRecipe(forked.toProto()).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun publishRecipe(request: PublishRecipeRequest): PublishRecipeResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))
        val recipeId =
            runCatching { UUID.fromString(request.recipeId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid recipe_id format").asException() }

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val published =
                    requireCatalog().publishRecipe(
                        recipeId = recipeId,
                        visibility = request.visibility.name,
                        authContext = auth,
                    )
                redisStore?.evictCachedRecipe("recipe:public:$recipeId")
                PublishRecipeResponse.newBuilder().setRecipe(published.toProto()).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    // =========================================================================
    // Управление планами питания (меню)
    // =========================================================================

    override suspend fun searchMealPlans(request: SearchMealPlansRequest): SearchMealPlansResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val result =
                    requireMealPlan().searchMealPlans(
                        query = request.query.takeIf { it.isNotBlank() },
                        authorType = request.authorType.name,
                        visibility = request.visibility.name,
                        pageSize = request.pageSize,
                        pageToken = request.pageToken.takeIf { it.isNotBlank() },
                        authContext = auth,
                    )
                SearchMealPlansResponse
                    .newBuilder()
                    .addAllMealPlans(result.items.map { it.toProto(includeItems = false) })
                    .setNextPageToken(result.nextPageToken)
                    .setTotalCount(result.totalCount)
                    .build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun getMealPlan(request: GetMealPlanRequest): GetMealPlanResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))
        val planId =
            runCatching { UUID.fromString(request.planId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid plan_id format").asException() }

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val (planEntity, canViewItems) = requireMealPlan().getMealPlan(planId, auth)
                GetMealPlanResponse
                    .newBuilder()
                    .setMealPlan(planEntity.toProto(includeItems = canViewItems))
                    .build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun createMealPlan(request: CreateMealPlanRequest): CreateMealPlanResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val created =
                    requireMealPlan().createMealPlan(
                        title = request.title,
                        description = request.description.takeIf { it.isNotBlank() },
                        durationDays = request.durationDays,
                        visibility = request.visibility.name,
                        accessMode = request.accessMode.name,
                        priceAmountMinor = request.priceAmountMinor,
                        currency = request.currency,
                        items = request.itemsList,
                        authContext = auth,
                    )
                CreateMealPlanResponse.newBuilder().setMealPlan(created.toProto()).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun updateMealPlan(request: UpdateMealPlanRequest): UpdateMealPlanResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))
        val planId =
            runCatching { UUID.fromString(request.planId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid plan_id format").asException() }

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val updated =
                    requireMealPlan().updateMealPlan(
                        planId = planId,
                        title = request.title,
                        description = request.description.takeIf { it.isNotBlank() },
                        durationDays = request.durationDays,
                        visibility = request.visibility.name,
                        accessMode = request.accessMode.name,
                        priceAmountMinor = request.priceAmountMinor,
                        currency = request.currency,
                        items = request.itemsList,
                        authContext = auth,
                    )
                UpdateMealPlanResponse.newBuilder().setMealPlan(updated.toProto()).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun deleteMealPlan(request: DeleteMealPlanRequest): DeleteMealPlanResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))
        val planId =
            runCatching { UUID.fromString(request.planId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid plan_id format").asException() }

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val success = requireMealPlan().deleteMealPlan(planId, auth)
                DeleteMealPlanResponse.newBuilder().setSuccess(success).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    // =========================================================================
    // Тренерский доступ и Entitlements
    // =========================================================================

    override suspend fun inviteStudent(request: InviteStudentRequest): InviteStudentResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val link =
                    requireCoaching().inviteStudent(
                        studentId = request.studentId,
                        canViewDiary = request.canViewDiary,
                        authContext = auth,
                    )
                InviteStudentResponse.newBuilder().setLink(link.toProto()).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun respondCoachInvite(request: RespondCoachInviteRequest): RespondCoachInviteResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))
        val linkId =
            runCatching { UUID.fromString(request.linkId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid link_id format").asException() }

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val link =
                    requireCoaching().respondCoachInvite(
                        linkId = linkId,
                        accept = request.accept,
                        authContext = auth,
                    )
                RespondCoachInviteResponse.newBuilder().setLink(link.toProto()).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun listCoachLinks(request: ListCoachLinksRequest): ListCoachLinksResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val links =
                    requireCoaching().listCoachLinks(
                        statusFilter = request.statusFilter.name,
                        authContext = auth,
                    )
                ListCoachLinksResponse
                    .newBuilder()
                    .addAllLinks(links.map { it.toProto() })
                    .build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun grantAccess(request: GrantAccessRequest): GrantAccessResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))
        val targetId =
            runCatching { UUID.fromString(request.targetId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid target_id format").asException() }

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val expiresAt = if (request.hasExpiresAt()) request.expiresAt.toInstant() else null
                val grant =
                    requireCoaching().grantAccess(
                        targetUserId = request.userId,
                        targetType = request.targetType,
                        targetId = targetId,
                        grantSource = request.grantSource.name,
                        expiresAt = expiresAt,
                        authContext = auth,
                    )
                GrantAccessResponse.newBuilder().setGrant(grant.toProto()).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun revokeAccess(request: RevokeAccessRequest): RevokeAccessResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))
        val grantId =
            runCatching { UUID.fromString(request.grantId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid grant_id format").asException() }

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val success = requireCoaching().revokeAccess(grantId, auth)
                RevokeAccessResponse.newBuilder().setSuccess(success).build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    override suspend fun checkCoachAccess(request: CheckCoachAccessRequest): CheckCoachAccessResponse {
        val auth = currentAuthContext()
        val logging = mapOf("userId" to (auth.userId ?: "anonymous"))

        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(logging)) {
            try {
                val (hasAccess, canViewDiary) =
                    requireCoaching().checkCoachAccess(
                        coachId = request.coachId,
                        studentId = request.studentId,
                    )
                CheckCoachAccessResponse
                    .newBuilder()
                    .setHasAccess(hasAccess)
                    .setCanViewDiary(canViewDiary)
                    .build()
            } catch (failure: Exception) {
                throw mapFailure(failure).asException()
            }
        }
    }

    private fun mapFailure(failure: Throwable): Status =
        when (failure) {
            is CancellationException -> {
                throw failure
            }

            is SecurityException -> {
                Status.PERMISSION_DENIED.withDescription(failure.message)
            }

            is NoSuchElementException -> {
                Status.NOT_FOUND.withDescription(failure.message)
            }

            is IllegalArgumentException -> {
                Status.INVALID_ARGUMENT.withDescription(failure.message)
            }

            is QuotaExceeded -> {
                Status.RESOURCE_EXHAUSTED.withDescription(failure.message)
            }

            is MenuUnavailable -> {
                Status.UNAVAILABLE.withDescription(failure.message)
            }

            else -> {
                logger.error("Recipe service call failed: {}", failure.javaClass.simpleName, failure)
                Status.INTERNAL.withDescription("Recipe service operation failed")
            }
        }

    private fun productIds(ids: List<String>): List<ProductId> {
        require(ids.size <= 50) { "At most 50 product IDs per list" }
        return ids
            .map {
                require(uuidPattern.matches(it)) { "Product IDs must be UUIDs" }
                ProductId(UUID.fromString(it).toString())
            }.distinct()
    }

    companion object {
        private val uuidPattern = Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
    }
}

@Component
@GlobalServerInterceptor
class RecipeIdentityInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        if (call.methodDescriptor.serviceName != RecipeAiServiceGrpc.SERVICE_NAME) {
            return next.startCall(call, headers)
        }
        val session = call.attributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION)
        val peer =
            try {
                session?.peerPrincipal
            } catch (_: SSLPeerUnverifiedException) {
                null
            }
        val subject = headers.getAll(CatalogClient.userHeader)?.singleOrNull()?.takeIf { it.isNotBlank() && it.length <= 255 }
        val devBypass = headers.get(CatalogClient.devBypassHeader) == "true"
        if ((peer != gatewayPrincipal && !devBypass) || subject == null) {
            call.close(Status.UNAUTHENTICATED.withDescription("Authenticated gateway and identity required"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }

        val rolesHeader = headers.get(Metadata.Key.of("x-user-roles", Metadata.ASCII_STRING_MARSHALLER))
        val roles = parseRoles(rolesHeader)
        val authContext = AuthContext(userId = subject, roles = roles, isAuthenticated = true)

        val trace = headers.get(CatalogClient.traceHeader)?.takeIf(tracePattern::matches)
        val context =
            Context
                .current()
                .withValue(requestContext, RequestContext(UserId(subject), trace))
                .withValue(AuthContext.AUTH_CONTEXT_KEY, authContext)

        return Contexts.interceptCall(context, call, headers, next)
    }

    private fun parseRoles(rolesHeader: String?): Set<String> {
        val trimmed = rolesHeader?.trim().orEmpty()
        if (trimmed.isEmpty()) return setOf(AuthContext.ROLE_USER)
        return trimmed
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
            .ifEmpty { setOf(AuthContext.ROLE_USER) }
    }

    companion object {
        internal val requestContext: Context.Key<RequestContext> = Context.key("recipe-request")
        private val gatewayPrincipal = X500Principal("CN=fitassistant-envoy")
        private val tracePattern = Regex("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}")
    }
}
