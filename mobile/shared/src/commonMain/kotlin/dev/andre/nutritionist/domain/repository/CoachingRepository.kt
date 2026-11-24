package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.AccessGrant
import dev.andre.nutritionist.domain.model.AccessGrantId
import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.CoachStudentLink
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.domain.model.GrantTargetType
import dev.andre.nutritionist.domain.model.UserId
import kotlin.time.Instant

interface CoachingRepository {
    suspend fun getCoachLinks(
        statusFilter: CoachLinkStatus? = null,
    ): Result<List<CoachStudentLink>>

    suspend fun inviteStudent(
        studentId: UserId,
        canViewDiary: Boolean = false,
    ): Result<CoachStudentLink>

    suspend fun respondCoachInvite(
        linkId: CoachLinkId,
        accept: Boolean,
    ): Result<CoachStudentLink>

    suspend fun grantAccess(
        userId: UserId,
        targetType: GrantTargetType,
        targetId: String,
        grantSource: GrantSource = GrantSource.COACH_GRANT,
        expiresAt: Instant? = null,
    ): Result<AccessGrant>

    suspend fun revokeAccess(grantId: AccessGrantId): Result<Unit>
}
