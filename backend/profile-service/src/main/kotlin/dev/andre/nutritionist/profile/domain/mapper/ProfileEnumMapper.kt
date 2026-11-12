package dev.andre.nutritionist.profile.domain.mapper

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GoalTypeProto
import dev.andre.nutritionist.profile.domain.model.ActivityLevel
import dev.andre.nutritionist.profile.domain.model.Gender
import dev.andre.nutritionist.profile.domain.model.GoalType

fun GenderProto.toDomain(): Gender =
    when (this) {
        GenderProto.GENDER_MALE -> Gender.MALE
        GenderProto.GENDER_FEMALE -> Gender.FEMALE
        GenderProto.GENDER_UNSPECIFIED,
        GenderProto.UNRECOGNIZED -> Gender.UNKNOWN
    }

fun Gender.toProto(): GenderProto =
    when (this) {
        Gender.MALE -> GenderProto.GENDER_MALE
        Gender.FEMALE -> GenderProto.GENDER_FEMALE
        Gender.UNKNOWN -> GenderProto.GENDER_UNSPECIFIED
    }

fun ActivityLevelProto.toDomain(): ActivityLevel =
    when (this) {
        ActivityLevelProto.ACTIVITY_LEVEL_SEDENTARY -> ActivityLevel.SEDENTARY
        ActivityLevelProto.ACTIVITY_LEVEL_LIGHT -> ActivityLevel.LIGHT
        ActivityLevelProto.ACTIVITY_LEVEL_MODERATE -> ActivityLevel.MODERATE
        ActivityLevelProto.ACTIVITY_LEVEL_VERY_ACTIVE -> ActivityLevel.VERY_ACTIVE
        ActivityLevelProto.ACTIVITY_LEVEL_UNSPECIFIED,
        ActivityLevelProto.UNRECOGNIZED -> ActivityLevel.UNKNOWN
    }

fun ActivityLevel.toProto(): ActivityLevelProto =
    when (this) {
        ActivityLevel.SEDENTARY -> ActivityLevelProto.ACTIVITY_LEVEL_SEDENTARY
        ActivityLevel.LIGHT -> ActivityLevelProto.ACTIVITY_LEVEL_LIGHT
        ActivityLevel.MODERATE -> ActivityLevelProto.ACTIVITY_LEVEL_MODERATE
        ActivityLevel.VERY_ACTIVE -> ActivityLevelProto.ACTIVITY_LEVEL_VERY_ACTIVE
        ActivityLevel.UNKNOWN -> ActivityLevelProto.ACTIVITY_LEVEL_UNSPECIFIED
    }

fun GoalTypeProto.toDomain(): GoalType =
    when (this) {
        GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT -> GoalType.LOSE_WEIGHT
        GoalTypeProto.GOAL_TYPE_MAINTAIN -> GoalType.MAINTAIN
        GoalTypeProto.GOAL_TYPE_GAIN_WEIGHT -> GoalType.GAIN_WEIGHT
        GoalTypeProto.GOAL_TYPE_UNSPECIFIED,
        GoalTypeProto.UNRECOGNIZED -> GoalType.UNKNOWN
    }

fun GoalType.toProto(): GoalTypeProto =
    when (this) {
        GoalType.LOSE_WEIGHT -> GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT
        GoalType.MAINTAIN -> GoalTypeProto.GOAL_TYPE_MAINTAIN
        GoalType.GAIN_WEIGHT -> GoalTypeProto.GOAL_TYPE_GAIN_WEIGHT
        GoalType.UNKNOWN -> GoalTypeProto.GOAL_TYPE_UNSPECIFIED
    }
