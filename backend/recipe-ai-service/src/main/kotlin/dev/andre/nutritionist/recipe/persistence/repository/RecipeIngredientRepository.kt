package dev.andre.nutritionist.recipe.persistence.repository

import dev.andre.nutritionist.recipe.persistence.entity.RecipeIngredientEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RecipeIngredientRepository : JpaRepository<RecipeIngredientEntity, UUID> {
    fun findByRecipeId(recipeId: UUID): List<RecipeIngredientEntity>
}
