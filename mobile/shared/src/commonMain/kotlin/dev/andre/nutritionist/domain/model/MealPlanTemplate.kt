package dev.andre.nutritionist.domain.model

import kotlinx.datetime.Instant

enum class AccessMode {
    UNSPECIFIED,
    FREE,
    COACH_ASSIGNED,
    PAID,
}

enum class PlanItemType {
    UNSPECIFIED,
    RECIPE,
    PRODUCT,
}

data class MealPlanItem(
    val id: MealPlanItemId,
    val planId: MealPlanTemplateId,
    val dayNumber: Int,
    val mealSlot: MealType,
    val itemType: PlanItemType,
    val itemId: String,
    val nameSnapshot: String,
    val weightGrams: Double,
    val nutrition: NutritionFacts,
    val estimatedCost: Money? = null,
)

data class MealPlanDay(
    val dayNumber: Int,
    val items: List<MealPlanItem> = emptyList(),
    val totalNutrition: NutritionFacts = items.fold(NutritionFacts.ZERO) { acc, item ->
        acc + item.nutrition
    },
    val estimatedCost: Money? = null,
)

data class MealPlanTemplate(
    val id: MealPlanTemplateId,
    val authorId: UserId,
    val authorType: RecipeAuthorType = RecipeAuthorType.COACH,
    val title: String,
    val description: String? = null,
    val durationDays: Int = 1,
    val visibility: ContentVisibility = ContentVisibility.PRIVATE,
    val accessMode: AccessMode = AccessMode.FREE,
    val price: Money = Money.ZERO,
    val currency: String = Money.CURRENCY_RUB,
    val avgDailyNutrition: NutritionFacts = NutritionFacts.ZERO,
    val days: List<MealPlanDay> = emptyList(),
    val estimatedDailyCost: Money? = null,
    val version: Long = 0L,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)
