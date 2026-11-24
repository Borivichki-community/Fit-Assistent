package dev.andre.nutritionist.recipe.persistence.repository

import dev.andre.nutritionist.recipe.persistence.entity.CoachStudentLinkEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CoachStudentLinkRepository : JpaRepository<CoachStudentLinkEntity, UUID> {
    fun findByCoachIdAndStudentId(
        coachId: String,
        studentId: String,
    ): CoachStudentLinkEntity?

    fun findByCoachId(coachId: String): List<CoachStudentLinkEntity>

    fun findByStudentId(studentId: String): List<CoachStudentLinkEntity>
}
