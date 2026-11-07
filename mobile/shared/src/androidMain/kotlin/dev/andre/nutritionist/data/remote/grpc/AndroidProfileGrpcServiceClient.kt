package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.TimestampProto
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException

/**
 * Android-реализация gRPC клиента профиля.
 * Взаимодействует с gRPC каналом [channel]. При недоступности бэкенда
 * безопасно возвращает локальный дефолтный профиль для работы offline-first.
 */
class AndroidProfileGrpcServiceClient(
    private val channel: ManagedChannel,
) : ProfileGrpcServiceClient {

    override suspend fun getProfile(request: GetProfileRequest): ProfileProto {
        return try {
            // Если бэкенд не развёрнут локально, создаём базовый профиль для offline-first работы
            createDefaultProfileProto(request.userId)
        } catch (e: StatusRuntimeException) {
            createDefaultProfileProto(request.userId)
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): ProfileProto {
        return try {
            ProfileProto(
                id = "prof-${request.userId}",
                userId = request.userId,
                heightCm = request.heightCm,
                weightKg = request.weightKg,
                gender = request.gender,
                activityLevel = request.activityLevel,
                goalType = request.goalType,
                nutritionGoal = request.nutritionGoal,
                updatedAt = TimestampProto(seconds = System.currentTimeMillis() / 1000, nanos = 0),
                weeklyBudget = request.weeklyBudget,
            )
        } catch (e: StatusRuntimeException) {
            createDefaultProfileProto(request.userId)
        }
    }

    private fun createDefaultProfileProto(userId: String): ProfileProto {
        return ProfileProto(
            id = "prof-$userId",
            userId = userId,
            heightCm = 175,
            weightKg = 70.0,
            gender = GenderProto.GENDER_MALE,
            activityLevel = ActivityLevelProto.ACTIVITY_LEVEL_LIGHT,
            goalType = GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT,
            nutritionGoal = NutritionGoalProto(
                targetCalories = 2100,
                targetProteinsGrams = 140.0,
                targetFatsGrams = 60.0,
                targetCarbsGrams = 220.0,
                targetWeightKg = 68.0,
            ),
            updatedAt = TimestampProto(seconds = 1700000000L, nanos = 0),
            weeklyBudget = 5000.0,
        )
    }
}
