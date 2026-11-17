package dev.andre.nutritionist.catalog

import dev.andre.nutritionist.catalog.domain.NumericBounds
import dev.andre.nutritionist.catalog.domain.ProductId
import dev.andre.nutritionist.catalog.domain.SearchFilter
import dev.andre.nutritionist.catalog.persistence.CatalogRepository
import dev.andre.nutritionist.catalog.persistence.ProductSearchSql
import liquibase.integration.spring.SpringLiquibase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files
import java.nio.file.Path

@Tag("benchmark")
class CatalogSearchBenchmarkTest {
    @Test
    fun searchPlansOnOneHundredThousandProducts() {
        PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:17-alpine")).use { postgres ->
            postgres.start()
            val source = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            SpringLiquibase().apply {
                dataSource = source
                changeLog = "classpath:db/changelog/db.changelog-master.yaml"
                afterPropertiesSet()
            }
            source.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource("catalog/benchmark.sql")) }
            val jdbc = NamedParameterJdbcTemplate(source)
            val repository = CatalogRepository(jdbc)
            assertThat(jdbc.queryForObject("SELECT count(*) FROM products", emptyMap<String, Any>(), Long::class.java))
                .isEqualTo(100_000L)
            val scenarios =
                listOf(
                    SearchFilter(),
                    SearchFilter(category = "benchmark", proteins = NumericBounds(20.0, 30.0)),
                    SearchFilter(query = "benchmark", maxPrice = 5000),
                    SearchFilter(category = "benchmark", fats = NumericBounds(1.0, 10.0), maxPrice = 5000),
                )
            val directory = Path.of("build/reports/catalog-explain")
            Files.createDirectories(directory)
            scenarios.forEachIndexed { index, filter ->
                val cursor = ProductId("80000000-0000-0000-0000-000000000000")
                val query = ProductSearchSql.build(filter, cursor, 51)
                val plan =
                    jdbc.queryForObject(
                        "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + query.sql,
                        query.parameters,
                        String::class.java,
                    )
                assertThat(plan).contains("Actual Rows", "Execution Time", "Shared Hit Blocks")
                Files.writeString(directory.resolve("search-$index.json"), checkNotNull(plan))
                val page = repository.search(filter, cursor, 50)
                assertThat(page).isNotEmpty()
                val next = repository.search(filter, page.last().id, 50)
                assertThat(page.map { it.id }.intersect(next.map { it.id }.toSet())).isEmpty()
            }
        }
    }
}
