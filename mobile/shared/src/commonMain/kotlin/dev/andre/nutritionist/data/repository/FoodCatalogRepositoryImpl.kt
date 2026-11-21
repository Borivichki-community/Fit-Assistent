package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.FoodItem
import dev.andre.nutritionist.domain.model.FoodItemId
import dev.andre.nutritionist.domain.repository.FoodCatalogRepository
import kotlinx.coroutines.withContext

/**
 * Реализация каталога продуктов с базовой базой данных популярных продуктов питания.
 */
class FoodCatalogRepositoryImpl(
    private val dispatchers: CoroutineDispatchers,
) : FoodCatalogRepository {

    private val seedProducts = listOf(
        FoodItem(
            id = FoodItemId("food-oatmeal"),
            name = "Овсяная каша на воде",
            brand = "Традиционная",
            caloriesPer100g = 68,
            proteinsPer100g = 2.4,
            fatsPer100g = 1.4,
            carbsPer100g = 12.0,
            defaultPortionGrams = 200.0,
            category = "Завтрак",
        ),
        FoodItem(
            id = FoodItemId("food-chicken-breast"),
            name = "Куриное филе отварное",
            brand = "Домашнее",
            caloriesPer100g = 165,
            proteinsPer100g = 31.0,
            fatsPer100g = 3.6,
            carbsPer100g = 0.0,
            defaultPortionGrams = 150.0,
            category = "Обед",
        ),
        FoodItem(
            id = FoodItemId("food-buckwheat"),
            name = "Гречневая каша рассыпчатая",
            brand = "Алтайская",
            caloriesPer100g = 110,
            proteinsPer100g = 4.2,
            fatsPer100g = 1.1,
            carbsPer100g = 21.3,
            defaultPortionGrams = 150.0,
            category = "Обед",
        ),
        FoodItem(
            id = FoodItemId("food-cottage-cheese"),
            name = "Творог 5%",
            brand = "Простоквашино",
            caloriesPer100g = 121,
            proteinsPer100g = 16.0,
            fatsPer100g = 5.0,
            carbsPer100g = 3.0,
            defaultPortionGrams = 180.0,
            category = "Ужин",
        ),
        FoodItem(
            id = FoodItemId("food-banana"),
            name = "Банан спелый",
            brand = "Эквадор",
            caloriesPer100g = 89,
            proteinsPer100g = 1.1,
            fatsPer100g = 0.3,
            carbsPer100g = 22.8,
            defaultPortionGrams = 120.0,
            category = "Перекус",
        ),
        FoodItem(
            id = FoodItemId("food-egg"),
            name = "Яйцо куриное варёное (С0)",
            brand = "Деревенское",
            caloriesPer100g = 155,
            proteinsPer100g = 12.6,
            fatsPer100g = 10.6,
            carbsPer100g = 0.8,
            defaultPortionGrams = 60.0,
            category = "Завтрак",
        ),
        FoodItem(
            id = FoodItemId("food-epica"),
            name = "Йогурт Epica натуральный",
            brand = "Ehrmann",
            caloriesPer100g = 65,
            proteinsPer100g = 3.8,
            fatsPer100g = 1.0,
            carbsPer100g = 10.6,
            defaultPortionGrams = 130.0,
            category = "Перекус",
        ),
        FoodItem(
            id = FoodItemId("food-salmon"),
            name = "Лосось на пару",
            brand = "Атлантика",
            caloriesPer100g = 197,
            proteinsPer100g = 21.6,
            fatsPer100g = 12.3,
            carbsPer100g = 0.0,
            defaultPortionGrams = 140.0,
            category = "Ужин",
        ),
        FoodItem(
            id = FoodItemId("food-protein-bar"),
            name = "Протеиновый батончик ChikaLab",
            brand = "Bombbar",
            caloriesPer100g = 320,
            proteinsPer100g = 33.3,
            fatsPer100g = 13.3,
            carbsPer100g = 6.7,
            defaultPortionGrams = 60.0,
            category = "Перекус",
        ),
        FoodItem(
            id = FoodItemId("food-avocado"),
            name = "Авокадо Хасс",
            brand = "Израиль",
            caloriesPer100g = 160,
            proteinsPer100g = 2.0,
            fatsPer100g = 14.7,
            carbsPer100g = 1.8,
            defaultPortionGrams = 70.0,
            category = "Завтрак",
        ),
    )

    override suspend fun searchFood(query: String, category: String?): Result<List<FoodItem>> =
        withContext(dispatchers.io) {
            runCatching {
                val cleanQuery = query.trim().lowercase()
                seedProducts.filter { item ->
                    val matchesQuery = cleanQuery.isEmpty() ||
                        item.name.lowercase().contains(cleanQuery) ||
                        item.brand.lowercase().contains(cleanQuery)
                    val matchesCategory = category.isNullOrBlank() ||
                        category == "Все" ||
                        item.category.equals(category, ignoreCase = true)
                    matchesQuery && matchesCategory
                }
            }
        }

    override suspend fun getRecentFood(): Result<List<FoodItem>> =
        withContext(dispatchers.io) {
            runCatching { seedProducts.take(4) }
        }

    override suspend fun getFavoriteFood(): Result<List<FoodItem>> =
        withContext(dispatchers.io) {
            runCatching { seedProducts.filter { it.caloriesPer100g < 150 } }
        }

    override suspend fun getFoodById(id: FoodItemId): Result<FoodItem?> =
        withContext(dispatchers.io) {
            runCatching { seedProducts.find { it.id == id } }
        }
}
