package com.budgetntr.proto.recipe.v1

import com.budgetntr.proto.profile.v1.TimestampProto

enum class TargetPeriodProto(val value: Int) {
    TARGET_PERIOD_UNSPECIFIED(0),
    TARGET_PERIOD_DAY(1),
    TARGET_PERIOD_BREAKFAST(2),
    TARGET_PERIOD_LUNCH(3),
    TARGET_PERIOD_DINNER(4),
    TARGET_PERIOD_SNACK(5),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): TargetPeriodProto =
            entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

enum class MealTypeProto(val value: Int) {
    MEAL_TYPE_UNSPECIFIED(0),
    MEAL_TYPE_BREAKFAST(1),
    MEAL_TYPE_LUNCH(2),
    MEAL_TYPE_DINNER(3),
    MEAL_TYPE_SNACK(4),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): MealTypeProto =
            entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

enum class MenuSourceProto(val value: Int) {
    MENU_SOURCE_UNSPECIFIED(0),
    MENU_SOURCE_GENERATED(1),
    MENU_SOURCE_CACHE(2),
    MENU_SOURCE_FALLBACK(3),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): MenuSourceProto =
            entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

enum class AuthorTypeProto(val value: Int) {
    AUTHOR_TYPE_UNSPECIFIED(0),
    AUTHOR_TYPE_SYSTEM(1),
    AUTHOR_TYPE_USER(2),
    AUTHOR_TYPE_COACH(3),
    AUTHOR_TYPE_AI(4),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): AuthorTypeProto =
            entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

enum class ContentVisibilityProto(val value: Int) {
    CONTENT_VISIBILITY_UNSPECIFIED(0),
    CONTENT_VISIBILITY_PRIVATE(1),
    CONTENT_VISIBILITY_LINK_ONLY(2),
    CONTENT_VISIBILITY_PUBLIC(3),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): ContentVisibilityProto =
            entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

enum class AccessModeProto(val value: Int) {
    ACCESS_MODE_UNSPECIFIED(0),
    ACCESS_MODE_FREE(1),
    ACCESS_MODE_COACH_ASSIGNED(2),
    ACCESS_MODE_PAID(3),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): AccessModeProto =
            entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

enum class GrantSourceProto(val value: Int) {
    GRANT_SOURCE_UNSPECIFIED(0),
    GRANT_SOURCE_COACH_GRANT(1),
    GRANT_SOURCE_PURCHASE(2),
    GRANT_SOURCE_SUBSCRIPTION(3),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): GrantSourceProto =
            entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

enum class CoachLinkStatusProto(val value: Int) {
    COACH_LINK_STATUS_UNSPECIFIED(0),
    COACH_LINK_STATUS_PENDING(1),
    COACH_LINK_STATUS_ACTIVE(2),
    COACH_LINK_STATUS_REVOKED(3),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): CoachLinkStatusProto =
            entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

enum class PlanItemTypeProto(val value: Int) {
    PLAN_ITEM_TYPE_UNSPECIFIED(0),
    PLAN_ITEM_TYPE_RECIPE(1),
    PLAN_ITEM_TYPE_PRODUCT(2),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): PlanItemTypeProto =
            entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

data class TargetNutritionProto(
    val calories: Double = 0.0,
    val proteinsGrams: Double = 0.0,
    val fatsGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
)

data class MenuIngredientProto(
    val productId: String = "",
    val name: String = "",
    val grams: Int = 0,
)

data class GeneratedMealProto(
    val type: MealTypeProto = MealTypeProto.MEAL_TYPE_UNSPECIFIED,
    val title: String = "",
    val ingredients: List<MenuIngredientProto> = emptyList(),
    val nutrition: TargetNutritionProto = TargetNutritionProto(),
    val estimatedCostMinor: Long = 0L,
    val cookingTimeMinutes: Int = 0,
)

data class GenerateMealPlanRequest(
    val period: TargetPeriodProto = TargetPeriodProto.TARGET_PERIOD_UNSPECIFIED,
    val targetNutrition: TargetNutritionProto = TargetNutritionProto(),
    val maxBudgetMinor: Long = 0L,
    val currency: String = "RUB",
    val regionId: String = "",
    val preferredProducts: List<String> = emptyList(),
    val excludedProducts: List<String> = emptyList(),
    val userNote: String = "",
)

data class GenerateMealPlanResponse(
    val planId: String = "",
    val period: TargetPeriodProto = TargetPeriodProto.TARGET_PERIOD_UNSPECIFIED,
    val meals: List<GeneratedMealProto> = emptyList(),
    val totalNutrition: TargetNutritionProto = TargetNutritionProto(),
    val estimatedCostMinor: Long = 0L,
    val currency: String = "RUB",
    val cookingInstructions: String = "",
    val source: MenuSourceProto = MenuSourceProto.MENU_SOURCE_GENERATED,
)

data class MenuMealProto(
    val type: MealTypeProto = MealTypeProto.MEAL_TYPE_UNSPECIFIED,
    val name: String = "",
    val ingredients: List<MenuIngredientProto> = emptyList(),
    val calories: Double = 0.0,
    val estimatedCostMinor: Long = 0L,
)

data class WeekMenuDayProto(
    val dayNumber: Int = 0,
    val meals: List<MenuMealProto> = emptyList(),
    val calories: Double = 0.0,
    val estimatedCostMinor: Long = 0L,
)

data class WeekMenuSummaryProto(
    val calories: Double = 0.0,
    val estimatedCostMinor: Long = 0L,
    val currency: String = "RUB",
    val source: MenuSourceProto = MenuSourceProto.MENU_SOURCE_GENERATED,
)

data class GenerateWeekMenuRequest(
    val dailyCalories: Int = 0,
    val weeklyBudgetMinor: Long = 0L,
    val excludedProductIds: List<String> = emptyList(),
    val preferredProductIds: List<String> = emptyList(),
)

data class GenerateWeekMenuResponse(
    val dayReady: WeekMenuDayProto? = null,
    val finalSummary: WeekMenuSummaryProto? = null,
)

// =============================================================================
// Catalog, Meal Plans & Coaching Entities
// =============================================================================

data class RecipeIngredientDtoProto(
    val id: String = "",
    val recipeId: String = "",
    val productId: String = "",
    val productNameSnapshot: String = "",
    val weightGrams: Double = 0.0,
    val caloriesPer100g: Double = 0.0,
    val proteinsPer100g: Double = 0.0,
    val fatsPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
)

data class RecipeCatalogDtoProto(
    val id: String = "",
    val authorType: AuthorTypeProto = AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED,
    val authorId: String = "",
    val originRecipeId: String = "",
    val title: String = "",
    val description: String = "",
    val instructions: String = "",
    val visibility: ContentVisibilityProto = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
    val servingsCount: Int = 0,
    val totalWeightG: Double = 0.0,
    val caloriesPer100g: Double = 0.0,
    val proteinsPer100g: Double = 0.0,
    val fatsPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val version: Long = 0L,
    val createdAt: TimestampProto? = null,
    val updatedAt: TimestampProto? = null,
    val ingredients: List<RecipeIngredientDtoProto> = emptyList(),
)

data class MealPlanItemDtoProto(
    val id: String = "",
    val planId: String = "",
    val dayNumber: Int = 0,
    val mealSlot: MealTypeProto = MealTypeProto.MEAL_TYPE_UNSPECIFIED,
    val itemType: PlanItemTypeProto = PlanItemTypeProto.PLAN_ITEM_TYPE_UNSPECIFIED,
    val itemId: String = "",
    val nameSnapshot: String = "",
    val weightGrams: Double = 0.0,
    val calories: Double = 0.0,
    val proteins: Double = 0.0,
    val fats: Double = 0.0,
    val carbs: Double = 0.0,
)

data class MealPlanTemplateDtoProto(
    val id: String = "",
    val authorId: String = "",
    val authorType: AuthorTypeProto = AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED,
    val title: String = "",
    val description: String = "",
    val durationDays: Int = 0,
    val visibility: ContentVisibilityProto = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
    val accessMode: AccessModeProto = AccessModeProto.ACCESS_MODE_UNSPECIFIED,
    val priceAmountMinor: Long = 0L,
    val currency: String = "RUB",
    val avgDailyCalories: Double = 0.0,
    val avgDailyProteins: Double = 0.0,
    val avgDailyFats: Double = 0.0,
    val avgDailyCarbs: Double = 0.0,
    val version: Long = 0L,
    val createdAt: TimestampProto? = null,
    val updatedAt: TimestampProto? = null,
    val items: List<MealPlanItemDtoProto> = emptyList(),
)

data class CoachStudentLinkDtoProto(
    val id: String = "",
    val coachId: String = "",
    val studentId: String = "",
    val status: CoachLinkStatusProto = CoachLinkStatusProto.COACH_LINK_STATUS_UNSPECIFIED,
    val canViewDiary: Boolean = false,
    val createdAt: TimestampProto? = null,
    val updatedAt: TimestampProto? = null,
)

data class AccessGrantDtoProto(
    val id: String = "",
    val userId: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val grantSource: GrantSourceProto = GrantSourceProto.GRANT_SOURCE_UNSPECIFIED,
    val grantedBy: String = "",
    val expiresAt: TimestampProto? = null,
    val createdAt: TimestampProto? = null,
)

// =============================================================================
// Requests & Responses: Recipes
// =============================================================================

data class SearchRecipesRequest(
    val query: String = "",
    val authorType: AuthorTypeProto = AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED,
    val visibility: ContentVisibilityProto = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
    val pageSize: Int = 20,
    val pageToken: String = "",
)

data class SearchRecipesResponse(
    val recipes: List<RecipeCatalogDtoProto> = emptyList(),
    val nextPageToken: String = "",
    val totalCount: Long = 0L,
)

data class GetRecipeRequest(
    val recipeId: String = "",
)

data class GetRecipeResponse(
    val recipe: RecipeCatalogDtoProto = RecipeCatalogDtoProto(),
)

data class CreateRecipeRequest(
    val title: String = "",
    val description: String = "",
    val instructions: String = "",
    val visibility: ContentVisibilityProto = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
    val servingsCount: Int = 1,
    val ingredients: List<RecipeIngredientDtoProto> = emptyList(),
)

data class CreateRecipeResponse(
    val recipe: RecipeCatalogDtoProto = RecipeCatalogDtoProto(),
)

data class UpdateRecipeRequest(
    val recipeId: String = "",
    val title: String = "",
    val description: String = "",
    val instructions: String = "",
    val visibility: ContentVisibilityProto = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
    val servingsCount: Int = 1,
    val ingredients: List<RecipeIngredientDtoProto> = emptyList(),
)

data class UpdateRecipeResponse(
    val recipe: RecipeCatalogDtoProto = RecipeCatalogDtoProto(),
)

data class DeleteRecipeRequest(
    val recipeId: String = "",
)

data class DeleteRecipeResponse(
    val success: Boolean = false,
)

data class ForkRecipeRequest(
    val sourceRecipeId: String = "",
    val newTitle: String = "",
)

data class ForkRecipeResponse(
    val recipe: RecipeCatalogDtoProto = RecipeCatalogDtoProto(),
)

data class PublishRecipeRequest(
    val recipeId: String = "",
    val visibility: ContentVisibilityProto = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
)

data class PublishRecipeResponse(
    val recipe: RecipeCatalogDtoProto = RecipeCatalogDtoProto(),
)

// =============================================================================
// Requests & Responses: Meal Plans
// =============================================================================

data class SearchMealPlansRequest(
    val query: String = "",
    val authorType: AuthorTypeProto = AuthorTypeProto.AUTHOR_TYPE_UNSPECIFIED,
    val visibility: ContentVisibilityProto = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
    val pageSize: Int = 20,
    val pageToken: String = "",
)

data class SearchMealPlansResponse(
    val mealPlans: List<MealPlanTemplateDtoProto> = emptyList(),
    val nextPageToken: String = "",
    val totalCount: Long = 0L,
)

data class GetMealPlanRequest(
    val planId: String = "",
)

data class GetMealPlanResponse(
    val mealPlan: MealPlanTemplateDtoProto = MealPlanTemplateDtoProto(),
)

data class CreateMealPlanRequest(
    val title: String = "",
    val description: String = "",
    val durationDays: Int = 1,
    val visibility: ContentVisibilityProto = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
    val accessMode: AccessModeProto = AccessModeProto.ACCESS_MODE_UNSPECIFIED,
    val priceAmountMinor: Long = 0L,
    val currency: String = "RUB",
    val items: List<MealPlanItemDtoProto> = emptyList(),
)

data class CreateMealPlanResponse(
    val mealPlan: MealPlanTemplateDtoProto = MealPlanTemplateDtoProto(),
)

data class UpdateMealPlanRequest(
    val planId: String = "",
    val title: String = "",
    val description: String = "",
    val durationDays: Int = 1,
    val visibility: ContentVisibilityProto = ContentVisibilityProto.CONTENT_VISIBILITY_UNSPECIFIED,
    val accessMode: AccessModeProto = AccessModeProto.ACCESS_MODE_UNSPECIFIED,
    val priceAmountMinor: Long = 0L,
    val currency: String = "RUB",
    val items: List<MealPlanItemDtoProto> = emptyList(),
)

data class UpdateMealPlanResponse(
    val mealPlan: MealPlanTemplateDtoProto = MealPlanTemplateDtoProto(),
)

data class DeleteMealPlanRequest(
    val planId: String = "",
)

data class DeleteMealPlanResponse(
    val success: Boolean = false,
)

// =============================================================================
// Requests & Responses: Coaching & Entitlements
// =============================================================================

data class InviteStudentRequest(
    val studentId: String = "",
    val canViewDiary: Boolean = false,
)

data class InviteStudentResponse(
    val link: CoachStudentLinkDtoProto = CoachStudentLinkDtoProto(),
)

data class RespondCoachInviteRequest(
    val linkId: String = "",
    val accept: Boolean = false,
)

data class RespondCoachInviteResponse(
    val link: CoachStudentLinkDtoProto = CoachStudentLinkDtoProto(),
)

data class ListCoachLinksRequest(
    val statusFilter: CoachLinkStatusProto = CoachLinkStatusProto.COACH_LINK_STATUS_UNSPECIFIED,
)

data class ListCoachLinksResponse(
    val links: List<CoachStudentLinkDtoProto> = emptyList(),
)

data class GrantAccessRequest(
    val userId: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val grantSource: GrantSourceProto = GrantSourceProto.GRANT_SOURCE_UNSPECIFIED,
    val expiresAt: TimestampProto? = null,
)

data class GrantAccessResponse(
    val grant: AccessGrantDtoProto = AccessGrantDtoProto(),
)

data class RevokeAccessRequest(
    val grantId: String = "",
)

data class RevokeAccessResponse(
    val success: Boolean = false,
)

data class CheckCoachAccessRequest(
    val coachId: String = "",
    val studentId: String = "",
)

data class CheckCoachAccessResponse(
    val hasAccess: Boolean = false,
    val canViewDiary: Boolean = false,
)
