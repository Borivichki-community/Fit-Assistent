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
    private var generationSeed = 0

    override suspend fun generateRecipe(prompt: AiRecipePrompt): Result<Recipe> = mutex.withLock {
        runCatching {
            val targetCalories = prompt.targetCalories.coerceIn(200, 1500)
            val mealCandidates = recipeArchetypes.filter { it.mealType == prompt.mealType }
            val cleanQuery = prompt.query.trim().lowercase()

            val chosenArchetype = if (cleanQuery.isNotBlank()) {
                val matched = mealCandidates.maxByOrNull { archetype ->
                    archetype.keywords.count { keyword -> cleanQuery.contains(keyword) }
                }
                if (matched != null && matched.keywords.any { cleanQuery.contains(it) }) {
                    matched
                } else {
                    mealCandidates[(generationSeed++) % mealCandidates.size]
                }
            } else {
                mealCandidates[(generationSeed++) % mealCandidates.size]
            }

            val scale = (targetCalories.toDouble() / chosenArchetype.baseCalories).coerceIn(0.5, 2.5)
            val proteins = (chosenArchetype.baseProteins * scale).let { kotlin.math.round(it * 10) / 10.0 }
            val fats = (chosenArchetype.baseFats * scale).let { kotlin.math.round(it * 10) / 10.0 }
            val carbs = (chosenArchetype.baseCarbs * scale).let { kotlin.math.round(it * 10) / 10.0 }

            val title = if (prompt.query.isNotBlank() && !chosenArchetype.title.lowercase().contains(cleanQuery.take(5))) {
                "${chosenArchetype.title} (по запросу: ${prompt.query.trim()})"
            } else {
                chosenArchetype.title
            }

            val scaledIngredients = chosenArchetype.baseIngredients.map { (name, baseAmount, unit) ->
                val scaledAmount = (baseAmount * scale).let {
                    if (unit == "г" || unit == "мл") kotlin.math.round(it / 5) * 5 else kotlin.math.round(it * 10) / 10.0
                }
                val formattedAmount = if (scaledAmount % 1.0 == 0.0) scaledAmount.toInt().toString() else scaledAmount.toString()
                RecipeIngredient(name, "$formattedAmount $unit")
            }

            val randomSuffix = kotlin.random.Random.nextInt(1000, 9999)
            val generatedId = RecipeId("gen-${prompt.mealType.name.lowercase()}-$targetCalories-$randomSuffix")

            val generatedRecipe = Recipe(
                id = generatedId,
                title = title,
                description = chosenArchetype.description + " Индивидуально сбалансировано нутрициологом под цель $targetCalories ккал.",
                mealType = prompt.mealType,
                calories = targetCalories,
                proteins = proteins,
                fats = fats,
                carbs = carbs,
                estimatedCostRub = if (prompt.targetBudgetRub > 0) prompt.targetBudgetRub else chosenArchetype.baseCostRub,
                cookingTimeMinutes = chosenArchetype.cookingTimeMinutes,
                ingredients = scaledIngredients,
                steps = chosenArchetype.steps,
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

private data class IngredientTemplate(
    val name: String,
    val baseAmount: Double,
    val unit: String,
)

private data class RecipeArchetype(
    val title: String,
    val description: String,
    val mealType: MealType,
    val baseCalories: Int,
    val baseProteins: Double,
    val baseFats: Double,
    val baseCarbs: Double,
    val baseCostRub: Int,
    val cookingTimeMinutes: Int,
    val baseIngredients: List<IngredientTemplate>,
    val steps: List<String>,
    val keywords: List<String>,
)

private val recipeArchetypes: List<RecipeArchetype> = listOf(
    // Завтраки
    RecipeArchetype(
        title = "Пышные сырники из фермерского творога с черничным конфи",
        description = "Нежнейшие сырники с хрустящей корочкой, минимумом рисовой муки и натуральным ягодным кули.",
        mealType = MealType.BREAKFAST,
        baseCalories = 420,
        baseProteins = 36.0,
        baseFats = 12.0,
        baseCarbs = 42.0,
        baseCostRub = 180,
        cookingTimeMinutes = 15,
        baseIngredients = listOf(
            IngredientTemplate("Творог 5% фермерский", 180.0, "г"),
            IngredientTemplate("Яйцо куриное С0", 1.0, "шт"),
            IngredientTemplate("Мука рисовая цельнозерновая", 25.0, "г"),
            IngredientTemplate("Черника свежая или замороженная", 50.0, "г"),
            IngredientTemplate("Натуральный сироп топинамбура", 10.0, "г"),
        ),
        steps = listOf(
            "Размять творог вилкой, добавить яйцо и рисовую муку до пластичной консистенции.",
            "Сформировать ровные сырники с помощью стакана или ножа.",
            "Обжарить на сковороде под крышкой по 4 минуты с каждой стороны до золотистой корочки.",
            "Чернику прогреть в сотейнике 2 минуты и полить готовые сырники.",
        ),
        keywords = listOf("сырник", "творог", "ягод", "сладк", "десерт"),
    ),
    RecipeArchetype(
        title = "Скрамбл с авокадо и слабосоленой форелью на тосте",
        description = "Шелковистый скрамбл со сливочной текстурой, спелым авокадо хасс и ломтиками мурманской форели.",
        mealType = MealType.BREAKFAST,
        baseCalories = 480,
        baseProteins = 28.0,
        baseFats = 26.0,
        baseCarbs = 34.0,
        baseCostRub = 290,
        cookingTimeMinutes = 10,
        baseIngredients = listOf(
            IngredientTemplate("Яйца куриные отборные", 2.0, "шт"),
            IngredientTemplate("Филе форели слабосоленое", 60.0, "г"),
            IngredientTemplate("Авокадо хасс", 50.0, "г"),
            IngredientTemplate("Тост цельнозерновой", 45.0, "г"),
            IngredientTemplate("Бэби-шпинат свежий", 20.0, "г"),
        ),
        steps = listOf(
            "Подсушить цельнозерновой тост на сухой сковороде или в тостере.",
            "Взбить яйца со щепоткой соли и вылить на разогретую сковороду с антипригарным покрытием.",
            "Готовить скрамбл на медленном огне, постоянно помешивая лопаткой до кремовой текстуры.",
            "На тост выложить размятый авокадо, шпинат, теплый скрамбл и ломтики форели.",
        ),
        keywords = listOf("яйц", "скрамбл", "омлет", "авокадо", "рыб", "форель", "лосось", "тост"),
    ),
    RecipeArchetype(
        title = "Овсяные панкейки с бананом и арахисовой пастой",
        description = "Воздушные панкейки без добавления сахара и пшеничной муки с мягким банановым вкусом.",
        mealType = MealType.BREAKFAST,
        baseCalories = 450,
        baseProteins = 22.0,
        baseFats = 14.0,
        baseCarbs = 60.0,
        baseCostRub = 140,
        cookingTimeMinutes = 15,
        baseIngredients = listOf(
            IngredientTemplate("Овсяные хлопья перемолотые", 60.0, "г"),
            IngredientTemplate("Банан спелый", 1.0, "шт"),
            IngredientTemplate("Яичный белок", 2.0, "шт"),
            IngredientTemplate("Паста арахисовая 100%", 15.0, "г"),
            IngredientTemplate("Корица молотая", 2.0, "г"),
        ),
        steps = listOf(
            "Измельчить в блендере овсяные хлопья, банан и белки до гладкого теста.",
            "Разогреть сухую сковороду с антипригарным покрытием.",
            "Выпекать панкейки по 2 минуты с каждой стороны под крышкой.",
            "Подавать теплыми, смазав натуральной арахисовой пастой и посыпав корицей.",
        ),
        keywords = listOf("панкейк", "оладь", "банан", "овсянк", "арахис", "без сахара"),
    ),
    RecipeArchetype(
        title = "Зеленый омлет со шпинатом, рикоттой и кедровыми орешками",
        description = "Легкий утренний белковый заряд со свежей зеленью, сливочной рикоттой и хрустящими орехами.",
        mealType = MealType.BREAKFAST,
        baseCalories = 390,
        baseProteins = 26.0,
        baseFats = 24.0,
        baseCarbs = 18.0,
        baseCostRub = 210,
        cookingTimeMinutes = 12,
        baseIngredients = listOf(
            IngredientTemplate("Яйца куриные", 3.0, "шт"),
            IngredientTemplate("Шпинат свежий листовой", 60.0, "г"),
            IngredientTemplate("Сыр рикотта 8%", 50.0, "г"),
            IngredientTemplate("Кедровые орехи", 10.0, "г"),
            IngredientTemplate("Томаты черри", 50.0, "г"),
        ),
        steps = listOf(
            "Шпинат припустить на сковороде 1 минуту до мягкости.",
            "Взбить яйца, влить к шпинату и готовить на среднем огне 3 минуты.",
            "Сверху выложить кнели из рикотты и половинки черри.",
            "Сложить омлет пополам, посыпать слегка обжаренными кедровыми орехами.",
        ),
        keywords = listOf("зелен", "омлет", "шпинат", "рикотт", "сыр", "орех"),
    ),

    // Обеды
    RecipeArchetype(
        title = "Стейк из грудки индейки с диким рисом и стручковой фасолью",
        description = "Максимально чистый белковый обед: сочная индейка с гарниром из смеси дикого риса и зеленых овощей.",
        mealType = MealType.LUNCH,
        baseCalories = 540,
        baseProteins = 48.0,
        baseFats = 12.0,
        baseCarbs = 60.0,
        baseCostRub = 240,
        cookingTimeMinutes = 20,
        baseIngredients = listOf(
            IngredientTemplate("Филе грудки индейки", 180.0, "г"),
            IngredientTemplate("Смесь дикого и бурого риса", 65.0, "г"),
            IngredientTemplate("Стручковая фасоль", 110.0, "г"),
            IngredientTemplate("Масло оливковое", 5.0, "мл"),
            IngredientTemplate("Паприка копченая и травы", 3.0, "г"),
        ),
        steps = listOf(
            "Рис промыть и отварить в подсоленной воде 20 минут до рассыпчатости.",
            "Индейку нарезать стейками толщиной 1.5 см, приправить паприкой и травами.",
            "Обжарить стейк на гриле или сковороде по 4 минуты с каждой стороны.",
            "Стручковую фасоль припустить на пару 4 минуты со щепоткой соли.",
        ),
        keywords = listOf("индейк", "рис", "фасоль", "птиц", "мяс", "белок", "спорт"),
    ),
    RecipeArchetype(
        title = "Филе атлантического лосося с брокколи в соусе терияки",
        description = "Премиальное рыбное блюдо, насыщенное омега-3 жирными кислотами, с хрустящими соцветиями брокколи.",
        mealType = MealType.LUNCH,
        baseCalories = 560,
        baseProteins = 42.0,
        baseFats = 28.0,
        baseCarbs = 36.0,
        baseCostRub = 390,
        cookingTimeMinutes = 18,
        baseIngredients = listOf(
            IngredientTemplate("Филе атлантического лосося", 160.0, "г"),
            IngredientTemplate("Брокколи свежая", 150.0, "г"),
            IngredientTemplate("Соус терияки без сахара", 20.0, "мл"),
            IngredientTemplate("Семена кунжута белого", 8.0, "г"),
            IngredientTemplate("Лимонный сок", 10.0, "мл"),
        ),
        steps = listOf(
            "Филе лосося сбрызнуть лимоном, смазать тонким слоем соуса терияки.",
            "Запекать в духовке при 190°C 12 минут или обжарить на сковороде-гриль.",
            "Брокколи разобрать на соцветия и отварить 3 минуты в кипящей воде аль-денте.",
            "Подавать лосось с брокколи, полив оставшимся соусом и посыпав кунжутом.",
        ),
        keywords = listOf("лосось", "семг", "форель", "рыб", "брокколи", "терияки", "омега"),
    ),
    RecipeArchetype(
        title = "Теплый боул с печеной говяжьей вырезкой, киноа и авокадо",
        description = "Сбалансированная нутрициологическая тарелка с постной говядиной, киноа и свежими фермерскими овощами.",
        mealType = MealType.LUNCH,
        baseCalories = 580,
        baseProteins = 44.0,
        baseFats = 22.0,
        baseCarbs = 52.0,
        baseCostRub = 350,
        cookingTimeMinutes = 25,
        baseIngredients = listOf(
            IngredientTemplate("Вырезка говяжья постная", 150.0, "г"),
            IngredientTemplate("Киноа отварное", 120.0, "г"),
            IngredientTemplate("Авокадо хасс", 50.0, "г"),
            IngredientTemplate("Томаты черри", 60.0, "г"),
            IngredientTemplate("Огурец свежий хрустящий", 50.0, "г"),
        ),
        steps = listOf(
            "Говяжью вырезку обжарить на сильном огне до образования корочки, затем довести в духовке 8 минут.",
            "Киноа отварить в соотношении 1:2 в подсоленной воде.",
            "Овощи нарезать аккуратными дольками.",
            "Собрать боул: выложить киноа, ломтики мяса, авокадо и свежие овощи.",
        ),
        keywords = listOf("говядин", "мяс", "киноа", "боул", "вырезк", "стейк"),
    ),
    RecipeArchetype(
        title = "Паста из твердых сортов пшеницы с цыпленком и шампиньонами",
        description = "Итальянская классика в фитнес-версии: цельнозерновая паста с нежным филе цыпленка в легком соусе.",
        mealType = MealType.LUNCH,
        baseCalories = 520,
        baseProteins = 40.0,
        baseFats = 14.0,
        baseCarbs = 58.0,
        baseCostRub = 210,
        cookingTimeMinutes = 15,
        baseIngredients = listOf(
            IngredientTemplate("Паста цельнозерновая пенне", 70.0, "г"),
            IngredientTemplate("Филе куриное", 150.0, "г"),
            IngredientTemplate("Шампиньоны свежие", 90.0, "г"),
            IngredientTemplate("Сливки 10% легкие", 40.0, "мл"),
            IngredientTemplate("Чеснок свежий", 1.0, "зуб"),
        ),
        steps = listOf(
            "Отварить пасту аль-денте согласно инструкции на упаковке.",
            "Куриное филе и шампиньоны нарезать слайсами, обжарить с чесноком 6 минут.",
            "Влить легкие сливки, тушить 2 минуты на медленном огне.",
            "Смешать пасту с соусом и цыпленком, украсить свежим базиликом.",
        ),
        keywords = listOf("паст", "макарон", "куриц", "цыпленок", "гриб", "итальян"),
    ),

    // Ужины
    RecipeArchetype(
        title = "Запеченная дорадо с прованскими травами и цукини",
        description = "Легкий восстановительный ужин: белая морская рыба с хрустящими овощами гриль и лимоном.",
        mealType = MealType.DINNER,
        baseCalories = 410,
        baseProteins = 38.0,
        baseFats = 11.0,
        baseCarbs = 38.0,
        baseCostRub = 270,
        cookingTimeMinutes = 20,
        baseIngredients = listOf(
            IngredientTemplate("Филе дорадо или сибаса", 190.0, "г"),
            IngredientTemplate("Цукини молодой", 140.0, "г"),
            IngredientTemplate("Томаты черри", 70.0, "г"),
            IngredientTemplate("Оливковое масло Extra Virgin", 6.0, "мл"),
            IngredientTemplate("Розмарин и лимон", 15.0, "г"),
        ),
        steps = listOf(
            "Рыбу выложить на пергамент, посолить, сбрызнуть лимоном и оливковым маслом.",
            "Рядом выложить кружочки цукини и целые томаты черри с веточкой розмарина.",
            "Запекать в духовке при 180°C в течение 18 минут.",
            "Подавать горячим со свежей зеленью.",
        ),
        keywords = listOf("дорадо", "сибас", "рыб", "цукини", "овощ", "легк", "ужин"),
    ),
    RecipeArchetype(
        title = "Филе трески на пару с пюре из цветной капусты и укропом",
        description = "Диетический шедевр: нежнейшее пюре со вкусом сливок без лишних калорий и сочная северная треска.",
        mealType = MealType.DINNER,
        baseCalories = 360,
        baseProteins = 36.0,
        baseFats = 7.0,
        baseCarbs = 36.0,
        baseCostRub = 190,
        cookingTimeMinutes = 15,
        baseIngredients = listOf(
            IngredientTemplate("Филе трески мурманской", 190.0, "г"),
            IngredientTemplate("Цветная капуста", 170.0, "г"),
            IngredientTemplate("Молоко 2.5%", 40.0, "мл"),
            IngredientTemplate("Мускатный орех молотый", 1.0, "г"),
            IngredientTemplate("Свежий укроп", 10.0, "г"),
        ),
        steps = listOf(
            "Цветную капусту отварить в подсоленной воде 10 минут, взбить блендером с молоком и мускатным орехом.",
            "Филе трески приготовить на пару 10 минут со щепоткой морской соли и веточками укропа.",
            "Выложить пюре на тарелку, сверху поместить филе рыбы.",
            "Украсить каплями оливкового масла и свежим укропом.",
        ),
        keywords = listOf("треск", "минтай", "капуст", "пюре", "диетическ", "пар"),
    ),
    RecipeArchetype(
        title = "Салат с тунцом гриль, перепелиным яйцом и стручковой фасолью",
        description = "Яркий салат в стиле Нисуаз с чистым стейком тунца, свежей зеленью и легкой горчичной заправкой.",
        mealType = MealType.DINNER,
        baseCalories = 430,
        baseProteins = 42.0,
        baseFats = 16.0,
        baseCarbs = 28.0,
        baseCostRub = 310,
        cookingTimeMinutes = 12,
        baseIngredients = listOf(
            IngredientTemplate("Стейк тунца", 150.0, "г"),
            IngredientTemplate("Яйца перепелиные", 4.0, "шт"),
            IngredientTemplate("Фасоль стручковая зеленая", 90.0, "г"),
            IngredientTemplate("Микс салатных листьев (айсберг/ромен)", 60.0, "г"),
            IngredientTemplate("Дижонская горчица и масло", 10.0, "мл"),
        ),
        steps = listOf(
            "Стейк тунца обжарить по 1.5 минуты с каждой стороны (серединка должна остаться розовой).",
            "Перепелиные яйца сварить вкрутую 3 минуты, остудить и разрезать пополам.",
            "Стручковую фасоль бланшировать 3 минуты и обдать холодной водой.",
            "Собрать салат: листья, фасоль, тунец слайсами, яйца и легкая заправка.",
        ),
        keywords = listOf("тунец", "салат", "яйц", "низуаз", "фасоль", "рыб"),
    ),
    RecipeArchetype(
        title = "Котлетки из индейки на пару с тушеными сезонными овощами",
        description = "Домашний уютный ужин: сочные диетические котлетки с кабачком, перцем и ароматными травами.",
        mealType = MealType.DINNER,
        baseCalories = 440,
        baseProteins = 40.0,
        baseFats = 13.0,
        baseCarbs = 40.0,
        baseCostRub = 220,
        cookingTimeMinutes = 20,
        baseIngredients = listOf(
            IngredientTemplate("Фарш из филе индейки", 170.0, "г"),
            IngredientTemplate("Кабачок свежий", 100.0, "г"),
            IngredientTemplate("Перец болгарский сладкий", 70.0, "г"),
            IngredientTemplate("Баклажан", 70.0, "г"),
            IngredientTemplate("Прованские травы и соль", 3.0, "г"),
        ),
        steps = listOf(
            "Фарш индейки приправить солью и травами, сформировать аккуратные котлетки.",
            "Приготовить котлеты на пару или в пароварке 15 минут.",
            "Овощи нарезать кубиками и потушить на сковороде в собственном соку 10 минут.",
            "Подавать с рубленой кинзой или петрушкой.",
        ),
        keywords = listOf("котлет", "индейк", "фарш", "пар", "овощ", "рагу"),
    ),

    // Перекусы
    RecipeArchetype(
        title = "Чиа-пудинг на кокосовом молоке с манговым муссом",
        description = "Суперфуд-перекус, богатый омега-3 и растворимой клетчаткой, с натуральным тропическим вкусом.",
        mealType = MealType.SNACK,
        baseCalories = 280,
        baseProteins = 9.0,
        baseFats = 13.0,
        baseCarbs = 32.0,
        baseCostRub = 160,
        cookingTimeMinutes = 5,
        baseIngredients = listOf(
            IngredientTemplate("Семена чиа", 25.0, "г"),
            IngredientTemplate("Молоко кокосовое легкое", 120.0, "мл"),
            IngredientTemplate("Пюре манго без сахара", 60.0, "г"),
            IngredientTemplate("Мята свежая", 2.0, "г"),
        ),
        steps = listOf(
            "Смешать семена чиа с кокосовым молоком, дать настояться 15 минут до желеобразной текстуры.",
            "Сверху аккуратно выложить слой мангового пюре.",
            "Украсить веточкой свежей мяты и подавать охлажденным.",
        ),
        keywords = listOf("чиа", "пудинг", "манго", "кокос", "десерт", "без сахара"),
    ),
    RecipeArchetype(
        title = "Протеиновый смузи с черникой, бананом и миндалем",
        description = "Быстрый восстановительный смузи для мышц с антиоксидантами лесных ягод и ореховыми нотками.",
        mealType = MealType.SNACK,
        baseCalories = 320,
        baseProteins = 28.0,
        baseFats = 7.0,
        baseCarbs = 36.0,
        baseCostRub = 180,
        cookingTimeMinutes = 5,
        baseIngredients = listOf(
            IngredientTemplate("Изолят сывороточного белка", 30.0, "г"),
            IngredientTemplate("Банан половинный", 60.0, "г"),
            IngredientTemplate("Черника замороженная", 60.0, "г"),
            IngredientTemplate("Миндальное молоко без сахара", 200.0, "мл"),
            IngredientTemplate("Миндальные лепестки", 5.0, "г"),
        ),
        steps = listOf(
            "Все ингредиенты поместить в чашу блендера.",
            "Взбивать на максимальной скорости 45 секунд до шелковистой текстуры.",
            "Перелить в высокий бокал и посыпать миндальными лепестками.",
        ),
        keywords = listOf("смузи", "протеин", "коктейль", "черник", "ягод", "спорт"),
    ),
    RecipeArchetype(
        title = "Хрустящий цельнозерновой тост с рикоттой и ягодами",
        description = "Изысканный перекус с нежной итальянской рикоттой, свежими ягодами малины и капелькой меда.",
        mealType = MealType.SNACK,
        baseCalories = 260,
        baseProteins = 12.0,
        baseFats = 8.0,
        baseCarbs = 35.0,
        baseCostRub = 150,
        cookingTimeMinutes = 5,
        baseIngredients = listOf(
            IngredientTemplate("Хлеб цельнозерновой с семенами", 45.0, "г"),
            IngredientTemplate("Сыр рикотта 8%", 50.0, "г"),
            IngredientTemplate("Малина свежая", 35.0, "г"),
            IngredientTemplate("Мед натуральный", 7.0, "г"),
        ),
        steps = listOf(
            "Подрумянить ломтик хлеба в тостере до хрустящей корочки.",
            "Намазать рикотту ровным слоем.",
            "Выложить свежие ягоды малины и капнуть немного меда.",
        ),
        keywords = listOf("тост", "рикотт", "ягод", "малин", "хлеб", "мед"),
    ),
)

