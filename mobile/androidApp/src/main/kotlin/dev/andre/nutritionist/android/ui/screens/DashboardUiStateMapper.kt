package dev.andre.nutritionist.android.ui.screens

import dev.andre.nutritionist.domain.calculator.DailyNutritionBalanceCalculator
import dev.andre.nutritionist.domain.model.ConsumedNutrition
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile

/**
 * Чистый маппер для формирования [DashboardUiState] из доменного профиля и записей дневника.
 * Использует [DailyNutritionBalanceCalculator] для мгновенного и детерминированного расчёта КБЖУ.
 */
class DashboardUiStateMapper {

    fun map(
        profile: Profile?,
        customMeals: List<MealSectionUiModel>? = null,
        expandedMeals: Set<MealType> = setOf(MealType.DINNER),
        streakDays: Int = 1,
        selectedDateLabel: String = "Сегодня",
        showCompleteProfileBanner: Boolean = false,
    ): DashboardUiState {
        val meals = customMeals ?: createDefaultMeals(expandedMeals)

        val allEntries = meals.flatMap { it.entries }
        val caloriesConsumed = allEntries.sumOf { it.calories }
        val proteinsConsumed = allEntries.sumOf { it.proteins }
        val fatsConsumed = allEntries.sumOf { it.fats }
        val carbsConsumed = allEntries.sumOf { it.carbs }

        val consumed = ConsumedNutrition(
            calories = caloriesConsumed,
            proteinsGrams = proteinsConsumed,
            fatsGrams = fatsConsumed,
            carbsGrams = carbsConsumed,
        )

        val effectiveGoal = profile?.nutritionGoal?.takeIf { it.targetCalories > 0 }
            ?: NutritionGoal(
                targetCalories = 2000,
                targetProteinsGrams = 120.0,
                targetFatsGrams = 70.0,
                targetCarbsGrams = 250.0,
            )

        val balance = DailyNutritionBalanceCalculator.calculate(
            goal = effectiveGoal,
            consumed = consumed,
        )

        val fatsUi = MacroNutrientUiModel(
            label = "Жиры",
            currentGrams = balance.fats.consumedGrams,
            targetGrams = balance.fats.targetGrams,
            remainingGrams = balance.fats.remainingGrams,
            isExceeded = balance.fats.isExceeded,
            formattedValue = "${formatMacroGrams(balance.fats.consumedGrams)} / ${formatMacroGrams(balance.fats.targetGrams)} г",
            displayRemaining = if (balance.fats.isExceeded) {
                "Перебор: ${formatMacroGrams(balance.fats.consumedGrams - balance.fats.targetGrams)} г"
            } else {
                "Осталось: ${formatMacroGrams(balance.fats.remainingGrams)} г"
            },
        )

        val carbsUi = MacroNutrientUiModel(
            label = "Углев",
            currentGrams = balance.carbs.consumedGrams,
            targetGrams = balance.carbs.targetGrams,
            remainingGrams = balance.carbs.remainingGrams,
            isExceeded = balance.carbs.isExceeded,
            formattedValue = "${formatMacroGrams(balance.carbs.consumedGrams)} / ${formatMacroGrams(balance.carbs.targetGrams)} г",
            displayRemaining = if (balance.carbs.isExceeded) {
                "Перебор: ${formatMacroGrams(balance.carbs.consumedGrams - balance.carbs.targetGrams)} г"
            } else {
                "Осталось: ${formatMacroGrams(balance.carbs.remainingGrams)} г"
            },
        )

        val proteinsUi = MacroNutrientUiModel(
            label = "Белк",
            currentGrams = balance.proteins.consumedGrams,
            targetGrams = balance.proteins.targetGrams,
            remainingGrams = balance.proteins.remainingGrams,
            isExceeded = balance.proteins.isExceeded,
            formattedValue = "${formatMacroGrams(balance.proteins.consumedGrams)} / ${formatMacroGrams(balance.proteins.targetGrams)} г",
            displayRemaining = if (balance.proteins.isExceeded) {
                "Перебор: ${formatMacroGrams(balance.proteins.consumedGrams - balance.proteins.targetGrams)} г"
            } else {
                "Осталось: ${formatMacroGrams(balance.proteins.remainingGrams)} г"
            },
        )

        val nutritionSummary = NutritionSummaryUiModel(
            caloriesConsumed = balance.consumedCalories,
            caloriesTarget = balance.targetCalories,
            caloriesRemaining = balance.remainingCalories,
            percentCompleted = balance.caloriesProgressPercent,
            isCaloriesExceeded = balance.isCaloriesExceeded,
            caloriesRemainingLabel = if (balance.isCaloriesExceeded) {
                "Перебор ${-balance.remainingCalories} ккал"
            } else {
                "Осталось ${balance.remainingCalories} ккал"
            },
            fats = fatsUi,
            carbs = carbsUi,
            proteins = proteinsUi,
        )

        return DashboardUiState(
            selectedDateLabel = selectedDateLabel,
            streakDays = streakDays,
            streakLabel = "Серия записи еды: $streakDays дн.",
            weekDays = defaultWeekDays(),
            nutritionSummary = nutritionSummary,
            meals = meals,
            showCompleteProfileBanner = showCompleteProfileBanner,
        )
    }

    fun createDefaultMeals(expandedMeals: Set<MealType> = setOf(MealType.DINNER)): List<MealSectionUiModel> {
        val sampleDinnerEntries = listOf(
            MealEntryUiModel(
                id = "dinner-epica-1",
                title = "Йогурт епика",
                portionDescription = "1 порция",
                calories = 85,
                proteins = 4.93,
                fats = 1.25,
                carbs = 13.8,
            ),
        )

        return listOf(
            MealSectionUiModel(
                type = MealType.BREAKFAST,
                title = "Завтрак",
                caloriesTotal = 0,
                entries = emptyList(),
                isExpanded = expandedMeals.contains(MealType.BREAKFAST),
            ),
            MealSectionUiModel(
                type = MealType.LUNCH,
                title = "Обед",
                caloriesTotal = 0,
                entries = emptyList(),
                isExpanded = expandedMeals.contains(MealType.LUNCH),
            ),
            MealSectionUiModel(
                type = MealType.DINNER,
                title = "Ужин",
                caloriesTotal = sampleDinnerEntries.sumOf { it.calories },
                entries = sampleDinnerEntries,
                isExpanded = expandedMeals.contains(MealType.DINNER),
            ),
            MealSectionUiModel(
                type = MealType.SNACK,
                title = "Перекус/Другое",
                caloriesTotal = 0,
                entries = emptyList(),
                isExpanded = expandedMeals.contains(MealType.SNACK),
            ),
        )
    }
}
