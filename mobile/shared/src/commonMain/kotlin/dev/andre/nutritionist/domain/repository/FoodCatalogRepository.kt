package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.FoodItem
import dev.andre.nutritionist.domain.model.FoodItemId

/**
 * Интерфейс репозитория поиска и получения продуктов из каталога.
 */
interface FoodCatalogRepository {
    suspend fun searchFood(query: String, category: String? = null): Result<List<FoodItem>>
    suspend fun getRecentFood(): Result<List<FoodItem>>
    suspend fun getFavoriteFood(): Result<List<FoodItem>>
    suspend fun getFoodById(id: FoodItemId): Result<FoodItem?>
}
