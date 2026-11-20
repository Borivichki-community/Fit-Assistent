package dev.andre.nutritionist.catalog.persistence.repository

import dev.andre.nutritionist.catalog.persistence.entity.ProductEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
interface ProductJpaRepository : JpaRepository<ProductEntity, UUID> {
    @EntityGraph(value = "Product.withPricesAndRegions", type = EntityGraph.EntityGraphType.LOAD)
    fun findWithPricesAndRegionsById(id: UUID): ProductEntity?

    fun findByCaloriesPer100gBetweenAndProteinsPer100gGreaterThanEqual(
        minCalories: BigDecimal,
        maxCalories: BigDecimal,
        minProteins: BigDecimal,
    ): List<ProductEntity>

    fun findByCategoryIgnoreCase(category: String): List<ProductEntity>

    fun findByBarcode(barcode: String): ProductEntity?

    fun findByNameContainingIgnoreCase(query: String): List<ProductEntity>
}
