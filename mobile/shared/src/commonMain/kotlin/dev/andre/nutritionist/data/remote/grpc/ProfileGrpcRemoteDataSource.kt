package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.data.mapper.ProfileDtoMapper.toDomain
import dev.andre.nutritionist.data.mapper.ProfileDtoMapper.toUpdateRequest
import dev.andre.nutritionist.data.remote.ProfileRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.coroutines.withContext

interface ProfileGrpcServiceClient {
    suspend fun getProfile(request: GetProfileRequest): ProfileProto
    suspend fun updateProfile(request: UpdateProfileRequest): ProfileProto
}

class ProfileGrpcRemoteDataSource(
    private val client: ProfileGrpcServiceClient,
    private val dispatchers: CoroutineDispatchers,
) : ProfileRemoteDataSource {

    override suspend fun getProfile(userId: UserId): Result<Profile> =
        withContext(dispatchers.io) {
            runCatching {
                val request = GetProfileRequest(userId = userId.raw)
                val response = client.getProfile(request)
                response.toDomain()
            }
        }

    override suspend fun updateProfile(profile: Profile): Result<Profile> =
        withContext(dispatchers.io) {
            runCatching {
                val request = profile.toUpdateRequest()
                val response = client.updateProfile(request)
                response.toDomain()
            }
        }
}
