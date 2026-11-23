package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.GroceryItem
import dev.andre.nutritionist.domain.model.GroceryItemId
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanDay
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.domain.model.MealPlanMealItem
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * Реализация [MealPlanRepository] с каталогом сбалансированных рационов,
 * недельным расписанием приемов пищи и списком необходимых покупок.
 */
class MealPlanRepositoryImpl(
    private val dispatchers: CoroutineDispatchers,
) : MealPlanRepository {

    private val plansFlow = MutableStateFlow(
        listOf(
            MealPlan(
                id = MealPlanId("dish-oatmeal"),
                title = "Овсянка с ягодами",
                description = "Овсяные хлопья с лесными ягодами и семенами чиа для идеального утра.",
                category = "Завтрак",
                targetCalories = 320,
                proteinsGrams = 12.0,
                fatsGrams = 8.0,
                carbsGrams = 48.0,
                estimatedWeeklyBudgetRub = 180.0,
                difficulty = "10 мин",
                durationDays = 1,
                rating = 4.9,
                isFavorite = false,
                tags = listOf("Завтрак", "ПП", "Низкоуглеводное"),
            ),
            MealPlan(
                id = MealPlanId("dish-omelette"),
                title = "Омлет со шпинатом",
                description = "Нежный омлет из яичных белков со свежим шпинатом и фетой.",
                category = "Полдник",
                targetCalories = 380,
                proteinsGrams = 22.0,
                fatsGrams = 18.0,
                carbsGrams = 4.0,
                estimatedWeeklyBudgetRub = 220.0,
                difficulty = "15 мин",
                durationDays = 1,
                rating = 4.8,
                isFavorite = true,
                tags = listOf("Полдник", "ПП", "Набор массы"),
            ),
            MealPlan(
                id = MealPlanId("dish-curd-bowl"),
                title = "Творожный боул",
                description = "Фермерский творог 5% с гранолой, малиной и миндальными лепестками.",
                category = "Ужин",
                targetCalories = 290,
                proteinsGrams = 28.0,
                fatsGrams = 10.0,
                carbsGrams = 22.0,
                estimatedWeeklyBudgetRub = 195.0,
                difficulty = "5 мин",
                durationDays = 1,
                rating = 4.9,
                isFavorite = false,
                tags = listOf("Ужин", "ПП", "Кето", "Омега-3"),
            ),
            MealPlan(
                id = MealPlanId("dish-salmon"),
                title = "Лосось с брокколи",
                description = "Стейк дикого лосося на пару с хрустящими соцветиями брокколи.",
                category = "Ужин",
                targetCalories = 420,
                proteinsGrams = 34.0,
                fatsGrams = 16.0,
                carbsGrams = 8.0,
                estimatedWeeklyBudgetRub = 410.0,
                difficulty = "20 мин",
                durationDays = 1,
                rating = 5.0,
                isFavorite = false,
                tags = listOf("Ужин", "ПП", "Кето", "Омега-3"),
            ),
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

    private val activePlansMap = MutableStateFlow<Map<String, MealPlanId>>(
        emptyMap(),
    )

    private val sampleDays: List<MealPlanDay> = listOf(
        MealPlanDay(
            dayNumber = 1,
            dayName = "Понедельник",
            shortDayName = "Пн",
            targetCalories = 2190,
            meals = listOf(
                MealPlanMealItem(
                    title = "Овсяная каша с ягодами и миндалем",
                    mealType = "Завтрак",
                    calories = 420,
                    proteinsGrams = 14.0,
                    fatsGrams = 12.0,
                    carbsGrams = 65.0,
                ),
                MealPlanMealItem(
                    title = "Лосось на гриле с диким рисом",
                    mealType = "Обед",
                    calories = 650,
                    proteinsGrams = 42.0,
                    fatsGrams = 24.0,
                    carbsGrams = 62.0,
                ),
                MealPlanMealItem(
                    title = "Греческий йогурт с грецкими орехами",
                    mealType = "Полдник",
                    calories = 280,
                    proteinsGrams = 18.0,
                    fatsGrams = 14.0,
                    carbsGrams = 18.0,
                ),
                MealPlanMealItem(
                    title = "Салат с тунцом, оливками и авокадо",
                    mealType = "Ужин",
                    calories = 550,
                    proteinsGrams = 38.0,
                    fatsGrams = 25.0,
                    carbsGrams = 35.0,
                ),
            ),
        ),
        MealPlanDay(
            dayNumber = 2,
            dayName = "Вторник",
            shortDayName = "Вт",
            targetCalories = 2210,
            meals = listOf(
                MealPlanMealItem(
                    title = "Омлет со шпинатом и сыром фета",
                    mealType = "Завтрак",
                    calories = 450,
                    proteinsGrams = 28.0,
                    fatsGrams = 26.0,
                    carbsGrams = 12.0,
                ),
                MealPlanMealItem(
                    title = "Куриная грудка с булгуром и овощами",
                    mealType = "Обед",
                    calories = 620,
                    proteinsGrams = 48.0,
                    fatsGrams = 16.0,
                    carbsGrams = 68.0,
                ),
                MealPlanMealItem(
                    title = "Яблоко и горсть кешью",
                    mealType = "Полдник",
                    calories = 240,
                    proteinsGrams = 6.0,
                    fatsGrams = 14.0,
                    carbsGrams = 26.0,
                ),
                MealPlanMealItem(
                    title = "Запеченная треска с брокколи и кунжутом",
                    mealType = "Ужин",
                    calories = 520,
                    proteinsGrams = 44.0,
                    fatsGrams = 18.0,
                    carbsGrams = 38.0,
                ),
            ),
        ),
        MealPlanDay(
            dayNumber = 3,
            dayName = "Среда",
            shortDayName = "Ср",
            targetCalories = 2200,
            meals = listOf(
                MealPlanMealItem(
                    title = "Сырники из творога с нежирной сметаной",
                    mealType = "Завтрак",
                    calories = 480,
                    proteinsGrams = 32.0,
                    fatsGrams = 18.0,
                    carbsGrams = 42.0,
                ),
                MealPlanMealItem(
                    title = "Суп-пюре из красной чечевицы и томатов",
                    mealType = "Обед",
                    calories = 540,
                    proteinsGrams = 26.0,
                    fatsGrams = 14.0,
                    carbsGrams = 74.0,
                ),
                MealPlanMealItem(
                    title = "Протеиновый смузи с бананом и шпинатом",
                    mealType = "Полдник",
                    calories = 290,
                    proteinsGrams = 22.0,
                    fatsGrams = 5.0,
                    carbsGrams = 38.0,
                ),
                MealPlanMealItem(
                    title = "Индейка су-вид с запеченной тыквой",
                    mealType = "Ужин",
                    calories = 580,
                    proteinsGrams = 45.0,
                    fatsGrams = 18.0,
                    carbsGrams = 48.0,
                ),
            ),
        ),
        MealPlanDay(
            dayNumber = 4,
            dayName = "Четверг",
            shortDayName = "Чт",
            targetCalories = 2230,
            meals = listOf(
                MealPlanMealItem(
                    title = "Авокадо-тост с яйцом пашот",
                    mealType = "Завтрак",
                    calories = 430,
                    proteinsGrams = 16.0,
                    fatsGrams = 22.0,
                    carbsGrams = 40.0,
                ),
                MealPlanMealItem(
                    title = "Паста из твердых сортов с морепродуктами",
                    mealType = "Обед",
                    calories = 670,
                    proteinsGrams = 38.0,
                    fatsGrams = 18.0,
                    carbsGrams = 85.0,
                ),
                MealPlanMealItem(
                    title = "Груша и горсть миндаля",
                    mealType = "Полдник",
                    calories = 220,
                    proteinsGrams = 5.0,
                    fatsGrams = 12.0,
                    carbsGrams = 25.0,
                ),
                MealPlanMealItem(
                    title = "Стейк из тунца с зеленым салатом",
                    mealType = "Ужин",
                    calories = 510,
                    proteinsGrams = 46.0,
                    fatsGrams = 15.0,
                    carbsGrams = 20.0,
                ),
            ),
        ),
        MealPlanDay(
            dayNumber = 5,
            dayName = "Пятница",
            shortDayName = "Пт",
            targetCalories = 2170,
            meals = listOf(
                MealPlanMealItem(
                    title = "Гранола с миндальным молоком и чиа",
                    mealType = "Завтрак",
                    calories = 410,
                    proteinsGrams = 12.0,
                    fatsGrams = 14.0,
                    carbsGrams = 58.0,
                ),
                MealPlanMealItem(
                    title = "Говядина на пару с киноа и цукини",
                    mealType = "Обед",
                    calories = 660,
                    proteinsGrams = 48.0,
                    fatsGrams = 22.0,
                    carbsGrams = 60.0,
                ),
                MealPlanMealItem(
                    title = "Кефир 2.5% с черникой",
                    mealType = "Полдник",
                    calories = 190,
                    proteinsGrams = 10.0,
                    fatsGrams = 5.0,
                    carbsGrams = 22.0,
                ),
                MealPlanMealItem(
                    title = "Кальмары гриль со спаржей",
                    mealType = "Ужин",
                    calories = 490,
                    proteinsGrams = 42.0,
                    fatsGrams = 12.0,
                    carbsGrams = 35.0,
                ),
            ),
        ),
        MealPlanDay(
            dayNumber = 6,
            dayName = "Суббота",
            shortDayName = "Сб",
            targetCalories = 2250,
            meals = listOf(
                MealPlanMealItem(
                    title = "Овсяноблин с слабосоленой семгой",
                    mealType = "Завтрак",
                    calories = 460,
                    proteinsGrams = 26.0,
                    fatsGrams = 18.0,
                    carbsGrams = 45.0,
                ),
                MealPlanMealItem(
                    title = "Стейк форели с печеным картофелем",
                    mealType = "Обед",
                    calories = 680,
                    proteinsGrams = 44.0,
                    fatsGrams = 26.0,
                    carbsGrams = 58.0,
                ),
                MealPlanMealItem(
                    title = "Грейпфрут и грецкие орехи",
                    mealType = "Полдник",
                    calories = 230,
                    proteinsGrams = 5.0,
                    fatsGrams = 14.0,
                    carbsGrams = 22.0,
                ),
                MealPlanMealItem(
                    title = "Теплый салат с ростбифом и руколой",
                    mealType = "Ужин",
                    calories = 530,
                    proteinsGrams = 40.0,
                    fatsGrams = 24.0,
                    carbsGrams = 28.0,
                ),
            ),
        ),
        MealPlanDay(
            dayNumber = 7,
            dayName = "Воскресенье",
            shortDayName = "Вс",
            targetCalories = 2180,
            meals = listOf(
                MealPlanMealItem(
                    title = "Шакшука с томатами и болгарским перцем",
                    mealType = "Завтрак",
                    calories = 440,
                    proteinsGrams = 22.0,
                    fatsGrams = 22.0,
                    carbsGrams = 34.0,
                ),
                MealPlanMealItem(
                    title = "Фаршированный перец с индейкой и бурым рисом",
                    mealType = "Обед",
                    calories = 630,
                    proteinsGrams = 42.0,
                    fatsGrams = 18.0,
                    carbsGrams = 65.0,
                ),
                MealPlanMealItem(
                    title = "Творожный мусс с манго",
                    mealType = "Полдник",
                    calories = 260,
                    proteinsGrams = 20.0,
                    fatsGrams = 6.0,
                    carbsGrams = 30.0,
                ),
                MealPlanMealItem(
                    title = "Белая рыба на пару с брокколи и лимоном",
                    mealType = "Ужин",
                    calories = 480,
                    proteinsGrams = 42.0,
                    fatsGrams = 10.0,
                    carbsGrams = 25.0,
                ),
            ),
        ),
    )

    private val groceriesFlow = MutableStateFlow(
        listOf(
            GroceryItem(GroceryItemId("g-1"), "Авокадо Хасс", "3 шт", "Овощи и зелень", false, 350.0),
            GroceryItem(GroceryItemId("g-2"), "Брокколи свежая", "500 г", "Овощи и зелень", true, 190.0),
            GroceryItem(GroceryItemId("g-3"), "Томаты черри", "400 г", "Овощи и зелень", false, 240.0),
            GroceryItem(GroceryItemId("g-4"), "Шпинат свежий бэби", "200 г", "Овощи и зелень", false, 180.0),
            GroceryItem(GroceryItemId("g-5"), "Филе лосося охлажденное", "600 г", "Мясо и рыба", false, 1200.0),
            GroceryItem(GroceryItemId("g-6"), "Стейк из тунца", "400 г", "Мясо и рыба", true, 750.0),
            GroceryItem(GroceryItemId("g-7"), "Филе индейки", "800 г", "Мясо и рыба", false, 540.0),
            GroceryItem(GroceryItemId("g-8"), "Греческий йогурт 2%", "400 г", "Молочные продукты", true, 170.0),
            GroceryItem(GroceryItemId("g-9"), "Сыр Фета греческий", "250 г", "Молочные продукты", false, 280.0),
            GroceryItem(GroceryItemId("g-10"), "Яйца фермерские СО", "10 шт", "Молочные продукты", true, 140.0),
            GroceryItem(GroceryItemId("g-11"), "Дикий рис микс", "500 г", "Бакалея и орехи", false, 220.0),
            GroceryItem(GroceryItemId("g-12"), "Орехи миндаль жареный", "200 г", "Бакалея и орехи", false, 340.0),
            GroceryItem(GroceryItemId("g-13"), "Оливковое масло Extra Virgin", "500 мл", "Бакалея и орехи", true, 690.0),
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

    override suspend fun getActiveMealPlan(userId: UserId): Result<MealPlan?> =
        withContext(dispatchers.io) {
            runCatching {
                val planId = activePlansMap.value[userId.raw]
                if (planId != null) {
                    plansFlow.value.find { it.id == planId }
                } else {
                    null
                }
            }
        }

    override suspend fun setActiveMealPlan(userId: UserId, planId: MealPlanId): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val current = activePlansMap.value.toMutableMap()
                current[userId.raw] = planId
                activePlansMap.value = current
            }
        }

    override suspend fun deactivateMealPlan(userId: UserId): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val current = activePlansMap.value.toMutableMap()
                current.remove(userId.raw)
                activePlansMap.value = current
            }
        }

    override suspend fun getMealPlanDays(planId: MealPlanId): Result<List<MealPlanDay>> =
        withContext(dispatchers.io) {
            runCatching {
                sampleDays
            }
        }

    override suspend fun getGroceryList(planId: MealPlanId): Result<List<GroceryItem>> =
        withContext(dispatchers.io) {
            runCatching {
                groceriesFlow.value
            }
        }

    override suspend fun toggleGroceryItem(planId: MealPlanId, itemId: GroceryItemId): Result<Boolean> =
        withContext(dispatchers.io) {
            runCatching {
                val current = groceriesFlow.value.toMutableList()
                val index = current.indexOfFirst { it.id == itemId }
                if (index >= 0) {
                    val updated = current[index].copy(isBought = !current[index].isBought)
                    current[index] = updated
                    groceriesFlow.value = current
                    updated.isBought
                } else {
                    false
                }
            }
        }
}
