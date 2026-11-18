package dev.andre.nutritionist.catalog.persistence

import dev.andre.nutritionist.catalog.domain.AveragePrice
import dev.andre.nutritionist.catalog.domain.Nutrition
import dev.andre.nutritionist.catalog.domain.Product
import dev.andre.nutritionist.catalog.domain.ProductId
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class CatalogRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    fun getProduct(id: ProductId): Product? =
        jdbc
            .query(
                "SELECT * FROM products WHERE id = :id",
                mapOf("id" to UUID.fromString(id.raw)),
            ) { row, _ -> product(row) }
            .singleOrNull()

    fun search(
        after: ProductId?,
        limit: Int,
    ): List<Product> =
        jdbc.query(
            "SELECT * FROM products WHERE (:after IS NULL OR id > CAST(:after AS uuid)) ORDER BY id LIMIT :limit",
            mapOf("after" to after?.raw, "limit" to limit),
        ) { row, _ -> product(row) }

    fun averagePrices(
        ids: List<ProductId>,
        currency: String,
    ): List<AveragePrice> {
        if (ids.isEmpty()) return emptyList()
        return jdbc.query(
            """
            SELECT product_id, round(avg(unit_price_minor::numeric * 100000 / price_basis_milligrams)) AS amount,
                   count(*) AS stores
            FROM prices WHERE product_id IN (:ids) AND currency = :currency GROUP BY product_id ORDER BY product_id
            """.trimIndent(),
            mapOf("ids" to ids.map { UUID.fromString(it.raw) }, "currency" to currency),
        ) { row, _ ->
            AveragePrice(ProductId(row.getString("product_id")), row.getLong("amount"), currency, row.getInt("stores"))
        }
    }

    private fun product(row: ResultSet): Product =
        Product(
            ProductId(row.getString("id")),
            row.getString("name"),
            row.getString("category"),
            Nutrition(
                row.getDouble("calories_per_100g"),
                row.getDouble("proteins_per_100g"),
                row.getDouble("fats_per_100g"),
                row.getDouble("carbs_per_100g"),
            ),
        )
}
