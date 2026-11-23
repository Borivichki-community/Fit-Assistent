package dev.andre.nutritionist.catalog.persistence.repository

import java.math.BigDecimal
import java.util.Base64
import java.util.UUID

data class ProductSearchCriteria(
    val query: String? = null,
    val category: String? = null,
    val minCalories: BigDecimal? = null,
    val maxCalories: BigDecimal? = null,
    val minProteins: BigDecimal? = null,
    val maxProteins: BigDecimal? = null,
    val pageSize: Int = 20,
    val pageToken: String? = null,
    val sortBy: String = "name",
)

data class KeysetCursor(
    val sortValue: String,
    val lastId: UUID,
) {
    fun toPageToken(): String {
        val payload = "$sortValue|$lastId"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray(Charsets.UTF_8))
    }

    companion object {
        fun fromPageToken(token: String?): KeysetCursor? {
            if (token.isNullOrBlank()) return null
            return try {
                val decoded = String(Base64.getUrlDecoder().decode(token), Charsets.UTF_8)
                val lastPipe = decoded.lastIndexOf('|')
                if (lastPipe <= 0 || lastPipe >= decoded.length - 1) return null
                val sortValue = decoded.substring(0, lastPipe)
                val idStr = decoded.substring(lastPipe + 1)
                KeysetCursor(sortValue = sortValue, lastId = UUID.fromString(idStr))
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class KeysetSearchResult<T>(
    val items: List<T>,
    val nextPageToken: String,
    val hasMore: Boolean,
)
