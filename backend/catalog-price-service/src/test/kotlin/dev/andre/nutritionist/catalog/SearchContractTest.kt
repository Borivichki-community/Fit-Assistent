package dev.andre.nutritionist.catalog

import com.budgetntr.proto.catalog.v1.NumericRange
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import dev.andre.nutritionist.catalog.domain.ProductId
import dev.andre.nutritionist.catalog.grpc.SearchCursor
import dev.andre.nutritionist.catalog.grpc.toFilter
import dev.andre.nutritionist.catalog.persistence.ProductSearchSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SearchContractTest {
    private val id = ProductId("00000000-0000-0000-0000-000000000101")

    @Test
    fun cursorRoundTripAllowsDifferentPageSize() {
        val request =
            SearchProductsRequest
                .newBuilder()
                .setCategory("grain")
                .setPageSize(5)
                .build()
        val token = SearchCursor.encode(id, request)
        assertThat(
            SearchCursor.decode(
                request
                    .toBuilder()
                    .setPageToken(token)
                    .setPageSize(10)
                    .build(),
            ),
        ).isEqualTo(id)
        assertThat(SearchCursor.decode(request)).isNull()
    }

    @Test
    fun changedFiltersCannotReuseCursor() {
        val request = SearchProductsRequest.newBuilder().setQuery("apple").build()
        val token = SearchCursor.encode(id, request)
        assertThrows(IllegalArgumentException::class.java) {
            SearchCursor.decode(
                request
                    .toBuilder()
                    .setQuery("pear")
                    .setPageToken(token)
                    .build(),
            )
        }
    }

    @Test
    fun malformedAndOversizedTokensAreRejected() {
        listOf("!", "a".repeat(257), "MQ").forEach { token ->
            assertThrows(IllegalArgumentException::class.java) {
                SearchCursor.decode(SearchProductsRequest.newBuilder().setPageToken(token).build())
            }
        }
    }

    @Test
    fun invalidNutritionIsRejected() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 101.0).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) {
                SearchProductsRequest
                    .newBuilder()
                    .setProteins(NumericRange.newBuilder().setMin(value))
                    .build()
                    .toFilter()
            }
        }
    }

    @Test
    fun reversedRangesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SearchProductsRequest
                .newBuilder()
                .setFats(NumericRange.newBuilder().setMin(20.0).setMax(10.0))
                .build()
                .toFilter()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SearchProductsRequest
                .newBuilder()
                .setMinPricePer100GMinor(100)
                .setMaxPricePer100GMinor(99)
                .build()
                .toFilter()
        }
    }

    @Test
    fun zeroBoundsRetainPresence() {
        val filter =
            SearchProductsRequest
                .newBuilder()
                .setProteins(NumericRange.newBuilder().setMax(0.0))
                .setMaxPricePer100GMinor(0)
                .build()
                .toFilter()
        assertThat(filter.proteins.max).isEqualTo(0.0)
        assertThat(filter.maxPrice).isZero()
        assertThat(filter.currency).isEqualTo("RUB")
    }

    @Test
    fun textAndCurrencyLimitsAreEnforced() {
        listOf(
            SearchProductsRequest.newBuilder().setQuery("a".repeat(201)).build(),
            SearchProductsRequest.newBuilder().setCurrency("rub").build(),
            SearchProductsRequest.newBuilder().setMinPricePer100GMinor(-1).build(),
        ).forEach { request -> assertThrows(IllegalArgumentException::class.java) { request.toFilter() } }
    }

    @Test
    fun searchTextIsBoundAndWildcardsAreLiteral() {
        val text = "%_!' OR true --"
        val request =
            SearchProductsRequest
                .newBuilder()
                .setQuery(text)
                .setCategory("grain")
                .build()
        val query = ProductSearchSql.build(request.toFilter(), id, 51)
        assertThat(query.sql).doesNotContain(text, "OFFSET", id.raw)
        assertThat(query.parameters["query"]).isEqualTo("%!%!_!!' OR true --%")
        assertThat(query.sql).contains("p.id > :after", "ORDER BY p.id", "LIMIT :limit")
    }
}
