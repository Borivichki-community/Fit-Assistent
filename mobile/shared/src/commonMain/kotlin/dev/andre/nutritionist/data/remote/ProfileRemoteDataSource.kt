package dev.andre.nutritionist.data.remote

import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId


interface ProfileRemoteDataSource {
    suspend fun getProfile(userId: UserId): Result<Profile>
    suspend fun updateProfile(profile: Profile): Result<Profile>
}