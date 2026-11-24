package dev.andre.nutritionist.diary.grpc.idempotency

import dev.andre.nutritionist.diary.domain.model.IdempotencyKey
import dev.andre.nutritionist.diary.domain.model.UserId
import dev.andre.nutritionist.diary.grpc.idempotency.IdempotencyService.IdempotencyConflictException
import dev.andre.nutritionist.diary.grpc.idempotency.IdempotencyStore.IdempotencyScope
import dev.andre.nutritionist.diary.persistence.repository.PostgresIdempotencyStore
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdempotencyServiceIntegrationTest {
    private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
    private val dataSource by lazy {
        DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
    }
    private val jdbc by lazy { JdbcTemplate(dataSource) }
    private val store by lazy { PostgresIdempotencyStore(jdbc) }
    private val service by lazy {
        IdempotencyService(store, DataSourceTransactionManager(dataSource), Duration.ofHours(24))
    }

    @BeforeAll
    fun startDatabase() {
        postgres.start()
        dataSource.connection.use { connection ->
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
            Liquibase("db/changelog/db.changelog-master.yaml", ClassLoaderResourceAccessor(), database).use {
                it.update(Contexts(), LabelExpression())
            }
        }
    }

    @AfterAll
    fun stopDatabase() {
        postgres.stop()
    }

    @Test
    fun `repeat returns the stored response and creates one diary entry`() {
        val scope = scope()
        val request = "oatmeal:200".encodeToByteArray()
        val first = service.execute(scope, request) { insertEntry(scope) }
        val repeated = service.execute(scope, request) { error("Repeat must not execute") }

        assertArrayEquals(first, repeated)
        assertEquals(1, entryCount(scope))
        assertEquals(1, keyCount(scope))
    }

    @Test
    fun `same key with different request fails without changing the diary`() {
        val scope = scope()
        val first = service.execute(scope, "200".encodeToByteArray()) { insertEntry(scope) }

        assertThrows(IdempotencyConflictException::class.java) {
            service.execute(scope, "300".encodeToByteArray()) { error("Conflict must not execute") }
        }

        assertArrayEquals(
            first,
            service.execute(scope, "200".encodeToByteArray()) { error("Repeat must not execute") },
        )
        assertEquals(1, entryCount(scope))
    }

    @Test
    fun `parallel requests execute one operation`() {
        val scope = scope()
        val workers = 8
        val ready = CountDownLatch(workers)
        val start = CountDownLatch(1)
        val executions = AtomicInteger()
        val executor = Executors.newFixedThreadPool(workers)
        try {
            val futures =
                List(workers) {
                    executor.submit<ByteArray> {
                        ready.countDown()
                        check(start.await(10, TimeUnit.SECONDS))
                        service.execute(scope, "same-request".encodeToByteArray()) {
                            executions.incrementAndGet()
                            insertEntry(scope)
                        }
                    }
                }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            val responses = futures.map { it.get(30, TimeUnit.SECONDS) }

            responses.forEach { assertArrayEquals(responses.first(), it) }
            assertEquals(1, executions.get())
            assertEquals(1, entryCount(scope))
            assertEquals(1, keyCount(scope))
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `failed operation rolls back diary and key and allows retry`() {
        val scope = scope()

        assertThrows(IllegalStateException::class.java) {
            service.execute(scope, byteArrayOf(1)) {
                insertEntry(scope)
                error("Failure after database write")
            }
        }

        assertEquals(0, entryCount(scope))
        assertEquals(0, keyCount(scope))
        service.execute(scope, byteArrayOf(1)) { insertEntry(scope) }
        assertEquals(1, entryCount(scope))
    }

    @Test
    fun `same key is isolated by user and rpc method`() {
        val first = scope()
        val otherUser = first.copy(userId = UserId(UUID.randomUUID().toString()))
        val otherMethod = first.copy(rpcMethod = "DiaryService/DeleteMeal")

        assertArrayEquals(byteArrayOf(1), service.execute(first, byteArrayOf(1)) { byteArrayOf(1) })
        assertArrayEquals(byteArrayOf(2), service.execute(otherUser, byteArrayOf(2)) { byteArrayOf(2) })
        assertArrayEquals(byteArrayOf(3), service.execute(otherMethod, byteArrayOf(3)) { byteArrayOf(3) })
    }

    @Test
    fun `cleanup removes only expired responses and keeps diary uniqueness`() {
        val expired = scope()
        val active = scope()
        service.execute(expired, byteArrayOf(1)) { insertEntry(expired) }
        val activeResponse = service.execute(active, byteArrayOf(2)) { insertEntry(active) }
        jdbc.update(
            """
            UPDATE idempotency_keys SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
            WHERE user_id = ? AND rpc_method = ? AND idempotency_key = ?
            """.trimIndent(),
            expired.userId.raw,
            expired.rpcMethod,
            expired.key.raw,
        )

        IdempotencyCleanup(store, 1000).cleanUp()

        assertEquals(0, keyCount(expired))
        assertEquals(1, keyCount(active))
        assertEquals(1, entryCount(expired))
        assertArrayEquals(
            activeResponse,
            service.execute(active, byteArrayOf(2)) { error("Active response must be replayed") },
        )
    }

    @Test
    fun `empty successful response is replayed`() {
        val scope = scope()
        service.execute(scope, byteArrayOf(1)) { byteArrayOf() }

        assertArrayEquals(
            byteArrayOf(),
            service.execute(scope, byteArrayOf(1)) { error("Empty response must be replayed") },
        )
    }

    private fun scope(): IdempotencyScope =
        IdempotencyScope(
            UserId(UUID.randomUUID().toString()),
            "DiaryService/AddEntry",
            IdempotencyKey(UUID.randomUUID().toString()),
        )

    private fun insertEntry(scope: IdempotencyScope): ByteArray =
        checkNotNull(
            jdbc.queryForObject(
                """
                INSERT INTO diary_entries (
                    id, user_id, idempotency_key, source_type, source_id, name_snapshot, weight_grams,
                    food_item_id, food_name, quantity_grams,
                    calories, proteins, fats, carbs, meal_type, recorded_at
                )
                VALUES (gen_random_uuid(), ?, ?, 'PRODUCT', gen_random_uuid(), 'Oatmeal', 200,
                    gen_random_uuid(), 'Oatmeal', 200, 150, 5, 3, 25, 'BREAKFAST', NOW())
                RETURNING id::text
                """.trimIndent(),
                String::class.java,
                scope.userId.raw,
                scope.key.raw,
            ),
        ).encodeToByteArray()

    private fun entryCount(scope: IdempotencyScope): Int =
        checkNotNull(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM diary_entries WHERE user_id = ? AND idempotency_key = ?",
                Int::class.java,
                scope.userId.raw,
                scope.key.raw,
            ),
        )

    private fun keyCount(scope: IdempotencyScope): Int =
        checkNotNull(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM idempotency_keys WHERE user_id = ? AND rpc_method = ? AND idempotency_key = ?",
                Int::class.java,
                scope.userId.raw,
                scope.rpcMethod,
                scope.key.raw,
            ),
        )
}
