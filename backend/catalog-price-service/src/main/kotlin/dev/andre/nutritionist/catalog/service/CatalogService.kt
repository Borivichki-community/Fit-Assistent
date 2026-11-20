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
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

data class AveragePriceResult(
    val productId: UUID,
    val averagePrice: BigDecimal,
    val currency: String,
    val regionCount: Int,
)

@Service
class CatalogService(
    private val productRepository: ProductJpaRepository,
    private val regionRepository: RegionJpaRepository,
    private val regionalPriceRepository: RegionalPriceJpaRepository,
) {
    @Transactional(readOnly = true)
    fun searchProducts(
        criteria: dev.andre.nutritionist.catalog.persistence.repository.ProductSearchCriteria,
    ): dev.andre.nutritionist.catalog.persistence.repository.KeysetSearchResult<ProductEntity> = productRepository.searchProducts(criteria)

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
    fun findProductsByCategory(category: String): List<ProductEntity> = productRepository.findByCategoryIgnoreCase(category)

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

    @Transactional(readOnly = true)
    fun getAveragePrices(
        productIds: List<UUID>,
        regionId: UUID? = null,
    ): List<AveragePriceResult> {
        if (productIds.isEmpty()) return emptyList()

        return productIds.mapNotNull { productId ->
            val prices =
                if (regionId != null) {
                    val p = regionalPriceRepository.findByProductIdAndRegionId(productId, regionId)
                    if (p != null) listOf(p) else emptyList()
                } else {
                    regionalPriceRepository.findByProductId(productId)
                }

            if (prices.isEmpty()) {
                null
            } else {
                val sum = prices.map { it.price }.reduce { acc, p -> acc + p }
                val avg = sum.divide(BigDecimal(prices.size), 2, RoundingMode.HALF_UP)
                AveragePriceResult(
                    productId = productId,
                    averagePrice = avg,
                    currency = prices.first().currency,
                    regionCount = prices.size,
                )
            }
        }
    }

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
