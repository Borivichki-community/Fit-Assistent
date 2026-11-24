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
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import io.grpc.MethodDescriptor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal object RecipeCatalogWireCodec {

    inline fun <T> marshall(crossinline write: (CodedOutputStream) -> Unit): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        write(cos)
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    inline fun <T> toBytes(crossinline write: (CodedOutputStream) -> Unit): ByteArray {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        write(cos)
        cos.flush()
        return baos.toByteArray()
    }

    fun writeTimestamp(cos: CodedOutputStream, fieldNumber: Int, ts: TimestampProto?) {
        if (ts == null) return
        val bytes = toBytes<TimestampProto> { c ->
            if (ts.seconds != 0L) c.writeInt64(1, ts.seconds)
            if (ts.nanos != 0) c.writeInt32(2, ts.nanos)
        }
        cos.writeByteArray(fieldNumber, bytes)
    }

    fun readTimestamp(bytes: ByteArray): TimestampProto {
        val cis = CodedInputStream.newInstance(bytes)
        var seconds = 0L
        var nanos = 0
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 0 -> seconds = cis.readInt64()
                (2 shl 3) or 0 -> nanos = cis.readInt32()
                else -> cis.skipField(tag)
            }
        }
        return TimestampProto(seconds = seconds, nanos = nanos)
    }

    fun ingredientToBytes(value: RecipeIngredientDtoProto): ByteArray = toBytes<RecipeIngredientDtoProto> { cos ->
        if (value.id.isNotEmpty()) cos.writeString(1, value.id)
        if (value.recipeId.isNotEmpty()) cos.writeString(2, value.recipeId)
        if (value.productId.isNotEmpty()) cos.writeString(3, value.productId)
        if (value.productNameSnapshot.isNotEmpty()) cos.writeString(4, value.productNameSnapshot)
        if (value.weightGrams != 0.0) cos.writeDouble(5, value.weightGrams)
        if (value.caloriesPer100g != 0.0) cos.writeDouble(6, value.caloriesPer100g)
        if (value.proteinsPer100g != 0.0) cos.writeDouble(7, value.proteinsPer100g)
        if (value.fatsPer100g != 0.0) cos.writeDouble(8, value.fatsPer100g)
        if (value.carbsPer100g != 0.0) cos.writeDouble(9, value.carbsPer100g)
    }

    fun ingredientFromBytes(bytes: ByteArray): RecipeIngredientDtoProto {
        val cis = CodedInputStream.newInstance(bytes)
        var id = ""
        var recipeId = ""
        var productId = ""
        var productNameSnapshot = ""
        var weightGrams = 0.0
        var caloriesPer100g = 0.0
        var proteinsPer100g = 0.0
        var fatsPer100g = 0.0
        var carbsPer100g = 0.0

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> id = cis.readString()
                (2 shl 3) or 2 -> recipeId = cis.readString()
                (3 shl 3) or 2 -> productId = cis.readString()
                (4 shl 3) or 2 -> productNameSnapshot = cis.readString()
                (5 shl 3) or 1 -> weightGrams = cis.readDouble()
                (6 shl 3) or 1 -> caloriesPer100g = cis.readDouble()
                (7 shl 3) or 1 -> proteinsPer100g = cis.readDouble()
                (8 shl 3) or 1 -> fatsPer100g = cis.readDouble()
                (9 shl 3) or 1 -> carbsPer100g = cis.readDouble()
                else -> cis.skipField(tag)
            }
        }
        return RecipeIngredientDtoProto(
            id = id,
            recipeId = recipeId,
            productId = productId,
            productNameSnapshot = productNameSnapshot,
            weightGrams = weightGrams,
            caloriesPer100g = caloriesPer100g,
            proteinsPer100g = proteinsPer100g,
            fatsPer100g = fatsPer100g,
            carbsPer100g = carbsPer100g,
        )
    }

    fun recipeToBytes(value: RecipeCatalogDtoProto): ByteArray = toBytes<RecipeCatalogDtoProto> { cos ->
        if (value.id.isNotEmpty()) cos.writeString(1, value.id)
        if (value.authorType != AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED) cos.writeInt32(2, value.authorType.value)
        if (value.authorId.isNotEmpty()) cos.writeString(3, value.authorId)
        if (value.originRecipeId.isNotEmpty()) cos.writeString(4, value.originRecipeId)
        if (value.title.isNotEmpty()) cos.writeString(5, value.title)
        if (value.description.isNotEmpty()) cos.writeString(6, value.description)
        if (value.instructions.isNotEmpty()) cos.writeString(7, value.instructions)
        if (value.visibility != ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED) {
            cos.writeInt32(8, value.visibility.value)
        }
        if (value.servingsCount != 0) cos.writeInt32(9, value.servingsCount)
        if (value.totalWeightG != 0.0) cos.writeDouble(10, value.totalWeightG)
        if (value.caloriesPer100g != 0.0) cos.writeDouble(11, value.caloriesPer100g)
        if (value.proteinsPer100g != 0.0) cos.writeDouble(12, value.proteinsPer100g)
        if (value.fatsPer100g != 0.0) cos.writeDouble(13, value.fatsPer100g)
        if (value.carbsPer100g != 0.0) cos.writeDouble(14, value.carbsPer100g)
        if (value.version != 0L) cos.writeInt64(15, value.version)
        writeTimestamp(cos, 16, value.createdAt)
        writeTimestamp(cos, 17, value.updatedAt)
        for (ing in value.ingredients) {
            cos.writeByteArray(18, ingredientToBytes(ing))
        }
    }

    fun recipeFromBytes(bytes: ByteArray): RecipeCatalogDtoProto {
        val cis = CodedInputStream.newInstance(bytes)
        var id = ""
        var authorType = AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED
        var authorId = ""
        var originRecipeId = ""
        var title = ""
        var description = ""
        var instructions = ""
        var visibility = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED
        var servingsCount = 0
        var totalWeightG = 0.0
        var caloriesPer100g = 0.0
        var proteinsPer100g = 0.0
        var fatsPer100g = 0.0
        var carbsPer100g = 0.0
        var version = 0L
        var createdAt: TimestampProto? = null
        var updatedAt: TimestampProto? = null
        val ingredients = mutableListOf<RecipeIngredientDtoProto>()

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> id = cis.readString()
                (2 shl 3) or 0 -> authorType = AuthorTypeProto.fromValue(cis.readInt32())
                (3 shl 3) or 2 -> authorId = cis.readString()
                (4 shl 3) or 2 -> originRecipeId = cis.readString()
                (5 shl 3) or 2 -> title = cis.readString()
                (6 shl 3) or 2 -> description = cis.readString()
                (7 shl 3) or 2 -> instructions = cis.readString()
                (8 shl 3) or 0 -> visibility = ContentVisibilityProto.fromValue(cis.readInt32())
                (9 shl 3) or 0 -> servingsCount = cis.readInt32()
                (10 shl 3) or 1 -> totalWeightG = cis.readDouble()
                (11 shl 3) or 1 -> caloriesPer100g = cis.readDouble()
                (12 shl 3) or 1 -> proteinsPer100g = cis.readDouble()
                (13 shl 3) or 1 -> fatsPer100g = cis.readDouble()
                (14 shl 3) or 1 -> carbsPer100g = cis.readDouble()
                (15 shl 3) or 0 -> version = cis.readInt64()
                (16 shl 3) or 2 -> createdAt = readTimestamp(cis.readByteArray())
                (17 shl 3) or 2 -> updatedAt = readTimestamp(cis.readByteArray())
                (18 shl 3) or 2 -> ingredients.add(ingredientFromBytes(cis.readByteArray()))
                else -> cis.skipField(tag)
            }
        }
        return RecipeCatalogDtoProto(
            id = id,
            authorType = authorType,
            authorId = authorId,
            originRecipeId = originRecipeId,
            title = title,
            description = description,
            instructions = instructions,
            visibility = visibility,
            servingsCount = servingsCount,
            totalWeightG = totalWeightG,
            caloriesPer100g = caloriesPer100g,
            proteinsPer100g = proteinsPer100g,
            fatsPer100g = fatsPer100g,
            carbsPer100g = carbsPer100g,
            version = version,
            createdAt = createdAt,
            updatedAt = updatedAt,
            ingredients = ingredients,
        )
    }

    fun mealPlanItemToBytes(value: MealPlanItemDtoProto): ByteArray = toBytes<MealPlanItemDtoProto> { cos ->
        if (value.id.isNotEmpty()) cos.writeString(1, value.id)
        if (value.planId.isNotEmpty()) cos.writeString(2, value.planId)
        if (value.dayNumber != 0) cos.writeInt32(3, value.dayNumber)
        if (value.mealSlot != MealTypeProto.MEAL_TYPE_UNSPECIFIED) cos.writeInt32(4, value.mealSlot.value)
        if (value.itemType != PlanItemTypeProto.PLAN_ITEM_TYPE_UNSPECIFIED) cos.writeInt32(5, value.itemType.value)
        if (value.itemId.isNotEmpty()) cos.writeString(6, value.itemId)
        if (value.nameSnapshot.isNotEmpty()) cos.writeString(7, value.nameSnapshot)
        if (value.weightGrams != 0.0) cos.writeDouble(8, value.weightGrams)
        if (value.calories != 0.0) cos.writeDouble(9, value.calories)
        if (value.proteins != 0.0) cos.writeDouble(10, value.proteins)
        if (value.fats != 0.0) cos.writeDouble(11, value.fats)
        if (value.carbs != 0.0) cos.writeDouble(12, value.carbs)
    }

    fun mealPlanItemFromBytes(bytes: ByteArray): MealPlanItemDtoProto {
        val cis = CodedInputStream.newInstance(bytes)
        var id = ""
        var planId = ""
        var dayNumber = 0
        var mealSlot = MealTypeProto.MEAL_TYPE_UNSPECIFIED
        var itemType = PlanItemTypeProto.PLAN_ITEM_TYPE_UNSPECIFIED
        var itemId = ""
        var nameSnapshot = ""
        var weightGrams = 0.0
        var calories = 0.0
        var proteins = 0.0
        var fats = 0.0
        var carbs = 0.0

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> id = cis.readString()
                (2 shl 3) or 2 -> planId = cis.readString()
                (3 shl 3) or 0 -> dayNumber = cis.readInt32()
                (4 shl 3) or 0 -> mealSlot = MealTypeProto.fromValue(cis.readInt32())
                (5 shl 3) or 0 -> itemType = PlanItemTypeProto.fromValue(cis.readInt32())
                (6 shl 3) or 2 -> itemId = cis.readString()
                (7 shl 3) or 2 -> nameSnapshot = cis.readString()
                (8 shl 3) or 1 -> weightGrams = cis.readDouble()
                (9 shl 3) or 1 -> calories = cis.readDouble()
                (10 shl 3) or 1 -> proteins = cis.readDouble()
                (11 shl 3) or 1 -> fats = cis.readDouble()
                (12 shl 3) or 1 -> carbs = cis.readDouble()
                else -> cis.skipField(tag)
            }
        }
        return MealPlanItemDtoProto(
            id = id,
            planId = planId,
            dayNumber = dayNumber,
            mealSlot = mealSlot,
            itemType = itemType,
            itemId = itemId,
            nameSnapshot = nameSnapshot,
            weightGrams = weightGrams,
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
        )
    }

    fun mealPlanTemplateToBytes(value: MealPlanTemplateDtoProto): ByteArray = toBytes<MealPlanTemplateDtoProto> { cos ->
        if (value.id.isNotEmpty()) cos.writeString(1, value.id)
        if (value.authorId.isNotEmpty()) cos.writeString(2, value.authorId)
        if (value.authorType != AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED) cos.writeInt32(3, value.authorType.value)
        if (value.title.isNotEmpty()) cos.writeString(4, value.title)
        if (value.description.isNotEmpty()) cos.writeString(5, value.description)
        if (value.durationDays != 0) cos.writeInt32(6, value.durationDays)
        if (value.visibility != ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED) {
            cos.writeInt32(7, value.visibility.value)
        }
        if (value.accessMode != AccessModeProto.ACCESS_MODE_UNSPECIFIED) cos.writeInt32(8, value.accessMode.value)
        if (value.priceAmountMinor != 0L) cos.writeInt64(9, value.priceAmountMinor)
        if (value.currency.isNotEmpty()) cos.writeString(10, value.currency)
        if (value.avgDailyCalories != 0.0) cos.writeDouble(11, value.avgDailyCalories)
        if (value.avgDailyProteins != 0.0) cos.writeDouble(12, value.avgDailyProteins)
        if (value.avgDailyFats != 0.0) cos.writeDouble(13, value.avgDailyFats)
        if (value.avgDailyCarbs != 0.0) cos.writeDouble(14, value.avgDailyCarbs)
        if (value.version != 0L) cos.writeInt64(15, value.version)
        writeTimestamp(cos, 16, value.createdAt)
        writeTimestamp(cos, 17, value.updatedAt)
        for (item in value.items) {
            cos.writeByteArray(18, mealPlanItemToBytes(item))
        }
    }

    fun mealPlanTemplateFromBytes(bytes: ByteArray): MealPlanTemplateDtoProto {
        val cis = CodedInputStream.newInstance(bytes)
        var id = ""
        var authorId = ""
        var authorType = AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED
        var title = ""
        var description = ""
        var durationDays = 0
        var visibility = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED
        var accessMode = AccessModeProto.ACCESS_MODE_UNSPECIFIED
        var priceAmountMinor = 0L
        var currency = "RUB"
        var avgDailyCalories = 0.0
        var avgDailyProteins = 0.0
        var avgDailyFats = 0.0
        var avgDailyCarbs = 0.0
        var version = 0L
        var createdAt: TimestampProto? = null
        var updatedAt: TimestampProto? = null
        val items = mutableListOf<MealPlanItemDtoProto>()

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> id = cis.readString()
                (2 shl 3) or 2 -> authorId = cis.readString()
                (3 shl 3) or 0 -> authorType = AuthorTypeProto.fromValue(cis.readInt32())
                (4 shl 3) or 2 -> title = cis.readString()
                (5 shl 3) or 2 -> description = cis.readString()
                (6 shl 3) or 0 -> durationDays = cis.readInt32()
                (7 shl 3) or 0 -> visibility = ContentVisibilityProto.fromValue(cis.readInt32())
                (8 shl 3) or 0 -> accessMode = AccessModeProto.fromValue(cis.readInt32())
                (9 shl 3) or 0 -> priceAmountMinor = cis.readInt64()
                (10 shl 3) or 2 -> currency = cis.readString()
                (11 shl 3) or 1 -> avgDailyCalories = cis.readDouble()
                (12 shl 3) or 1 -> avgDailyProteins = cis.readDouble()
                (13 shl 3) or 1 -> avgDailyFats = cis.readDouble()
                (14 shl 3) or 1 -> avgDailyCarbs = cis.readDouble()
                (15 shl 3) or 0 -> version = cis.readInt64()
                (16 shl 3) or 2 -> createdAt = readTimestamp(cis.readByteArray())
                (17 shl 3) or 2 -> updatedAt = readTimestamp(cis.readByteArray())
                (18 shl 3) or 2 -> items.add(mealPlanItemFromBytes(cis.readByteArray()))
                else -> cis.skipField(tag)
            }
        }
        return MealPlanTemplateDtoProto(
            id = id,
            authorId = authorId,
            authorType = authorType,
            title = title,
            description = description,
            durationDays = durationDays,
            visibility = visibility,
            accessMode = accessMode,
            priceAmountMinor = priceAmountMinor,
            currency = currency,
            avgDailyCalories = avgDailyCalories,
            avgDailyProteins = avgDailyProteins,
            avgDailyFats = avgDailyFats,
            avgDailyCarbs = avgDailyCarbs,
            version = version,
            createdAt = createdAt,
            updatedAt = updatedAt,
            items = items,
        )
    }

    fun coachLinkToBytes(value: CoachStudentLinkDtoProto): ByteArray = toBytes<CoachStudentLinkDtoProto> { cos ->
        if (value.id.isNotEmpty()) cos.writeString(1, value.id)
        if (value.coachId.isNotEmpty()) cos.writeString(2, value.coachId)
        if (value.studentId.isNotEmpty()) cos.writeString(3, value.studentId)
        if (value.status != CoachLinkStatusProto.COACH_LINK_STATUS_UNSPECIFIED) cos.writeInt32(4, value.status.value)
        if (value.canViewDiary) cos.writeBool(5, value.canViewDiary)
        writeTimestamp(cos, 6, value.createdAt)
        writeTimestamp(cos, 7, value.updatedAt)
    }

    fun coachLinkFromBytes(bytes: ByteArray): CoachStudentLinkDtoProto {
        val cis = CodedInputStream.newInstance(bytes)
        var id = ""
        var coachId = ""
        var studentId = ""
        var status = CoachLinkStatusProto.COACH_LINK_STATUS_UNSPECIFIED
        var canViewDiary = false
        var createdAt: TimestampProto? = null
        var updatedAt: TimestampProto? = null

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> id = cis.readString()
                (2 shl 3) or 2 -> coachId = cis.readString()
                (3 shl 3) or 2 -> studentId = cis.readString()
                (4 shl 3) or 0 -> status = CoachLinkStatusProto.fromValue(cis.readInt32())
                (5 shl 3) or 0 -> canViewDiary = cis.readBool()
                (6 shl 3) or 2 -> createdAt = readTimestamp(cis.readByteArray())
                (7 shl 3) or 2 -> updatedAt = readTimestamp(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return CoachStudentLinkDtoProto(
            id = id,
            coachId = coachId,
            studentId = studentId,
            status = status,
            canViewDiary = canViewDiary,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    fun accessGrantToBytes(value: AccessGrantDtoProto): ByteArray = toBytes<AccessGrantDtoProto> { cos ->
        if (value.id.isNotEmpty()) cos.writeString(1, value.id)
        if (value.userId.isNotEmpty()) cos.writeString(2, value.userId)
        if (value.targetType.isNotEmpty()) cos.writeString(3, value.targetType)
        if (value.targetId.isNotEmpty()) cos.writeString(4, value.targetId)
        if (value.grantSource != GrantSourceProto.GRANT_SOURCE_UNSPECIFIED) cos.writeInt32(5, value.grantSource.value)
        if (value.grantedBy.isNotEmpty()) cos.writeString(6, value.grantedBy)
        writeTimestamp(cos, 7, value.expiresAt)
        writeTimestamp(cos, 8, value.createdAt)
    }

    fun accessGrantFromBytes(bytes: ByteArray): AccessGrantDtoProto {
        val cis = CodedInputStream.newInstance(bytes)
        var id = ""
        var userId = ""
        var targetType = ""
        var targetId = ""
        var grantSource = GrantSourceProto.GRANT_SOURCE_UNSPECIFIED
        var grantedBy = ""
        var expiresAt: TimestampProto? = null
        var createdAt: TimestampProto? = null

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> id = cis.readString()
                (2 shl 3) or 2 -> userId = cis.readString()
                (3 shl 3) or 2 -> targetType = cis.readString()
                (4 shl 3) or 2 -> targetId = cis.readString()
                (5 shl 3) or 0 -> grantSource = GrantSourceProto.fromValue(cis.readInt32())
                (6 shl 3) or 2 -> grantedBy = cis.readString()
                (7 shl 3) or 2 -> expiresAt = readTimestamp(cis.readByteArray())
                (8 shl 3) or 2 -> createdAt = readTimestamp(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return AccessGrantDtoProto(
            id = id,
            userId = userId,
            targetType = targetType,
            targetId = targetId,
            grantSource = grantSource,
            grantedBy = grantedBy,
            expiresAt = expiresAt,
            createdAt = createdAt,
        )
    }
}

internal object SearchRecipesRequestMarshaller : MethodDescriptor.Marshaller<SearchRecipesRequest> {
    override fun stream(value: SearchRecipesRequest): InputStream = RecipeCatalogWireCodec.marshall<SearchRecipesRequest> { cos ->
        if (value.query.isNotEmpty()) cos.writeString(1, value.query)
        if (value.authorType != AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED) cos.writeInt32(2, value.authorType.value)
        if (value.visibility != ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED) {
            cos.writeInt32(3, value.visibility.value)
        }
        if (value.pageSize != 0) cos.writeInt32(4, value.pageSize)
        if (value.pageToken.isNotEmpty()) cos.writeString(5, value.pageToken)
    }

    override fun parse(stream: InputStream): SearchRecipesRequest {
        val cis = CodedInputStream.newInstance(stream)
        var query = ""
        var authorType = AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED
        var visibility = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED
        var pageSize = 20
        var pageToken = ""
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> query = cis.readString()
                (2 shl 3) or 0 -> authorType = AuthorTypeProto.fromValue(cis.readInt32())
                (3 shl 3) or 0 -> visibility = ContentVisibilityProto.fromValue(cis.readInt32())
                (4 shl 3) or 0 -> pageSize = cis.readInt32()
                (5 shl 3) or 2 -> pageToken = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return SearchRecipesRequest(query, authorType, visibility, pageSize, pageToken)
    }
}

internal object SearchRecipesResponseMarshaller : MethodDescriptor.Marshaller<SearchRecipesResponse> {
    override fun stream(value: SearchRecipesResponse): InputStream = RecipeCatalogWireCodec.marshall<SearchRecipesResponse> { cos ->
        for (r in value.recipes) cos.writeByteArray(1, RecipeCatalogWireCodec.recipeToBytes(r))
        if (value.nextPageToken.isNotEmpty()) cos.writeString(2, value.nextPageToken)
        if (value.totalCount != 0L) cos.writeInt64(3, value.totalCount)
    }

    override fun parse(stream: InputStream): SearchRecipesResponse {
        val cis = CodedInputStream.newInstance(stream)
        val recipes = mutableListOf<RecipeCatalogDtoProto>()
        var nextPageToken = ""
        var totalCount = 0L
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> recipes.add(RecipeCatalogWireCodec.recipeFromBytes(cis.readByteArray()))
                (2 shl 3) or 2 -> nextPageToken = cis.readString()
                (3 shl 3) or 0 -> totalCount = cis.readInt64()
                else -> cis.skipField(tag)
            }
        }
        return SearchRecipesResponse(recipes, nextPageToken, totalCount)
    }
}

internal object GetRecipeRequestMarshaller : MethodDescriptor.Marshaller<GetRecipeRequest> {
    override fun stream(value: GetRecipeRequest): InputStream = RecipeCatalogWireCodec.marshall<GetRecipeRequest> { cos ->
        if (value.recipeId.isNotEmpty()) cos.writeString(1, value.recipeId)
    }

    override fun parse(stream: InputStream): GetRecipeRequest {
        val cis = CodedInputStream.newInstance(stream)
        var recipeId = ""
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> recipeId = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return GetRecipeRequest(recipeId)
    }
}

internal object GetRecipeResponseMarshaller : MethodDescriptor.Marshaller<GetRecipeResponse> {
    override fun stream(value: GetRecipeResponse): InputStream = RecipeCatalogWireCodec.marshall<GetRecipeResponse> { cos ->
        cos.writeByteArray(1, RecipeCatalogWireCodec.recipeToBytes(value.recipe))
    }

    override fun parse(stream: InputStream): GetRecipeResponse {
        val cis = CodedInputStream.newInstance(stream)
        var recipe = RecipeCatalogDtoProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> recipe = RecipeCatalogWireCodec.recipeFromBytes(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return GetRecipeResponse(recipe)
    }
}

internal object CreateRecipeRequestMarshaller : MethodDescriptor.Marshaller<CreateRecipeRequest> {
    override fun stream(value: CreateRecipeRequest): InputStream = RecipeCatalogWireCodec.marshall<CreateRecipeRequest> { cos ->
        if (value.title.isNotEmpty()) cos.writeString(1, value.title)
        if (value.description.isNotEmpty()) cos.writeString(2, value.description)
        if (value.instructions.isNotEmpty()) cos.writeString(3, value.instructions)
        if (value.visibility != ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED) {
            cos.writeInt32(4, value.visibility.value)
        }
        if (value.servingsCount != 0) cos.writeInt32(5, value.servingsCount)
        for (ing in value.ingredients) cos.writeByteArray(6, RecipeCatalogWireCodec.ingredientToBytes(ing))
    }

    override fun parse(stream: InputStream): CreateRecipeRequest {
        val cis = CodedInputStream.newInstance(stream)
        var title = ""
        var description = ""
        var instructions = ""
        var visibility = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED
        var servingsCount = 1
        val ingredients = mutableListOf<RecipeIngredientDtoProto>()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> title = cis.readString()
                (2 shl 3) or 2 -> description = cis.readString()
                (3 shl 3) or 2 -> instructions = cis.readString()
                (4 shl 3) or 0 -> visibility = ContentVisibilityProto.fromValue(cis.readInt32())
                (5 shl 3) or 0 -> servingsCount = cis.readInt32()
                (6 shl 3) or 2 -> ingredients.add(RecipeCatalogWireCodec.ingredientFromBytes(cis.readByteArray()))
                else -> cis.skipField(tag)
            }
        }
        return CreateRecipeRequest(title, description, instructions, visibility, servingsCount, ingredients)
    }
}

internal object CreateRecipeResponseMarshaller : MethodDescriptor.Marshaller<CreateRecipeResponse> {
    override fun stream(value: CreateRecipeResponse): InputStream = RecipeCatalogWireCodec.marshall<CreateRecipeResponse> { cos ->
        cos.writeByteArray(1, RecipeCatalogWireCodec.recipeToBytes(value.recipe))
    }

    override fun parse(stream: InputStream): CreateRecipeResponse {
        val cis = CodedInputStream.newInstance(stream)
        var recipe = RecipeCatalogDtoProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> recipe = RecipeCatalogWireCodec.recipeFromBytes(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return CreateRecipeResponse(recipe)
    }
}

internal object UpdateRecipeRequestMarshaller : MethodDescriptor.Marshaller<UpdateRecipeRequest> {
    override fun stream(value: UpdateRecipeRequest): InputStream = RecipeCatalogWireCodec.marshall<UpdateRecipeRequest> { cos ->
        if (value.recipeId.isNotEmpty()) cos.writeString(1, value.recipeId)
        if (value.title.isNotEmpty()) cos.writeString(2, value.title)
        if (value.description.isNotEmpty()) cos.writeString(3, value.description)
        if (value.instructions.isNotEmpty()) cos.writeString(4, value.instructions)
        if (value.visibility != ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED) {
            cos.writeInt32(5, value.visibility.value)
        }
        if (value.servingsCount != 0) cos.writeInt32(6, value.servingsCount)
        for (ing in value.ingredients) cos.writeByteArray(7, RecipeCatalogWireCodec.ingredientToBytes(ing))
    }

    override fun parse(stream: InputStream): UpdateRecipeRequest {
        val cis = CodedInputStream.newInstance(stream)
        var recipeId = ""
        var title = ""
        var description = ""
        var instructions = ""
        var visibility = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED
        var servingsCount = 1
        val ingredients = mutableListOf<RecipeIngredientDtoProto>()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> recipeId = cis.readString()
                (2 shl 3) or 2 -> title = cis.readString()
                (3 shl 3) or 2 -> description = cis.readString()
                (4 shl 3) or 2 -> instructions = cis.readString()
                (5 shl 3) or 0 -> visibility = ContentVisibilityProto.fromValue(cis.readInt32())
                (6 shl 3) or 0 -> servingsCount = cis.readInt32()
                (7 shl 3) or 2 -> ingredients.add(RecipeCatalogWireCodec.ingredientFromBytes(cis.readByteArray()))
                else -> cis.skipField(tag)
            }
        }
        return UpdateRecipeRequest(recipeId, title, description, instructions, visibility, servingsCount, ingredients)
    }
}

internal object UpdateRecipeResponseMarshaller : MethodDescriptor.Marshaller<UpdateRecipeResponse> {
    override fun stream(value: UpdateRecipeResponse): InputStream = RecipeCatalogWireCodec.marshall<UpdateRecipeResponse> { cos ->
        cos.writeByteArray(1, RecipeCatalogWireCodec.recipeToBytes(value.recipe))
    }

    override fun parse(stream: InputStream): UpdateRecipeResponse {
        val cis = CodedInputStream.newInstance(stream)
        var recipe = RecipeCatalogDtoProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> recipe = RecipeCatalogWireCodec.recipeFromBytes(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return UpdateRecipeResponse(recipe)
    }
}

internal object DeleteRecipeRequestMarshaller : MethodDescriptor.Marshaller<DeleteRecipeRequest> {
    override fun stream(value: DeleteRecipeRequest): InputStream = RecipeCatalogWireCodec.marshall<DeleteRecipeRequest> { cos ->
        if (value.recipeId.isNotEmpty()) cos.writeString(1, value.recipeId)
    }

    override fun parse(stream: InputStream): DeleteRecipeRequest {
        val cis = CodedInputStream.newInstance(stream)
        var recipeId = ""
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> recipeId = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return DeleteRecipeRequest(recipeId)
    }
}

internal object DeleteRecipeResponseMarshaller : MethodDescriptor.Marshaller<DeleteRecipeResponse> {
    override fun stream(value: DeleteRecipeResponse): InputStream = RecipeCatalogWireCodec.marshall<DeleteRecipeResponse> { cos ->
        if (value.success) cos.writeBool(1, value.success)
    }

    override fun parse(stream: InputStream): DeleteRecipeResponse {
        val cis = CodedInputStream.newInstance(stream)
        var success = false
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 0 -> success = cis.readBool()
                else -> cis.skipField(tag)
            }
        }
        return DeleteRecipeResponse(success)
    }
}

internal object ForkRecipeRequestMarshaller : MethodDescriptor.Marshaller<ForkRecipeRequest> {
    override fun stream(value: ForkRecipeRequest): InputStream = RecipeCatalogWireCodec.marshall<ForkRecipeRequest> { cos ->
        if (value.sourceRecipeId.isNotEmpty()) cos.writeString(1, value.sourceRecipeId)
        if (value.newTitle.isNotEmpty()) cos.writeString(2, value.newTitle)
    }

    override fun parse(stream: InputStream): ForkRecipeRequest {
        val cis = CodedInputStream.newInstance(stream)
        var sourceRecipeId = ""
        var newTitle = ""
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> sourceRecipeId = cis.readString()
                (2 shl 3) or 2 -> newTitle = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return ForkRecipeRequest(sourceRecipeId, newTitle)
    }
}

internal object ForkRecipeResponseMarshaller : MethodDescriptor.Marshaller<ForkRecipeResponse> {
    override fun stream(value: ForkRecipeResponse): InputStream = RecipeCatalogWireCodec.marshall<ForkRecipeResponse> { cos ->
        cos.writeByteArray(1, RecipeCatalogWireCodec.recipeToBytes(value.recipe))
    }

    override fun parse(stream: InputStream): ForkRecipeResponse {
        val cis = CodedInputStream.newInstance(stream)
        var recipe = RecipeCatalogDtoProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> recipe = RecipeCatalogWireCodec.recipeFromBytes(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return ForkRecipeResponse(recipe)
    }
}

internal object PublishRecipeRequestMarshaller : MethodDescriptor.Marshaller<PublishRecipeRequest> {
    override fun stream(value: PublishRecipeRequest): InputStream = RecipeCatalogWireCodec.marshall<PublishRecipeRequest> { cos ->
        if (value.recipeId.isNotEmpty()) cos.writeString(1, value.recipeId)
        if (value.visibility != ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED) {
            cos.writeInt32(2, value.visibility.value)
        }
    }

    override fun parse(stream: InputStream): PublishRecipeRequest {
        val cis = CodedInputStream.newInstance(stream)
        var recipeId = ""
        var visibility = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> recipeId = cis.readString()
                (2 shl 3) or 0 -> visibility = ContentVisibilityProto.fromValue(cis.readInt32())
                else -> cis.skipField(tag)
            }
        }
        return PublishRecipeRequest(recipeId, visibility)
    }
}

internal object PublishRecipeResponseMarshaller : MethodDescriptor.Marshaller<PublishRecipeResponse> {
    override fun stream(value: PublishRecipeResponse): InputStream = RecipeCatalogWireCodec.marshall<PublishRecipeResponse> { cos ->
        cos.writeByteArray(1, RecipeCatalogWireCodec.recipeToBytes(value.recipe))
    }

    override fun parse(stream: InputStream): PublishRecipeResponse {
        val cis = CodedInputStream.newInstance(stream)
        var recipe = RecipeCatalogDtoProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> recipe = RecipeCatalogWireCodec.recipeFromBytes(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return PublishRecipeResponse(recipe)
    }
}

internal object SearchMealPlansRequestMarshaller : MethodDescriptor.Marshaller<SearchMealPlansRequest> {
    override fun stream(value: SearchMealPlansRequest): InputStream = RecipeCatalogWireCodec.marshall<SearchMealPlansRequest> { cos ->
        if (value.query.isNotEmpty()) cos.writeString(1, value.query)
        if (value.authorType != AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED) cos.writeInt32(2, value.authorType.value)
        if (value.visibility != ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED) {
            cos.writeInt32(3, value.visibility.value)
        }
        if (value.pageSize != 0) cos.writeInt32(4, value.pageSize)
        if (value.pageToken.isNotEmpty()) cos.writeString(5, value.pageToken)
    }

    override fun parse(stream: InputStream): SearchMealPlansRequest {
        val cis = CodedInputStream.newInstance(stream)
        var query = ""
        var authorType = AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED
        var visibility = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED
        var pageSize = 20
        var pageToken = ""
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> query = cis.readString()
                (2 shl 3) or 0 -> authorType = AuthorTypeProto.fromValue(cis.readInt32())
                (3 shl 3) or 0 -> visibility = ContentVisibilityProto.fromValue(cis.readInt32())
                (4 shl 3) or 0 -> pageSize = cis.readInt32()
                (5 shl 3) or 2 -> pageToken = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return SearchMealPlansRequest(query, authorType, visibility, pageSize, pageToken)
    }
}

internal object SearchMealPlansResponseMarshaller : MethodDescriptor.Marshaller<SearchMealPlansResponse> {
    override fun stream(value: SearchMealPlansResponse): InputStream = RecipeCatalogWireCodec.marshall<SearchMealPlansResponse> { cos ->
        for (mp in value.mealPlans) cos.writeByteArray(1, RecipeCatalogWireCodec.mealPlanTemplateToBytes(mp))
        if (value.nextPageToken.isNotEmpty()) cos.writeString(2, value.nextPageToken)
        if (value.totalCount != 0L) cos.writeInt64(3, value.totalCount)
    }

    override fun parse(stream: InputStream): SearchMealPlansResponse {
        val cis = CodedInputStream.newInstance(stream)
        val mealPlans = mutableListOf<MealPlanTemplateDtoProto>()
        var nextPageToken = ""
        var totalCount = 0L
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> mealPlans.add(RecipeCatalogWireCodec.mealPlanTemplateFromBytes(cis.readByteArray()))
                (2 shl 3) or 2 -> nextPageToken = cis.readString()
                (3 shl 3) or 0 -> totalCount = cis.readInt64()
                else -> cis.skipField(tag)
            }
        }
        return SearchMealPlansResponse(mealPlans, nextPageToken, totalCount)
    }
}

internal object GetMealPlanRequestMarshaller : MethodDescriptor.Marshaller<GetMealPlanRequest> {
    override fun stream(value: GetMealPlanRequest): InputStream = RecipeCatalogWireCodec.marshall<GetMealPlanRequest> { cos ->
        if (value.planId.isNotEmpty()) cos.writeString(1, value.planId)
    }

    override fun parse(stream: InputStream): GetMealPlanRequest {
        val cis = CodedInputStream.newInstance(stream)
        var planId = ""
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> planId = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return GetMealPlanRequest(planId)
    }
}

internal object GetMealPlanResponseMarshaller : MethodDescriptor.Marshaller<GetMealPlanResponse> {
    override fun stream(value: GetMealPlanResponse): InputStream = RecipeCatalogWireCodec.marshall<GetMealPlanResponse> { cos ->
        cos.writeByteArray(1, RecipeCatalogWireCodec.mealPlanTemplateToBytes(value.mealPlan))
    }

    override fun parse(stream: InputStream): GetMealPlanResponse {
        val cis = CodedInputStream.newInstance(stream)
        var mealPlan = MealPlanTemplateDtoProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> mealPlan = RecipeCatalogWireCodec.mealPlanTemplateFromBytes(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return GetMealPlanResponse(mealPlan)
    }
}

internal object CreateMealPlanRequestMarshaller : MethodDescriptor.Marshaller<CreateMealPlanRequest> {
    override fun stream(value: CreateMealPlanRequest): InputStream = RecipeCatalogWireCodec.marshall<CreateMealPlanRequest> { cos ->
        if (value.title.isNotEmpty()) cos.writeString(1, value.title)
        if (value.description.isNotEmpty()) cos.writeString(2, value.description)
        if (value.durationDays != 0) cos.writeInt32(3, value.durationDays)
        if (value.visibility != ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED) {
            cos.writeInt32(4, value.visibility.value)
        }
        if (value.accessMode != AccessModeProto.ACCESS_MODE_UNSPECIFIED) cos.writeInt32(5, value.accessMode.value)
        if (value.priceAmountMinor != 0L) cos.writeInt64(6, value.priceAmountMinor)
        if (value.currency.isNotEmpty()) cos.writeString(7, value.currency)
        for (item in value.items) cos.writeByteArray(8, RecipeCatalogWireCodec.mealPlanItemToBytes(item))
    }

    override fun parse(stream: InputStream): CreateMealPlanRequest {
        val cis = CodedInputStream.newInstance(stream)
        var title = ""
        var description = ""
        var durationDays = 1
        var visibility = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED
        var accessMode = AccessModeProto.ACCESS_MODE_UNSPECIFIED
        var priceAmountMinor = 0L
        var currency = "RUB"
        val items = mutableListOf<MealPlanItemDtoProto>()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> title = cis.readString()
                (2 shl 3) or 2 -> description = cis.readString()
                (3 shl 3) or 0 -> durationDays = cis.readInt32()
                (4 shl 3) or 0 -> visibility = ContentVisibilityProto.fromValue(cis.readInt32())
                (5 shl 3) or 0 -> accessMode = AccessModeProto.fromValue(cis.readInt32())
                (6 shl 3) or 0 -> priceAmountMinor = cis.readInt64()
                (7 shl 3) or 2 -> currency = cis.readString()
                (8 shl 3) or 2 -> items.add(RecipeCatalogWireCodec.mealPlanItemFromBytes(cis.readByteArray()))
                else -> cis.skipField(tag)
            }
        }
        return CreateMealPlanRequest(title, description, durationDays, visibility, accessMode, priceAmountMinor, currency, items)
    }
}

internal object CreateMealPlanResponseMarshaller : MethodDescriptor.Marshaller<CreateMealPlanResponse> {
    override fun stream(value: CreateMealPlanResponse): InputStream = RecipeCatalogWireCodec.marshall<CreateMealPlanResponse> { cos ->
        cos.writeByteArray(1, RecipeCatalogWireCodec.mealPlanTemplateToBytes(value.mealPlan))
    }

    override fun parse(stream: InputStream): CreateMealPlanResponse {
        val cis = CodedInputStream.newInstance(stream)
        var mealPlan = MealPlanTemplateDtoProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> mealPlan = RecipeCatalogWireCodec.mealPlanTemplateFromBytes(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return CreateMealPlanResponse(mealPlan)
    }
}

internal object UpdateMealPlanRequestMarshaller : MethodDescriptor.Marshaller<UpdateMealPlanRequest> {
    override fun stream(value: UpdateMealPlanRequest): InputStream = RecipeCatalogWireCodec.marshall<UpdateMealPlanRequest> { cos ->
        if (value.planId.isNotEmpty()) cos.writeString(1, value.planId)
        if (value.title.isNotEmpty()) cos.writeString(2, value.title)
        if (value.description.isNotEmpty()) cos.writeString(3, value.description)
        if (value.durationDays != 0) cos.writeInt32(4, value.durationDays)
        if (value.visibility != ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED) {
            cos.writeInt32(5, value.visibility.value)
        }
        if (value.accessMode != AccessModeProto.ACCESS_MODE_UNSPECIFIED) cos.writeInt32(6, value.accessMode.value)
        if (value.priceAmountMinor != 0L) cos.writeInt64(7, value.priceAmountMinor)
        if (value.currency.isNotEmpty()) cos.writeString(8, value.currency)
        for (item in value.items) cos.writeByteArray(9, RecipeCatalogWireCodec.mealPlanItemToBytes(item))
    }

    override fun parse(stream: InputStream): UpdateMealPlanRequest {
        val cis = CodedInputStream.newInstance(stream)
        var planId = ""
        var title = ""
        var description = ""
        var durationDays = 1
        var visibility = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED
        var accessMode = AccessModeProto.ACCESS_MODE_UNSPECIFIED
        var priceAmountMinor = 0L
        var currency = "RUB"
        val items = mutableListOf<MealPlanItemDtoProto>()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> planId = cis.readString()
                (2 shl 3) or 2 -> title = cis.readString()
                (3 shl 3) or 2 -> description = cis.readString()
                (4 shl 3) or 0 -> durationDays = cis.readInt32()
                (5 shl 3) or 0 -> visibility = ContentVisibilityProto.fromValue(cis.readInt32())
                (6 shl 3) or 0 -> accessMode = AccessModeProto.fromValue(cis.readInt32())
                (7 shl 3) or 0 -> priceAmountMinor = cis.readInt64()
                (8 shl 3) or 2 -> currency = cis.readString()
                (9 shl 3) or 2 -> items.add(RecipeCatalogWireCodec.mealPlanItemFromBytes(cis.readByteArray()))
                else -> cis.skipField(tag)
            }
        }
        return UpdateMealPlanRequest(planId, title, description, durationDays, visibility, accessMode, priceAmountMinor, currency, items)
    }
}

internal object UpdateMealPlanResponseMarshaller : MethodDescriptor.Marshaller<UpdateMealPlanResponse> {
    override fun stream(value: UpdateMealPlanResponse): InputStream = RecipeCatalogWireCodec.marshall<UpdateMealPlanResponse> { cos ->
        cos.writeByteArray(1, RecipeCatalogWireCodec.mealPlanTemplateToBytes(value.mealPlan))
    }

    override fun parse(stream: InputStream): UpdateMealPlanResponse {
        val cis = CodedInputStream.newInstance(stream)
        var mealPlan = MealPlanTemplateDtoProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> mealPlan = RecipeCatalogWireCodec.mealPlanTemplateFromBytes(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return UpdateMealPlanResponse(mealPlan)
    }
}

internal object DeleteMealPlanRequestMarshaller : MethodDescriptor.Marshaller<DeleteMealPlanRequest> {
    override fun stream(value: DeleteMealPlanRequest): InputStream = RecipeCatalogWireCodec.marshall<DeleteMealPlanRequest> { cos ->
        if (value.planId.isNotEmpty()) cos.writeString(1, value.planId)
    }

    override fun parse(stream: InputStream): DeleteMealPlanRequest {
        val cis = CodedInputStream.newInstance(stream)
        var planId = ""
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> planId = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return DeleteMealPlanRequest(planId)
    }
}

internal object DeleteMealPlanResponseMarshaller : MethodDescriptor.Marshaller<DeleteMealPlanResponse> {
    override fun stream(value: DeleteMealPlanResponse): InputStream = RecipeCatalogWireCodec.marshall<DeleteMealPlanResponse> { cos ->
        if (value.success) cos.writeBool(1, value.success)
    }

    override fun parse(stream: InputStream): DeleteMealPlanResponse {
        val cis = CodedInputStream.newInstance(stream)
        var success = false
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 0 -> success = cis.readBool()
                else -> cis.skipField(tag)
            }
        }
        return DeleteMealPlanResponse(success)
    }
}

internal object InviteStudentRequestMarshaller : MethodDescriptor.Marshaller<InviteStudentRequest> {
    override fun stream(value: InviteStudentRequest): InputStream = RecipeCatalogWireCodec.marshall<InviteStudentRequest> { cos ->
        if (value.studentId.isNotEmpty()) cos.writeString(1, value.studentId)
        if (value.canViewDiary) cos.writeBool(2, value.canViewDiary)
    }

    override fun parse(stream: InputStream): InviteStudentRequest {
        val cis = CodedInputStream.newInstance(stream)
        var studentId = ""
        var canViewDiary = false
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> studentId = cis.readString()
                (2 shl 3) or 0 -> canViewDiary = cis.readBool()
                else -> cis.skipField(tag)
            }
        }
        return InviteStudentRequest(studentId, canViewDiary)
    }
}

internal object InviteStudentResponseMarshaller : MethodDescriptor.Marshaller<InviteStudentResponse> {
    override fun stream(value: InviteStudentResponse): InputStream = RecipeCatalogWireCodec.marshall<InviteStudentResponse> { cos ->
        cos.writeByteArray(1, RecipeCatalogWireCodec.coachLinkToBytes(value.link))
    }

    override fun parse(stream: InputStream): InviteStudentResponse {
        val cis = CodedInputStream.newInstance(stream)
        var link = CoachStudentLinkDtoProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> link = RecipeCatalogWireCodec.coachLinkFromBytes(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return InviteStudentResponse(link)
    }
}

internal object RespondCoachInviteRequestMarshaller : MethodDescriptor.Marshaller<RespondCoachInviteRequest> {
    override fun stream(value: RespondCoachInviteRequest): InputStream = RecipeCatalogWireCodec.marshall<RespondCoachInviteRequest> { cos ->
        if (value.linkId.isNotEmpty()) cos.writeString(1, value.linkId)
        if (value.accept) cos.writeBool(2, value.accept)
    }

    override fun parse(stream: InputStream): RespondCoachInviteRequest {
        val cis = CodedInputStream.newInstance(stream)
        var linkId = ""
        var accept = false
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> linkId = cis.readString()
                (2 shl 3) or 0 -> accept = cis.readBool()
                else -> cis.skipField(tag)
            }
        }
        return RespondCoachInviteRequest(linkId, accept)
    }
}

internal object RespondCoachInviteResponseMarshaller : MethodDescriptor.Marshaller<RespondCoachInviteResponse> {
    override fun stream(value: RespondCoachInviteResponse): InputStream = RecipeCatalogWireCodec.marshall<RespondCoachInviteResponse> { cos ->
        cos.writeByteArray(1, RecipeCatalogWireCodec.coachLinkToBytes(value.link))
    }

    override fun parse(stream: InputStream): RespondCoachInviteResponse {
        val cis = CodedInputStream.newInstance(stream)
        var link = CoachStudentLinkDtoProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> link = RecipeCatalogWireCodec.coachLinkFromBytes(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return RespondCoachInviteResponse(link)
    }
}

internal object ListCoachLinksRequestMarshaller : MethodDescriptor.Marshaller<ListCoachLinksRequest> {
    override fun stream(value: ListCoachLinksRequest): InputStream = RecipeCatalogWireCodec.marshall<ListCoachLinksRequest> { cos ->
        if (value.statusFilter != CoachLinkStatusProto.COACH_LINK_STATUS_UNSPECIFIED) {
            cos.writeInt32(1, value.statusFilter.value)
        }
    }

    override fun parse(stream: InputStream): ListCoachLinksRequest {
        val cis = CodedInputStream.newInstance(stream)
        var statusFilter = CoachLinkStatusProto.COACH_LINK_STATUS_UNSPECIFIED
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 0 -> statusFilter = CoachLinkStatusProto.fromValue(cis.readInt32())
                else -> cis.skipField(tag)
            }
        }
        return ListCoachLinksRequest(statusFilter)
    }
}

internal object ListCoachLinksResponseMarshaller : MethodDescriptor.Marshaller<ListCoachLinksResponse> {
    override fun stream(value: ListCoachLinksResponse): InputStream = RecipeCatalogWireCodec.marshall<ListCoachLinksResponse> { cos ->
        for (link in value.links) cos.writeByteArray(1, RecipeCatalogWireCodec.coachLinkToBytes(link))
    }

    override fun parse(stream: InputStream): ListCoachLinksResponse {
        val cis = CodedInputStream.newInstance(stream)
        val links = mutableListOf<CoachStudentLinkDtoProto>()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> links.add(RecipeCatalogWireCodec.coachLinkFromBytes(cis.readByteArray()))
                else -> cis.skipField(tag)
            }
        }
        return ListCoachLinksResponse(links)
    }
}

internal object GrantAccessRequestMarshaller : MethodDescriptor.Marshaller<GrantAccessRequest> {
    override fun stream(value: GrantAccessRequest): InputStream = RecipeCatalogWireCodec.marshall<GrantAccessRequest> { cos ->
        if (value.userId.isNotEmpty()) cos.writeString(1, value.userId)
        if (value.targetType.isNotEmpty()) cos.writeString(2, value.targetType)
        if (value.targetId.isNotEmpty()) cos.writeString(3, value.targetId)
        if (value.grantSource != GrantSourceProto.GRANT_SOURCE_UNSPECIFIED) cos.writeInt32(4, value.grantSource.value)
        RecipeCatalogWireCodec.writeTimestamp(cos, 5, value.expiresAt)
    }

    override fun parse(stream: InputStream): GrantAccessRequest {
        val cis = CodedInputStream.newInstance(stream)
        var userId = ""
        var targetType = ""
        var targetId = ""
        var grantSource = GrantSourceProto.GRANT_SOURCE_UNSPECIFIED
        var expiresAt: TimestampProto? = null
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> userId = cis.readString()
                (2 shl 3) or 2 -> targetType = cis.readString()
                (3 shl 3) or 2 -> targetId = cis.readString()
                (4 shl 3) or 0 -> grantSource = GrantSourceProto.fromValue(cis.readInt32())
                (5 shl 3) or 2 -> expiresAt = RecipeCatalogWireCodec.readTimestamp(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return GrantAccessRequest(userId, targetType, targetId, grantSource, expiresAt)
    }
}

internal object GrantAccessResponseMarshaller : MethodDescriptor.Marshaller<GrantAccessResponse> {
    override fun stream(value: GrantAccessResponse): InputStream = RecipeCatalogWireCodec.marshall<GrantAccessResponse> { cos ->
        cos.writeByteArray(1, RecipeCatalogWireCodec.accessGrantToBytes(value.grant))
    }

    override fun parse(stream: InputStream): GrantAccessResponse {
        val cis = CodedInputStream.newInstance(stream)
        var grant = AccessGrantDtoProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> grant = RecipeCatalogWireCodec.accessGrantFromBytes(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return GrantAccessResponse(grant)
    }
}

internal object RevokeAccessRequestMarshaller : MethodDescriptor.Marshaller<RevokeAccessRequest> {
    override fun stream(value: RevokeAccessRequest): InputStream = RecipeCatalogWireCodec.marshall<RevokeAccessRequest> { cos ->
        if (value.grantId.isNotEmpty()) cos.writeString(1, value.grantId)
    }

    override fun parse(stream: InputStream): RevokeAccessRequest {
        val cis = CodedInputStream.newInstance(stream)
        var grantId = ""
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> grantId = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return RevokeAccessRequest(grantId)
    }
}

internal object RevokeAccessResponseMarshaller : MethodDescriptor.Marshaller<RevokeAccessResponse> {
    override fun stream(value: RevokeAccessResponse): InputStream = RecipeCatalogWireCodec.marshall<RevokeAccessResponse> { cos ->
        if (value.success) cos.writeBool(1, value.success)
    }

    override fun parse(stream: InputStream): RevokeAccessResponse {
        val cis = CodedInputStream.newInstance(stream)
        var success = false
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 0 -> success = cis.readBool()
                else -> cis.skipField(tag)
            }
        }
        return RevokeAccessResponse(success)
    }
}
