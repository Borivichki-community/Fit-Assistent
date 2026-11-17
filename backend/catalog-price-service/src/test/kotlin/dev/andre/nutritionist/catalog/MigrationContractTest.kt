package dev.andre.nutritionist.catalog

import liquibase.changelog.ChangeLogParameters
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MigrationContractTest {
    @Test
    fun changelogParsesWithBundledLiquibaseSchema() {
        ClassLoaderResourceAccessor().use { resources ->
            val path = "db/changelog/db.changelog-master.yaml"
            val log =
                ChangeLogParserFactory
                    .getInstance()
                    .getParser(path, resources)
                    .parse(path, ChangeLogParameters(), resources)
            assertThat(log.changeSets.map { it.id })
                .containsExactly("catalog-001", "catalog-002", "catalog-003", "catalog-004-demo", "catalog-005")
        }
    }
}
