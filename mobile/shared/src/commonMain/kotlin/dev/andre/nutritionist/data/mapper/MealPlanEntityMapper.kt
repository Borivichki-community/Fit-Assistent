package dev.andre.nutritionist.data.mapper

import dev.andre.nutritionist.data.local.entity.LocalMealPlanEntity
import dev.andre.nutritionist.data.local.entity.LocalMealPlanItemEntity
import dev.andre.nutritionist.data.local.entity.LocalMealPlanWithItems
import dev.andre.nutritionist.domain.model.AccessMode
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.MealPlanDay
import dev.andre.nutritionist.domain.model.MealPlanItem
import dev.andre.nutritionist.domain.model.MealPlanItemId
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.PlanItemType
import dev.andre.nutritionist.domain.model.RecipeAuthorType
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import kotlin.time.Clock
import kotlin.time.Instant

object MealPlanEntityMapper {

    fun LocalMealPlanWithItems.toDomain(): MealPlanTemplate {
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
            id = MealPlanTemplateId(plan.id),
            authorId = UserId(plan.authorId),
            authorType = plan.authorType.toAuthorType(),
            title = plan.title,
            description = plan.description,
            durationDays = plan.durationDays,
            visibility = plan.visibility.toVisibility(),
            accessMode = plan.accessMode.toAccessMode(),
            price = Money.fromMinor(plan.priceAmountMinor),
            currency = plan.currency,
            avgDailyNutrition = NutritionFacts(
                calories = plan.avgDailyCalories,
                proteins = plan.avgDailyProteins,
                fats = plan.avgDailyFats,
                carbs = plan.avgDailyCarbs,
            ),
            days = days,
            updatedAt = Instant.fromEpochMilliseconds(plan.updatedAtEpochMs),
        )
    }

    fun LocalMealPlanItemEntity.toDomain(): MealPlanItem = MealPlanItem(
        id = MealPlanItemId(id),
        planId = MealPlanTemplateId(planId),
        dayNumber = dayNumber,
        mealSlot = mealSlot.toMealType(),
        itemType = itemType.toPlanItemType(),
        itemId = itemId,
        nameSnapshot = nameSnapshot,
        weightGrams = weightGrams,
        nutrition = NutritionFacts(
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
        ),
        estimatedCost = estimatedCostMinor?.let(Money::fromMinor),
    )

    fun MealPlanTemplate.toEntity(
        syncStatus: SyncStatus = SyncStatus.SYNCED,
        now: Instant = Clock.System.now(),
    ): LocalMealPlanEntity = LocalMealPlanEntity(
        id = id.raw,
        authorId = authorId.raw,
        authorType = authorType.toDbName(),
        title = title,
        description = description,
        durationDays = durationDays,
        visibility = visibility.toDbName(),
        accessMode = accessMode.toDbName(),
        priceAmountMinor = price.amountMinor,
        currency = currency,
        avgDailyCalories = avgDailyNutrition.calories,
        avgDailyProteins = avgDailyNutrition.proteins,
        avgDailyFats = avgDailyNutrition.fats,
        avgDailyCarbs = avgDailyNutrition.carbs,
        syncStatus = syncStatus.toDbName(),
        updatedAtEpochMs = (updatedAt ?: now).toEpochMilliseconds(),
    )

    fun MealPlanTemplate.toItemEntities(): List<LocalMealPlanItemEntity> =
        days.flatMap { day ->
            day.items.map { item -> item.toEntity(planIdOverride = id.raw) }
        }

    fun MealPlanItem.toEntity(planIdOverride: String = planId.raw): LocalMealPlanItemEntity =
        LocalMealPlanItemEntity(
            id = id.raw,
            planId = planIdOverride,
            dayNumber = dayNumber,
            mealSlot = mealSlot.toDbName(),
            itemType = itemType.toDbName(),
            itemId = itemId,
            nameSnapshot = nameSnapshot,
            weightGrams = weightGrams,
            calories = nutrition.calories,
            proteins = nutrition.proteins,
            fats = nutrition.fats,
            carbs = nutrition.carbs,
            estimatedCostMinor = estimatedCost?.amountMinor,
        )

    private fun RecipeAuthorType.toDbName(): String = when (this) {
        RecipeAuthorType.UNSPECIFIED -> RecipeAuthorType.UNSPECIFIED.name
        RecipeAuthorType.SYSTEM -> RecipeAuthorType.SYSTEM.name
        RecipeAuthorType.USER -> RecipeAuthorType.USER.name
        RecipeAuthorType.COACH -> RecipeAuthorType.COACH.name
        RecipeAuthorType.AI -> RecipeAuthorType.AI.name
    }

    private fun ContentVisibility.toDbName(): String = when (this) {
        ContentVisibility.UNSPECIFIED -> ContentVisibility.UNSPECIFIED.name
        ContentVisibility.PRIVATE -> ContentVisibility.PRIVATE.name
        ContentVisibility.LINK_ONLY -> ContentVisibility.LINK_ONLY.name
        ContentVisibility.PUBLIC -> ContentVisibility.PUBLIC.name
    }

    private fun AccessMode.toDbName(): String = when (this) {
        AccessMode.UNSPECIFIED -> AccessMode.UNSPECIFIED.name
        AccessMode.FREE -> AccessMode.FREE.name
        AccessMode.COACH_ASSIGNED -> AccessMode.COACH_ASSIGNED.name
        AccessMode.PAID -> AccessMode.PAID.name
    }

    private fun MealType.toDbName(): String = when (this) {
        MealType.BREAKFAST -> MealType.BREAKFAST.name
        MealType.LUNCH -> MealType.LUNCH.name
        MealType.DINNER -> MealType.DINNER.name
        MealType.SNACK -> MealType.SNACK.name
    }

    private fun PlanItemType.toDbName(): String = when (this) {
        PlanItemType.UNSPECIFIED -> PlanItemType.UNSPECIFIED.name
        PlanItemType.RECIPE -> PlanItemType.RECIPE.name
        PlanItemType.PRODUCT -> PlanItemType.PRODUCT.name
    }

    private fun SyncStatus.toDbName(): String = when (this) {
        SyncStatus.PENDING -> SyncStatus.PENDING.name
        SyncStatus.SYNCED -> SyncStatus.SYNCED.name
        SyncStatus.FAILED -> SyncStatus.FAILED.name
        SyncStatus.DELETED_PENDING -> SyncStatus.DELETED_PENDING.name
    }

    private fun String.toAuthorType(): RecipeAuthorType =
        RecipeAuthorType.entries.firstOrNull { it.name == uppercase() }
            ?: RecipeAuthorType.UNSPECIFIED

    private fun String.toVisibility(): ContentVisibility =
        ContentVisibility.entries.firstOrNull { it.name == uppercase() }
            ?: ContentVisibility.UNSPECIFIED

    private fun String.toAccessMode(): AccessMode =
        AccessMode.entries.firstOrNull { it.name == uppercase() }
            ?: AccessMode.UNSPECIFIED

    private fun String.toMealType(): MealType =
        MealType.entries.firstOrNull { it.name == uppercase() }
            ?: MealType.SNACK

    private fun String.toPlanItemType(): PlanItemType =
        PlanItemType.entries.firstOrNull { it.name == uppercase() }
            ?: PlanItemType.UNSPECIFIED
}
