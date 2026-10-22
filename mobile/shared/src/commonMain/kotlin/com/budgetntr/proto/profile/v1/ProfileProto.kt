package com.budgetntr.proto.profile.v1

enum class GenderProto(val value: Int) {
    GENDER_UNSPECIFIED(0),
    GENDER_MALE(1),
    GENDER_FEMALE(2),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): GenderProto = entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

enum class ActivityLevelProto(val value: Int) {
    ACTIVITY_LEVEL_UNSPECIFIED(0),
    ACTIVITY_LEVEL_SEDENTARY(1),
    ACTIVITY_LEVEL_LIGHT(2),
    ACTIVITY_LEVEL_MODERATE(3),
    ACTIVITY_LEVEL_VERY_ACTIVE(4),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): ActivityLevelProto = entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

enum class GoalTypeProto(val value: Int) {
    GOAL_TYPE_UNSPECIFIED(0),
    GOAL_TYPE_LOSE_WEIGHT(1),
    GOAL_TYPE_MAINTAIN(2),
    GOAL_TYPE_GAIN_WEIGHT(3),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): GoalTypeProto = entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

data class TimestampProto(
    val seconds: Long = 0L,
    val nanos: Int = 0,
)

data class NutritionGoalProto(
    val targetCalories: Int = 0,
    val targetProteinsGrams: Double = 0.0,
    val targetFatsGrams: Double = 0.0,
    val targetCarbsGrams: Double = 0.0,
    val targetWeightKg: Double = 0.0,
)

data class ProfileProto(
    val id: String = "",
    val userId: String = "",
    val heightCm: Int = 0,
    val weightKg: Double = 0.0,
    val gender: GenderProto = GenderProto.GENDER_UNSPECIFIED,
    val activityLevel: ActivityLevelProto = ActivityLevelProto.ACTIVITY_LEVEL_UNSPECIFIED,
    val goalType: GoalTypeProto = GoalTypeProto.GOAL_TYPE_UNSPECIFIED,
    val nutritionGoal: NutritionGoalProto = NutritionGoalProto(),
    val updatedAt: TimestampProto = TimestampProto(),
)

data class GetProfileRequest(
    val userId: String = "",
)

data class UpdateProfileRequest(
    val userId: String = "",
    val heightCm: Int = 0,
    val weightKg: Double = 0.0,
    val gender: GenderProto = GenderProto.GENDER_UNSPECIFIED,
    val activityLevel: ActivityLevelProto = ActivityLevelProto.ACTIVITY_LEVEL_UNSPECIFIED,
    val goalType: GoalTypeProto = GoalTypeProto.GOAL_TYPE_UNSPECIFIED,
    val nutritionGoal: NutritionGoalProto = NutritionGoalProto(),
)
