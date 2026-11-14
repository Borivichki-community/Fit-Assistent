package dev.andre.nutritionist.profile.domain.mapper

import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import com.github.f4b6a3.uuid.UuidCreator
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.profile.domain.model.NutritionGoal
import dev.andre.nutritionist.profile.domain.model.UpsertProfileCommand
import dev.andre.nutritionist.profile.domain.model.UserId
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
        .setNutritionGoal(nutritionGoal.toDomain().toProto())
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
        userId = UserId(userId),
        heightCm = heightCm,
        weightKg = BigDecimal.valueOf(weightKg).setScale(2, RoundingMode.HALF_UP),
        gender = gender.toDomain(),
        activityLevel = activityLevel.toDomain(),
        goalType = goalType.toDomain(),
        nutritionGoal = domainGoal,
        weeklyBudget = BigDecimal.valueOf(weeklyBudget).setScale(2, RoundingMode.HALF_UP),
    )
}

fun ProfileEntity.updateFrom(
    command: UpsertProfileCommand,
    finalGoal: NutritionGoal,
): ProfileEntity {
    heightCm = command.heightCm
    weightKg = command.weightKg
    gender = command.gender
    activityLevel = command.activityLevel
    goalType = command.goalType
    nutritionGoal = finalGoal.toEmbeddable()
    weeklyBudget = command.weeklyBudget
    updatedAt = Instant.now()
    return this
}

fun UpsertProfileCommand.toNewEntity(finalGoal: NutritionGoal): ProfileEntity {
    val now = Instant.now()
    return ProfileEntity(
        id = UuidCreator.getTimeOrderedEpoch(),
        userId = userId.raw,
        heightCm = heightCm,
        weightKg = weightKg,
        gender = gender,
        activityLevel = activityLevel,
        goalType = goalType,
        nutritionGoal = finalGoal.toEmbeddable(),
        weeklyBudget = weeklyBudget,
        createdAt = now,
        updatedAt = now,
    )
}
