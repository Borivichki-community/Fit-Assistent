package dev.andre.nutritionist.recipe.persistence.repository

import dev.andre.nutritionist.recipe.persistence.entity.MealPlanEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.stereotype.Repository
import java.util.UUID

data class MealPlanSearchCriteria(
    val query: String? = null,
    val authorType: String? = null,
    val visibility: String? = null,
    val currentUserId: String? = null,
    val allowedPlanIds: Set<UUID> = emptySet(),
    val pageSize: Int = 20,
    val pageToken: String? = null,
)

interface MealPlanSearchRepository {
    fun searchMealPlans(criteria: MealPlanSearchCriteria): KeysetSearchResult<MealPlanEntity>
}

@Repository
class MealPlanSearchRepositoryImpl(
    private val entityManager: EntityManager,
) : MealPlanSearchRepository {
    override fun searchMealPlans(criteria: MealPlanSearchCriteria): KeysetSearchResult<MealPlanEntity> {
        val cb: CriteriaBuilder = entityManager.criteriaBuilder
        val cq = cb.createQuery(MealPlanEntity::class.java)
        val root: Root<MealPlanEntity> = cq.from(MealPlanEntity::class.java)

        val predicates = mutableListOf<Predicate>()

        if (criteria.visibility != null && criteria.visibility.isNotBlank()) {
            predicates.add(cb.equal(root.get<String>("visibility"), criteria.visibility))
        } else {
            val publicPred = cb.equal(root.get<String>("visibility"), "PUBLIC")
            val accessPredicates = mutableListOf<Predicate>(publicPred)

            if (!criteria.currentUserId.isNullOrBlank()) {
                accessPredicates.add(cb.equal(root.get<String>("authorId"), criteria.currentUserId))
            }
            if (criteria.allowedPlanIds.isNotEmpty()) {
                accessPredicates.add(root.get<UUID>("id").`in`(criteria.allowedPlanIds))
            }
            predicates.add(cb.or(*accessPredicates.toTypedArray()))
        }

        criteria.authorType?.trim()?.takeIf { it.isNotEmpty() }?.let { type ->
            predicates.add(cb.equal(root.get<String>("authorType"), type))
        }

        criteria.query?.trim()?.takeIf { it.isNotEmpty() }?.let { q ->
            val pattern = "%${q.lowercase()}%"
            predicates.add(cb.like(cb.lower(root.get("title")), pattern))
        }

        val cursor = KeysetCursor.fromPageToken(criteria.pageToken)
        if (cursor != null) {
            val idPath: Path<UUID> = root.get("id")
            val colPath: Path<String> = root.get("title")
            val keysetPredicate =
                cb.or(
                    cb.greaterThan(colPath, cursor.sortValue),
                    cb.and(
                        cb.equal(colPath, cursor.sortValue),
                        cb.greaterThan(idPath, cursor.lastId),
                    ),
                )
            predicates.add(keysetPredicate)
        }

        cq.where(*predicates.toTypedArray())
        cq.orderBy(
            cb.asc(root.get<String>("title")),
            cb.asc(root.get<UUID>("id")),
        )

        val boundedPageSize = criteria.pageSize.coerceIn(1, 100)
        val query = entityManager.createQuery(cq)
        query.maxResults = boundedPageSize + 1

        val resultList = query.resultList
        val hasMore = resultList.size > boundedPageSize
        val pageItems = if (hasMore) resultList.subList(0, boundedPageSize) else resultList

        val nextToken =
            if (hasMore && pageItems.isNotEmpty()) {
                val lastItem = pageItems.last()
                KeysetCursor(sortValue = lastItem.title, lastId = lastItem.id).toPageToken()
            } else {
                ""
            }

        return KeysetSearchResult(
            items = pageItems,
            nextPageToken = nextToken,
            totalCount = pageItems.size.toLong(),
        )
    }
}
