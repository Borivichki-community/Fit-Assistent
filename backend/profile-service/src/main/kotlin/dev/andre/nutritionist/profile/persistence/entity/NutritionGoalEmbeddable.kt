package dev.andre.nutritionist.profile.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.math.BigDecimal

/**
 * Встраиваемый компонент JPA для нутрициологических целей пользователя.
 * Полностью соответствует NutritionGoalProto из profile.proto.
 */
@Embeddable
class NutritionGoalEmbeddable(
    @Column(name = "target_calories", nullable = false)
    var targetCalories: Int = 0,

    @Column(name = "target_proteins_grams", nullable = false, precision = 6, scale = 2)
    var targetProteinsGrams: BigDecimal = BigDecimal.ZERO,

    @Column(name = "target_fats_grams", nullable = false, precision = 6, scale = 2)
    var targetFatsGrams: BigDecimal = BigDecimal.ZERO,

    @Column(name = "target_carbs_grams", nullable = false, precision = 6, scale = 2)
    var targetCarbsGrams: BigDecimal = BigDecimal.ZERO,

    @Column(name = "target_weight_kg", nullable = false, precision = 5, scale = 2)
    var targetWeightKg: BigDecimal = BigDecimal.ZERO,
)
