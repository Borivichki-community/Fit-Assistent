package dev.andre.nutritionist.data.mapper

import com.budgetntr.proto.profile.v1.TimestampProto
import com.budgetntr.proto.recipe.v1.AccessGrantDtoProto
import com.budgetntr.proto.recipe.v1.AccessModeProto
import com.budgetntr.proto.recipe.v1.AuthorTypeProto
import com.budgetntr.proto.recipe.v1.CoachLinkStatusProto
import com.budgetntr.proto.recipe.v1.CoachStudentLinkDtoProto
import com.budgetntr.proto.recipe.v1.ContentVisibilityProto
import com.budgetntr.proto.recipe.v1.CreateMealPlanRequest
import com.budgetntr.proto.recipe.v1.CreateRecipeRequest
import com.budgetntr.proto.recipe.v1.GrantSourceProto
import com.budgetntr.proto.recipe.v1.MealPlanItemDtoProto
import com.budgetntr.proto.recipe.v1.MealPlanTemplateDtoProto
import com.budgetntr.proto.recipe.v1.MealTypeProto
import com.budgetntr.proto.recipe.v1.PlanItemTypeProto
import com.budgetntr.proto.recipe.v1.RecipeCatalogDtoProto
import com.budgetntr.proto.recipe.v1.RecipeIngredientDtoProto
import com.budgetntr.proto.recipe.v1.SearchMealPlansRequest
import com.budgetntr.proto.recipe.v1.SearchMealPlansResponse
import com.budgetntr.proto.recipe.v1.SearchRecipesRequest
import com.budgetntr.proto.recipe.v1.SearchRecipesResponse
import com.budgetntr.proto.recipe.v1.UpdateMealPlanRequest
import com.budgetntr.proto.recipe.v1.UpdateRecipeRequest
import dev.andre.nutritionist.domain.model.AccessGrant
import dev.andre.nutritionist.domain.model.AccessGrantId
import dev.andre.nutritionist.domain.model.AccessMode
import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.CoachStudentLink
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.domain.model.GrantTargetType
import dev.andre.nutritionist.domain.model.MealPlanDay
import dev.andre.nutritionist.domain.model.MealPlanItem
import dev.andre.nutritionist.domain.model.MealPlanItemId
import dev.andre.nutritionist.domain.model.MealPlanSearchCriteria
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.PagedResult
import dev.andre.nutritionist.domain.model.PlanItemType
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeAuthorType
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.model.RecipeIngredientId
import dev.andre.nutritionist.domain.model.RecipeSearchCriteria
import dev.andre.nutritionist.domain.model.UserId
import kotlin.time.Instant

object RecipeCatalogDtoMapper {

    fun RecipeSearchCriteria.toProtoRequest(): SearchRecipesRequest = SearchRecipesRequest(
        query = query,
        authorType = authorType?.toProto() ?: AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED,
        visibility = visibility?.toProto() ?: ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
        pageSize = pageSize,
        pageToken = pageToken.orEmpty(),
    )

    fun SearchRecipesResponse.toDomain(): PagedResult<Recipe> = PagedResult(
        items = recipes.map { it.toDomain() },
        nextPageToken = nextPageToken.ifBlank { null },
        totalCount = totalCount,
    )

    fun RecipeCatalogDtoProto.toDomain(): Recipe = Recipe(
        id = RecipeId(id),
        title = title,
        authorType = authorType.toDomain(),
        authorId = authorId.takeIf { it.isNotBlank() }?.let(::UserId),
        originRecipeId = originRecipeId.takeIf { it.isNotBlank() }?.let(::RecipeId),
        description = description.takeIf { it.isNotBlank() },
        instructions = instructions.takeIf { it.isNotBlank() },
        visibility = visibility.toDomain(),
        servingsCount = servingsCount.coerceAtLeast(1),
        totalWeightGrams = totalWeightG,
        nutrition = NutritionFacts(
            calories = caloriesPer100g,
            proteins = proteinsPer100g,
            fats = fatsPer100g,
            carbs = carbsPer100g,
        ),
        ingredients = ingredients.map { it.toDomain() },
        version = version,
        createdAt = createdAt?.toInstantOrNull(),
        updatedAt = updatedAt?.toInstantOrNull(),
    )

    fun RecipeIngredientDtoProto.toDomain(): RecipeIngredient = RecipeIngredient(
        id = RecipeIngredientId(id),
        productId = productId.takeIf { it.isNotBlank() }?.let(::ProductId),
        name = productNameSnapshot,
        weightGrams = weightGrams,
        nutritionPer100g = NutritionFacts(
            calories = caloriesPer100g,
            proteins = proteinsPer100g,
            fats = fatsPer100g,
            carbs = carbsPer100g,
        ),
    )

    fun Recipe.toCreateRequest(): CreateRecipeRequest = CreateRecipeRequest(
        title = title,
        description = description.orEmpty(),
        instructions = instructions.orEmpty(),
        visibility = visibility.toProto(),
        servingsCount = servingsCount,
        ingredients = ingredients.map { it.toProto(id.raw) },
    )

    fun Recipe.toUpdateRequest(): UpdateRecipeRequest = UpdateRecipeRequest(
        recipeId = id.raw,
        title = title,
        description = description.orEmpty(),
        instructions = instructions.orEmpty(),
        visibility = visibility.toProto(),
        servingsCount = servingsCount,
        ingredients = ingredients.map { it.toProto(id.raw) },
    )

    fun RecipeIngredient.toProto(recipeId: String = ""): RecipeIngredientDtoProto = RecipeIngredientDtoProto(
        id = id.raw,
        recipeId = recipeId,
        productId = productId?.raw.orEmpty(),
        productNameSnapshot = name,
        weightGrams = weightGrams,
        caloriesPer100g = nutritionPer100g.calories,
        proteinsPer100g = nutritionPer100g.proteins,
        fatsPer100g = nutritionPer100g.fats,
        carbsPer100g = nutritionPer100g.carbs,
    )

    fun MealPlanSearchCriteria.toProtoRequest(): SearchMealPlansRequest = SearchMealPlansRequest(
        query = query,
        authorType = authorType?.toProto() ?: AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED,
        visibility = visibility?.toProto() ?: ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
        pageSize = pageSize,
        pageToken = pageToken.orEmpty(),
    )

    fun SearchMealPlansResponse.toDomain(): PagedResult<MealPlanTemplate> = PagedResult(
        items = mealPlans.map { it.toDomain() },
        nextPageToken = nextPageToken.ifBlank { null },
        totalCount = totalCount,
    )

    fun MealPlanTemplateDtoProto.toDomain(): MealPlanTemplate {
        val domainItems = items.map { it.toDomain() }
        val days = domainItems
            .groupBy { it.dayNumber }
            .entries
            .sortedBy { it.key }
            .map { (dayNumber, dayItems) ->
                MealPlanDay(
                    dayNumber = dayNumber,
                    items = dayItems,
                )
            }

        return MealPlanTemplate(
            id = MealPlanTemplateId(id),
            authorId = UserId(authorId),
            authorType = authorType.toDomain(),
            title = title,
            description = description.takeIf { it.isNotBlank() },
            durationDays = durationDays.coerceAtLeast(1),
            visibility = visibility.toDomain(),
            accessMode = accessMode.toDomain(),
            price = Money.fromMinor(priceAmountMinor),
            currency = currency.ifBlank { Money.CURRENCY_RUB },
            avgDailyNutrition = NutritionFacts(
                calories = avgDailyCalories,
                proteins = avgDailyProteins,
                fats = avgDailyFats,
                carbs = avgDailyCarbs,
            ),
            days = days,
            version = version,
            createdAt = createdAt?.toInstantOrNull(),
            updatedAt = updatedAt?.toInstantOrNull(),
        )
    }

    fun MealPlanItemDtoProto.toDomain(): MealPlanItem = MealPlanItem(
        id = MealPlanItemId(id),
        planId = MealPlanTemplateId(planId),
        dayNumber = dayNumber,
        mealSlot = mealSlot.toDomain(),
        itemType = itemType.toDomain(),
        itemId = itemId,
        nameSnapshot = nameSnapshot,
        weightGrams = weightGrams,
        nutrition = NutritionFacts(
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
        ),
    )

    fun MealPlanTemplate.toCreateRequest(): CreateMealPlanRequest = CreateMealPlanRequest(
        title = title,
        description = description.orEmpty(),
        durationDays = durationDays,
        visibility = visibility.toProto(),
        accessMode = accessMode.toProto(),
        priceAmountMinor = price.amountMinor,
        currency = currency,
        items = days.flatMap { day -> day.items.map { it.toProto(id.raw) } },
    )

    fun MealPlanTemplate.toUpdateRequest(): UpdateMealPlanRequest = UpdateMealPlanRequest(
        planId = id.raw,
        title = title,
        description = description.orEmpty(),
        durationDays = durationDays,
        visibility = visibility.toProto(),
        accessMode = accessMode.toProto(),
        priceAmountMinor = price.amountMinor,
        currency = currency,
        items = days.flatMap { day -> day.items.map { it.toProto(id.raw) } },
    )

    fun MealPlanItem.toProto(planIdOverride: String = planId.raw): MealPlanItemDtoProto = MealPlanItemDtoProto(
        id = id.raw,
        planId = planIdOverride,
        dayNumber = dayNumber,
        mealSlot = mealSlot.toProto(),
        itemType = itemType.toProto(),
        itemId = itemId,
        nameSnapshot = nameSnapshot,
        weightGrams = weightGrams,
        calories = nutrition.calories,
        proteins = nutrition.proteins,
        fats = nutrition.fats,
        carbs = nutrition.carbs,
    )

    fun CoachStudentLinkDtoProto.toDomain(): CoachStudentLink = CoachStudentLink(
        id = CoachLinkId(id),
        coachId = UserId(coachId),
        studentId = UserId(studentId),
        status = status.toDomain(),
        canViewDiary = canViewDiary,
        createdAt = createdAt?.toInstantOrNull(),
        updatedAt = updatedAt?.toInstantOrNull(),
    )

    fun AccessGrantDtoProto.toDomain(): AccessGrant = AccessGrant(
        id = AccessGrantId(id),
        userId = UserId(userId),
        targetType = when (targetType.uppercase()) {
            "MEAL_PLAN" -> GrantTargetType.MEAL_PLAN
            else -> GrantTargetType.RECIPE
        },
        targetId = targetId,
        grantSource = grantSource.toDomain(),
        grantedBy = grantedBy.takeIf { it.isNotBlank() }?.let(::UserId),
        expiresAt = expiresAt?.toInstantOrNull(),
        createdAt = createdAt?.toInstantOrNull(),
    )

    fun GrantTargetType.toProtoString(): String = when (this) {
        GrantTargetType.RECIPE -> "RECIPE"
        GrantTargetType.MEAL_PLAN -> "MEAL_PLAN"
    }

    fun AuthorTypeProto.toDomain(): RecipeAuthorType = when (this) {
        AuthorTypeProto.AUTHOR_TYPE_SYSTEM -> RecipeAuthorType.SYSTEM
        AuthorTypeProto.AUTHOR_TYPE_USER -> RecipeAuthorType.USER
        AuthorTypeProto.AUTHOR_TYPE_COACH -> RecipeAuthorType.COACH
        AuthorTypeProto.AUTHOR_TYPE_AI -> RecipeAuthorType.AI
        AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED,
        AuthorTypeProto.UNRECOGNIZED,
        -> RecipeAuthorType.UNSPECIFIED
    }

    fun RecipeAuthorType.toProto(): AuthorTypeProto = when (this) {
        RecipeAuthorType.UNSPECIFIED -> AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED
        RecipeAuthorType.SYSTEM -> AuthorTypeProto.AUTHOR_TYPE_SYSTEM
        RecipeAuthorType.USER -> AuthorTypeProto.AUTHOR_TYPE_USER
        RecipeAuthorType.COACH -> AuthorTypeProto.AUTHOR_TYPE_COACH
        RecipeAuthorType.AI -> AuthorTypeProto.AUTHOR_TYPE_AI
    }

    fun ContentVisibilityProto.toDomain(): ContentVisibility = when (this) {
        ContentVisibilityProto.CONTENT_VISIBILITY_PRIVATE -> ContentVisibility.PRIVATE
        ContentVisibilityProto.CONTENT_VISIBILITY_LINK_ONLY -> ContentVisibility.LINK_ONLY
        ContentVisibilityProto.CONTENT_VISIBILITY_PUBLIC -> ContentVisibility.PUBLIC
        ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
        ContentVisibilityProto.UNRECOGNIZED,
        -> ContentVisibility.UNSPECIFIED
    }

    fun ContentVisibility.toProto(): ContentVisibilityProto = when (this) {
        ContentVisibility.UNSPECIFIED -> ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED
        ContentVisibility.PRIVATE -> ContentVisibilityProto.CONTENT_VISIBILITY_PRIVATE
        ContentVisibility.LINK_ONLY -> ContentVisibilityProto.CONTENT_VISIBILITY_LINK_ONLY
        ContentVisibility.PUBLIC -> ContentVisibilityProto.CONTENT_VISIBILITY_PUBLIC
    }

    fun AccessModeProto.toDomain(): AccessMode = when (this) {
        AccessModeProto.ACCESS_MODE_FREE -> AccessMode.FREE
        AccessModeProto.ACCESS_MODE_COACH_ASSIGNED -> AccessMode.COACH_ASSIGNED
        AccessModeProto.ACCESS_MODE_PAID -> AccessMode.PAID
        AccessModeProto.ACCESS_MODE_UNSPECIFIED,
        AccessModeProto.UNRECOGNIZED,
        -> AccessMode.UNSPECIFIED
    }

    fun AccessMode.toProto(): AccessModeProto = when (this) {
        AccessMode.UNSPECIFIED -> AccessModeProto.ACCESS_MODE_UNSPECIFIED
        AccessMode.FREE -> AccessModeProto.ACCESS_MODE_FREE
        AccessMode.COACH_ASSIGNED -> AccessModeProto.ACCESS_MODE_COACH_ASSIGNED
        AccessMode.PAID -> AccessModeProto.ACCESS_MODE_PAID
    }

    fun GrantSourceProto.toDomain(): GrantSource = when (this) {
        GrantSourceProto.GRANT_SOURCE_COACH_GRANT -> GrantSource.COACH_GRANT
        GrantSourceProto.GRANT_SOURCE_PURCHASE -> GrantSource.PURCHASE
        GrantSourceProto.GRANT_SOURCE_SUBSCRIPTION -> GrantSource.SUBSCRIPTION
        GrantSourceProto.GRANT_SOURCE_UNSPECIFIED,
        GrantSourceProto.UNRECOGNIZED,
        -> GrantSource.UNSPECIFIED
    }

    fun GrantSource.toProto(): GrantSourceProto = when (this) {
        GrantSource.UNSPECIFIED -> GrantSourceProto.GRANT_SOURCE_UNSPECIFIED
        GrantSource.COACH_GRANT -> GrantSourceProto.GRANT_SOURCE_COACH_GRANT
        GrantSource.PURCHASE -> GrantSourceProto.GRANT_SOURCE_PURCHASE
        GrantSource.SUBSCRIPTION -> GrantSourceProto.GRANT_SOURCE_SUBSCRIPTION
    }

    fun CoachLinkStatusProto.toDomain(): CoachLinkStatus = when (this) {
        CoachLinkStatusProto.COACH_LINK_STATUS_PENDING -> CoachLinkStatus.PENDING
        CoachLinkStatusProto.COACH_LINK_STATUS_ACTIVE -> CoachLinkStatus.ACTIVE
        CoachLinkStatusProto.COACH_LINK_STATUS_REVOKED -> CoachLinkStatus.REVOKED
        CoachLinkStatusProto.COACH_LINK_STATUS_UNSPECIFIED,
        CoachLinkStatusProto.UNRECOGNIZED,
        -> CoachLinkStatus.UNSPECIFIED
    }

    fun CoachLinkStatus.toProto(): CoachLinkStatusProto = when (this) {
        CoachLinkStatus.UNSPECIFIED -> CoachLinkStatusProto.COACH_LINK_STATUS_UNSPECIFIED
        CoachLinkStatus.PENDING -> CoachLinkStatusProto.COACH_LINK_STATUS_PENDING
        CoachLinkStatus.ACTIVE -> CoachLinkStatusProto.COACH_LINK_STATUS_ACTIVE
        CoachLinkStatus.REVOKED -> CoachLinkStatusProto.COACH_LINK_STATUS_REVOKED
    }

    fun PlanItemTypeProto.toDomain(): PlanItemType = when (this) {
        PlanItemTypeProto.PLAN_ITEM_TYPE_RECIPE -> PlanItemType.RECIPE
        PlanItemTypeProto.PLAN_ITEM_TYPE_PRODUCT -> PlanItemType.PRODUCT
        PlanItemTypeProto.PLAN_ITEM_TYPE_UNSPECIFIED,
        PlanItemTypeProto.UNRECOGNIZED,
        -> PlanItemType.UNSPECIFIED
    }

    fun PlanItemType.toProto(): PlanItemTypeProto = when (this) {
        PlanItemType.UNSPECIFIED -> PlanItemTypeProto.PLAN_ITEM_TYPE_UNSPECIFIED
        PlanItemType.RECIPE -> PlanItemTypeProto.PLAN_ITEM_TYPE_RECIPE
        PlanItemType.PRODUCT -> PlanItemTypeProto.PLAN_ITEM_TYPE_PRODUCT
    }

    fun MealTypeProto.toDomain(): MealType = when (this) {
        MealTypeProto.MEAL_TYPE_BREAKFAST -> MealType.BREAKFAST
        MealTypeProto.MEAL_TYPE_LUNCH -> MealType.LUNCH
        MealTypeProto.MEAL_TYPE_DINNER -> MealType.DINNER
        MealTypeProto.MEAL_TYPE_SNACK -> MealType.SNACK
        MealTypeProto.MEAL_TYPE_UNSPECIFIED,
        MealTypeProto.UNRECOGNIZED,
        -> MealType.SNACK
    }

    fun MealType.toProto(): MealTypeProto = when (this) {
        MealType.BREAKFAST -> MealTypeProto.MEAL_TYPE_BREAKFAST
        MealType.LUNCH -> MealTypeProto.MEAL_TYPE_LUNCH
        MealType.DINNER -> MealTypeProto.MEAL_TYPE_DINNER
        MealType.SNACK -> MealTypeProto.MEAL_TYPE_SNACK
    }

    fun Instant.toTimestampProto(): TimestampProto = TimestampProto(
        seconds = epochSeconds,
        nanos = nanosecondsOfSecond,
    )

    private fun TimestampProto.toInstantOrNull(): Instant? =
        if (seconds > 0L || nanos > 0) {
            Instant.fromEpochSeconds(seconds, nanos)
        } else {
            null
        }
}
