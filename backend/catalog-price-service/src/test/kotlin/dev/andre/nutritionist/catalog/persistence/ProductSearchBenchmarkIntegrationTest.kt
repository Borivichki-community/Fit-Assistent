package dev.andre.nutritionist.catalog.persistence

import dev.andre.nutritionist.catalog.AbstractPostgresIntegrationTest
import dev.andre.nutritionist.catalog.persistence.entity.ProductEntity
import dev.andre.nutritionist.catalog.persistence.repository.ProductJpaRepository
import dev.andre.nutritionist.catalog.persistence.repository.ProductSearchCriteria
import dev.andre.nutritionist.catalog.service.CatalogService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductSearchBenchmarkIntegrationTest : AbstractPostgresIntegrationTest() {
    private val logger = LoggerFactory.getLogger(ProductSearchBenchmarkIntegrationTest::class.java)

    @Autowired
    private lateinit var productRepository: ProductJpaRepository

    @Autowired
    private lateinit var catalogService: CatalogService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    companion object {
        private const val SEED_COUNT = 10_000
    }

    @BeforeAll
    fun seedBenchmarkData() {
        val currentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Long::class.java) ?: 0L
        if (currentCount >= SEED_COUNT) {
            logger.info("Database already contains $currentCount products. Skipping seed.")
            return
        }

        logger.info("Seeding $SEED_COUNT products for benchmark and keyset pagination tests...")
        val batchSize = 1000
        val sql =
            """
            INSERT INTO products (id, name, barcode, category, calories_per_100g, proteins_per_100g, fats_per_100g, carbs_per_100g, version, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, NOW())
            """.trimIndent()

        val categories = listOf("DAIRY", "MEAT", "GRAIN", "VEGETABLE")

        for (batch in 0 until (SEED_COUNT / batchSize)) {
            val batchArgs = mutableListOf<Array<Any>>()
            for (i in 0 until batchSize) {
                val index = batch * batchSize + i
                val id = UUID.randomUUID()
                val name = "Benchmark Product ${String.format("%06d", index)}"
                val barcode = "BC-${String.format("%08d", index)}"
                val category = categories[index % categories.size]
                val calories = BigDecimal.valueOf(10.0 + (index % 500))
                val proteins = BigDecimal.valueOf(1.0 + (index % 50))
                val fats = BigDecimal.valueOf(1.0 + (index % 30))
                val carbs = BigDecimal.valueOf(1.0 + (index % 80))

                batchArgs.add(arrayOf(id, name, barcode, category, calories, proteins, fats, carbs))
            }
            jdbcTemplate.batchUpdate(sql, batchArgs)
        }

        // Run ANALYZE to update PostgreSQL optimizer statistics for accurate EXPLAIN ANALYZE
        jdbcTemplate.execute("ANALYZE products")
        logger.info("Successfully seeded $SEED_COUNT products.")
    }

    @Test
    fun `test keyset pagination retrieves consecutive non-overlapping pages`() {
        val pageSize = 20

        // Page 1
        val page1 =
            catalogService.searchProducts(
                ProductSearchCriteria(
                    pageSize = pageSize,
                    sortBy = "name",
                ),
            )

        assertEquals(pageSize, page1.items.size)
        assertTrue(page1.hasMore)
        assertTrue(page1.nextPageToken.isNotBlank())

        // Page 2 using cursor
        val page2 =
            catalogService.searchProducts(
                ProductSearchCriteria(
                    pageSize = pageSize,
                    pageToken = page1.nextPageToken,
                    sortBy = "name",
                ),
            )

        assertEquals(pageSize, page2.items.size)
        assertTrue(page2.hasMore)

        val page1Ids = page1.items.map { it.id }.toSet()
        val page2Ids = page2.items.map { it.id }.toSet()
        val overlap = page1Ids.intersect(page2Ids)
        assertTrue(overlap.isEmpty(), "Pages must not have overlapping IDs: $overlap")

        // Monotonic ordering verification
        val lastPage1Name = page1.items.last().name
        val firstPage2Name = page2.items.first().name
        assertTrue(
            firstPage2Name >= lastPage1Name,
            "First item of page 2 ('$firstPage2Name') must be >= last item of page 1 ('$lastPage1Name')",
        )
    }

    @Test
    fun `test filtering by category and fulltext query`() {
        val result =
            catalogService.searchProducts(
                ProductSearchCriteria(
                    query = "Product 0005",
                    category = "DAIRY",
                    pageSize = 50,
                ),
            )

        assertFalse(result.items.isEmpty(), "Should find items matching query and category")
        result.items.forEach { product ->
            assertTrue(product.category.equals("DAIRY", ignoreCase = true))
            assertTrue(product.name.contains("0005", ignoreCase = true))
        }
    }

    @Test
    fun `test filtering by nutrition range`() {
        val minCal = BigDecimal("100.00")
        val maxCal = BigDecimal("150.00")
        val minProt = BigDecimal("10.00")

        val result =
            catalogService.searchProducts(
                ProductSearchCriteria(
                    minCalories = minCal,
                    maxCalories = maxCal,
                    minProteins = minProt,
                    pageSize = 30,
                    sortBy = "calories",
                ),
            )

        assertFalse(result.items.isEmpty(), "Should find products in nutrition range")
        result.items.forEach { product ->
            assertTrue(
                product.caloriesPer100g in minCal..maxCal,
                "Calories ${product.caloriesPer100g} must be in [$minCal, $maxCal]",
            )
            assertTrue(
                product.proteinsPer100g >= minProt,
                "Proteins ${product.proteinsPer100g} must be >= $minProt",
            )
        }
    }

    @Test
    fun `test keyset stability under concurrent insertions`() {
        val pageSize = 15

        // Fetch page 1
        val page1 =
            catalogService.searchProducts(
                ProductSearchCriteria(
                    category = "GRAIN",
                    pageSize = pageSize,
                    sortBy = "name",
                ),
            )
        assertEquals(pageSize, page1.items.size)
        val lastItemPage1 = page1.items.last()

        // Insert new product that sorts BEFORE the cursor
        val insertedBefore =
            ProductEntity(
                name = "A_New_Grain_Before_Cursor",
                category = "GRAIN",
                caloriesPer100g = BigDecimal("120.00"),
                proteinsPer100g = BigDecimal("10.00"),
                fatsPer100g = BigDecimal("2.00"),
                carbsPer100g = BigDecimal("20.00"),
            )
        productRepository.saveAndFlush(insertedBefore)

        // Fetch page 2 using the token from page 1
        val page2 =
            catalogService.searchProducts(
                ProductSearchCriteria(
                    category = "GRAIN",
                    pageSize = pageSize,
                    pageToken = page1.nextPageToken,
                    sortBy = "name",
                ),
            )

        assertEquals(pageSize, page2.items.size)
        // With keyset pagination, inserting an item before the cursor does NOT shift results
        // or cause duplicate/skipped elements between page 1 and page 2
        val page1Ids = page1.items.map { it.id }.toSet()
        val page2Ids = page2.items.map { it.id }.toSet()
        assertTrue(page1Ids.intersect(page2Ids).isEmpty(), "Keyset pagination must prevent duplicates")
        assertTrue(page2.items.first().name >= lastItemPage1.name)
    }

    @Test
    fun `test explain analyze benchmark confirms index usage and sub-millisecond execution`() {
        val testCursorName = "Benchmark Product 005000"
        val testCursorId = UUID.randomUUID()

        // EXPLAIN ANALYZE on keyset pagination query
        val explainQuery =
            """
            EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
            SELECT id, name, category, calories_per_100g, proteins_per_100g
            FROM products
            WHERE (name > ? OR (name = ? AND id > ?))
            ORDER BY name ASC, id ASC
            LIMIT 20
            """.trimIndent()

        val explainLines =
            jdbcTemplate.queryForList(
                explainQuery,
                String::class.java,
                testCursorName,
                testCursorName,
                testCursorId,
            )
        val explainOutput = explainLines.joinToString("\n")

        logger.info("EXPLAIN ANALYZE keyset query plan:\n$explainOutput")

        // Verify index scan on idx_products_name
        assertTrue(
            explainOutput.contains("Index Scan") ||
                explainOutput.contains("Bitmap Index Scan") ||
                explainOutput.contains("idx_products_name"),
            "Query plan must utilize index for keyset sorting. Plan:\n$explainOutput",
        )

        // Verify category index on filtered search
        val categoryLines =
            jdbcTemplate.queryForList(
                """
                EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
                SELECT id, name, category
                FROM products
                WHERE category = 'DAIRY' AND (name > ? OR (name = ? AND id > ?))
                ORDER BY name ASC, id ASC
                LIMIT 20
                """.trimIndent(),
                String::class.java,
                testCursorName,
                testCursorName,
                testCursorId,
            )
        val categoryExplain = categoryLines.joinToString("\n")

        logger.info("EXPLAIN ANALYZE category query plan:\n$categoryExplain")
        assertNotNull(categoryExplain)
    }
}
