package dev.andre.nutritionist.recipe

import dev.andre.nutritionist.recipe.domain.service.CoachingAccessService
import dev.andre.nutritionist.recipe.infrastructure.security.AuthContext
import dev.andre.nutritionist.recipe.persistence.entity.CoachStudentLinkEntity
import dev.andre.nutritionist.recipe.persistence.repository.AccessGrantRepository
import dev.andre.nutritionist.recipe.persistence.repository.CoachStudentLinkRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.UUID

class CoachingAccessServiceTest {
    private val linkRepository: CoachStudentLinkRepository = mock(CoachStudentLinkRepository::class.java)
    private val grantRepository: AccessGrantRepository = mock(AccessGrantRepository::class.java)
    private val service = CoachingAccessService(linkRepository, grantRepository)

    @Test
    fun `listCoachLinks treats proto unspecified filter as unfiltered and supports prefixed statuses`() {
        val studentId = "student-1"
        val coachId = "coach-1"
        val activeLink =
            CoachStudentLinkEntity(
                id = UUID.randomUUID(),
                coachId = coachId,
                studentId = studentId,
                status = "ACTIVE",
                canViewDiary = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        val pendingLink =
            CoachStudentLinkEntity(
                id = UUID.randomUUID(),
                coachId = "coach-2",
                studentId = studentId,
                status = "PENDING",
                canViewDiary = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        `when`(linkRepository.findByCoachId(studentId)).thenReturn(emptyList())
        `when`(linkRepository.findByStudentId(studentId)).thenReturn(listOf(activeLink, pendingLink))

        val auth = AuthContext(userId = studentId, roles = setOf(AuthContext.ROLE_USER), isAuthenticated = true)

        assertEquals(listOf(activeLink, pendingLink), service.listCoachLinks(null, auth))
        assertEquals(listOf(activeLink, pendingLink), service.listCoachLinks("COACH_LINK_STATUS_UNSPECIFIED", auth))
        assertEquals(listOf(activeLink), service.listCoachLinks("COACH_LINK_STATUS_ACTIVE", auth))
        assertEquals(listOf(pendingLink), service.listCoachLinks("PENDING", auth))
    }
}
