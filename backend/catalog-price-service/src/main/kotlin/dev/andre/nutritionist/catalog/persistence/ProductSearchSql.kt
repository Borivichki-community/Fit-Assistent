package dev.andre.nutritionist.catalog.persistence

import dev.andre.nutritionist.catalog.domain.ProductId
import dev.andre.nutritionist.catalog.domain.SearchFilter
import java.util.UUID

data class SearchSql(
    val sql: String,
    val parameters: Map<String, Any>,
)

object ProductSearchSql {
    fun build(
        filter: SearchFilter,
        after: ProductId?,
        limit: Int,
    ): SearchSql {
        val predicates = mutableListOf<String>()
        val parameters = mutableMapOf<String, Any>("limit" to limit)
        after?.let {
            predicates.add("p.id > :after")
            parameters["after"] = UUID.fromString(it.raw)
        }
        if (filter.query.isNotEmpty()) {
            predicates.add("p.name ILIKE :query ESCAPE '!'")
            parameters["query"] = "%" +
                filter.query
                    .replace("!", "!!")
                    .replace("%", "!%")
                    .replace("_", "!_") + "%"
        }
        if (filter.category.isNotEmpty()) {
            predicates.add("p.category = :category")
            parameters["category"] = filter.category
        }
        listOf(
            "calories_per_100g" to filter.calories,
            "proteins_per_100g" to filter.proteins,
            "fats_per_100g" to filter.fats,
            "carbs_per_100g" to filter.carbs,
        ).forEach { (column, bounds) ->
            bounds.min?.let {
                predicates.add("p.$column >= :min_$column")
                parameters["min_$column"] = it
            }
            bounds.max?.let {
                predicates.add("p.$column <= :max_$column")
                parameters["max_$column"] = it
            }
        }
        val priceFilter = filter.minPrice != null || filter.maxPrice != null
        if (priceFilter) {
            parameters["currency"] = filter.currency
            filter.minPrice?.let {
                predicates.add("a.amount >= :min_price")
                parameters["min_price"] = it
            }
            filter.maxPrice?.let {
                predicates.add("a.amount <= :max_price")
                parameters["max_price"] = it
            }
        }
        val priceJoin =
            if (priceFilter) {
                """
                JOIN LATERAL (
                    SELECT round(avg(r.unit_price_minor::numeric * 100000 / r.price_basis_milligrams)) AS amount
                    FROM prices r WHERE r.product_id = p.id AND r.currency = :currency
                ) a ON a.amount IS NOT NULL
                """.trimIndent()
            } else {
                ""
            }
        val where = predicates.takeIf { it.isNotEmpty() }?.joinToString(" AND ", prefix = "WHERE ").orEmpty()
        return SearchSql("SELECT p.* FROM products p $priceJoin $where ORDER BY p.id LIMIT :limit", parameters.toMap())
    }
}
