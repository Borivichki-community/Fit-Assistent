package dev.andre.nutritionist.fake

import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.data.remote.grpc.ProfileGrpcServiceClient

/**
 * Ручная Fake-реализация ProfileGrpcServiceClient для commonTest (запрет MockK).
 */
class FakeProfileGrpcServiceClient : ProfileGrpcServiceClient {
    var responseToReturn: ProfileProto? = null
    var shouldThrow: Exception? = null
    var lastGetRequest: GetProfileRequest? = null
    var lastUpdateRequest: UpdateProfileRequest? = null

    override suspend fun getProfile(request: GetProfileRequest): ProfileProto {
        lastGetRequest = request
        shouldThrow?.let { throw it }
        return responseToReturn ?: ProfileProto(id = "fake-profile-id", userId = request.userId)
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): ProfileProto {
        lastUpdateRequest = request
        shouldThrow?.let { throw it }
        return responseToReturn ?: ProfileProto(
            id = "fake-profile-id",
            userId = request.userId,
            heightCm = request.heightCm,
            weightKg = request.weightKg,
            gender = request.gender,
            activityLevel = request.activityLevel,
            goalType = request.goalType,
            nutritionGoal = request.nutritionGoal,
            weeklyBudget = request.weeklyBudget,
        )
    }

    fun reset() {
        responseToReturn = null
        shouldThrow = null
        lastGetRequest = null
        lastUpdateRequest = null
    }
}
