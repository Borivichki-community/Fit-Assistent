package dev.andre.nutritionist.catalog.service

import dev.andre.nutritionist.catalog.domain.IdempotencyKey
import dev.andre.nutritionist.catalog.domain.UserId
import io.grpc.Status
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.UUID

data class PriceWrite(
    val productId: dev.andre.nutritionist.catalog.domain.ProductId,
    val storeId: dev.andre.nutritionist.catalog.domain.StoreId,
    val amount: Long,
    val currency: String,
    val basis: Long,
)

@Service
class PriceService(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    @Transactional
    fun upsert(
        subject: UserId,
        key: IdempotencyKey,
        command: PriceWrite,
        request: ByteArray,
        encode: (java.time.Instant) -> ByteArray,
    ): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256").digest(request).joinToString("") { "%02x".format(it) }
        val identity = mapOf("subject" to subject.raw, "key" to key.raw, "hash" to hash)
        jdbc.update(
            """
            INSERT INTO price_requests(subject, request_key, request_hash) VALUES (:subject, :key, :hash)
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            identity,
        )
        val stored =
            jdbc
                .query(
                    "SELECT request_hash, response FROM price_requests WHERE subject = :subject AND request_key = :key FOR UPDATE",
                    identity,
                ) { row, _ -> row.getString("request_hash") to row.getBytes("response") }
                .single()
        require(stored.first == hash) { "Idempotency key reused with a different request" }
        stored.second?.let { return it }
        val parameters =
            mapOf(
                "product" to UUID.fromString(command.productId.raw),
                "store" to UUID.fromString(command.storeId.raw),
                "amount" to command.amount,
                "currency" to command.currency,
                "basis" to command.basis,
            )
        val exists =
            jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM products WHERE id = :product) AND EXISTS(SELECT 1 FROM stores WHERE id = :store)",
                parameters,
                Boolean::class.java,
            ) == true
        if (!exists) throw Status.NOT_FOUND.withDescription("Product or store not found").asRuntimeException()
        val updatedAt =
            jdbc
                .query(
                    """
                    INSERT INTO prices(product_id, store_id, currency, unit_price_minor, price_basis_milligrams, updated_at)
                    VALUES (:product, :store, :currency, :amount, :basis, clock_timestamp())
                    ON CONFLICT (product_id, store_id, currency) DO UPDATE
                    SET unit_price_minor = EXCLUDED.unit_price_minor,
                        price_basis_milligrams = EXCLUDED.price_basis_milligrams,
                        updated_at = EXCLUDED.updated_at
                    RETURNING updated_at
                    """.trimIndent(),
                    parameters,
                ) { row, _ -> row.getTimestamp("updated_at").toInstant() }
                .single()
        val response = encode(updatedAt)
        jdbc.update(
            "UPDATE price_requests SET response = :response WHERE subject = :subject AND request_key = :key",
            identity + ("response" to response),
        )
        return response
    }
}
