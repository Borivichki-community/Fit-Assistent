package dev.andre.nutritionist.domain.model

enum class TargetPeriod {
    UNSPECIFIED,
    DAY,
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
}

enum class MenuSource {
    UNSPECIFIED,
    GENERATED,
    CACHE,
    FALLBACK,
}

data class MealPlan(
    val id: String,
    val period: TargetPeriod,
    val meals: List<Recipe>,
    val totalNutrition: NutritionFacts,
    val estimatedCost: Money,
    val cookingInstructions: String? = null,
    val source: MenuSource = MenuSource.GENERATED,
)
