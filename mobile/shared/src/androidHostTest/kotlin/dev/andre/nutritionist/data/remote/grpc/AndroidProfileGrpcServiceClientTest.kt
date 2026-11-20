package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.TimestampProto
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidProfileGrpcServiceClientTest {

    @Test
    fun getProfileRequestMarshaller_roundtripSerialization() {
        val original = GetProfileRequest(userId = "usr-test-123")
        val stream = GetProfileRequestMarshaller.stream(original)
        val deserialized = GetProfileRequestMarshaller.parse(stream)

        assertEquals(original, deserialized)
    }

    @Test
    fun updateProfileRequestMarshaller_roundtripSerialization() {
        val original = UpdateProfileRequest(
            userId = "usr-test-456",
            heightCm = 182,
            weightKg = 78.5,
            gender = GenderProto.GENDER_MALE,
            activityLevel = ActivityLevelProto.ACTIVITY_LEVEL_MODERATE,
            goalType = GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT,
            nutritionGoal = NutritionGoalProto(
                targetCalories = 2150,
                targetProteinsGrams = 157.0,
                targetFatsGrams = 78.5,
                targetCarbsGrams = 205.0,
                targetWeightKg = 73.0,
            ),
            weeklyBudget = 6500.0,
        )

        val stream = UpdateProfileRequestMarshaller.stream(original)
        val deserialized = UpdateProfileRequestMarshaller.parse(stream)

        assertEquals(original, deserialized)
    }

    @Test
    fun profileProtoMarshaller_roundtripSerialization() {
        val original = ProfileProto(
            id = "prof-999",
            userId = "usr-test-789",
            heightCm = 168,
            weightKg = 62.0,
            gender = GenderProto.GENDER_FEMALE,
            activityLevel = ActivityLevelProto.ACTIVITY_LEVEL_LIGHT,
            goalType = GoalTypeProto.GOAL_TYPE_MAINTAIN,
            nutritionGoal = NutritionGoalProto(
                targetCalories = 1850,
                targetProteinsGrams = 99.2,
                targetFatsGrams = 62.0,
                targetCarbsGrams = 223.8,
                targetWeightKg = 62.0,
            ),
            updatedAt = TimestampProto(seconds = 1700001234L, nanos = 500),
            weeklyBudget = 4000.0,
        )

        val stream = ProfileProtoMarshaller.stream(original)
        val deserialized = ProfileProtoMarshaller.parse(stream)

        assertEquals(original, deserialized)
    }
}
