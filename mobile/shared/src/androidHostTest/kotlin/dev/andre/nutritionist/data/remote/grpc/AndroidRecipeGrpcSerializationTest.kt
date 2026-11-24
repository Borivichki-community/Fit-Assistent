package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.profile.v1.TimestampProto
import com.budgetntr.proto.recipe.v1.AccessGrantDtoProto
import com.budgetntr.proto.recipe.v1.AccessModeProto
import com.budgetntr.proto.recipe.v1.AuthorTypeProto
import com.budgetntr.proto.recipe.v1.CoachLinkStatusProto
import com.budgetntr.proto.recipe.v1.CoachStudentLinkDtoProto
import com.budgetntr.proto.recipe.v1.ContentVisibilityProto
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
import com.budgetntr.proto.recipe.v1.GetMealPlanRequest
import com.budgetntr.proto.recipe.v1.GetMealPlanResponse
import com.budgetntr.proto.recipe.v1.GetRecipeRequest
import com.budgetntr.proto.recipe.v1.GetRecipeResponse
import com.budgetntr.proto.recipe.v1.GrantAccessRequest
import com.budgetntr.proto.recipe.v1.GrantAccessResponse
import com.budgetntr.proto.recipe.v1.GrantSourceProto
import com.budgetntr.proto.recipe.v1.InviteStudentRequest
import com.budgetntr.proto.recipe.v1.InviteStudentResponse
import com.budgetntr.proto.recipe.v1.ListCoachLinksRequest
import com.budgetntr.proto.recipe.v1.ListCoachLinksResponse
import com.budgetntr.proto.recipe.v1.MealPlanItemDtoProto
import com.budgetntr.proto.recipe.v1.MealPlanTemplateDtoProto
import com.budgetntr.proto.recipe.v1.MealTypeProto
import com.budgetntr.proto.recipe.v1.PlanItemTypeProto
import com.budgetntr.proto.recipe.v1.PublishRecipeRequest
import com.budgetntr.proto.recipe.v1.PublishRecipeResponse
import com.budgetntr.proto.recipe.v1.RecipeCatalogDtoProto
import com.budgetntr.proto.recipe.v1.RecipeIngredientDtoProto
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
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidRecipeGrpcSerializationTest {

    private val sampleRecipeDto = RecipeCatalogDtoProto(
        id = "rec-1",
        authorType = AuthorTypeProto.AUTHOR_TYPE_COACH,
        authorId = "coach-1",
        originRecipeId = "sys-1",
        title = "Куриная грудка с киноа",
        description = "Лёгкий ужин",
        instructions = "1. Сварить киноа\n2. Запечь грудку",
        visibility = ContentVisibilityProto.CONTENT_VISIBILITY_PUBLIC,
        servingsCount = 2,
        totalWeightG = 350.0,
        caloriesPer100g = 145.5,
        proteinsPer100g = 19.2,
        fatsPer100g = 3.1,
        carbsPer100g = 11.4,
        ingredients = listOf(
            RecipeIngredientDtoProto(
                id = "ing-1",
                recipeId = "rec-1",
                productId = "prod-chicken",
                productNameSnapshot = "Куриная грудка",
                weightGrams = 200.0,
                caloriesPer100g = 113.0,
                proteinsPer100g = 23.6,
                fatsPer100g = 1.9,
                carbsPer100g = 0.0,
            ),
        ),
        version = 2L,
        createdAt = TimestampProto(1700000000L, 123),
        updatedAt = TimestampProto(1700003600L, 456),
    )

    private val samplePlanDto = MealPlanTemplateDtoProto(
        id = "plan-1",
        authorId = "coach-1",
        authorType = AuthorTypeProto.AUTHOR_TYPE_COACH,
        title = "Сушка на 1800 ккал",
        description = "План на неделю",
        durationDays = 7,
        visibility = ContentVisibilityProto.CONTENT_VISIBILITY_PUBLIC,
        accessMode = AccessModeProto.ACCESS_MODE_COACH_ASSIGNED,
        priceAmountMinor = 99000L,
        currency = "RUB",
        avgDailyCalories = 1800.0,
        avgDailyProteins = 140.0,
        avgDailyFats = 55.0,
        avgDailyCarbs = 185.0,
        items = listOf(
            MealPlanItemDtoProto(
                id = "item-1",
                planId = "plan-1",
                dayNumber = 1,
                mealSlot = MealTypeProto.MEAL_TYPE_LUNCH,
                itemType = PlanItemTypeProto.PLAN_ITEM_TYPE_RECIPE,
                itemId = "rec-1",
                nameSnapshot = "Куриная грудка с киноа",
                weightGrams = 350.0,
                calories = 509.25,
                proteins = 67.2,
                fats = 10.85,
                carbs = 39.9,
            ),
        ),
        version = 1L,
        createdAt = TimestampProto(1700000000L, 0),
        updatedAt = TimestampProto(1700005000L, 0),
    )

    @Test
    fun searchAndGetRecipe_roundtripSerialization() {
        val searchReq = SearchRecipesRequest(
            query = "курица",
            authorType = AuthorTypeProto.AUTHOR_TYPE_COACH,
            visibility = ContentVisibilityProto.CONTENT_VISIBILITY_PUBLIC,
            pageSize = 20,
            pageToken = "cursor-1",
        )
        assertEquals(searchReq, SearchRecipesRequestMarshaller.parse(SearchRecipesRequestMarshaller.stream(searchReq)))

        val searchResp = SearchRecipesResponse(
            recipes = listOf(sampleRecipeDto),
            nextPageToken = "cursor-2",
            totalCount = 1,
        )
        assertEquals(searchResp, SearchRecipesResponseMarshaller.parse(SearchRecipesResponseMarshaller.stream(searchResp)))

        val getReq = GetRecipeRequest(recipeId = "rec-1")
        assertEquals(getReq, GetRecipeRequestMarshaller.parse(GetRecipeRequestMarshaller.stream(getReq)))

        val getResp = GetRecipeResponse(recipe = sampleRecipeDto)
        assertEquals(getResp, GetRecipeResponseMarshaller.parse(GetRecipeResponseMarshaller.stream(getResp)))
    }

    @Test
    fun createUpdateDeleteForkPublishRecipe_roundtripSerialization() {
        val createReq = CreateRecipeRequest(
            title = "Омлет",
            description = "Быстрый завтрак",
            instructions = "Взбить и пожарить",
            visibility = ContentVisibilityProto.CONTENT_VISIBILITY_PRIVATE,
            servingsCount = 1,
            ingredients = sampleRecipeDto.ingredients,
        )
        assertEquals(createReq, CreateRecipeRequestMarshaller.parse(CreateRecipeRequestMarshaller.stream(createReq)))

        val createResp = CreateRecipeResponse(recipe = sampleRecipeDto)
        assertEquals(createResp, CreateRecipeResponseMarshaller.parse(CreateRecipeResponseMarshaller.stream(createResp)))

        val updateReq = UpdateRecipeRequest(
            recipeId = "rec-1",
            title = "Омлет с зеленью",
            description = "Обновлённый",
            instructions = "Добавить укроп",
            visibility = ContentVisibilityProto.CONTENT_VISIBILITY_LINK_ONLY,
            servingsCount = 2,
            ingredients = sampleRecipeDto.ingredients,
        )
        assertEquals(updateReq, UpdateRecipeRequestMarshaller.parse(UpdateRecipeRequestMarshaller.stream(updateReq)))

        val updateResp = UpdateRecipeResponse(recipe = sampleRecipeDto)
        assertEquals(updateResp, UpdateRecipeResponseMarshaller.parse(UpdateRecipeResponseMarshaller.stream(updateResp)))

        val deleteReq = DeleteRecipeRequest(recipeId = "rec-1")
        assertEquals(deleteReq, DeleteRecipeRequestMarshaller.parse(DeleteRecipeRequestMarshaller.stream(deleteReq)))

        val deleteResp = DeleteRecipeResponse(success = true)
        assertEquals(deleteResp, DeleteRecipeResponseMarshaller.parse(DeleteRecipeResponseMarshaller.stream(deleteResp)))

        val forkReq = ForkRecipeRequest(sourceRecipeId = "sys-1", newTitle = "Мой форк")
        assertEquals(forkReq, ForkRecipeRequestMarshaller.parse(ForkRecipeRequestMarshaller.stream(forkReq)))

        val forkResp = ForkRecipeResponse(recipe = sampleRecipeDto)
        assertEquals(forkResp, ForkRecipeResponseMarshaller.parse(ForkRecipeResponseMarshaller.stream(forkResp)))

        val publishReq = PublishRecipeRequest(
            recipeId = "rec-1",
            visibility = ContentVisibilityProto.CONTENT_VISIBILITY_PUBLIC,
        )
        assertEquals(publishReq, PublishRecipeRequestMarshaller.parse(PublishRecipeRequestMarshaller.stream(publishReq)))

        val publishResp = PublishRecipeResponse(recipe = sampleRecipeDto)
        assertEquals(publishResp, PublishRecipeResponseMarshaller.parse(PublishRecipeResponseMarshaller.stream(publishResp)))
    }

    @Test
    fun mealPlans_roundtripSerialization() {
        val searchReq = SearchMealPlansRequest(
            query = "сушка",
            authorType = AuthorTypeProto.AUTHOR_TYPE_COACH,
            visibility = ContentVisibilityProto.CONTENT_VISIBILITY_PUBLIC,
            pageSize = 10,
            pageToken = "tok-1",
        )
        assertEquals(searchReq, SearchMealPlansRequestMarshaller.parse(SearchMealPlansRequestMarshaller.stream(searchReq)))

        val searchResp = SearchMealPlansResponse(
            mealPlans = listOf(samplePlanDto),
            nextPageToken = "tok-2",
            totalCount = 1,
        )
        assertEquals(searchResp, SearchMealPlansResponseMarshaller.parse(SearchMealPlansResponseMarshaller.stream(searchResp)))

        val getReq = GetMealPlanRequest(planId = "plan-1")
        assertEquals(getReq, GetMealPlanRequestMarshaller.parse(GetMealPlanRequestMarshaller.stream(getReq)))

        val getResp = GetMealPlanResponse(mealPlan = samplePlanDto)
        assertEquals(getResp, GetMealPlanResponseMarshaller.parse(GetMealPlanResponseMarshaller.stream(getResp)))

        val createReq = CreateMealPlanRequest(
            title = samplePlanDto.title,
            description = samplePlanDto.description,
            durationDays = 7,
            visibility = ContentVisibilityProto.CONTENT_VISIBILITY_PUBLIC,
            accessMode = AccessModeProto.ACCESS_MODE_FREE,
            priceAmountMinor = 0L,
            currency = "RUB",
            items = samplePlanDto.items,
        )
        assertEquals(createReq, CreateMealPlanRequestMarshaller.parse(CreateMealPlanRequestMarshaller.stream(createReq)))

        val createResp = CreateMealPlanResponse(mealPlan = samplePlanDto)
        assertEquals(createResp, CreateMealPlanResponseMarshaller.parse(CreateMealPlanResponseMarshaller.stream(createResp)))

        val updateReq = UpdateMealPlanRequest(
            planId = "plan-1",
            title = "Сушка v2",
            description = "Новое описание",
            durationDays = 14,
            visibility = ContentVisibilityProto.CONTENT_VISIBILITY_PUBLIC,
            accessMode = AccessModeProto.ACCESS_MODE_PAID,
            priceAmountMinor = 150000L,
            currency = "RUB",
            items = samplePlanDto.items,
        )
        assertEquals(updateReq, UpdateMealPlanRequestMarshaller.parse(UpdateMealPlanRequestMarshaller.stream(updateReq)))

        val updateResp = UpdateMealPlanResponse(mealPlan = samplePlanDto)
        assertEquals(updateResp, UpdateMealPlanResponseMarshaller.parse(UpdateMealPlanResponseMarshaller.stream(updateResp)))

        val deleteReq = DeleteMealPlanRequest(planId = "plan-1")
        assertEquals(deleteReq, DeleteMealPlanRequestMarshaller.parse(DeleteMealPlanRequestMarshaller.stream(deleteReq)))

        val deleteResp = DeleteMealPlanResponse(success = true)
        assertEquals(deleteResp, DeleteMealPlanResponseMarshaller.parse(DeleteMealPlanResponseMarshaller.stream(deleteResp)))
    }

    @Test
    fun coachingAndAccessGrants_roundtripSerialization() {
        val sampleLink = CoachStudentLinkDtoProto(
            id = "link-1",
            coachId = "coach-1",
            studentId = "student-1",
            status = CoachLinkStatusProto.COACH_LINK_STATUS_ACTIVE,
            canViewDiary = true,
            createdAt = TimestampProto(1700000000L, 0),
        )
        val sampleGrant = AccessGrantDtoProto(
            id = "grant-1",
            userId = "student-1",
            targetType = "MEAL_PLAN",
            targetId = "plan-1",
            grantSource = GrantSourceProto.GRANT_SOURCE_COACH_GRANT,
            grantedBy = "coach-1",
            expiresAt = TimestampProto(1800000000L, 0),
            createdAt = TimestampProto(1700000000L, 0),
        )

        val inviteReq = InviteStudentRequest(studentId = "student-1", canViewDiary = true)
        assertEquals(inviteReq, InviteStudentRequestMarshaller.parse(InviteStudentRequestMarshaller.stream(inviteReq)))

        val inviteResp = InviteStudentResponse(link = sampleLink)
        assertEquals(inviteResp, InviteStudentResponseMarshaller.parse(InviteStudentResponseMarshaller.stream(inviteResp)))

        val respondReq = RespondCoachInviteRequest(linkId = "link-1", accept = true)
        assertEquals(respondReq, RespondCoachInviteRequestMarshaller.parse(RespondCoachInviteRequestMarshaller.stream(respondReq)))

        val respondResp = RespondCoachInviteResponse(link = sampleLink)
        assertEquals(respondResp, RespondCoachInviteResponseMarshaller.parse(RespondCoachInviteResponseMarshaller.stream(respondResp)))

        val listReq = ListCoachLinksRequest(statusFilter = CoachLinkStatusProto.COACH_LINK_STATUS_ACTIVE)
        assertEquals(listReq, ListCoachLinksRequestMarshaller.parse(ListCoachLinksRequestMarshaller.stream(listReq)))

        val listResp = ListCoachLinksResponse(links = listOf(sampleLink))
        assertEquals(listResp, ListCoachLinksResponseMarshaller.parse(ListCoachLinksResponseMarshaller.stream(listResp)))

        val grantReq = GrantAccessRequest(
            userId = "student-1",
            targetType = "MEAL_PLAN",
            targetId = "plan-1",
            grantSource = GrantSourceProto.GRANT_SOURCE_COACH_GRANT,
            expiresAt = TimestampProto(1800000000L, 0),
        )
        assertEquals(grantReq, GrantAccessRequestMarshaller.parse(GrantAccessRequestMarshaller.stream(grantReq)))

        val grantResp = GrantAccessResponse(grant = sampleGrant)
        assertEquals(grantResp, GrantAccessResponseMarshaller.parse(GrantAccessResponseMarshaller.stream(grantResp)))

        val revokeReq = RevokeAccessRequest(grantId = "grant-1")
        assertEquals(revokeReq, RevokeAccessRequestMarshaller.parse(RevokeAccessRequestMarshaller.stream(revokeReq)))

        val revokeResp = RevokeAccessResponse(success = true)
        assertEquals(revokeResp, RevokeAccessResponseMarshaller.parse(RevokeAccessResponseMarshaller.stream(revokeResp)))
    }
}
