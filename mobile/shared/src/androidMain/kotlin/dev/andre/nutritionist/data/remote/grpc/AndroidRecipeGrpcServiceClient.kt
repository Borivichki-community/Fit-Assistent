package dev.andre.nutritionist.data.remote.grpc

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
import com.budgetntr.proto.recipe.v1.GeneratedMealProto
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
import com.budgetntr.proto.recipe.v1.MealTypeProto
import com.budgetntr.proto.recipe.v1.MenuIngredientProto
import com.budgetntr.proto.recipe.v1.MenuMealProto
import com.budgetntr.proto.recipe.v1.MenuSourceProto
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
import com.budgetntr.proto.recipe.v1.TargetNutritionProto
import com.budgetntr.proto.recipe.v1.TargetPeriodProto
import com.budgetntr.proto.recipe.v1.UpdateMealPlanRequest
import com.budgetntr.proto.recipe.v1.UpdateMealPlanResponse
import com.budgetntr.proto.recipe.v1.UpdateRecipeRequest
import com.budgetntr.proto.recipe.v1.UpdateRecipeResponse
import com.budgetntr.proto.recipe.v1.WeekMenuDayProto
import com.budgetntr.proto.recipe.v1.WeekMenuSummaryProto
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import dev.andre.nutritionist.data.remote.grpc.interceptors.UnifiedGrpcClientInterceptor
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class AndroidRecipeGrpcServiceClient(
    private val channel: Channel,
) : RecipeGrpcServiceClient {

    override suspend fun generateMealPlan(request: GenerateMealPlanRequest): GenerateMealPlanResponse =
        channel.unaryCall(
            generateMealPlanMethod,
            request,
            CallOptions.DEFAULT.withDeadlineAfter(65L, TimeUnit.SECONDS),
        )

    override fun generateWeekMenu(request: GenerateWeekMenuRequest): Flow<GenerateWeekMenuResponse> =
        channel.serverStreamingCall(generateWeekMenuMethod, request)

    override suspend fun searchRecipes(request: SearchRecipesRequest): SearchRecipesResponse =
        channel.unaryCall(searchRecipesMethod, request)

    override suspend fun getRecipe(request: GetRecipeRequest): GetRecipeResponse =
        channel.unaryCall(getRecipeMethod, request)

    override suspend fun createRecipe(request: CreateRecipeRequest, idempotencyKey: String?): CreateRecipeResponse =
        channel.unaryCall(createRecipeMethod, request, idempotencyCallOptions(idempotencyKey))

    override suspend fun updateRecipe(request: UpdateRecipeRequest, idempotencyKey: String?): UpdateRecipeResponse =
        channel.unaryCall(updateRecipeMethod, request, idempotencyCallOptions(idempotencyKey))

    override suspend fun deleteRecipe(request: DeleteRecipeRequest, idempotencyKey: String?): DeleteRecipeResponse =
        channel.unaryCall(deleteRecipeMethod, request, idempotencyCallOptions(idempotencyKey))

    override suspend fun forkRecipe(request: ForkRecipeRequest, idempotencyKey: String?): ForkRecipeResponse =
        channel.unaryCall(forkRecipeMethod, request, idempotencyCallOptions(idempotencyKey))

    override suspend fun publishRecipe(request: PublishRecipeRequest, idempotencyKey: String?): PublishRecipeResponse =
        channel.unaryCall(publishRecipeMethod, request, idempotencyCallOptions(idempotencyKey))

    override suspend fun searchMealPlans(request: SearchMealPlansRequest): SearchMealPlansResponse =
        channel.unaryCall(searchMealPlansMethod, request)

    override suspend fun getMealPlan(request: GetMealPlanRequest): GetMealPlanResponse =
        channel.unaryCall(getMealPlanMethod, request)

    override suspend fun createMealPlan(request: CreateMealPlanRequest, idempotencyKey: String?): CreateMealPlanResponse =
        channel.unaryCall(createMealPlanMethod, request, idempotencyCallOptions(idempotencyKey))

    override suspend fun updateMealPlan(request: UpdateMealPlanRequest, idempotencyKey: String?): UpdateMealPlanResponse =
        channel.unaryCall(updateMealPlanMethod, request, idempotencyCallOptions(idempotencyKey))

    override suspend fun deleteMealPlan(request: DeleteMealPlanRequest, idempotencyKey: String?): DeleteMealPlanResponse =
        channel.unaryCall(deleteMealPlanMethod, request, idempotencyCallOptions(idempotencyKey))

    override suspend fun listCoachLinks(request: ListCoachLinksRequest): ListCoachLinksResponse =
        channel.unaryCall(listCoachLinksMethod, request)

    override suspend fun inviteStudent(request: InviteStudentRequest, idempotencyKey: String?): InviteStudentResponse =
        channel.unaryCall(inviteStudentMethod, request, idempotencyCallOptions(idempotencyKey))

    override suspend fun respondCoachInvite(
        request: RespondCoachInviteRequest,
        idempotencyKey: String?,
    ): RespondCoachInviteResponse =
        channel.unaryCall(respondCoachInviteMethod, request, idempotencyCallOptions(idempotencyKey))

    override suspend fun grantAccess(request: GrantAccessRequest, idempotencyKey: String?): GrantAccessResponse =
        channel.unaryCall(grantAccessMethod, request, idempotencyCallOptions(idempotencyKey))

    override suspend fun revokeAccess(request: RevokeAccessRequest, idempotencyKey: String?): RevokeAccessResponse =
        channel.unaryCall(revokeAccessMethod, request, idempotencyCallOptions(idempotencyKey))

    private fun idempotencyCallOptions(idempotencyKey: String?): CallOptions =
        if (!idempotencyKey.isNullOrBlank()) {
            CallOptions.DEFAULT.withOption(
                UnifiedGrpcClientInterceptor.IDEMPOTENCY_KEY_CALL_OPTION,
                idempotencyKey,
            )
        } else {
            CallOptions.DEFAULT
        }

    companion object {
        private const val SERVICE_NAME = "com.budgetntr.recipe.v1.RecipeAiService"

        val generateMealPlanMethod: MethodDescriptor<GenerateMealPlanRequest, GenerateMealPlanResponse> =
            unaryDescriptor("GenerateMealPlan", GenerateMealPlanRequestMarshaller, GenerateMealPlanResponseMarshaller)

        val generateWeekMenuMethod: MethodDescriptor<GenerateWeekMenuRequest, GenerateWeekMenuResponse> =
            MethodDescriptor.newBuilder<GenerateWeekMenuRequest, GenerateWeekMenuResponse>()
                .setType(MethodDescriptor.MethodType.SERVER_STREAMING)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "GenerateWeekMenu"))
                .setRequestMarshaller(GenerateWeekMenuRequestMarshaller)
                .setResponseMarshaller(GenerateWeekMenuResponseMarshaller)
                .build()

        val searchRecipesMethod: MethodDescriptor<SearchRecipesRequest, SearchRecipesResponse> =
            unaryDescriptor("SearchRecipes", SearchRecipesRequestMarshaller, SearchRecipesResponseMarshaller)

        val getRecipeMethod: MethodDescriptor<GetRecipeRequest, GetRecipeResponse> =
            unaryDescriptor("GetRecipe", GetRecipeRequestMarshaller, GetRecipeResponseMarshaller)

        val createRecipeMethod: MethodDescriptor<CreateRecipeRequest, CreateRecipeResponse> =
            unaryDescriptor("CreateRecipe", CreateRecipeRequestMarshaller, CreateRecipeResponseMarshaller)

        val updateRecipeMethod: MethodDescriptor<UpdateRecipeRequest, UpdateRecipeResponse> =
            unaryDescriptor("UpdateRecipe", UpdateRecipeRequestMarshaller, UpdateRecipeResponseMarshaller)

        val deleteRecipeMethod: MethodDescriptor<DeleteRecipeRequest, DeleteRecipeResponse> =
            unaryDescriptor("DeleteRecipe", DeleteRecipeRequestMarshaller, DeleteRecipeResponseMarshaller)

        val forkRecipeMethod: MethodDescriptor<ForkRecipeRequest, ForkRecipeResponse> =
            unaryDescriptor("ForkRecipe", ForkRecipeRequestMarshaller, ForkRecipeResponseMarshaller)

        val publishRecipeMethod: MethodDescriptor<PublishRecipeRequest, PublishRecipeResponse> =
            unaryDescriptor("PublishRecipe", PublishRecipeRequestMarshaller, PublishRecipeResponseMarshaller)

        val searchMealPlansMethod: MethodDescriptor<SearchMealPlansRequest, SearchMealPlansResponse> =
            unaryDescriptor("SearchMealPlans", SearchMealPlansRequestMarshaller, SearchMealPlansResponseMarshaller)

        val getMealPlanMethod: MethodDescriptor<GetMealPlanRequest, GetMealPlanResponse> =
            unaryDescriptor("GetMealPlan", GetMealPlanRequestMarshaller, GetMealPlanResponseMarshaller)

        val createMealPlanMethod: MethodDescriptor<CreateMealPlanRequest, CreateMealPlanResponse> =
            unaryDescriptor("CreateMealPlan", CreateMealPlanRequestMarshaller, CreateMealPlanResponseMarshaller)

        val updateMealPlanMethod: MethodDescriptor<UpdateMealPlanRequest, UpdateMealPlanResponse> =
            unaryDescriptor("UpdateMealPlan", UpdateMealPlanRequestMarshaller, UpdateMealPlanResponseMarshaller)

        val deleteMealPlanMethod: MethodDescriptor<DeleteMealPlanRequest, DeleteMealPlanResponse> =
            unaryDescriptor("DeleteMealPlan", DeleteMealPlanRequestMarshaller, DeleteMealPlanResponseMarshaller)

        val listCoachLinksMethod: MethodDescriptor<ListCoachLinksRequest, ListCoachLinksResponse> =
            unaryDescriptor("ListCoachLinks", ListCoachLinksRequestMarshaller, ListCoachLinksResponseMarshaller)

        val inviteStudentMethod: MethodDescriptor<InviteStudentRequest, InviteStudentResponse> =
            unaryDescriptor("InviteStudent", InviteStudentRequestMarshaller, InviteStudentResponseMarshaller)

        val respondCoachInviteMethod: MethodDescriptor<RespondCoachInviteRequest, RespondCoachInviteResponse> =
            unaryDescriptor("RespondCoachInvite", RespondCoachInviteRequestMarshaller, RespondCoachInviteResponseMarshaller)

        val grantAccessMethod: MethodDescriptor<GrantAccessRequest, GrantAccessResponse> =
            unaryDescriptor("GrantAccess", GrantAccessRequestMarshaller, GrantAccessResponseMarshaller)

        val revokeAccessMethod: MethodDescriptor<RevokeAccessRequest, RevokeAccessResponse> =
            unaryDescriptor("RevokeAccess", RevokeAccessRequestMarshaller, RevokeAccessResponseMarshaller)

        private fun <Req, Resp> unaryDescriptor(
            methodName: String,
            reqMarshaller: MethodDescriptor.Marshaller<Req>,
            respMarshaller: MethodDescriptor.Marshaller<Resp>,
        ): MethodDescriptor<Req, Resp> =
            MethodDescriptor.newBuilder<Req, Resp>()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, methodName))
                .setRequestMarshaller(reqMarshaller)
                .setResponseMarshaller(respMarshaller)
                .build()
    }
}

internal object GenerateMealPlanRequestMarshaller : MethodDescriptor.Marshaller<GenerateMealPlanRequest> {
    override fun stream(value: GenerateMealPlanRequest): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.period != TargetPeriodProto.TARGET_PERIOD_UNSPECIFIED) {
            cos.writeInt32(1, value.period.value)
        }
        // target_nutrition: 2
        val tnBaos = ByteArrayOutputStream()
        val tnCos = CodedOutputStream.newInstance(tnBaos)
        if (value.targetNutrition.calories != 0.0) tnCos.writeDouble(1, value.targetNutrition.calories)
        if (value.targetNutrition.proteinsGrams != 0.0) tnCos.writeDouble(2, value.targetNutrition.proteinsGrams)
        if (value.targetNutrition.fatsGrams != 0.0) tnCos.writeDouble(3, value.targetNutrition.fatsGrams)
        if (value.targetNutrition.carbsGrams != 0.0) tnCos.writeDouble(4, value.targetNutrition.carbsGrams)
        tnCos.flush()
        cos.writeByteArray(2, tnBaos.toByteArray())

        if (value.maxBudgetMinor != 0L) cos.writeInt64(3, value.maxBudgetMinor)
        if (value.currency.isNotEmpty()) cos.writeString(4, value.currency)
        if (value.regionId.isNotEmpty()) cos.writeString(5, value.regionId)
        for (p in value.preferredProducts) cos.writeString(6, p)
        for (e in value.excludedProducts) cos.writeString(7, e)
        if (value.userNote.isNotEmpty()) cos.writeString(8, value.userNote)

        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): GenerateMealPlanRequest {
        val cis = CodedInputStream.newInstance(stream)
        var period = TargetPeriodProto.TARGET_PERIOD_UNSPECIFIED
        var targetNutrition = TargetNutritionProto()
        var maxBudgetMinor = 0L
        var currency = "RUB"
        var regionId = ""
        val preferred = mutableListOf<String>()
        val excluded = mutableListOf<String>()
        var userNote = ""

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 0 -> period = TargetPeriodProto.fromValue(cis.readInt32())
                (2 shl 3) or 2 -> {
                    val b = cis.readByteArray()
                    val c = CodedInputStream.newInstance(b)
                    var cal = 0.0
                    var p = 0.0
                    var f = 0.0
                    var cb = 0.0
                    while (true) {
                        val t = c.readTag()
                        if (t == 0) break
                        when (t) {
                            (1 shl 3) or 1 -> cal = c.readDouble()
                            (2 shl 3) or 1 -> p = c.readDouble()
                            (3 shl 3) or 1 -> f = c.readDouble()
                            (4 shl 3) or 1 -> cb = c.readDouble()
                            else -> c.skipField(t)
                        }
                    }
                    targetNutrition = TargetNutritionProto(cal, p, f, cb)
                }
                (3 shl 3) or 0 -> maxBudgetMinor = cis.readInt64()
                (4 shl 3) or 2 -> currency = cis.readString()
                (5 shl 3) or 2 -> regionId = cis.readString()
                (6 shl 3) or 2 -> preferred.add(cis.readString())
                (7 shl 3) or 2 -> excluded.add(cis.readString())
                (8 shl 3) or 2 -> userNote = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return GenerateMealPlanRequest(
            period = period,
            targetNutrition = targetNutrition,
            maxBudgetMinor = maxBudgetMinor,
            currency = currency,
            regionId = regionId,
            preferredProducts = preferred,
            excludedProducts = excluded,
            userNote = userNote,
        )
    }
}

internal object GenerateMealPlanResponseMarshaller : MethodDescriptor.Marshaller<GenerateMealPlanResponse> {
    override fun stream(value: GenerateMealPlanResponse): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.planId.isNotEmpty()) cos.writeString(1, value.planId)
        if (value.period != TargetPeriodProto.TARGET_PERIOD_UNSPECIFIED) {
            cos.writeInt32(2, value.period.value)
        }
        for (meal in value.meals) {
            cos.writeByteArray(3, RecipeItemMarshallerHelper.mealToByteArray(meal))
        }
        // total_nutrition: 4
        val tnBaos = ByteArrayOutputStream()
        val tnCos = CodedOutputStream.newInstance(tnBaos)
        if (value.totalNutrition.calories != 0.0) tnCos.writeDouble(1, value.totalNutrition.calories)
        if (value.totalNutrition.proteinsGrams != 0.0) tnCos.writeDouble(2, value.totalNutrition.proteinsGrams)
        if (value.totalNutrition.fatsGrams != 0.0) tnCos.writeDouble(3, value.totalNutrition.fatsGrams)
        if (value.totalNutrition.carbsGrams != 0.0) tnCos.writeDouble(4, value.totalNutrition.carbsGrams)
        tnCos.flush()
        cos.writeByteArray(4, tnBaos.toByteArray())

        if (value.estimatedCostMinor != 0L) cos.writeInt64(5, value.estimatedCostMinor)
        if (value.currency.isNotEmpty()) cos.writeString(6, value.currency)
        if (value.cookingInstructions.isNotEmpty()) cos.writeString(7, value.cookingInstructions)
        if (value.source != MenuSourceProto.MENU_SOURCE_UNSPECIFIED) {
            cos.writeInt32(8, value.source.value)
        }

        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): GenerateMealPlanResponse {
        val cis = CodedInputStream.newInstance(stream)
        var planId = ""
        var period = TargetPeriodProto.TARGET_PERIOD_UNSPECIFIED
        val meals = mutableListOf<GeneratedMealProto>()
        var totalNutrition = TargetNutritionProto()
        var estimatedCostMinor = 0L
        var currency = "RUB"
        var cookingInstructions = ""
        var source = MenuSourceProto.MENU_SOURCE_GENERATED

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> planId = cis.readString()
                (2 shl 3) or 0 -> period = TargetPeriodProto.fromValue(cis.readInt32())
                (3 shl 3) or 2 -> meals.add(RecipeItemMarshallerHelper.mealFromByteArray(cis.readByteArray()))
                (4 shl 3) or 2 -> {
                    val b = cis.readByteArray()
                    val c = CodedInputStream.newInstance(b)
                    var cal = 0.0
                    var p = 0.0
                    var f = 0.0
                    var cb = 0.0
                    while (true) {
                        val t = c.readTag()
                        if (t == 0) break
                        when (t) {
                            (1 shl 3) or 1 -> cal = c.readDouble()
                            (2 shl 3) or 1 -> p = c.readDouble()
                            (3 shl 3) or 1 -> f = c.readDouble()
                            (4 shl 3) or 1 -> cb = c.readDouble()
                            else -> c.skipField(t)
                        }
                    }
                    totalNutrition = TargetNutritionProto(cal, p, f, cb)
                }
                (5 shl 3) or 0 -> estimatedCostMinor = cis.readInt64()
                (6 shl 3) or 2 -> currency = cis.readString()
                (7 shl 3) or 2 -> cookingInstructions = cis.readString()
                (8 shl 3) or 0 -> source = MenuSourceProto.fromValue(cis.readInt32())
                else -> cis.skipField(tag)
            }
        }
        return GenerateMealPlanResponse(
            planId = planId,
            period = period,
            meals = meals,
            totalNutrition = totalNutrition,
            estimatedCostMinor = estimatedCostMinor,
            currency = currency,
            cookingInstructions = cookingInstructions,
            source = source,
        )
    }
}

internal object GenerateWeekMenuRequestMarshaller : MethodDescriptor.Marshaller<GenerateWeekMenuRequest> {
    override fun stream(value: GenerateWeekMenuRequest): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.dailyCalories != 0) cos.writeInt32(1, value.dailyCalories)
        if (value.weeklyBudgetMinor != 0L) cos.writeInt64(2, value.weeklyBudgetMinor)
        for (e in value.excludedProductIds) cos.writeString(3, e)
        for (p in value.preferredProductIds) cos.writeString(4, p)
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): GenerateWeekMenuRequest {
        val cis = CodedInputStream.newInstance(stream)
        var dailyCalories = 0
        var weeklyBudgetMinor = 0L
        val excluded = mutableListOf<String>()
        val preferred = mutableListOf<String>()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 0 -> dailyCalories = cis.readInt32()
                (2 shl 3) or 0 -> weeklyBudgetMinor = cis.readInt64()
                (3 shl 3) or 2 -> excluded.add(cis.readString())
                (4 shl 3) or 2 -> preferred.add(cis.readString())
                else -> cis.skipField(tag)
            }
        }
        return GenerateWeekMenuRequest(
            dailyCalories = dailyCalories,
            weeklyBudgetMinor = weeklyBudgetMinor,
            excludedProductIds = excluded,
            preferredProductIds = preferred,
        )
    }
}

internal object GenerateWeekMenuResponseMarshaller : MethodDescriptor.Marshaller<GenerateWeekMenuResponse> {
    override fun stream(value: GenerateWeekMenuResponse): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        value.dayReady?.let { day ->
            val dayBaos = ByteArrayOutputStream()
            val dayCos = CodedOutputStream.newInstance(dayBaos)
            if (day.dayNumber != 0) dayCos.writeInt32(1, day.dayNumber)
            for (m in day.meals) {
                val mBaos = ByteArrayOutputStream()
                val mCos = CodedOutputStream.newInstance(mBaos)
                if (m.type != MealTypeProto.MEAL_TYPE_UNSPECIFIED) mCos.writeInt32(1, m.type.value)
                if (m.name.isNotEmpty()) mCos.writeString(2, m.name)
                for (ing in m.ingredients) {
                    val ingBaos = ByteArrayOutputStream()
                    val ingCos = CodedOutputStream.newInstance(ingBaos)
                    if (ing.productId.isNotEmpty()) ingCos.writeString(1, ing.productId)
                    if (ing.name.isNotEmpty()) ingCos.writeString(2, ing.name)
                    if (ing.grams != 0) ingCos.writeInt32(3, ing.grams)
                    ingCos.flush()
                    mCos.writeByteArray(3, ingBaos.toByteArray())
                }
                if (m.calories != 0.0) mCos.writeDouble(4, m.calories)
                if (m.estimatedCostMinor != 0L) mCos.writeInt64(5, m.estimatedCostMinor)
                mCos.flush()
                dayCos.writeByteArray(2, mBaos.toByteArray())
            }
            if (day.calories != 0.0) dayCos.writeDouble(3, day.calories)
            if (day.estimatedCostMinor != 0L) dayCos.writeInt64(4, day.estimatedCostMinor)
            dayCos.flush()
            cos.writeByteArray(1, dayBaos.toByteArray())
        }
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): GenerateWeekMenuResponse {
        val cis = CodedInputStream.newInstance(stream)
        var dayReady: WeekMenuDayProto? = null
        val summary: WeekMenuSummaryProto? = null

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> {
                    val b = cis.readByteArray()
                    val c = CodedInputStream.newInstance(b)
                    var dayNum = 0
                    val meals = mutableListOf<MenuMealProto>()
                    var cal = 0.0
                    var cost = 0L
                    while (true) {
                        val t = c.readTag()
                        if (t == 0) break
                        when (t) {
                            (1 shl 3) or 0 -> dayNum = c.readInt32()
                            (2 shl 3) or 2 -> {
                                val mb = c.readByteArray()
                                val mc = CodedInputStream.newInstance(mb)
                                var mType = MealTypeProto.MEAL_TYPE_UNSPECIFIED
                                var name = ""
                                val ings = mutableListOf<MenuIngredientProto>()
                                var mCal = 0.0
                                var mCost = 0L
                                while (true) {
                                    val mt = mc.readTag()
                                    if (mt == 0) break
                                    when (mt) {
                                        (1 shl 3) or 0 -> mType = MealTypeProto.fromValue(mc.readInt32())
                                        (2 shl 3) or 2 -> name = mc.readString()
                                        (3 shl 3) or 2 -> {
                                            val ib = mc.readByteArray()
                                            val ic = CodedInputStream.newInstance(ib)
                                            var pId = ""
                                            var iName = ""
                                            var grams = 0
                                            while (true) {
                                                val it = ic.readTag()
                                                if (it == 0) break
                                                when (it) {
                                                    (1 shl 3) or 2 -> pId = ic.readString()
                                                    (2 shl 3) or 2 -> iName = ic.readString()
                                                    (3 shl 3) or 0 -> grams = ic.readInt32()
                                                    else -> ic.skipField(it)
                                                }
                                            }
                                            ings.add(MenuIngredientProto(pId, iName, grams))
                                        }
                                        (4 shl 3) or 1 -> mCal = mc.readDouble()
                                        (5 shl 3) or 0 -> mCost = mc.readInt64()
                                        else -> mc.skipField(mt)
                                    }
                                }
                                meals.add(MenuMealProto(mType, name, ings, mCal, mCost))
                            }
                            (3 shl 3) or 1 -> cal = c.readDouble()
                            (4 shl 3) or 0 -> cost = c.readInt64()
                            else -> c.skipField(t)
                        }
                    }
                    dayReady = WeekMenuDayProto(dayNum, meals, cal, cost)
                }
                else -> cis.skipField(tag)
            }
        }
        return GenerateWeekMenuResponse(dayReady = dayReady, finalSummary = summary)
    }
}

internal object RecipeItemMarshallerHelper {
    fun mealToByteArray(meal: GeneratedMealProto): ByteArray {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (meal.type != MealTypeProto.MEAL_TYPE_UNSPECIFIED) cos.writeInt32(1, meal.type.value)
        if (meal.title.isNotEmpty()) cos.writeString(2, meal.title)
        for (ing in meal.ingredients) {
            val ingBaos = ByteArrayOutputStream()
            val ingCos = CodedOutputStream.newInstance(ingBaos)
            if (ing.productId.isNotEmpty()) ingCos.writeString(1, ing.productId)
            if (ing.name.isNotEmpty()) ingCos.writeString(2, ing.name)
            if (ing.grams != 0) ingCos.writeInt32(3, ing.grams)
            ingCos.flush()
            cos.writeByteArray(3, ingBaos.toByteArray())
        }
        // nutrition: 4
        val nBaos = ByteArrayOutputStream()
        val nCos = CodedOutputStream.newInstance(nBaos)
        if (meal.nutrition.calories != 0.0) nCos.writeDouble(1, meal.nutrition.calories)
        if (meal.nutrition.proteinsGrams != 0.0) nCos.writeDouble(2, meal.nutrition.proteinsGrams)
        if (meal.nutrition.fatsGrams != 0.0) nCos.writeDouble(3, meal.nutrition.fatsGrams)
        if (meal.nutrition.carbsGrams != 0.0) nCos.writeDouble(4, meal.nutrition.carbsGrams)
        nCos.flush()
        cos.writeByteArray(4, nBaos.toByteArray())

        if (meal.estimatedCostMinor != 0L) cos.writeInt64(5, meal.estimatedCostMinor)
        if (meal.cookingTimeMinutes != 0) cos.writeInt32(6, meal.cookingTimeMinutes)
        cos.flush()
        return baos.toByteArray()
    }

    fun mealFromByteArray(bytes: ByteArray): GeneratedMealProto {
        val cis = CodedInputStream.newInstance(bytes)
        var type = MealTypeProto.MEAL_TYPE_UNSPECIFIED
        var title = ""
        val ingredients = mutableListOf<MenuIngredientProto>()
        var nutrition = TargetNutritionProto()
        var estimatedCostMinor = 0L
        var cookingTimeMinutes = 0

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 0 -> type = MealTypeProto.fromValue(cis.readInt32())
                (2 shl 3) or 2 -> title = cis.readString()
                (3 shl 3) or 2 -> {
                    val ib = cis.readByteArray()
                    val ic = CodedInputStream.newInstance(ib)
                    var pId = ""
                    var name = ""
                    var grams = 0
                    while (true) {
                        val it = ic.readTag()
                        if (it == 0) break
                        when (it) {
                            (1 shl 3) or 2 -> pId = ic.readString()
                            (2 shl 3) or 2 -> name = ic.readString()
                            (3 shl 3) or 0 -> grams = ic.readInt32()
                            else -> ic.skipField(it)
                        }
                    }
                    ingredients.add(MenuIngredientProto(pId, name, grams))
                }
                (4 shl 3) or 2 -> {
                    val nb = cis.readByteArray()
                    val nc = CodedInputStream.newInstance(nb)
                    var cal = 0.0
                    var p = 0.0
                    var f = 0.0
                    var cb = 0.0
                    while (true) {
                        val nt = nc.readTag()
                        if (nt == 0) break
                        when (nt) {
                            (1 shl 3) or 1 -> cal = nc.readDouble()
                            (2 shl 3) or 1 -> p = nc.readDouble()
                            (3 shl 3) or 1 -> f = nc.readDouble()
                            (4 shl 3) or 1 -> cb = nc.readDouble()
                            else -> nc.skipField(nt)
                        }
                    }
                    nutrition = TargetNutritionProto(cal, p, f, cb)
                }
                (5 shl 3) or 0 -> estimatedCostMinor = cis.readInt64()
                (6 shl 3) or 0 -> cookingTimeMinutes = cis.readInt32()
                else -> cis.skipField(tag)
            }
        }
        return GeneratedMealProto(
            type = type,
            title = title,
            ingredients = ingredients,
            nutrition = nutrition,
            estimatedCostMinor = estimatedCostMinor,
            cookingTimeMinutes = cookingTimeMinutes,
        )
    }
}
