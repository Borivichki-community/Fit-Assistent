package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * Реализация [MealPlanRepository] с каталогом сбалансированных рационов.
 */
class MealPlanRepositoryImpl(
    private val dispatchers: CoroutineDispatchers,
) : MealPlanRepository {

    private val plansFlow = MutableStateFlow(
        listOf(
            MealPlan(
                id = MealPlanId("plan-mediterranean"),
                title = "Средиземноморский баланс",
                description = "Свежая рыба, оливковое масло, цельнозерновые и зелень для долголетия и энергии.",
                category = "Баланс",
                targetCalories = 2200,
                proteinsGrams = 130.0,
                fatsGrams = 75.0,
                carbsGrams = 250.0,
                estimatedWeeklyBudgetRub = 4500.0,
                difficulty = "Легко",
                durationDays = 7,
                rating = 4.9,
                isFavorite = true,
                tags = listOf("Рыба", "Омега-3", "Без сахара"),
            ),
            MealPlan(
                id = MealPlanId("plan-economy"),
                title = "Студенческий эконом",
                description = "Максимальная питательность при минимальном чеке в магазине. Простые и вкусные блюда.",
                category = "Эконом",
                targetCalories = 2000,
                proteinsGrams = 110.0,
                fatsGrams = 65.0,
                carbsGrams = 240.0,
                estimatedWeeklyBudgetRub = 2800.0,
                difficulty = "Очень легко",
                durationDays = 7,
                rating = 4.7,
                isFavorite = false,
                tags = listOf("Бюджетно", "Быстро", "Гречка и птица"),
            ),
            MealPlan(
                id = MealPlanId("plan-cutting"),
                title = "Спортивная сушка",
                description = "Высокое содержание белка и умеренный дефицит калорий для максимального рельефа мышц.",
                category = "Похудение",
                targetCalories = 1800,
                proteinsGrams = 160.0,
                fatsGrams = 50.0,
                carbsGrams = 175.0,
                estimatedWeeklyBudgetRub = 4900.0,
                difficulty = "Средне",
                durationDays = 14,
                rating = 4.8,
                isFavorite = false,
                tags = listOf("Высокий белок", "Дефицит", "Сушка"),
            ),
            MealPlan(
                id = MealPlanId("plan-mass-gain"),
                title = "Массонабор Про",
                description = "Плотный профицит калорий и углеводов для чистого набора качественной мышечной массы.",
                category = "Набор массы",
                targetCalories = 2800,
                proteinsGrams = 170.0,
                fatsGrams = 90.0,
                carbsGrams = 330.0,
                estimatedWeeklyBudgetRub = 5500.0,
                difficulty = "Средне",
                durationDays = 28,
                rating = 4.9,
                isFavorite = false,
                tags = listOf("Профицит", "Сила", "Масса"),
            ),
            MealPlan(
                id = MealPlanId("plan-vegan"),
                title = "Зелёный детокс",
                description = "100% растительный рацион с растительным белком: бобовые, орехи, семена и суперфуды.",
                category = "Веган",
                targetCalories = 1700,
                proteinsGrams = 95.0,
                fatsGrams = 55.0,
                carbsGrams = 210.0,
                estimatedWeeklyBudgetRub = 3900.0,
                difficulty = "Легко",
                durationDays = 7,
                rating = 4.6,
                isFavorite = false,
                tags = listOf("Веган", "Детокс", "Клетчатка"),
            ),
            MealPlan(
                id = MealPlanId("plan-keto"),
                title = "Кето-старт",
                description = "Минимум углеводов и полезные жиры для адаптации к кетозу и стабильного чувства сытости.",
                category = "Кето",
                targetCalories = 2100,
                proteinsGrams = 120.0,
                fatsGrams = 160.0,
                carbsGrams = 30.0,
                estimatedWeeklyBudgetRub = 5200.0,
                difficulty = "Средне",
                durationDays = 14,
                rating = 4.7,
                isFavorite = false,
                tags = listOf("Низкоуглеводное", "Кетоз", "Сытость"),
            ),
        ),
    )

    override suspend fun getMealPlans(query: String, category: String): Result<List<MealPlan>> =
        withContext(dispatchers.io) {
            runCatching {
                val cleanQuery = query.trim().lowercase()
                plansFlow.value.filter { plan ->
                    val matchesQuery = cleanQuery.isEmpty() ||
                        plan.title.lowercase().contains(cleanQuery) ||
                        plan.description.lowercase().contains(cleanQuery) ||
                        plan.tags.any { it.lowercase().contains(cleanQuery) }
                    val matchesCategory = category.isBlank() ||
                        category == "Все" ||
                        plan.category.equals(category, ignoreCase = true)
                    matchesQuery && matchesCategory
                }
            }
        }

    override suspend fun getMealPlanById(id: MealPlanId): Result<MealPlan?> =
        withContext(dispatchers.io) {
            runCatching {
                plansFlow.value.find { it.id == id }
            }
        }

    override suspend fun toggleFavorite(id: MealPlanId): Result<Boolean> =
        withContext(dispatchers.io) {
            runCatching {
                val current = plansFlow.value.toMutableList()
                val index = current.indexOfFirst { it.id == id }
                if (index >= 0) {
                    val updated = current[index].copy(isFavorite = !current[index].isFavorite)
                    current[index] = updated
                    plansFlow.value = current
                    updated.isFavorite
                } else {
                    false
                }
            }
        }
}
