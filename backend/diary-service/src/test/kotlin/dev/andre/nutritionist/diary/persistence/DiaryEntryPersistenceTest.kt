package dev.andre.nutritionist.diary.persistence

import dev.andre.nutritionist.diary.persistence.entity.DiaryEntryEntity
import dev.andre.nutritionist.diary.domain.model.DiaryEntryId
import dev.andre.nutritionist.diary.domain.model.FoodItemId
import dev.andre.nutritionist.diary.domain.model.IdempotencyKey
import dev.andre.nutritionist.diary.domain.model.MealType
import dev.andre.nutritionist.diary.domain.model.UserId
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.hibernate.cfg.Configuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.sql.DriverManager
import java.time.Instant
import java.util.UUID

class DiaryEntryPersistenceTest {
    @Test
    fun migrationMatchesEntityAndSnapshotsSurviveRoundTrip() {
        PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).use { postgres ->
            postgres.start()

            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
                val database =
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
                Liquibase("db/changelog/db.changelog-master.yaml", ClassLoaderResourceAccessor(), database).use {
                    it.update(Contexts(), LabelExpression())
                }
            }

            Configuration()
                .addAnnotatedClass(DiaryEntryEntity::class.java)
                .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                .setProperty("hibernate.connection.url", postgres.jdbcUrl)
                .setProperty("hibernate.connection.username", postgres.username)
                .setProperty("hibernate.connection.password", postgres.password)
                .setProperty("hibernate.hbm2ddl.auto", "validate")
                .buildSessionFactory()
                .use { factory ->
                    val entries =
                        listOf(
                            entry(),
                            entry(price = BigDecimal("0.00")),
                            entry(
                                price = BigDecimal("123.45"),
                                unitPriceMinor = 8230,
                                priceBasisMilligrams = 100000,
                            ),
                        )

                    factory.openSession().use { session ->
                        val transaction = session.beginTransaction()
                        entries.forEach(session::persist)
                        transaction.commit()
                    }

                    factory.openSession().use { session ->
                        entries.forEach { expected ->
                            val actual =
                                requireNotNull(session.find(DiaryEntryEntity::class.java, expected.id.raw))
                            assertEquals(expected.id, actual.id)
                            assertEquals(expected.userId, actual.userId)
                            assertEquals(expected.idempotencyKey, actual.idempotencyKey)
                            assertEquals(expected.foodItemId, actual.foodItemId)
                            assertEquals(expected.foodName, actual.foodName)
                            assertEquals(expected.quantityGrams, actual.quantityGrams)
                            assertEquals(expected.calories, actual.calories)
                            assertEquals(expected.proteins, actual.proteins)
                            assertEquals(expected.fats, actual.fats)
                            assertEquals(expected.carbs, actual.carbs)
                            assertEquals(expected.mealType, actual.mealType)
                            assertEquals(expected.recordedAt, actual.recordedAt)
                            assertEquals(expected.price, actual.price)
                            assertEquals(expected.unitPriceMinor, actual.unitPriceMinor)
                            assertEquals(expected.priceBasisMilligrams, actual.priceBasisMilligrams)
                            assertNotNull(actual.createdAt)
                        }
                    }
                }
        }
    }

    private fun entry(
        price: BigDecimal? = null,
        unitPriceMinor: Long? = null,
        priceBasisMilligrams: Long? = null,
    ): DiaryEntryEntity =
        DiaryEntryEntity(
            id = DiaryEntryId(UUID.randomUUID()),
            userId = UserId("test-user"),
            idempotencyKey = IdempotencyKey(UUID.randomUUID().toString()),
            foodItemId = FoodItemId(UUID.randomUUID()),
            foodName = "Гречка",
            quantityGrams = BigDecimal("150.000"),
            calories = BigDecimal("180.00"),
            proteins = BigDecimal("6.00"),
            fats = BigDecimal("2.00"),
            carbs = BigDecimal("35.00"),
            mealType = MealType.LUNCH,
            recordedAt = Instant.parse("2026-11-14T09:00:00Z"),
            price = price,
            unitPriceMinor = unitPriceMinor,
            priceBasisMilligrams = priceBasisMilligrams,
        )
}
