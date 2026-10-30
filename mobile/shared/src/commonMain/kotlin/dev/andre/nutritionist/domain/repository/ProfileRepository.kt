package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId

interface ProfileRepository {
    suspend fun getProfile(userId: UserId) : Profile
    suspend fun updateProfile(profile: Profile) : Profile
}