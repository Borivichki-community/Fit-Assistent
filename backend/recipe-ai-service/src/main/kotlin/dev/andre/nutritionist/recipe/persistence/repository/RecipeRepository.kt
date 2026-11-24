package dev.andre.nutritionist.recipe.persistence.repository

import dev.andre.nutritionist.recipe.persistence.entity.RecipeEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RecipeRepository : JpaRepository<RecipeEntity, UUID> {
    @EntityGraph(attributePaths = ["ingredients"])
    fun findWithIngredientsById(id: UUID): RecipeEntity?

    fun findByAuthorId(authorId: String): List<RecipeEntity>

    fun findByVisibility(visibility: String): List<RecipeEntity>

    fun findByAuthorType(authorType: String): List<RecipeEntity>
}
