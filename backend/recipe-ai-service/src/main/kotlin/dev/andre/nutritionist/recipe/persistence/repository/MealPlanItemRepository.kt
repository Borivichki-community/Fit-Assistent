package dev.andre.nutritionist.recipe.persistence.repository

import dev.andre.nutritionist.recipe.persistence.entity.MealPlanItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MealPlanItemRepository : JpaRepository<MealPlanItemEntity, UUID> {
    fun findByPlanId(planId: UUID): List<MealPlanItemEntity>
}
