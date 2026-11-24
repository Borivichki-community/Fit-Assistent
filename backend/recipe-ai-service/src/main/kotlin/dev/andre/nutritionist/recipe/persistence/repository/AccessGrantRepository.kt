package dev.andre.nutritionist.recipe.persistence.repository

import dev.andre.nutritionist.recipe.persistence.entity.AccessGrantEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AccessGrantRepository : JpaRepository<AccessGrantEntity, UUID> {
    fun findByUserIdAndTargetTypeAndTargetId(
        userId: String,
        targetType: String,
        targetId: UUID,
    ): AccessGrantEntity?

    fun findByUserId(userId: String): List<AccessGrantEntity>
}
