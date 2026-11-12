package dev.andre.nutritionist.profile.grpc.validation

import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Валидатор транспортного gRPC-уровня: обязательность полей, диапазоны, конечность чисел.
 */
@Component
class ProfileRequestValidator {
    private val logger = LoggerFactory.getLogger(ProfileRequestValidator::class.java)

    fun validate(request: GetProfileRequest) =
        validation {
            notBlank("user_id", request.userId)
        }

    fun validate(request: UpdateProfileRequest) =
        validation {
            notBlank("user_id", request.userId)
            intInRange("height_cm", request.heightCm, HEIGHT_CM)
            doubleInRange("weight_kg", request.weightKg, WEIGHT_KG)
            doubleInRange("weekly_budget", request.weeklyBudget, WEEKLY_BUDGET)

            if (request.hasNutritionGoal()) {
                val goal = request.nutritionGoal
                intInRange("nutrition_goal.target_calories", goal.targetCalories, CALORIES)
                doubleInRange("nutrition_goal.target_proteins_grams", goal.targetProteinsGrams, GRAMS)
                doubleInRange("nutrition_goal.target_fats_grams", goal.targetFatsGrams, GRAMS)
                doubleInRange("nutrition_goal.target_carbs_grams", goal.targetCarbsGrams, GRAMS)
                doubleInRange("nutrition_goal.target_weight_kg", goal.targetWeightKg, TARGET_WEIGHT_KG)
            }
        }

    private inline fun validation(block: Violations.() -> Unit) {
        val violations = Violations().apply(block).build()
        if (violations.isNotEmpty()) {
            logger.debug("Request validation failed with {} violation(s): {}", violations.size, violations)
            throw violations.toStatusException()
        }
    }

    private companion object {
        val HEIGHT_CM = 50..300
        val WEIGHT_KG = 20.0..500.0
        val WEEKLY_BUDGET = 0.0..1_000_000.0
        val CALORIES = 0..10_000
        val GRAMS = 0.0..2_000.0
        val TARGET_WEIGHT_KG = 0.0..500.0
    }
}
