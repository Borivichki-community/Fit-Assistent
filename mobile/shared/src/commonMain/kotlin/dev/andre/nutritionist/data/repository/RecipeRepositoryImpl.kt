package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.domain.model.AiRecipePrompt
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Реализация [RecipeRepository] с потокобезопасным кэшем и симулятором AI-генерации.
 */
class RecipeRepositoryImpl : RecipeRepository {

    private val mutex = Mutex()

    private val initialRecipes = listOf(
        Recipe(
            id = RecipeId("recipe-1"),
            title = "Овсяная каша с карамелизированным яблоком",
            description = "Нежная овсянка долгой варки с корицей, томлеными кусочками яблока и грецким орехом.",
            mealType = MealType.BREAKFAST,
            calories = 380,
            proteins = 12.0,
            fats = 9.0,
            carbs = 62.0,
            estimatedCostRub = 95,
            cookingTimeMinutes = 15,
            ingredients = listOf(
                RecipeIngredient("Овсяные хлопья монастырские", "70 г"),
                RecipeIngredient("Яблоко сезонное", "1 шт"),
                RecipeIngredient("Молоко 2.5%", "150 мл"),
                RecipeIngredient("Корица молотая", "1/2 ч. л."),
                RecipeIngredient("Грецкие орехи", "15 г"),
            ),
            steps = listOf(
                "В сотейнике подогреть молоко с добавлением 50 мл воды.",
                "Всыпать хлопья, варить на слабом огне 10 минут, периодически помешивая.",
                "Яблоко нарезать кубиками, слегка припустить на сухой сковороде с корицей 3 минуты.",
                "Выложить кашу в тарелку, сверху украсить теплыми яблоками и измельченными орехами.",
            ),
            isFavorite = true,
        ),
        Recipe(
            id = RecipeId("recipe-2"),
            title = "Филе индейки с гречкой и печеными овощами",
            description = "Сочное филе индейки на гриле с рассыпчатой ядрицей, кабачком и болгарским перцем.",
            mealType = MealType.LUNCH,
            calories = 520,
            proteins = 46.0,
            fats = 11.0,
            carbs = 58.0,
            estimatedCostRub = 210,
            cookingTimeMinutes = 25,
            ingredients = listOf(
                RecipeIngredient("Грудка индейки", "180 г"),
                RecipeIngredient("Крупа гречневая", "70 г"),
                RecipeIngredient("Кабачок молодой", "100 г"),
                RecipeIngredient("Перец болгарский сладкий", "80 г"),
                RecipeIngredient("Оливковое масло", "1 ч. л."),
            ),
            steps = listOf(
                "Гречку промыть и отварить в подсоленной воде в соотношении 1:2 до готовности.",
                "Индейку нарезать медальонами, приправить паприкой и солью.",
                "Овощи нарезать ломтиками, сбрызнуть маслом.",
                "Обжарить индейку и овощи на сковороде-гриль по 4-5 минут с каждой стороны.",
                "Подавать с ароматной зеленью.",
            ),
            isFavorite = false,
        ),
        Recipe(
            id = RecipeId("recipe-3"),
            title = "Запеченный минтай с диким рисом и брокколи",
            description = "Диетический ужин, богатый омега-3 и белком, с легкой лимонной ноткой.",
            mealType = MealType.DINNER,
            calories = 430,
            proteins = 38.0,
            fats = 8.0,
            carbs = 51.0,
            estimatedCostRub = 175,
            cookingTimeMinutes = 30,
            ingredients = listOf(
                RecipeIngredient("Филе минтая", "200 г"),
                RecipeIngredient("Рис бурый или смесь сортов", "60 г"),
                RecipeIngredient("Соцветия брокколи", "120 г"),
                RecipeIngredient("Сок лимона", "1 ст. л."),
                RecipeIngredient("Прованские травы", "1/2 ч. л."),
            ),
            steps = listOf(
                "Рис отварить до готовности (около 25 минут).",
                "Филе минтая выложить на пергамент, сбрызнуть лимоном, посыпать травами.",
                "Рядом выложить брокколи.",
                "Запекать в духовке при 180°C в течение 18-20 минут.",
            ),
            isFavorite = true,
        ),
        Recipe(
            id = RecipeId("recipe-4"),
            title = "Творожные сырники с ванилью и ягодным кули",
            description = "Пышные сырники из фермерского творога 5% с минимальным содержанием муки.",
            mealType = MealType.BREAKFAST,
            calories = 410,
            proteins = 34.0,
            fats = 12.0,
            carbs = 41.0,
            estimatedCostRub = 150,
            cookingTimeMinutes = 20,
            ingredients = listOf(
                RecipeIngredient("Творог 5%", "200 г"),
                RecipeIngredient("Яйцо куриное", "1 шт"),
                RecipeIngredient("Мука рисовая", "25 г"),
                RecipeIngredient("Ванилин", "на кончике ножа"),
                RecipeIngredient("Замороженные ягоды (вишня/малина)", "60 г"),
            ),
            steps = listOf(
                "Творог размять вилкой, смешать с яйцом, ванилином и рисовой мукой.",
                "Сформировать аккуратные сырники шайбочками.",
                "Обжарить на среднем огне под крышкой по 4 минуты с каждой стороны.",
                "Ягоды прогреть в сотейнике 2 минуты и полить готовые сырники.",
            ),
            isFavorite = false,
        ),
    )

    private val savedRecipesFlow = MutableStateFlow(initialRecipes)

    override suspend fun generateRecipe(prompt: AiRecipePrompt): Result<Recipe> = mutex.withLock {
        runCatching {
            val title = if (prompt.query.isNotBlank()) {
                "Авторское блюдо: ${prompt.query.trim().replaceFirstChar { it.uppercase() }}"
            } else {
                when (prompt.mealType) {
                    MealType.BREAKFAST -> "Сбалансированный белковый завтрак Шефа"
                    MealType.LUNCH -> "Сытный фермерский обед с высоким белком"
                    MealType.DINNER -> "Легкий восстановительный ужин"
                    MealType.SNACK -> "Энергетический питательный перекус"
                }
            }

            val targetCalories = prompt.targetCalories.coerceIn(200, 1500)
            val proteins = (targetCalories * 0.28 / 4.0).let { kotlin.math.round(it * 10) / 10.0 }
            val fats = (targetCalories * 0.25 / 9.0).let { kotlin.math.round(it * 10) / 10.0 }
            val carbs = (targetCalories * 0.47 / 4.0).let { kotlin.math.round(it * 10) / 10.0 }
            val generatedId = RecipeId("gen-${prompt.mealType.name.lowercase()}-${targetCalories}")

            val generatedRecipe = Recipe(
                id = generatedId,
                title = title,
                description = "Персональный рецепт, сгенерированный AI-нутрициологом под цель ${targetCalories} ккал и бюджет до ${prompt.targetBudgetRub} ₽.",
                mealType = prompt.mealType,
                calories = targetCalories,
                proteins = proteins,
                fats = fats,
                carbs = carbs,
                estimatedCostRub = prompt.targetBudgetRub.coerceAtLeast(80),
                cookingTimeMinutes = 20,
                ingredients = listOf(
                    RecipeIngredient("Основной источник белка", "${(proteins * 4.5).toInt()} г"),
                    RecipeIngredient("Сложные углеводы", "${(carbs * 1.5).toInt()} г"),
                    RecipeIngredient("Клетчатка (зелень и овощи)", "150 г"),
                    RecipeIngredient("Полезные жиры (масло/семена)", "10 г"),
                    RecipeIngredient("Специи и натуральные травы", "по вкусу"),
                ),
                steps = listOf(
                    "Подготовить и взвесить все ингредиенты согласно граммовкам.",
                    "Обработать белковый компонент щадящим методом (запекание, варка или гриль).",
                    "Приготовить гарнир из сложных углеводов до состояния аль-денте.",
                    "Собрать сбалансированную тарелку нутрициолога, добавить свежие овощи и зелень.",
                ),
                isFavorite = false,
            )

            // Сохраняем в кэш
            val current = savedRecipesFlow.value.toMutableList()
            current.removeAll { it.id == generatedId }
            current.add(0, generatedRecipe)
            savedRecipesFlow.value = current

            generatedRecipe
        }
    }

    override suspend fun getSavedRecipes(): Result<List<Recipe>> = mutex.withLock {
        Result.success(savedRecipesFlow.value)
    }

    override fun observeSavedRecipes(): Flow<List<Recipe>> = savedRecipesFlow.asStateFlow()

    override suspend fun toggleFavorite(recipeId: RecipeId): Result<Recipe> = mutex.withLock {
        runCatching {
            val list = savedRecipesFlow.value.toMutableList()
            val index = list.indexOfFirst { it.id == recipeId }
            require(index != -1) { "Рецепт с ID ${recipeId.raw} не найден" }
            val old = list[index]
            val updated = old.copy(isFavorite = !old.isFavorite)
            list[index] = updated
            savedRecipesFlow.value = list
            updated
        }
    }

    override suspend fun saveRecipe(recipe: Recipe): Result<Recipe> = mutex.withLock {
        runCatching {
            val list = savedRecipesFlow.value.toMutableList()
            list.removeAll { it.id == recipe.id }
            list.add(0, recipe)
            savedRecipesFlow.value = list
            recipe
        }
    }
}
