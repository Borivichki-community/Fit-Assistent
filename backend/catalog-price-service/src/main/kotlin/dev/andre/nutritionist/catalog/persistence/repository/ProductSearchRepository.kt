package dev.andre.nutritionist.catalog.persistence.repository

import dev.andre.nutritionist.catalog.persistence.entity.ProductEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

interface ProductSearchRepository {
    fun searchProducts(criteria: ProductSearchCriteria): KeysetSearchResult<ProductEntity>
}

@Repository
class ProductSearchRepositoryImpl(
    private val entityManager: EntityManager,
) : ProductSearchRepository {
    override fun searchProducts(criteria: ProductSearchCriteria): KeysetSearchResult<ProductEntity> {
        val cb: CriteriaBuilder = entityManager.criteriaBuilder
        val cq = cb.createQuery(ProductEntity::class.java)
        val root: Root<ProductEntity> = cq.from(ProductEntity::class.java)

        val predicates = mutableListOf<Predicate>()

        criteria.query?.trim()?.takeIf { it.isNotEmpty() }?.let { q ->
            val pattern = "%${q.lowercase()}%"
            predicates.add(cb.like(cb.lower(root.get("name")), pattern))
        }

        criteria.category?.trim()?.takeIf { it.isNotEmpty() }?.let { cat ->
            predicates.add(cb.equal(cb.lower(root.get("category")), cat.lowercase()))
        }

        criteria.minCalories?.let {
            predicates.add(cb.greaterThanOrEqualTo(root.get("caloriesPer100g"), it))
        }
        criteria.maxCalories?.let {
            predicates.add(cb.lessThanOrEqualTo(root.get("caloriesPer100g"), it))
        }
        criteria.minProteins?.let {
            predicates.add(cb.greaterThanOrEqualTo(root.get("proteinsPer100g"), it))
        }
        criteria.maxProteins?.let {
            predicates.add(cb.lessThanOrEqualTo(root.get("proteinsPer100g"), it))
        }

        val sortColName =
            when (criteria.sortBy.lowercase()) {
                "calories" -> "caloriesPer100g"
                "proteins" -> "proteinsPer100g"
                else -> "name"
            }

        val cursor = KeysetCursor.fromPageToken(criteria.pageToken)
        if (cursor != null) {
            val idPath: Path<UUID> = root.get("id")
            val keysetPredicate =
                when (sortColName) {
                    "caloriesPer100g", "proteinsPer100g" -> {
                        val sortValueNum = cursor.sortValue.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val colPath: Path<BigDecimal> = root.get(sortColName)
                        cb.or(
                            cb.greaterThan(colPath, sortValueNum),
                            cb.and(
                                cb.equal(colPath, sortValueNum),
                                cb.greaterThan(idPath, cursor.lastId),
                            ),
                        )
                    }

                    else -> {
                        val colPath: Path<String> = root.get("name")
                        cb.or(
                            cb.greaterThan(colPath, cursor.sortValue),
                            cb.and(
                                cb.equal(colPath, cursor.sortValue),
                                cb.greaterThan(idPath, cursor.lastId),
                            ),
                        )
                    }
                }
            predicates.add(keysetPredicate)
        }

        cq.where(*predicates.toTypedArray())

        cq.orderBy(
            cb.asc(root.get<Any>(sortColName)),
            cb.asc(root.get<UUID>("id")),
        )

        val boundedPageSize = criteria.pageSize.coerceIn(1, 100)
        val query = entityManager.createQuery(cq)
        val entityGraph = entityManager.getEntityGraph("Product.withPricesAndRegions")
        query.setHint("jakarta.persistence.loadgraph", entityGraph)
        query.maxResults = boundedPageSize + 1

        val resultList = query.resultList
        val hasMore = resultList.size > boundedPageSize
        val pageItems = if (hasMore) resultList.subList(0, boundedPageSize) else resultList

        val nextToken =
            if (hasMore && pageItems.isNotEmpty()) {
                val lastItem = pageItems.last()
                val sortValue =
                    when (sortColName) {
                        "caloriesPer100g" -> lastItem.caloriesPer100g.toPlainString()
                        "proteinsPer100g" -> lastItem.proteinsPer100g.toPlainString()
                        else -> lastItem.name
                    }
                val lastId = checkNotNull(lastItem.id) { "Product id cannot be null" }
                KeysetCursor(sortValue = sortValue, lastId = lastId).toPageToken()
            } else {
                ""
            }

        return KeysetSearchResult(
            items = pageItems,
            nextPageToken = nextToken,
            hasMore = hasMore,
        )
    }
}
