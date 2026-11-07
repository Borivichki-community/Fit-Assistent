package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.local.dao.ProfileDao
import dev.andre.nutritionist.data.mapper.ProfileEntityMapper.toDomain
import dev.andre.nutritionist.data.mapper.ProfileEntityMapper.toEntity
import dev.andre.nutritionist.data.remote.ProfileRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProfileRepositoryImpl(
    private val dao: ProfileDao,
    private val remoteDataSource: ProfileRemoteDataSource,
    private val dispatchers: CoroutineDispatchers,
) : ProfileRepository {

    override fun observeProfile(userId: UserId): Flow<Profile?> =
        dao.observeProfile(userId.raw)
            .map { entity -> entity?.toDomain() }
            .flowOn(dispatchers.io)

    override suspend fun getProfile(userId: UserId): Profile? =
        withContext(dispatchers.io) {
            val localEntity = dao.getProfile(userId.raw)
            if (localEntity != null) {
                return@withContext localEntity.toDomain()
            }

            remoteDataSource.getProfile(userId)
                .onSuccess { remoteProfile ->
                    dao.insertOrReplace(remoteProfile.toEntity())
                }
                .getOrNull() }

    override suspend fun refreshProfile(userId: UserId): Result<Unit> =
        withContext(dispatchers.io) {
            remoteDataSource.getProfile(userId)
                .mapCatching { remoteProfile ->
                    dao.insertOrReplace(remoteProfile.toEntity())
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                }
        }

    override suspend fun updateProfile(profile: Profile): Result<Profile> =
        withContext(dispatchers.io) {
            remoteDataSource.updateProfile(profile)
                .mapCatching { updatedProfile ->
                    dao.insertOrReplace(updatedProfile.toEntity())
                    updatedProfile
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                }
        }
}