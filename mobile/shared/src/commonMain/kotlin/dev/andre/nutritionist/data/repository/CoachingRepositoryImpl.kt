package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.remote.RecipeRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.AccessGrant
import dev.andre.nutritionist.domain.model.AccessGrantId
import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.CoachStudentLink
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.domain.model.GrantTargetType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.CoachingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class CoachingRepositoryImpl(
    private val remoteDataSource: RecipeRemoteDataSource,
    private val dispatchers: CoroutineDispatchers,
) : CoachingRepository {

    override suspend fun getCoachLinks(statusFilter: CoachLinkStatus?): Result<List<CoachStudentLink>> =
        withContext(dispatchers.io) {
            remoteDataSource.listCoachLinks(statusFilter)
                .onFailure { error ->
                    if (error is CancellationException) throw error
                }
        }

    override suspend fun inviteStudent(
        studentId: UserId,
        canViewDiary: Boolean,
    ): Result<CoachStudentLink> =
        withContext(dispatchers.io) {
            remoteDataSource.inviteStudent(
                studentId = studentId,
                canViewDiary = canViewDiary,
                idempotencyKey = IdempotencyKey.generate().raw,
            ).onFailure { error ->
                if (error is CancellationException) throw error
            }
        }

    override suspend fun respondCoachInvite(
        linkId: CoachLinkId,
        accept: Boolean,
    ): Result<CoachStudentLink> =
        withContext(dispatchers.io) {
            remoteDataSource.respondCoachInvite(
                linkId = linkId,
                accept = accept,
                idempotencyKey = IdempotencyKey.generate().raw,
            ).onFailure { error ->
                if (error is CancellationException) throw error
            }
        }

    override suspend fun grantAccess(
        userId: UserId,
        targetType: GrantTargetType,
        targetId: String,
        grantSource: GrantSource,
        expiresAt: Instant?,
    ): Result<AccessGrant> =
        withContext(dispatchers.io) {
            remoteDataSource.grantAccess(
                userId = userId,
                targetType = targetType,
                targetId = targetId,
                grantSource = grantSource,
                expiresAt = expiresAt,
                idempotencyKey = IdempotencyKey.generate().raw,
            ).onFailure { error ->
                if (error is CancellationException) throw error
            }
        }

    override suspend fun revokeAccess(grantId: AccessGrantId): Result<Unit> =
        withContext(dispatchers.io) {
            remoteDataSource.revokeAccess(
                grantId = grantId,
                idempotencyKey = IdempotencyKey.generate().raw,
            ).onFailure { error ->
                if (error is CancellationException) throw error
            }
        }
}
