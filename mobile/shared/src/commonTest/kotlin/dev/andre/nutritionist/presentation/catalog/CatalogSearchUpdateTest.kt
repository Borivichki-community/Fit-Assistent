package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Price
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RegionalPrice
import dev.andre.nutritionist.domain.model.RegionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CatalogSearchUpdateTest {

    private val update = CatalogSearchUpdate()

    private fun product(id: String, name: String) = Product(
        id = ProductId(id),
        name = name,
        category = "Крупы",
        nutritionPer100g = NutritionFacts(calories = 310.0, proteins = 12.6, fats = 3.3, carbs = 57.1),
        prices = listOf(
            RegionalPrice(
                id = "rp-$id",
                productId = ProductId(id),
                regionId = RegionId("ru-msk"),
                price = Price(amount = Money.fromMinor(9_900L)),
            ),
        ),
    )

    @Test
    fun `QueryChanged sets Loading state resets pageToken and emits Search command`() {
        val initial = CatalogSearchState(pageToken = "page-2", category = "Крупы")

        val next = update.update(initial, CatalogSearchEvent.Ui.QueryChanged("гречка"))

        assertEquals("гречка", next.state?.query)
        assertEquals("", next.state?.pageToken)
        assertIs<CatalogSearchState.Content.Loading>(next.state?.content)
        assertEquals(
            listOf(CatalogSearchCommand.Search(query = "гречка", category = "Крупы", pageToken = "")),
            next.commands,
        )
    }

    @Test
    fun `CategorySelected sets Loading and emits Search command`() {
        val initial = CatalogSearchState(query = "творог", pageToken = "page-3")

        val next = update.update(initial, CatalogSearchEvent.Ui.CategorySelected("Молочные"))

        assertEquals("Молочные", next.state?.category)
        assertEquals("", next.state?.pageToken)
        assertIs<CatalogSearchState.Content.Loading>(next.state?.content)
        assertEquals(
            listOf(CatalogSearchCommand.Search(query = "творог", category = "Молочные", pageToken = "")),
            next.commands,
        )
    }

    @Test
    fun `Loaded populates items domainProducts and sets Empty or Success`() {
        val p1 = product("p-1", "Гречка")
        val withFavorite = CatalogSearchState(isFavoriteMap = mapOf(ProductId("p-1") to true))

        val loadedNext = update.update(
            withFavorite,
            CatalogSearchEvent.Internal.Loaded(products = listOf(p1), nextPageToken = "next-1"),
        )
        assertIs<CatalogSearchState.Content.Success>(loadedNext.state?.content)
        assertEquals("next-1", loadedNext.state?.pageToken)
        assertEquals(1, loadedNext.state?.items?.size)
        assertTrue(loadedNext.state?.items?.first()?.isFavorite == true)

        val emptyNext = update.update(
            withFavorite,
            CatalogSearchEvent.Internal.Loaded(products = emptyList(), nextPageToken = ""),
        )
        assertIs<CatalogSearchState.Content.Empty>(emptyNext.state?.content)
    }

    @Test
    fun `ToggleFavorite updates optimistic state and emits ToggleFavorite command`() {
        val p1 = product("p-1", "Гречка")
        val loadedState = requireNotNull(
            update.update(
                CatalogSearchState(),
                CatalogSearchEvent.Internal.Loaded(products = listOf(p1), nextPageToken = ""),
            ).state,
        )

        val addedFav = update.update(loadedState, CatalogSearchEvent.Ui.ToggleFavorite(ProductId("p-1")))
        val addedState = requireNotNull(addedFav.state)
        assertTrue(addedState.items.first().isFavorite)
        assertEquals(listOf(CatalogSearchCommand.ToggleFavorite(p1)), addedFav.commands)

        val removedFav = update.update(
            addedState,
            CatalogSearchEvent.Ui.ToggleFavorite(ProductId("p-1")),
        )
        assertFalse(requireNotNull(removedFav.state).items.first().isFavorite)
    }

    @Test
    fun `LoadNextPage emits Search command only when token present and Success`() {
        val p1 = product("p-1", "Гречка")
        val loadedState = requireNotNull(
            update.update(
                CatalogSearchState(query = "гр"),
                CatalogSearchEvent.Internal.Loaded(products = listOf(p1), nextPageToken = "token-2"),
            ).state,
        )

        val next = update.update(loadedState, CatalogSearchEvent.Ui.LoadNextPage)
        assertEquals(
            listOf(CatalogSearchCommand.Search(query = "гр", category = null, pageToken = "token-2")),
            next.commands,
        )

        val appended = update.update(
            loadedState,
            CatalogSearchEvent.Internal.NextPageLoaded(
                products = listOf(p1, product("p-2", "Рис")),
                nextPageToken = "",
            ),
        )
        assertEquals(2, appended.state?.items?.size)
        assertEquals("", appended.state?.pageToken)
    }

    @Test
    fun `Failed sets Error when items empty and always emits Snackbar news`() {
        val emptyFailure = update.update(
            CatalogSearchState(),
            CatalogSearchEvent.Internal.Failed(RuntimeException("Сеть недоступна")),
        )
        val err = assertIs<CatalogSearchState.Content.Error>(emptyFailure.state?.content)
        assertEquals("Сеть недоступна", err.message)
        assertEquals(listOf(CatalogSearchNews.ShowSnackbar("Сеть недоступна")), emptyFailure.news)

        val loadedState = requireNotNull(
            update.update(
                CatalogSearchState(),
                CatalogSearchEvent.Internal.Loaded(listOf(product("p-1", "Гречка")), ""),
            ).state,
        )
        val nonEmptyFailure = update.update(
            loadedState,
            CatalogSearchEvent.Internal.Failed(RuntimeException("Таймаут")),
        )
        assertEquals(null, nonEmptyFailure.state)
        assertEquals(listOf(CatalogSearchNews.ShowSnackbar("Таймаут")), nonEmptyFailure.news)
    }
}
