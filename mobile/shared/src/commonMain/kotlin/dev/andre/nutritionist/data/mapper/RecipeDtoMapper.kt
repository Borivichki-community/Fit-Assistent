package dev.andre.nutritionist.data.mapper

import com.budgetntr.proto.recipe.v1.GenerateMealPlanRequest
import com.budgetntr.proto.recipe.v1.GenerateMealPlanResponse
import com.budgetntr.proto.recipe.v1.GeneratedMealProto
import com.budgetntr.proto.recipe.v1.MealTypeProto
import com.budgetntr.proto.recipe.v1.MenuIngredientProto
import com.budgetntr.proto.recipe.v1.MenuSourceProto
import com.budgetntr.proto.recipe.v1.TargetNutritionProto
import com.budgetntr.proto.recipe.v1.TargetPeriodProto
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.MenuSource
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams

object RecipeDtoMapper {

    fun GenerateBudgetMenuParams.toRequest(): GenerateMealPlanRequest = GenerateMealPlanRequest(
        period = when (period) {
            TargetPeriod.DAY -> TargetPeriodProto.TARGET_PERIOD_DAY
            TargetPeriod.BREAKFAST -> TargetPeriodProto.TARGET_PERIOD_BREAKFAST
            TargetPeriod.LUNCH -> TargetPeriodProto.TARGET_PERIOD_LUNCH
            TargetPeriod.DINNER -> TargetPeriodProto.TARGET_PERIOD_DINNER
            TargetPeriod.SNACK -> TargetPeriodProto.TARGET_PERIOD_SNACK
            TargetPeriod.UNSPECIFIED -> TargetPeriodProto.TARGET_PERIOD_UNSPECIFIED
        },
        targetNutrition = TargetNutritionProto(
            calories = targetCalories,
            proteinsGrams = targetProteinsGrams,
            fatsGrams = targetFatsGrams,
            carbsGrams = targetCarbsGrams,
        ),
        maxBudgetMinor = maxBudget.amountMinor,
        currency = Money.CURRENCY_RUB,
        regionId = regionId?.raw ?: "",
        preferredProducts = preferredProductIds.map { it.raw },
        excludedProducts = excludedProductIds.map { it.raw },
        userNote = userNote,
    )

    fun GenerateMealPlanResponse.toDomain(): MealPlan = MealPlan(
        id = planId,
        period = when (period) {
            TargetPeriodProto.TARGET_PERIOD_DAY -> TargetPeriod.DAY
            TargetPeriodProto.TARGET_PERIOD_BREAKFAST -> TargetPeriod.BREAKFAST
            TargetPeriodProto.TARGET_PERIOD_LUNCH -> TargetPeriod.LUNCH
            TargetPeriodProto.TARGET_PERIOD_DINNER -> TargetPeriod.DINNER
            TargetPeriodProto.TARGET_PERIOD_SNACK -> TargetPeriod.SNACK
            TargetPeriodProto.TARGET_PERIOD_UNSPECIFIED,
            TargetPeriodProto.UNRECOGNIZED,
            -> TargetPeriod.UNSPECIFIED
        },
        meals = meals.map { it.toDomain() },
        totalNutrition = totalNutrition.toDomain(),
        estimatedCost = Money.fromMinor(estimatedCostMinor),
        cookingInstructions = cookingInstructions.ifEmpty { null },
        source = when (source) {
            MenuSourceProto.MENU_SOURCE_GENERATED -> MenuSource.GENERATED
            MenuSourceProto.MENU_SOURCE_CACHE -> MenuSource.CACHE
            MenuSourceProto.MENU_SOURCE_FALLBACK -> MenuSource.FALLBACK
            MenuSourceProto.MENU_SOURCE_UNSPECIFIED,
            MenuSourceProto.UNRECOGNIZED,
            -> MenuSource.UNSPECIFIED
        },
    )

    fun GeneratedMealProto.toDomain(): Recipe = Recipe(
        id = RecipeId(title.hashCode().toString()),
        title = title,
        mealType = when (type) {
            MealTypeProto.MEAL_TYPE_BREAKFAST -> MealType.BREAKFAST
            MealTypeProto.MEAL_TYPE_LUNCH -> MealType.LUNCH
            MealTypeProto.MEAL_TYPE_DINNER -> MealType.DINNER
            MealTypeProto.MEAL_TYPE_SNACK -> MealType.SNACK
            MealTypeProto.MEAL_TYPE_UNSPECIFIED,
            MealTypeProto.UNRECOGNIZED,
            -> MealType.SNACK
        },
        ingredients = ingredients.map { it.toDomain() },
        nutrition = nutrition.toDomain(),
        estimatedCost = Money.fromMinor(estimatedCostMinor),
        cookingTimeMinutes = cookingTimeMinutes,
    )

    fun MenuIngredientProto.toDomain(): RecipeIngredient = RecipeIngredient(
        productId = if (productId.isNotEmpty()) ProductId(productId) else null,
        name = name,
        weightGrams = grams.toDouble(),
        cost = null,
    )

    fun TargetNutritionProto.toDomain(): NutritionFacts = NutritionFacts(
        calories = calories,
        proteins = proteinsGrams,
        fats = fatsGrams,
        carbs = carbsGrams,
    )
}
