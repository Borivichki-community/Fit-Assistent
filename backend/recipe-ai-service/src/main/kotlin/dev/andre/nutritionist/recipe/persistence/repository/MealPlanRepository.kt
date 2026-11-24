package dev.andre.nutritionist.recipe.persistence.repository

import dev.andre.nutritionist.recipe.persistence.entity.MealPlanEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MealPlanRepository : JpaRepository<MealPlanEntity, UUID> {
    @EntityGraph(attributePaths = ["items"])
    fun findWithItemsById(id: UUID): MealPlanEntity?

    fun findByAuthorId(authorId: String): List<MealPlanEntity>
}
