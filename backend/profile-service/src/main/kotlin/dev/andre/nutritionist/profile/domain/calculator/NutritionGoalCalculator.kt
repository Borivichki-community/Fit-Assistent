package dev.andre.nutritionist.profile.domain.calculator

import dev.andre.nutritionist.profile.domain.model.ActivityLevel
import dev.andre.nutritionist.profile.domain.model.Gender
import dev.andre.nutritionist.profile.domain.model.GoalType
import dev.andre.nutritionist.profile.domain.model.NutritionGoal
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Чистый доменный калькулятор суточной нормы калорий и макронутриентов (КБЖУ).
 *
 * Архитектурное решение (AGENTS.md / Finding 9):
 * Реализован как stateless `object` без внешнего состояния или зависимостей от I/O.
 * Все формулы (Миффлина — Сан-Жеора, PAL, макронутриенты) являются детерминированными
 * математическими функциями. При необходимости введения внешних динамических стратегий
 * (например, ML-моделей рекомендаций или подгружаемых коэффициентов ВОЗ) класс может быть
 * преобразован в интерфейс со стратегиями, инжектируемыми через Spring DI.
 *
 * Расчёт базового обмена веществ (BMR) по формуле Миффлина — Сан-Жеора:
 * - Мужчины: 10 * weight + 6.25 * height - 5 * age + 5
 * - Женщины: 10 * weight + 6.25 * height - 5 * age - 161
 * - Неизвестный пол: медианное значение константы (-78)
 *
 * Коэффициенты физической активности (PAL):
 * - SEDENTARY: 1.2
 * - LIGHT: 1.375
 * - MODERATE: 1.55
 * - VERY_ACTIVE: 1.725
 * - UNKNOWN: 1.2
 *
 * Корректировка калоража под цель (GoalType):
 * - LOSE_WEIGHT: дефицит 18% (калории * 0.82)
 * - MAINTAIN: поддержание (калории * 1.0)
 * - GAIN_WEIGHT: профицит 12% (калории * 1.12)
 * - UNKNOWN: поддержание (калории * 1.0)
 *
 * Нормы макронутриентов:
 * - Белки: 2.0 г/кг веса (похудение/набор) или 1.5 г/кг (поддержание)
 * - Жиры: 1.0 г/кг веса, но не менее 25% от суточного калоража
 * - Углеводы: остаток суточного калоража ((калории - (белки * 4 + жиры * 9)) / 4)
 */
object NutritionGoalCalculator {
    const val DEFAULT_AGE_YEARS: Int = 25
    private const val DEFAULT_FALLBACK_HEIGHT_CM = 170
    private const val MIN_TARGET_CALORIES = 500

    private val MIN_BMR = BigDecimal("500.00")
    private val BMR_WEIGHT_FACTOR = BigDecimal("10")
    private val BMR_HEIGHT_FACTOR = BigDecimal("6.25")
    private val BMR_AGE_FACTOR = BigDecimal("5")

    private val BMR_OFFSET_MALE = BigDecimal("5")
    private val BMR_OFFSET_FEMALE = BigDecimal("-161")
    private val BMR_OFFSET_UNKNOWN = BigDecimal("-78")

    private val PAL_SEDENTARY = BigDecimal("1.20")
    private val PAL_LIGHT = BigDecimal("1.375")
    private val PAL_MODERATE = BigDecimal("1.55")
    private val PAL_VERY_ACTIVE = BigDecimal("1.725")
    private val PAL_DEFAULT = BigDecimal("1.20")

    private val GOAL_FACTOR_LOSE = BigDecimal("0.82")
    private val GOAL_FACTOR_MAINTAIN = BigDecimal("1.00")
    private val GOAL_FACTOR_GAIN = BigDecimal("1.12")

    private val PROTEIN_RATIO_CHANGE = BigDecimal("2.0")
    private val PROTEIN_RATIO_MAINTAIN = BigDecimal("1.5")

    private val FAT_RATIO_BASE = BigDecimal("1.0")
    private val MIN_FAT_CALORIES_SHARE = BigDecimal("0.25")

    private val CALORIES_PER_GRAM_PROTEIN = BigDecimal("4")
    private val CALORIES_PER_GRAM_FAT = BigDecimal("9")
    private val CALORIES_PER_GRAM_CARB = BigDecimal("4")

    private val TARGET_WEIGHT_LOSE_FACTOR = BigDecimal("0.90")
    private val TARGET_WEIGHT_GAIN_FACTOR = BigDecimal("1.10")

    private val DEFAULT_FALLBACK_WEIGHT_KG = BigDecimal("70.00")

    @Suppress("LongParameterList")
    fun calculate(
        heightCm: Int,
        weightKg: BigDecimal,
        gender: Gender,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        ageYears: Int = DEFAULT_AGE_YEARS,
        targetWeightKg: BigDecimal? = null,
    ): NutritionGoal {
        val safeHeight = if (heightCm <= 0) DEFAULT_FALLBACK_HEIGHT_CM else heightCm
        val safeWeight = if (weightKg <= BigDecimal.ZERO) DEFAULT_FALLBACK_WEIGHT_KG else weightKg
        val safeAge = if (ageYears <= 0) DEFAULT_AGE_YEARS else ageYears

        val bmr = calculateBmr(safeHeight, safeWeight, gender, safeAge)
        val pal = resolvePal(activityLevel)
        val goalFactor = resolveGoalFactor(goalType)

        val tdee = bmr.multiply(pal)
        val targetCaloriesDecimal = tdee.multiply(goalFactor)
        val rawCalories = targetCaloriesDecimal.setScale(0, RoundingMode.HALF_UP).toInt()
        val targetCalories = if (rawCalories < MIN_TARGET_CALORIES) MIN_TARGET_CALORIES else rawCalories

        val finalTargetWeight = calculateTargetWeight(safeWeight, goalType, targetWeightKg)
        val (proteins, fats, carbs) = calculateMacronutrients(safeWeight, goalType, targetCalories)

        return NutritionGoal(
            targetCalories = targetCalories,
            targetProteinsGrams = proteins,
            targetFatsGrams = fats,
            targetCarbsGrams = carbs,
            targetWeightKg = finalTargetWeight,
        )
    }

    private fun calculateBmr(
        heightCm: Int,
        weightKg: BigDecimal,
        gender: Gender,
        ageYears: Int,
    ): BigDecimal {
        val weightPart = weightKg.multiply(BMR_WEIGHT_FACTOR)
        val heightPart = BigDecimal.valueOf(heightCm.toLong()).multiply(BMR_HEIGHT_FACTOR)
        val agePart = BigDecimal.valueOf(ageYears.toLong()).multiply(BMR_AGE_FACTOR)

        val genderOffset =
            when (gender) {
                Gender.MALE -> BMR_OFFSET_MALE
                Gender.FEMALE -> BMR_OFFSET_FEMALE
                Gender.UNKNOWN -> BMR_OFFSET_UNKNOWN
            }

        val rawBmr = weightPart.add(heightPart).subtract(agePart).add(genderOffset)
        return if (rawBmr < MIN_BMR) MIN_BMR else rawBmr
    }

    private fun resolvePal(activityLevel: ActivityLevel): BigDecimal =
        when (activityLevel) {
            ActivityLevel.SEDENTARY -> PAL_SEDENTARY
            ActivityLevel.LIGHT -> PAL_LIGHT
            ActivityLevel.MODERATE -> PAL_MODERATE
            ActivityLevel.VERY_ACTIVE -> PAL_VERY_ACTIVE
            ActivityLevel.UNKNOWN -> PAL_DEFAULT
        }

    private fun resolveGoalFactor(goalType: GoalType): BigDecimal =
        when (goalType) {
            GoalType.LOSE_WEIGHT -> GOAL_FACTOR_LOSE
            GoalType.MAINTAIN -> GOAL_FACTOR_MAINTAIN
            GoalType.GAIN_WEIGHT -> GOAL_FACTOR_GAIN
            GoalType.UNKNOWN -> GOAL_FACTOR_MAINTAIN
        }

    private fun calculateTargetWeight(
        weightKg: BigDecimal,
        goalType: GoalType,
        customTargetWeight: BigDecimal?,
    ): BigDecimal {
        if (customTargetWeight != null && customTargetWeight > BigDecimal.ZERO) {
            return customTargetWeight.setScale(2, RoundingMode.HALF_UP)
        }
        return when (goalType) {
            GoalType.LOSE_WEIGHT -> weightKg.multiply(TARGET_WEIGHT_LOSE_FACTOR).setScale(2, RoundingMode.HALF_UP)
            GoalType.MAINTAIN -> weightKg.setScale(2, RoundingMode.HALF_UP)
            GoalType.GAIN_WEIGHT -> weightKg.multiply(TARGET_WEIGHT_GAIN_FACTOR).setScale(2, RoundingMode.HALF_UP)
            GoalType.UNKNOWN -> weightKg.setScale(2, RoundingMode.HALF_UP)
        }
    }

    private fun calculateMacronutrients(
        weightKg: BigDecimal,
        goalType: GoalType,
        targetCalories: Int,
    ): Triple<BigDecimal, BigDecimal, BigDecimal> {
        val proteinRatio =
            when (goalType) {
                GoalType.LOSE_WEIGHT,
                GoalType.GAIN_WEIGHT,
                -> PROTEIN_RATIO_CHANGE

                GoalType.MAINTAIN,
                GoalType.UNKNOWN,
                -> PROTEIN_RATIO_MAINTAIN
            }
        val targetProteinsGrams = weightKg.multiply(proteinRatio).setScale(2, RoundingMode.HALF_UP)

        val baseFats = weightKg.multiply(FAT_RATIO_BASE).setScale(2, RoundingMode.HALF_UP)
        val minFatCalories = BigDecimal.valueOf(targetCalories.toLong()).multiply(MIN_FAT_CALORIES_SHARE)
        val minFatsFromCalories = minFatCalories.divide(CALORIES_PER_GRAM_FAT, 2, RoundingMode.HALF_UP)
        val targetFatsGrams = if (baseFats < minFatsFromCalories) minFatsFromCalories else baseFats

        val proteinCalories = targetProteinsGrams.multiply(CALORIES_PER_GRAM_PROTEIN)
        val fatCalories = targetFatsGrams.multiply(CALORIES_PER_GRAM_FAT)
        val allocatedCalories = proteinCalories.add(fatCalories)
        val totalCaloriesDecimal = BigDecimal.valueOf(targetCalories.toLong())
        val remainingCalories = totalCaloriesDecimal.subtract(allocatedCalories)

        val targetCarbsGrams =
            if (remainingCalories > BigDecimal.ZERO) {
                remainingCalories.divide(CALORIES_PER_GRAM_CARB, 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            }

        return Triple(targetProteinsGrams, targetFatsGrams, targetCarbsGrams)
    }
}
