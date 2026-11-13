package dev.andre.nutritionist.profile.domain.mapper

import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import com.github.f4b6a3.uuid.UuidCreator
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.profile.domain.model.NutritionGoal
import dev.andre.nutritionist.profile.domain.model.UpsertProfileCommand
import dev.andre.nutritionist.profile.persistence.entity.NutritionGoalEmbeddable
import dev.andre.nutritionist.profile.persistence.entity.ProfileEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

fun NutritionGoal.toProto(): NutritionGoalProto =
    NutritionGoalProto
        .newBuilder()
        .setTargetCalories(targetCalories)
        .setTargetProteinsGrams(targetProteinsGrams.toDouble())
        .setTargetFatsGrams(targetFatsGrams.toDouble())
        .setTargetCarbsGrams(targetCarbsGrams.toDouble())
        .setTargetWeightKg(targetWeightKg.toDouble())
        .build()

fun NutritionGoalProto.toDomain(): NutritionGoal =
    NutritionGoal(
        targetCalories = targetCalories,
        targetProteinsGrams = BigDecimal.valueOf(targetProteinsGrams).setScale(2, RoundingMode.HALF_UP),
        targetFatsGrams = BigDecimal.valueOf(targetFatsGrams).setScale(2, RoundingMode.HALF_UP),
        targetCarbsGrams = BigDecimal.valueOf(targetCarbsGrams).setScale(2, RoundingMode.HALF_UP),
        targetWeightKg = BigDecimal.valueOf(targetWeightKg).setScale(2, RoundingMode.HALF_UP),
    )

fun NutritionGoal.toEmbeddable(): NutritionGoalEmbeddable =
    NutritionGoalEmbeddable(
        targetCalories = targetCalories,
        targetProteinsGrams = targetProteinsGrams,
        targetFatsGrams = targetFatsGrams,
        targetCarbsGrams = targetCarbsGrams,
        targetWeightKg = targetWeightKg,
    )

fun NutritionGoalEmbeddable.toDomain(): NutritionGoal =
    NutritionGoal(
        targetCalories = targetCalories,
        targetProteinsGrams = targetProteinsGrams,
        targetFatsGrams = targetFatsGrams,
        targetCarbsGrams = targetCarbsGrams,
        targetWeightKg = targetWeightKg,
    )

fun NutritionGoalEmbeddable.toProto(): NutritionGoalProto = toDomain().toProto()

fun NutritionGoalProto.toEmbeddable(): NutritionGoalEmbeddable = toDomain().toEmbeddable()

fun ProfileEntity.toProto(): ProfileProto =
    ProfileProto
        .newBuilder()
        .setId(id.toString())
        .setUserId(userId)
        .setHeightCm(heightCm)
        .setWeightKg(weightKg.toDouble())
        .setGender(gender.toProto())
        .setActivityLevel(activityLevel.toProto())
        .setGoalType(goalType.toProto())
        .setNutritionGoal(nutritionGoal.toProto())
        .setWeeklyBudget(weeklyBudget.toDouble())
        .setUpdatedAt(
            Timestamp
                .newBuilder()
                .setSeconds(updatedAt.epochSecond)
                .setNanos(updatedAt.nano)
                .build(),
        ).build()

fun UpdateProfileRequest.toCommand(): UpsertProfileCommand {
    val domainGoal =
        if (hasNutritionGoal() && nutritionGoal.targetCalories > 0) {
            nutritionGoal.toDomain()
        } else {
            null
        }

    return UpsertProfileCommand(
        userId = userId,
        heightCm = heightCm,
        weightKg = BigDecimal.valueOf(weightKg).setScale(2, RoundingMode.HALF_UP),
        gender = gender.toDomain(),
        activityLevel = activityLevel.toDomain(),
        goalType = goalType.toDomain(),
        nutritionGoal = domainGoal,
        weeklyBudget = BigDecimal.valueOf(weeklyBudget).setScale(2, RoundingMode.HALF_UP),
    )
}

fun UpdateProfileRequest.toNewEntity(): ProfileEntity {
    val now = Instant.now()
    return ProfileEntity(
        id = UuidCreator.getTimeOrderedEpoch(),
        userId = userId,
        heightCm = heightCm,
        weightKg = BigDecimal.valueOf(weightKg).setScale(2, RoundingMode.HALF_UP),
        gender = gender.toDomain(),
        activityLevel = activityLevel.toDomain(),
        goalType = goalType.toDomain(),
        nutritionGoal = if (hasNutritionGoal()) nutritionGoal.toEmbeddable() else NutritionGoalEmbeddable(),
        weeklyBudget = BigDecimal.valueOf(weeklyBudget).setScale(2, RoundingMode.HALF_UP),
        createdAt = now,
        updatedAt = now,
    )
}

fun ProfileEntity.applyUpdates(request: UpdateProfileRequest): ProfileEntity {
    heightCm = request.heightCm
    weightKg = BigDecimal.valueOf(request.weightKg).setScale(2, RoundingMode.HALF_UP)
    gender = request.gender.toDomain()
    activityLevel = request.activityLevel.toDomain()
    goalType = request.goalType.toDomain()
    if (request.hasNutritionGoal()) {
        nutritionGoal = request.nutritionGoal.toEmbeddable()
    }
    weeklyBudget = BigDecimal.valueOf(request.weeklyBudget).setScale(2, RoundingMode.HALF_UP)
    updatedAt = Instant.now()
    return this
}
