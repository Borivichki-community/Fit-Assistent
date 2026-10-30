package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.remote.ProfileRemoteDataSource
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.ProfileRepository

class ProfileRepositoryImpl(
    private val remoteDataSource: ProfileRemoteDataSource
) : ProfileRepository {
    override suspend fun getProfile(userId: UserId): Profile {
        return remoteDataSource.getProfile(userId = userId).getOrThrow();
    }

    override suspend fun updateProfile(profile: Profile): Profile {
        return remoteDataSource.updateProfile(profile = profile).getOrThrow();
    }
}