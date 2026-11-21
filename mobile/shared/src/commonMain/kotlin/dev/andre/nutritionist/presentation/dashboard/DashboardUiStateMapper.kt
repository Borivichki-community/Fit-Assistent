package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.calculator.DailyNutritionBalanceCalculator
import dev.andre.nutritionist.domain.model.ConsumedNutrition
import dev.andre.nutritionist.domain.model.DailyDiary
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.FoodItemId
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * Создаёт тестовый дневник с образцовой записью (Йогурт епика) для отображения по умолчанию.
 */
fun defaultSampleDiary(userId: UserId = UserId("testuser")): DailyDiary = DailyDiary(
    userId = userId,
    date = LocalDate(2026, 1, 1),
    streakDays = 1,
    entries = listOf(
        DiaryEntry(
            id = DiaryEntryId("dinner-epica-1"),
            userId = userId,
            foodItemId = FoodItemId("food-epica-1"),
            foodName = "Йогурт епика",
            portionGrams = 130.0,
            calories = 85,
            proteins = 4.93,
            fats = 1.25,
            carbs = 13.8,
            mealType = MealType.DINNER,
            date = LocalDate(2026, 1, 1),
            recordedAt = Instant.fromEpochMilliseconds(0),
            idempotencyKey = IdempotencyKey("sample-epica-1"),
            portionDescription = "1 порция",
        ),
    ),
)

/**
 * Чистый маппер [DashboardState] в [DashboardUiState].
 * Подготавливает все текстовые метки и форматирование на уровне presentation-слоя в :shared.
 */
class DashboardUiStateMapper : UiStateMapper<DashboardState, DashboardUiState> {

    override fun map(state: DashboardState): DashboardUiState {
        return when (val content = state.content) {
            DashboardState.Content.Loading -> DashboardUiState(content = DashboardUiState.Content.Loading)
            is DashboardState.Content.Error -> DashboardUiState(content = DashboardUiState.Content.Error(content.message))
            is DashboardState.Content.Success -> {
                val data = mapSuccess(
                    profile = content.profile,
                    diary = content.diary,
                    expandedMeals = content.expandedMeals,
                    showCompleteProfileBanner = content.showCompleteProfileBanner,
                )
                DashboardUiState(content = DashboardUiState.Content.Success(data))
            }
        }
    }

    fun map(
        profile: Profile?,
        diary: DailyDiary? = null,
        customMeals: List<MealSectionUiModel>? = null,
        expandedMeals: Set<MealType> = setOf(MealType.DINNER),
        streakDays: Int = 1,
        selectedDateLabel: String = "Сегодня",
        showCompleteProfileBanner: Boolean = false,
    ): DashboardUiState {
        val baseDiary = diary ?: defaultSampleDiary(profile?.userId ?: UserId("testuser"))
        val effectiveDiary = if (baseDiary.streakDays != streakDays) {
            baseDiary.copy(streakDays = streakDays)
        } else {
            baseDiary
        }
        val data = mapSuccess(
            profile = profile,
            diary = effectiveDiary,
            customMeals = customMeals,
            expandedMeals = expandedMeals,
            showCompleteProfileBanner = showCompleteProfileBanner,
            selectedDateLabel = selectedDateLabel,
        )
        return DashboardUiState(content = DashboardUiState.Content.Success(data))
    }

    fun mapSuccess(
        profile: Profile?,
        diary: DailyDiary,
        customMeals: List<MealSectionUiModel>? = null,
        expandedMeals: Set<MealType> = setOf(MealType.DINNER),
        showCompleteProfileBanner: Boolean = false,
        selectedDateLabel: String = "Сегодня",
    ): DashboardSuccessUiModel {
        val effectiveDiary = if (diary.entries.isEmpty() && customMeals == null) {
            defaultSampleDiary(diary.userId)
        } else {
            diary
        }

        val allEntries = effectiveDiary.entries
        val caloriesConsumed = if (customMeals != null) {
            customMeals.flatMap { it.entries }.sumOf { it.calories }
        } else {
            allEntries.sumOf { it.calories }
        }
        val proteinsConsumed = if (customMeals != null) {
            customMeals.flatMap { it.entries }.sumOf { it.proteins }
        } else {
            allEntries.sumOf { it.proteins }
        }
        val fatsConsumed = if (customMeals != null) {
            customMeals.flatMap { it.entries }.sumOf { it.fats }
        } else {
            allEntries.sumOf { it.fats }
        }
        val carbsConsumed = if (customMeals != null) {
            customMeals.flatMap { it.entries }.sumOf { it.carbs }
        } else {
            allEntries.sumOf { it.carbs }
        }

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

        val meals = customMeals ?: MealType.entries.map { mealType ->
            val typeEntries = allEntries.filter { it.mealType == mealType }
            MealSectionUiModel(
                type = mealType,
                title = when (mealType) {
                    MealType.BREAKFAST -> "Завтрак"
                    MealType.LUNCH -> "Обед"
                    MealType.DINNER -> "Ужин"
                    MealType.SNACK -> "Перекус/Другое"
                },
                caloriesTotal = typeEntries.sumOf { it.calories },
                entries = typeEntries.map { entry ->
                    MealEntryUiModel(
                        id = entry.id.raw,
                        title = entry.foodName,
                        portionDescription = entry.portionDescription,
                        calories = entry.calories,
                        proteins = entry.proteins,
                        fats = entry.fats,
                        carbs = entry.carbs,
                    )
                },
                isExpanded = expandedMeals.contains(mealType),
            )
        }

        return DashboardSuccessUiModel(
            selectedDateLabel = selectedDateLabel,
            streakDays = effectiveDiary.streakDays,
            streakLabel = "Серия записи еды: ${effectiveDiary.streakDays} дн.",
            weekDays = defaultWeekDays(),
            nutritionSummary = nutritionSummary,
            meals = meals,
            showCompleteProfileBanner = showCompleteProfileBanner,
        )
    }
}
