package dev.andre.nutritionist.catalog.persistence.repository

import dev.andre.nutritionist.catalog.persistence.entity.RegionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RegionJpaRepository : JpaRepository<RegionEntity, UUID> {
    fun findByIsActiveTrue(): List<RegionEntity>

    fun findByNameIgnoreCase(name: String): RegionEntity?
}
