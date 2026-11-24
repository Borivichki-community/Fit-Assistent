package dev.andre.nutritionist.recipe.domain.service

import dev.andre.nutritionist.recipe.infrastructure.security.AuthContext
import dev.andre.nutritionist.recipe.persistence.entity.AccessGrantEntity
import dev.andre.nutritionist.recipe.persistence.entity.CoachStudentLinkEntity
import dev.andre.nutritionist.recipe.persistence.repository.AccessGrantRepository
import dev.andre.nutritionist.recipe.persistence.repository.CoachStudentLinkRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class CoachingAccessService(
    private val linkRepository: CoachStudentLinkRepository,
    private val grantRepository: AccessGrantRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun inviteStudent(
        studentId: String,
        canViewDiary: Boolean,
        authContext: AuthContext,
    ): CoachStudentLinkEntity {
        val coachId = checkNotNull(authContext.userId) { "Authentication required to invite student" }
        require(studentId.isNotBlank()) { "studentId must not be blank" }
        require(coachId != studentId) { "Cannot invite yourself" }

        val existing = linkRepository.findByCoachIdAndStudentId(coachId, studentId)
        if (existing != null) {
            existing.status = "PENDING"
            existing.canViewDiary = canViewDiary
            existing.updatedAt = Instant.now()
            return linkRepository.save(existing)
        }

        val link =
            CoachStudentLinkEntity(
                id = UUID.randomUUID(),
                coachId = coachId,
                studentId = studentId,
                status = "PENDING",
                canViewDiary = canViewDiary,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        return linkRepository.save(link)
    }

    @Transactional
    fun respondCoachInvite(
        linkId: UUID,
        accept: Boolean,
        authContext: AuthContext,
    ): CoachStudentLinkEntity {
        val studentId = checkNotNull(authContext.userId) { "Authentication required to respond to invitation" }
        val link =
            linkRepository.findById(linkId).orElseThrow {
                NoSuchElementException("Coach link with id $linkId not found")
            }

        if (link.studentId != studentId && !authContext.isAdmin) {
            throw SecurityException("Only the invited student can respond to this invite")
        }

        link.status = if (accept) "ACTIVE" else "REVOKED"
        link.updatedAt = Instant.now()
        return linkRepository.save(link)
    }

    @Transactional(readOnly = true)
    fun listCoachLinks(
        statusFilter: String?,
        authContext: AuthContext,
    ): List<CoachStudentLinkEntity> {
        val currentUserId = checkNotNull(authContext.userId) { "Authentication required to list links" }

        // Find links where caller is coach or student
        val asCoach = linkRepository.findByCoachId(currentUserId)
        val asStudent = linkRepository.findByStudentId(currentUserId)
        val combined = (asCoach + asStudent).distinctBy { it.id }

        return if (!statusFilter.isNullOrBlank() && statusFilter != "UNSPECIFIED") {
            val filterUpper = statusFilter.uppercase()
            combined.filter { it.status.uppercase() == filterUpper }
        } else {
            combined
        }
    }

    @Transactional
    fun grantAccess(
        targetUserId: String,
        targetType: String,
        targetId: UUID,
        grantSource: String,
        expiresAt: Instant?,
        authContext: AuthContext,
    ): AccessGrantEntity {
        val grantedBy = checkNotNull(authContext.userId) { "Authentication required to grant access" }
        require(targetUserId.isNotBlank()) { "targetUserId must not be blank" }

        val existing = grantRepository.findByUserIdAndTargetTypeAndTargetId(targetUserId, targetType.uppercase(), targetId)
        if (existing != null) {
            existing.grantSource = grantSource.uppercase()
            existing.grantedBy = grantedBy
            existing.expiresAt = expiresAt
            return grantRepository.save(existing)
        }

        val grant =
            AccessGrantEntity(
                id = UUID.randomUUID(),
                userId = targetUserId,
                targetType = targetType.uppercase(),
                targetId = targetId,
                grantSource = grantSource.uppercase(),
                grantedBy = grantedBy,
                expiresAt = expiresAt,
                createdAt = Instant.now(),
            )
        return grantRepository.save(grant)
    }

    @Transactional
    fun revokeAccess(
        grantId: UUID,
        authContext: AuthContext,
    ): Boolean {
        val currentUserId = checkNotNull(authContext.userId) { "Authentication required to revoke access" }
        val grant =
            grantRepository.findById(grantId).orElseThrow {
                NoSuchElementException("Access grant with id $grantId not found")
            }

        if (grant.grantedBy != currentUserId && grant.userId != currentUserId && !authContext.isAdmin) {
            throw SecurityException("Not authorized to revoke this access grant")
        }

        grantRepository.delete(grant)
        return true
    }

    @Transactional(readOnly = true)
    fun checkCoachAccess(
        coachId: String,
        studentId: String,
    ): Pair<Boolean, Boolean> {
        val link = linkRepository.findByCoachIdAndStudentId(coachId, studentId)
        val isActive = link != null && link.status.equals("ACTIVE", ignoreCase = true)
        val canView = isActive && link.canViewDiary
        return Pair(isActive, canView)
    }

    @Transactional(readOnly = true)
    fun hasActiveAccessGrant(
        userId: String,
        targetType: String,
        targetId: UUID,
    ): Boolean {
        val grant =
            grantRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType.uppercase(), targetId)
                ?: return false
        val exp = grant.expiresAt
        return exp == null || exp.isAfter(Instant.now())
    }

    @Transactional(readOnly = true)
    fun getAllowedTargetIds(
        userId: String,
        targetType: String,
    ): Set<UUID> {
        val now = Instant.now()
        return grantRepository
            .findByUserId(userId)
            .filter { it.targetType.equals(targetType, ignoreCase = true) }
            .filter { it.expiresAt == null || it.expiresAt?.isAfter(now) == true }
            .map { it.targetId }
            .toSet()
    }
}
