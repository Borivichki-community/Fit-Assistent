package dev.andre.nutritionist.recipe.infrastructure.grpc.mapper

import com.budgetntr.proto.recipe.v1.AccessGrantProto
import com.budgetntr.proto.recipe.v1.AccessMode
import com.budgetntr.proto.recipe.v1.AuthorType
import com.budgetntr.proto.recipe.v1.CoachLinkStatus
import com.budgetntr.proto.recipe.v1.CoachStudentLinkProto
import com.budgetntr.proto.recipe.v1.ContentVisibility
import com.budgetntr.proto.recipe.v1.GrantSource
import com.budgetntr.proto.recipe.v1.MealPlanItemProto
import com.budgetntr.proto.recipe.v1.MealPlanTemplateProto
import com.budgetntr.proto.recipe.v1.MealType
import com.budgetntr.proto.recipe.v1.PlanItemType
import com.budgetntr.proto.recipe.v1.RecipeIngredientProto
import com.budgetntr.proto.recipe.v1.RecipeProto
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.recipe.persistence.entity.AccessGrantEntity
import dev.andre.nutritionist.recipe.persistence.entity.CoachStudentLinkEntity
import dev.andre.nutritionist.recipe.persistence.entity.MealPlanEntity
import dev.andre.nutritionist.recipe.persistence.entity.MealPlanItemEntity
import dev.andre.nutritionist.recipe.persistence.entity.RecipeEntity
import dev.andre.nutritionist.recipe.persistence.entity.RecipeIngredientEntity
import java.time.Instant

fun Instant.toProtoTimestamp(): Timestamp =
    Timestamp
        .newBuilder()
        .setSeconds(epochSecond)
        .setNanos(nano)
        .build()

fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanos.toLong())

fun RecipeIngredientEntity.toProto(): RecipeIngredientProto =
    RecipeIngredientProto
        .newBuilder()
        .setId(id.toString())
        .setRecipeId(recipe?.id?.toString().orEmpty())
        .setProductId(productId?.toString().orEmpty())
        .setProductNameSnapshot(productNameSnapshot)
        .setWeightGrams(weightGrams.toDouble())
        .setCaloriesPer100G(caloriesPer100g?.toDouble() ?: 0.0)
        .setProteinsPer100G(proteinsPer100g?.toDouble() ?: 0.0)
        .setFatsPer100G(fatsPer100g?.toDouble() ?: 0.0)
        .setCarbsPer100G(carbsPer100g?.toDouble() ?: 0.0)
        .build()

fun RecipeEntity.toProto(includeIngredients: Boolean = true): RecipeProto {
    val builder =
        RecipeProto
            .newBuilder()
            .setId(id.toString())
            .setAuthorType(AuthorType.valueOf("AUTHOR_TYPE_${authorType.uppercase()}"))
            .setAuthorId(authorId.orEmpty())
            .setOriginRecipeId(originRecipe?.id?.toString().orEmpty())
            .setTitle(title)
            .setDescription(description.orEmpty())
            .setInstructions(instructions.orEmpty())
            .setVisibility(ContentVisibility.valueOf("CONTENT_VISIBILITY_${visibility.uppercase()}"))
            .setServingsCount(servingsCount)
            .setTotalWeightG(totalWeightG.toDouble())
            .setCaloriesPer100G(caloriesPer100g.toDouble())
            .setProteinsPer100G(proteinsPer100g.toDouble())
            .setFatsPer100G(fatsPer100g.toDouble())
            .setCarbsPer100G(carbsPer100g.toDouble())
            .setVersion(version)
            .setCreatedAt(createdAt.toProtoTimestamp())
            .setUpdatedAt(updatedAt.toProtoTimestamp())

    if (includeIngredients) {
        builder.addAllIngredients(ingredients.map { it.toProto() })
    }
    return builder.build()
}

fun MealPlanItemEntity.toProto(): MealPlanItemProto =
    MealPlanItemProto
        .newBuilder()
        .setId(id.toString())
        .setPlanId(plan?.id?.toString().orEmpty())
        .setDayNumber(dayNumber)
        .setMealSlot(
            runCatching { MealType.valueOf("MEAL_TYPE_${mealSlot.uppercase()}") }
                .getOrDefault(MealType.MEAL_TYPE_UNSPECIFIED),
        ).setItemType(
            runCatching { PlanItemType.valueOf("PLAN_ITEM_TYPE_${itemType.uppercase()}") }
                .getOrDefault(PlanItemType.PLAN_ITEM_TYPE_UNSPECIFIED),
        ).setItemId(itemId.toString())
        .setNameSnapshot(nameSnapshot)
        .setWeightGrams(weightGrams.toDouble())
        .setCalories(calories?.toDouble() ?: 0.0)
        .setProteins(proteins?.toDouble() ?: 0.0)
        .setFats(fats?.toDouble() ?: 0.0)
        .setCarbs(carbs?.toDouble() ?: 0.0)
        .build()

fun MealPlanEntity.toProto(includeItems: Boolean = true): MealPlanTemplateProto {
    val builder =
        MealPlanTemplateProto
            .newBuilder()
            .setId(id.toString())
            .setAuthorId(authorId)
            .setAuthorType(
                runCatching { AuthorType.valueOf("AUTHOR_TYPE_${authorType.uppercase()}") }
                    .getOrDefault(AuthorType.AUTHOR_TYPE_USER),
            ).setTitle(title)
            .setDescription(description.orEmpty())
            .setDurationDays(durationDays)
            .setVisibility(
                runCatching { ContentVisibility.valueOf("CONTENT_VISIBILITY_${visibility.uppercase()}") }
                    .getOrDefault(ContentVisibility.CONTENT_VISIBILITY_PRIVATE),
            ).setAccessMode(
                runCatching { AccessMode.valueOf("ACCESS_MODE_${accessMode.uppercase()}") }
                    .getOrDefault(AccessMode.ACCESS_MODE_FREE),
            ).setPriceAmountMinor(priceAmountMinor)
            .setCurrency(currency)
            .setAvgDailyCalories(avgDailyCalories?.toDouble() ?: 0.0)
            .setAvgDailyProteins(avgDailyProteins?.toDouble() ?: 0.0)
            .setAvgDailyFats(avgDailyFats?.toDouble() ?: 0.0)
            .setAvgDailyCarbs(avgDailyCarbs?.toDouble() ?: 0.0)
            .setVersion(version)
            .setCreatedAt(createdAt.toProtoTimestamp())
            .setUpdatedAt(updatedAt.toProtoTimestamp())

    if (includeItems) {
        builder.addAllItems(items.map { it.toProto() })
    }
    return builder.build()
}

fun CoachStudentLinkEntity.toProto(): CoachStudentLinkProto =
    CoachStudentLinkProto
        .newBuilder()
        .setId(id.toString())
        .setCoachId(coachId)
        .setStudentId(studentId)
        .setStatus(
            runCatching { CoachLinkStatus.valueOf("COACH_LINK_STATUS_${status.uppercase()}") }
                .getOrDefault(CoachLinkStatus.COACH_LINK_STATUS_UNSPECIFIED),
        ).setCanViewDiary(canViewDiary)
        .setCreatedAt(createdAt.toProtoTimestamp())
        .setUpdatedAt(updatedAt.toProtoTimestamp())
        .build()

fun AccessGrantEntity.toProto(): AccessGrantProto =
    AccessGrantProto
        .newBuilder()
        .setId(id.toString())
        .setUserId(userId)
        .setTargetType(targetType)
        .setTargetId(targetId.toString())
        .setGrantSource(
            runCatching { GrantSource.valueOf("GRANT_SOURCE_${grantSource.uppercase()}") }
                .getOrDefault(GrantSource.GRANT_SOURCE_UNSPECIFIED),
        ).setGrantedBy(grantedBy.orEmpty())
        .apply {
            val exp = this@toProto.expiresAt
            if (exp != null) {
                setExpiresAt(exp.toProtoTimestamp())
            }
        }.setCreatedAt(createdAt.toProtoTimestamp())
        .build()
