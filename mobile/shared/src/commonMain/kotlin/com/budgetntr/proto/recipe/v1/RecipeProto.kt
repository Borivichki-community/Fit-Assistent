package com.budgetntr.proto.recipe.v1

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
