package dev.andre.nutritionist.catalog.seed

import dev.andre.nutritionist.catalog.AbstractPostgresIntegrationTest
import dev.andre.nutritionist.catalog.persistence.repository.ProductJpaRepository
import dev.andre.nutritionist.catalog.persistence.repository.RegionJpaRepository
import dev.andre.nutritionist.catalog.persistence.repository.RegionalPriceJpaRepository
import dev.andre.nutritionist.catalog.service.CatalogService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.util.UUID

class CatalogSeedDataIntegrationTest : AbstractPostgresIntegrationTest() {
    @Autowired
    private lateinit var productRepository: ProductJpaRepository

    @Autowired
    private lateinit var regionRepository: RegionJpaRepository

    @Autowired
    private lateinit var regionalPriceRepository: RegionalPriceJpaRepository

    @Autowired
    private lateinit var catalogService: CatalogService

    @Test
    fun `testInitialProductsSeededContainsAtLeast100ProductsWithAccurateNutrition`() {
        val count = productRepository.count()
        assertTrue(count >= 100, "Database must contain >= 100 products from Liquibase seed, but found $count")

        // Verify staple nutrition products exist
        val buckwheat = productRepository.findByBarcode("4607001000011")
        assertNotNull(buckwheat, "Buckwheat must be seeded")
        assertEquals("Гречневая крупа ядрица", buckwheat?.name)
        assertEquals("GRAIN", buckwheat?.category)
        assertEquals(BigDecimal("313.00"), buckwheat?.caloriesPer100g)
        assertEquals(BigDecimal("12.60"), buckwheat?.proteinsPer100g)

        val chicken = productRepository.findByBarcode("4607001000578")
        assertNotNull(chicken, "Chicken breast must be seeded")
        assertEquals("Куриное филе грудки охл", chicken?.name)
        assertEquals("POULTRY", chicken?.category)
        assertEquals(BigDecimal("113.00"), chicken?.caloriesPer100g)
        assertEquals(BigDecimal("23.60"), chicken?.proteinsPer100g)

        val cottageCheese = productRepository.findByBarcode("4607001000318")
        assertNotNull(cottageCheese, "Cottage cheese 5% must be seeded")
        assertEquals("Творог рассыпчатый 5%", cottageCheese?.name)
        assertEquals("DAIRY", cottageCheese?.category)
        assertEquals(BigDecimal("121.00"), cottageCheese?.caloriesPer100g)
        assertEquals(BigDecimal("16.00"), cottageCheese?.proteinsPer100g)

        val salmon = productRepository.findByBarcode("4607001000707")
        assertNotNull(salmon, "Salmon must be seeded")
        assertEquals("Лосось атлантический филе", salmon?.name)
        assertEquals("FISH", salmon?.category)
        assertEquals(BigDecimal("208.00"), salmon?.caloriesPer100g)

        val oliveOil = productRepository.findByBarcode("4607001001049")
        assertNotNull(oliveOil, "Olive oil must be seeded")
        assertEquals("Масло оливковое Extra Virgin", oliveOil?.name)
        assertEquals("OILS_FATS", oliveOil?.category)
        assertEquals(BigDecimal("898.00"), oliveOil?.caloriesPer100g)
    }

    @Test
    fun `testInitialRegionsSeededContainsActiveRegions`() {
        val count = regionRepository.count()
        assertTrue(count >= 6, "Database must contain >= 6 Russian regions, but found $count")

        val moscow = regionRepository.findByName("Москва")
        assertNotNull(moscow)
        assertTrue(moscow?.isActive == true)

        val spb = regionRepository.findByName("Санкт-Петербург")
        assertNotNull(spb)
        assertTrue(spb?.isActive == true)

        val nsk = regionRepository.findByName("Новосибирская область")
        assertNotNull(nsk)
        assertTrue(nsk?.isActive == true)

        val ekb = regionRepository.findByName("Свердловская область")
        assertNotNull(ekb)
        assertTrue(ekb?.isActive == true)

        val krd = regionRepository.findByName("Краснодарский край")
        assertNotNull(krd)
        assertTrue(krd?.isActive == true)

        val tatarstan = regionRepository.findByName("Республика Татарстан")
        assertNotNull(tatarstan)
        assertTrue(tatarstan?.isActive == true)
    }

    @Test
    fun `testInitialRegionalPricesSeededContainsSufficientPriceRecords`() {
        val count = regionalPriceRepository.count()
        assertTrue(count >= 600, "Database must contain >= 600 regional prices, but found $count")
    }

    @Test
    fun `testGetAveragePricesCalculatesAccurateAverageAcrossAllRegions`() {
        val buckwheatId = UUID.fromString("20000000-0000-0000-0000-000000000001")
        val averages = catalogService.getAveragePrices(listOf(buckwheatId), regionId = null)

        assertEquals(1, averages.size)
        val item = averages[0]
        assertEquals(buckwheatId, item.productId)
        assertEquals(6, item.regionCount)
        assertEquals("RUB", item.currency)
        // Expected avg price: (104.50 + 99.75 + 95.00 + 93.10 + 90.25 + 92.15) / 6 = 95.79
        assertTrue(
            item.averagePrice in BigDecimal("95.50")..BigDecimal("96.00"),
            "Expected avg price ~95.79, but got ${item.averagePrice}",
        )
    }

    @Test
    fun `testGetAveragePricesFilteredBySpecificRegionReturnsExactRegionalPrice`() {
        val buckwheatId = UUID.fromString("20000000-0000-0000-0000-000000000001")
        val moscowId = UUID.fromString("10000000-0000-0000-0000-000000000001")

        val moscowPrices = catalogService.getAveragePrices(listOf(buckwheatId), regionId = moscowId)

        assertEquals(1, moscowPrices.size)
        val item = moscowPrices[0]
        assertEquals(buckwheatId, item.productId)
        assertEquals(1, item.regionCount)
        assertEquals("RUB", item.currency)
        assertEquals(BigDecimal("104.50"), item.averagePrice)
    }
}
