package dev.andre.nutritionist.domain.model

import kotlinx.datetime.Instant

enum class CoachLinkStatus {
    UNSPECIFIED,
    PENDING,
    ACTIVE,
    REVOKED,
}

data class CoachStudentLink(
    val id: CoachLinkId,
    val coachId: UserId,
    val studentId: UserId,
    val status: CoachLinkStatus,
    val canViewDiary: Boolean = false,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

enum class GrantSource {
    UNSPECIFIED,
    COACH_GRANT,
    PURCHASE,
    SUBSCRIPTION,
}

enum class GrantTargetType {
    RECIPE,
    MEAL_PLAN,
}

data class AccessGrant(
    val id: AccessGrantId,
    val userId: UserId,
    val targetType: GrantTargetType,
    val targetId: String,
    val grantSource: GrantSource,
    val grantedBy: UserId? = null,
    val expiresAt: Instant? = null,
    val createdAt: Instant? = null,
)
