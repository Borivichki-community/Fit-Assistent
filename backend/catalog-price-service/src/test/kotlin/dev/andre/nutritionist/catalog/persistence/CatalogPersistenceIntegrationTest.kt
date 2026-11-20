package dev.andre.nutritionist.catalog.persistence

import dev.andre.nutritionist.catalog.AbstractPostgresIntegrationTest
import dev.andre.nutritionist.catalog.persistence.entity.ProductEntity
import dev.andre.nutritionist.catalog.persistence.entity.RegionEntity
import dev.andre.nutritionist.catalog.persistence.entity.RegionalPriceEntity
import dev.andre.nutritionist.catalog.persistence.repository.ProductJpaRepository
import dev.andre.nutritionist.catalog.persistence.repository.RegionJpaRepository
import dev.andre.nutritionist.catalog.persistence.repository.RegionalPriceJpaRepository
import dev.andre.nutritionist.catalog.service.CatalogService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

class CatalogPersistenceIntegrationTest : AbstractPostgresIntegrationTest() {
    @Autowired
    private lateinit var regionRepository: RegionJpaRepository

    @Autowired
    private lateinit var productRepository: ProductJpaRepository

    @Autowired
    private lateinit var regionalPriceRepository: RegionalPriceJpaRepository

    @Autowired
    private lateinit var catalogService: CatalogService

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @BeforeEach
    fun cleanDatabase() {
        regionalPriceRepository.deleteAll()
        productRepository.deleteAll()
        regionRepository.deleteAll()
    }

    @Test
    fun `testLiquibaseMigrationAppliedAndRegionProductSaved successfully persists entities`() {
        val region =
            RegionEntity(
                name = "Центральный ФО",
            )
        val savedRegion = regionRepository.save(region)
        assertNotNull(savedRegion.id)
        assertEquals("Центральный ФО", savedRegion.name)

        val product =
            ProductEntity(
                name = "Грудка куриная",
                barcode = "4601234567890",
                category = "POULTRY",
                caloriesPer100g = BigDecimal("113.00"),
                proteinsPer100g = BigDecimal("23.60"),
                fatsPer100g = BigDecimal("1.90"),
                carbsPer100g = BigDecimal("0.40"),
            )
        val savedProduct = productRepository.save(product)
        assertNotNull(savedProduct.id)
        assertEquals(0L, savedProduct.version)
        assertEquals("Грудка куриная", savedProduct.name)
        assertEquals("POULTRY", savedProduct.category)
    }

    @Test
    fun `testOptimisticLockingIncrementsVersionOnUpdate`() {
        val product =
            productRepository.saveAndFlush(
                ProductEntity(
                    name = "Гречневая крупа",
                    barcode = "4609876543210",
                    category = "GRAINS",
                    caloriesPer100g = BigDecimal("308.00"),
                    proteinsPer100g = BigDecimal("12.60"),
                    fatsPer100g = BigDecimal("3.30"),
                    carbsPer100g = BigDecimal("57.10"),
                ),
            )
        assertEquals(0L, product.version)

        product.name = "Гречневая крупа Ядрица"
        val updatedProduct = productRepository.saveAndFlush(product)
        assertEquals(1L, updatedProduct.version)
    }

    @Test
    fun `testUniqueBarcodeThrowsDataIntegrityViolationExceptionOnDuplicate`() {
        val product1 =
            ProductEntity(
                name = "Молоко 3.2%",
                barcode = "4600000000001",
                category = "DAIRY",
                caloriesPer100g = BigDecimal("60.00"),
                proteinsPer100g = BigDecimal("3.00"),
                fatsPer100g = BigDecimal("3.20"),
                carbsPer100g = BigDecimal("4.70"),
            )
        productRepository.saveAndFlush(product1)

        val product2 =
            ProductEntity(
                name = "Кефир 3.2%",
                barcode = "4600000000001",
                category = "DAIRY",
                caloriesPer100g = BigDecimal("59.00"),
                proteinsPer100g = BigDecimal("3.00"),
                fatsPer100g = BigDecimal("3.20"),
                carbsPer100g = BigDecimal("4.00"),
            )

        assertThrows(DataIntegrityViolationException::class.java) {
            productRepository.saveAndFlush(product2)
        }
    }

    @Test
    fun `testUniqueRegionNameThrowsDataIntegrityViolationExceptionOnDuplicate`() {
        val region1 = RegionEntity(name = "Дальневосточный ФО")
        regionRepository.saveAndFlush(region1)

        val region2 = RegionEntity(name = "Дальневосточный ФО")
        assertThrows(DataIntegrityViolationException::class.java) {
            regionRepository.saveAndFlush(region2)
        }
    }

    @Test
    fun `testCategoryFilteringReturnsMatchingProducts`() {
        val apple =
            ProductEntity(
                name = "Яблоки Гренни",
                category = "FRUITS",
                caloriesPer100g = BigDecimal("52.00"),
                proteinsPer100g = BigDecimal("0.30"),
                fatsPer100g = BigDecimal("0.20"),
                carbsPer100g = BigDecimal("13.80"),
            )
        val beef =
            ProductEntity(
                name = "Говядина вырезка",
                category = "MEAT",
                caloriesPer100g = BigDecimal("218.00"),
                proteinsPer100g = BigDecimal("26.10"),
                fatsPer100g = BigDecimal("11.80"),
                carbsPer100g = BigDecimal("0.00"),
            )
        productRepository.saveAllAndFlush(listOf(apple, beef))

        val fruits = productRepository.findByCategoryIgnoreCase("FRUITS")
        assertEquals(1, fruits.size)
        assertEquals("Яблоки Гренни", fruits[0].name)
    }

    @Test
    fun `testNamedEntityGraphLoadsPricesAndRegionsWithoutLazyInitializationException`() {
        val region =
            regionRepository.save(
                RegionEntity(
                    name = "Северо-Западный ФО",
                ),
            )
        val product =
            productRepository.save(
                ProductEntity(
                    name = "Овсяные хлопья",
                    category = "GRAINS",
                    caloriesPer100g = BigDecimal("352.00"),
                    proteinsPer100g = BigDecimal("12.30"),
                    fatsPer100g = BigDecimal("6.20"),
                    carbsPer100g = BigDecimal("61.80"),
                ),
            )

        val price =
            RegionalPriceEntity(
                product = product,
                region = region,
                price = BigDecimal("99.90"),
                currency = "RUB",
                unit = "PIECE",
            )
        regionalPriceRepository.saveAndFlush(price)

        val loadedProduct =
            transactionTemplate.execute {
                productRepository.findWithPricesAndRegionsById(product.id)
            }

        assertNotNull(loadedProduct)
        assertEquals(1, loadedProduct?.prices?.size)
        val loadedPrice = loadedProduct?.prices?.firstOrNull()
        assertNotNull(loadedPrice)
        assertEquals(BigDecimal("99.90"), loadedPrice?.price)
        assertEquals("Северо-Западный ФО", loadedPrice?.region?.name)
    }

    @Test
    fun `testNutritionFiltering finds products matching calories range and min protein`() {
        val chickenBreast =
            ProductEntity(
                name = "Куриное филе",
                category = "POULTRY",
                caloriesPer100g = BigDecimal("115.00"),
                proteinsPer100g = BigDecimal("24.00"),
                fatsPer100g = BigDecimal("2.00"),
                carbsPer100g = BigDecimal("0.00"),
            )
        val chocolateBar =
            ProductEntity(
                name = "Шоколад молочный",
                category = "SWEETS",
                caloriesPer100g = BigDecimal("550.00"),
                proteinsPer100g = BigDecimal("7.00"),
                fatsPer100g = BigDecimal("34.00"),
                carbsPer100g = BigDecimal("54.00"),
            )
        val proteinShake =
            ProductEntity(
                name = "Протеиновый пудинг",
                category = "SNACKS",
                caloriesPer100g = BigDecimal("160.00"),
                proteinsPer100g = BigDecimal("20.00"),
                fatsPer100g = BigDecimal("1.50"),
                carbsPer100g = BigDecimal("15.00"),
            )

        productRepository.saveAll(listOf(chickenBreast, chocolateBar, proteinShake))

        val matching =
            productRepository.findByCaloriesPer100gBetweenAndProteinsPer100gGreaterThanEqual(
                minCalories = BigDecimal("100.00"),
                maxCalories = BigDecimal("200.00"),
                minProteins = BigDecimal("21.00"),
            )

        assertEquals(1, matching.size)
        assertEquals("Куриное филе", matching[0].name)
    }

    @Test
    fun `testUniqueConstraintProductRegion throws DataIntegrityViolationException on duplicate price`() {
        val region =
            regionRepository.save(
                RegionEntity(name = "Уральский ФО"),
            )
        val product =
            productRepository.save(
                ProductEntity(
                    name = "Творог 5%",
                    category = "DAIRY",
                    caloriesPer100g = BigDecimal("121.00"),
                    proteinsPer100g = BigDecimal("16.00"),
                    fatsPer100g = BigDecimal("5.00"),
                    carbsPer100g = BigDecimal("3.00"),
                ),
            )

        val price1 =
            RegionalPriceEntity(
                product = product,
                region = region,
                price = BigDecimal("85.00"),
            )
        regionalPriceRepository.saveAndFlush(price1)

        val price2 =
            RegionalPriceEntity(
                product = product,
                region = region,
                price = BigDecimal("92.00"),
            )

        assertThrows(DataIntegrityViolationException::class.java) {
            regionalPriceRepository.saveAndFlush(price2)
        }
    }

    @Test
    fun `testCatalogServiceUpsertRegionalPrice creates and updates price correctly`() {
        val region =
            regionRepository.save(
                RegionEntity(name = "Сибирский ФО"),
            )
        val product =
            productRepository.save(
                ProductEntity(
                    name = "Яйца C0 10шт",
                    category = "EGGS",
                    caloriesPer100g = BigDecimal("157.00"),
                    proteinsPer100g = BigDecimal("12.70"),
                    fatsPer100g = BigDecimal("11.50"),
                    carbsPer100g = BigDecimal("0.70"),
                ),
            )

        val initialPrice =
            catalogService.upsertRegionalPrice(
                productId = product.id,
                regionId = region.id,
                price = BigDecimal("119.99"),
            )
        assertNotNull(initialPrice.id)
        assertEquals(BigDecimal("119.99"), initialPrice.price)

        val updatedPrice =
            catalogService.upsertRegionalPrice(
                productId = product.id,
                regionId = region.id,
                price = BigDecimal("109.99"),
            )
        assertEquals(initialPrice.id, updatedPrice.id)
        assertEquals(BigDecimal("109.99"), updatedPrice.price)

        val allPrices = regionalPriceRepository.findByProductId(product.id)
        assertEquals(1, allPrices.size)
        assertEquals(BigDecimal("109.99"), allPrices[0].price)
    }
}
