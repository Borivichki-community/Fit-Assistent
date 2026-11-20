package dev.andre.nutritionist.catalog.persistence.repository

import dev.andre.nutritionist.catalog.persistence.entity.RegionalPriceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RegionalPriceJpaRepository : JpaRepository<RegionalPriceEntity, UUID> {
    fun findByProductIdAndRegionId(
        productId: UUID,
        regionId: UUID,
    ): RegionalPriceEntity?

    fun findByProductId(productId: UUID): List<RegionalPriceEntity>

    fun findByRegionId(regionId: UUID): List<RegionalPriceEntity>

    @Query("SELECT p FROM RegionalPriceEntity p JOIN FETCH p.region WHERE p.product.id = :productId")
    fun findByProductIdWithRegion(
        @Param("productId") productId: UUID,
    ): List<RegionalPriceEntity>
}
