package dev.andre.nutritionist.profile.persistence.repository

import dev.andre.nutritionist.profile.persistence.entity.ProfileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProfileJpaRepository : JpaRepository<ProfileEntity, UUID> {
    fun findByUserId(userId: String): ProfileEntity?
}
