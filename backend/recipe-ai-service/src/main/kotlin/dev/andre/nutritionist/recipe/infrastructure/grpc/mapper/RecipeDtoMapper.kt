package dev.andre.nutritionist.recipe.infrastructure.grpc.mapper

import com.budgetntr.proto.recipe.v1.GenerateMealPlanRequest
import com.budgetntr.proto.recipe.v1.GenerateMealPlanResponse
import dev.andre.nutritionist.recipe.domain.model.GeneratedMeal
import dev.andre.nutritionist.recipe.domain.model.GeneratedMealPlan
import dev.andre.nutritionist.recipe.domain.model.MealPlanRequest
import dev.andre.nutritionist.recipe.domain.model.MenuIngredient
import dev.andre.nutritionist.recipe.domain.model.TargetNutrition
import dev.andre.nutritionist.recipe.domain.model.TargetPeriod
import com.budgetntr.proto.recipe.v1.GeneratedMeal as ProtoGeneratedMeal
import com.budgetntr.proto.recipe.v1.MenuIngredient as ProtoMenuIngredient
import com.budgetntr.proto.recipe.v1.TargetNutrition as ProtoTargetNutrition
import com.budgetntr.proto.recipe.v1.TargetPeriod as ProtoTargetPeriod

fun GenerateMealPlanRequest.toDomain(): MealPlanRequest =
    MealPlanRequest(
        period = period.toDomain(),
        targetNutrition = targetNutrition.toDomain(),
        maxBudgetMinor = maxBudgetMinor,
        currency = currency.ifBlank { "RUB" },
        regionId = regionId.takeIf { it.isNotBlank() },
        preferredProducts = preferredProductsList,
        excludedProducts = excludedProductsList,
        userNote = userNote,
    )

fun ProtoTargetPeriod.toDomain(): TargetPeriod =
    when (this) {
        ProtoTargetPeriod.TARGET_PERIOD_DAY -> TargetPeriod.DAY
        ProtoTargetPeriod.TARGET_PERIOD_BREAKFAST -> TargetPeriod.BREAKFAST
        ProtoTargetPeriod.TARGET_PERIOD_LUNCH -> TargetPeriod.LUNCH
        ProtoTargetPeriod.TARGET_PERIOD_DINNER -> TargetPeriod.DINNER
        ProtoTargetPeriod.TARGET_PERIOD_SNACK -> TargetPeriod.SNACK
        else -> TargetPeriod.UNSPECIFIED
    }

fun TargetPeriod.toProto(): ProtoTargetPeriod =
    when (this) {
        TargetPeriod.DAY -> ProtoTargetPeriod.TARGET_PERIOD_DAY
        TargetPeriod.BREAKFAST -> ProtoTargetPeriod.TARGET_PERIOD_BREAKFAST
        TargetPeriod.LUNCH -> ProtoTargetPeriod.TARGET_PERIOD_LUNCH
        TargetPeriod.DINNER -> ProtoTargetPeriod.TARGET_PERIOD_DINNER
        TargetPeriod.SNACK -> ProtoTargetPeriod.TARGET_PERIOD_SNACK
        TargetPeriod.UNSPECIFIED -> ProtoTargetPeriod.TARGET_PERIOD_UNSPECIFIED
    }

fun ProtoTargetNutrition.toDomain(): TargetNutrition =
    TargetNutrition(
        calories = calories,
        proteinsGrams = proteinsGrams,
        fatsGrams = fatsGrams,
        carbsGrams = carbsGrams,
    )

fun TargetNutrition.toProto(): ProtoTargetNutrition =
    ProtoTargetNutrition
        .newBuilder()
        .setCalories(calories)
        .setProteinsGrams(proteinsGrams)
        .setFatsGrams(fatsGrams)
        .setCarbsGrams(carbsGrams)
        .build()

fun MenuIngredient.toProto(): ProtoMenuIngredient =
    ProtoMenuIngredient
        .newBuilder()
        .setProductId(productId)
        .setName(name)
        .setGrams(grams)
        .build()

fun GeneratedMeal.toProto(): ProtoGeneratedMeal =
    ProtoGeneratedMeal
        .newBuilder()
        .setType(type)
        .setTitle(title)
        .addAllIngredients(ingredients.map { it.toProto() })
        .setNutrition(nutrition.toProto())
        .setEstimatedCostMinor(estimatedCostMinor)
        .setCookingTimeMinutes(cookingTimeMinutes)
        .build()

fun GeneratedMealPlan.toProto(): GenerateMealPlanResponse =
    GenerateMealPlanResponse
        .newBuilder()
        .setPlanId(planId)
        .setPeriod(period.toProto())
        .addAllMeals(meals.map { it.toProto() })
        .setTotalNutrition(totalNutrition.toProto())
        .setEstimatedCostMinor(estimatedCostMinor)
        .setCurrency(currency)
        .setCookingInstructions(cookingInstructions)
        .setSource(source)
        .build()
