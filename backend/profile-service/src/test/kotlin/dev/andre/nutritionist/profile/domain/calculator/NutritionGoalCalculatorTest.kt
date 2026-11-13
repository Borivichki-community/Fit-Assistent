package dev.andre.nutritionist.profile.domain.calculator

import dev.andre.nutritionist.profile.domain.model.ActivityLevel
import dev.andre.nutritionist.profile.domain.model.Gender
import dev.andre.nutritionist.profile.domain.model.GoalType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

class NutritionGoalCalculatorTest {
    // ── Базовый обмен (BMR) и расчёт для мужчин ──────────────────────────────

    @Test
    fun `male maintain sedentary calculation matches Mifflin-St Jeor formula`() {
        // Рост: 180 см, Вес: 80 кг, Возраст: 25 лет
        // BMR = 10 * 80 + 6.25 * 180 - 5 * 25 + 5 = 800 + 1125 - 125 + 5 = 1805
        // TDEE (SEDENTARY, 1.2) = 1805 * 1.2 = 2166 ккал
        // MAINTAIN (1.0) = 2166 ккал
        // Белки: 80 * 1.5 = 120 г (480 ккал)
        // Жиры: max(80 * 1.0, 2166 * 0.25 / 9 = 60.17) = 80 г (720 ккал)
        // Углеводы: (2166 - 480 - 720) / 4 = 966 / 4 = 241.5 г (966 ккал)
        val weight = BigDecimal("80.00")
        val height = 180
        val age = 25

        val goal =
            NutritionGoalCalculator.calculate(
                heightCm = height,
                weightKg = weight,
                gender = Gender.MALE,
                activityLevel = ActivityLevel.SEDENTARY,
                goalType = GoalType.MAINTAIN,
                ageYears = age,
            )

        assertEquals(2166, goal.targetCalories)
        assertEquals(0, BigDecimal("120.00").compareTo(goal.targetProteinsGrams))
        assertEquals(0, BigDecimal("80.00").compareTo(goal.targetFatsGrams))
        assertEquals(0, BigDecimal("241.50").compareTo(goal.targetCarbsGrams))
        assertEquals(0, BigDecimal("80.00").compareTo(goal.targetWeightKg))
    }

    // ── Базовый обмен (BMR) и расчёт для женщин ──────────────────────────────

    @Test
    fun `female lose weight moderate calculation matches Mifflin-St Jeor formula`() {
        // Рост: 165 см, Вес: 60 кг, Возраст: 25 лет
        // BMR = 10 * 60 + 6.25 * 165 - 5 * 25 - 161 = 600 + 1031.25 - 125 - 161 = 1345.25
        // TDEE (MODERATE, 1.55) = 1345.25 * 1.55 = 2085.1375
        // LOSE_WEIGHT (0.82) = 2085.1375 * 0.82 = 1709.81275 -> 1710 ккал
        // Белки: 60 * 2.0 = 120 г (480 ккал)
        // Жиры: max(60 * 1.0, 1710 * 0.25 / 9 = 47.50) = 60 г (540 ккал)
        // Углеводы: (1710 - 480 - 540) / 4 = 690 / 4 = 172.50 г (690 ккал)
        // Целевой вес: 60 * 0.90 = 54.00 кг
        val weight = BigDecimal("60.00")
        val height = 165
        val age = 25

        val goal =
            NutritionGoalCalculator.calculate(
                heightCm = height,
                weightKg = weight,
                gender = Gender.FEMALE,
                activityLevel = ActivityLevel.MODERATE,
                goalType = GoalType.LOSE_WEIGHT,
                ageYears = age,
            )

        assertEquals(1710, goal.targetCalories)
        assertEquals(0, BigDecimal("120.00").compareTo(goal.targetProteinsGrams))
        assertEquals(0, BigDecimal("60.00").compareTo(goal.targetFatsGrams))
        assertEquals(0, BigDecimal("172.50").compareTo(goal.targetCarbsGrams))
        assertEquals(0, BigDecimal("54.00").compareTo(goal.targetWeightKg))
    }

    // ── Баланс калорий КБЖУ для всех комбинаций активности и целей ───────────

    @ParameterizedTest
    @EnumSource(ActivityLevel::class)
    fun `calorie balance is preserved across all activity levels for males`(level: ActivityLevel) {
        val weight = BigDecimal("75.00")
        val goal =
            NutritionGoalCalculator.calculate(
                heightCm = 178,
                weightKg = weight,
                gender = Gender.MALE,
                activityLevel = level,
                goalType = GoalType.MAINTAIN,
            )

        val proteinKcal = goal.targetProteinsGrams.multiply(BigDecimal("4")).toDouble()
        val fatKcal = goal.targetFatsGrams.multiply(BigDecimal("9")).toDouble()
        val carbKcal = goal.targetCarbsGrams.multiply(BigDecimal("4")).toDouble()
        val totalCalculatedKcal = proteinKcal + fatKcal + carbKcal

        assertTrue(
            abs(totalCalculatedKcal - goal.targetCalories) <= 2.0,
            "Balance delta must be within 2 kcal, expected ${goal.targetCalories}, got $totalCalculatedKcal",
        )
    }

    @ParameterizedTest
    @EnumSource(GoalType::class)
    fun `calorie balance is preserved across all goal types for females`(goalType: GoalType) {
        val weight = BigDecimal("65.00")
        val goal =
            NutritionGoalCalculator.calculate(
                heightCm = 168,
                weightKg = weight,
                gender = Gender.FEMALE,
                activityLevel = ActivityLevel.LIGHT,
                goalType = goalType,
            )

        val proteinKcal = goal.targetProteinsGrams.multiply(BigDecimal("4")).toDouble()
        val fatKcal = goal.targetFatsGrams.multiply(BigDecimal("9")).toDouble()
        val carbKcal = goal.targetCarbsGrams.multiply(BigDecimal("4")).toDouble()
        val totalCalculatedKcal = proteinKcal + fatKcal + carbKcal

        assertTrue(
            abs(totalCalculatedKcal - goal.targetCalories) <= 2.0,
            "Balance delta must be within 2 kcal, expected ${goal.targetCalories}, got $totalCalculatedKcal",
        )
    }

    // ── Целевой вес (targetWeightKg) ──────────────────────────────────────────

    @Test
    fun `target weight is calculated based on goal type when not explicitly provided`() {
        val weight = BigDecimal("70.00")

        val lose =
            NutritionGoalCalculator.calculate(
                170,
                weight,
                Gender.MALE,
                ActivityLevel.SEDENTARY,
                GoalType.LOSE_WEIGHT,
            )
        assertEquals(0, BigDecimal("63.00").compareTo(lose.targetWeightKg)) // 70 * 0.90

        val maintain =
            NutritionGoalCalculator.calculate(
                170,
                weight,
                Gender.MALE,
                ActivityLevel.SEDENTARY,
                GoalType.MAINTAIN,
            )
        assertEquals(0, BigDecimal("70.00").compareTo(maintain.targetWeightKg)) // 70.00

        val gain =
            NutritionGoalCalculator.calculate(
                170,
                weight,
                Gender.MALE,
                ActivityLevel.SEDENTARY,
                GoalType.GAIN_WEIGHT,
            )
        assertEquals(0, BigDecimal("77.00").compareTo(gain.targetWeightKg)) // 70 * 1.10
    }

    @Test
    fun `custom target weight is preserved when explicitly provided`() {
        val explicitTargetWeight = BigDecimal("68.50")
        val goal =
            NutritionGoalCalculator.calculate(
                heightCm = 175,
                weightKg = BigDecimal("75.00"),
                gender = Gender.MALE,
                activityLevel = ActivityLevel.MODERATE,
                goalType = GoalType.LOSE_WEIGHT,
                targetWeightKg = explicitTargetWeight,
            )

        assertEquals(0, explicitTargetWeight.compareTo(goal.targetWeightKg))
    }

    // ── Граничные значения и защита от некорректных входных данных ────────────

    @Test
    fun `fallback values are used for non-positive body parameters`() {
        val goal =
            NutritionGoalCalculator.calculate(
                heightCm = -10,
                weightKg = BigDecimal("-50.00"),
                gender = Gender.UNKNOWN,
                activityLevel = ActivityLevel.UNKNOWN,
                goalType = GoalType.UNKNOWN,
                ageYears = -5,
            )

        assertTrue(goal.targetCalories >= 500)
        assertTrue(goal.targetProteinsGrams > BigDecimal.ZERO)
        assertTrue(goal.targetFatsGrams > BigDecimal.ZERO)
        assertTrue(goal.targetCarbsGrams >= BigDecimal.ZERO)
        assertTrue(goal.targetWeightKg > BigDecimal.ZERO)
    }

    @Test
    fun `fats grams are never lower than 25 percent of target calories`() {
        // Очень легкий вес при высокой активности: 40 кг, VERY_ACTIVE
        val goal =
            NutritionGoalCalculator.calculate(
                heightCm = 160,
                weightKg = BigDecimal("40.00"),
                gender = Gender.FEMALE,
                activityLevel = ActivityLevel.VERY_ACTIVE,
                goalType = GoalType.GAIN_WEIGHT,
            )

        val minFatKcal = BigDecimal.valueOf(goal.targetCalories.toLong()).multiply(BigDecimal("0.25"))
        val actualFatKcal = goal.targetFatsGrams.multiply(BigDecimal("9"))
        assertTrue(
            actualFatKcal >= minFatKcal.setScale(2, RoundingMode.HALF_UP),
            "Fats must provide at least 25% of calories: actual=$actualFatKcal, min=$minFatKcal",
        )
    }
}
