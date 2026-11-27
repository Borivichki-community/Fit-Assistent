package dev.andre.nutritionist.fake

import dev.andre.nutritionist.domain.model.AccessGrant
import dev.andre.nutritionist.domain.model.AccessGrantId
import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.CoachStudentLink
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.domain.model.GrantTargetType
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.CoachingRepository
import kotlin.time.Instant

class FakeCoachingRepository : CoachingRepository {
    val links = mutableListOf<CoachStudentLink>()
    val grants = mutableListOf<AccessGrant>()
    var errorToThrow: Throwable? = null
    var lastInvitedCoachId: UserId = UserId("coach-1")

    override suspend fun getCoachLinks(statusFilter: CoachLinkStatus?): Result<List<CoachStudentLink>> {
        errorToThrow?.let { return Result.failure(it) }
        val filtered = if (statusFilter == null) links.toList() else links.filter { it.status == statusFilter }
        return Result.success(filtered)
    }

    override suspend fun inviteStudent(
        studentId: UserId,
        canViewDiary: Boolean,
    ): Result<CoachStudentLink> {
        errorToThrow?.let { return Result.failure(it) }
        val link = CoachStudentLink(
            id = CoachLinkId("link-${links.size + 1}"),
            coachId = lastInvitedCoachId,
            studentId = studentId,
            status = CoachLinkStatus.PENDING,
            canViewDiary = canViewDiary,
        )
        links.removeAll { it.id == link.id }
        links.add(link)
        return Result.success(link)
    }

    override suspend fun respondCoachInvite(
        linkId: CoachLinkId,
        accept: Boolean,
    ): Result<CoachStudentLink> {
        errorToThrow?.let { return Result.failure(it) }
        val index = links.indexOfFirst { it.id == linkId }
        if (index < 0) return Result.failure(NoSuchElementException("Link $linkId not found"))
        val updated = links[index].copy(
            status = if (accept) CoachLinkStatus.ACTIVE else CoachLinkStatus.REVOKED,
        )
        links[index] = updated
        return Result.success(updated)
    }

    override suspend fun grantAccess(
        userId: UserId,
        targetType: GrantTargetType,
        targetId: String,
        grantSource: GrantSource,
        expiresAt: Instant?,
    ): Result<AccessGrant> {
        errorToThrow?.let { return Result.failure(it) }
        val grant = AccessGrant(
            id = AccessGrantId("grant-${grants.size + 1}"),
            userId = userId,
            targetType = targetType,
            targetId = targetId,
            grantSource = grantSource,
            grantedBy = lastInvitedCoachId,
        )
        grants.add(grant)
        return Result.success(grant)
    }

    override suspend fun revokeAccess(grantId: AccessGrantId): Result<Unit> {
        errorToThrow?.let { return Result.failure(it) }
        grants.removeAll { it.id == grantId }
        return Result.success(Unit)
    }
}
