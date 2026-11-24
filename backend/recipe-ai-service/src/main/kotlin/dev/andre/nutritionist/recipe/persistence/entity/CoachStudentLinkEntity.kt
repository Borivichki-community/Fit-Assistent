package dev.andre.nutritionist.recipe.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "coach_student_links",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_coach_student", columnNames = ["coach_id", "student_id"]),
    ],
)
class CoachStudentLinkEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "coach_id", nullable = false, length = 64)
    var coachId: String = "",
    @Column(name = "student_id", nullable = false, length = 64)
    var studentId: String = "",
    @Column(name = "status", nullable = false, length = 20)
    var status: String = "PENDING",
    @Column(name = "can_view_diary", nullable = false)
    var canViewDiary: Boolean = true,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CoachStudentLinkEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
