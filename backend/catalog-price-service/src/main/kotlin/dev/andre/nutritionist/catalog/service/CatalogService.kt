package dev.andre.nutritionist.catalog.service

import dev.andre.nutritionist.catalog.persistence.entity.ProductEntity
import dev.andre.nutritionist.catalog.persistence.entity.RegionEntity
import dev.andre.nutritionist.catalog.persistence.entity.RegionalPriceEntity
import dev.andre.nutritionist.catalog.persistence.repository.ProductJpaRepository
import dev.andre.nutritionist.catalog.persistence.repository.RegionJpaRepository
import dev.andre.nutritionist.catalog.persistence.repository.RegionalPriceJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class CatalogService(
    private val productRepository: ProductJpaRepository,
    private val regionRepository: RegionJpaRepository,
    private val regionalPriceRepository: RegionalPriceJpaRepository,
) {
    @Transactional(readOnly = true)
    fun findProductsByNutrition(
        minCalories: BigDecimal,
        maxCalories: BigDecimal,
        minProteins: BigDecimal,
    ): List<ProductEntity> =
        productRepository.findByCaloriesPer100gBetweenAndProteinsPer100gGreaterThanEqual(
            minCalories = minCalories,
            maxCalories = maxCalories,
            minProteins = minProteins,
        )

    @Transactional(readOnly = true)
    fun getProductWithPrices(productId: UUID): ProductEntity =
        productRepository.findWithPricesAndRegionsById(productId)
            ?: throw NoSuchElementException("Product with id $productId not found")

    @Transactional(readOnly = true)
    fun getActiveRegions(): List<RegionEntity> = regionRepository.findByIsActiveTrue()

    @Transactional(readOnly = true)
    fun getRegionById(regionId: UUID): RegionEntity =
        regionRepository.findById(regionId).orElseThrow {
            NoSuchElementException("Region with id $regionId not found")
        }

    @Transactional
    fun createOrUpdateProduct(product: ProductEntity): ProductEntity = productRepository.save(product)

    @Transactional
    fun createOrUpdateRegion(region: RegionEntity): RegionEntity = regionRepository.save(region)

    @Transactional
    fun upsertRegionalPrice(
        productId: UUID,
        regionId: UUID,
        price: BigDecimal,
        currency: String = "RUB",
        unit: String = "PIECE",
    ): RegionalPriceEntity {
        val product =
            productRepository.findById(productId).orElseThrow {
                NoSuchElementException("Product with id $productId not found")
            }
        val region =
            regionRepository.findById(regionId).orElseThrow {
                NoSuchElementException("Region with id $regionId not found")
            }

        val existingPrice = regionalPriceRepository.findByProductIdAndRegionId(productId, regionId)
        val priceEntity =
            if (existingPrice != null) {
                existingPrice.price = price
                existingPrice.currency = currency
                existingPrice.unit = unit
                existingPrice.updatedAt = Instant.now()
                existingPrice
            } else {
                RegionalPriceEntity(
                    id = UUID.randomUUID(),
                    product = product,
                    region = region,
                    price = price,
                    currency = currency,
                    unit = unit,
                    updatedAt = Instant.now(),
                )
            }
        return regionalPriceRepository.save(priceEntity)
    }
}
