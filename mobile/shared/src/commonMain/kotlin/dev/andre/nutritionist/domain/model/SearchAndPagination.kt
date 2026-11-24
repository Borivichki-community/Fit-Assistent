package dev.andre.nutritionist.domain.model

data class RecipeSearchCriteria(
    val query: String = "",
    val authorType: RecipeAuthorType? = null,
    val visibility: ContentVisibility? = null,
    val pageSize: Int = 20,
    val pageToken: String? = null,
)

data class MealPlanSearchCriteria(
    val query: String = "",
    val authorType: RecipeAuthorType? = null,
    val visibility: ContentVisibility? = null,
    val pageSize: Int = 20,
    val pageToken: String? = null,
)

data class PagedResult<T>(
    val items: List<T>,
    val nextPageToken: String? = null,
    val totalCount: Long = items.size.toLong(),
) {
    val hasNextPage: Boolean
        get() = !nextPageToken.isNullOrBlank()
}
