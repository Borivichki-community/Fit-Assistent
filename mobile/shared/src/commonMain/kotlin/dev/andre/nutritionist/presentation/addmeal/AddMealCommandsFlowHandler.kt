package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.calculator.NutritionCalculator
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeSearchCriteria
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.repository.CatalogRepository
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.RecipeCatalogRepository
import dev.andre.nutritionist.domain.usecase.LogRecipeToDiaryUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Асинхронный обработчик команд меню выбора блюда (@fitapp_4):
 * параллельно ищет продукты в [CatalogRepository] и рецепты в [RecipeCatalogRepository],
 * а при выборе элемента сохраняет [MealRecord] в [DiaryRepository] / [LogRecipeToDiaryUseCase].
 */
class AddMealCommandsFlowHandler(
    private val catalogRepository: CatalogRepository,
    private val recipeCatalogRepository: RecipeCatalogRepository,
    private val diaryRepository: DiaryRepository,
    private val logRecipeToDiaryUseCase: LogRecipeToDiaryUseCase,
    private val dispatchers: CoroutineDispatchers,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : CommandsFlowHandler<AddMealCommand, AddMealEvent> {

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun handle(commands: Flow<AddMealCommand>): Flow<AddMealEvent> = channelFlow {
        val searchChannel = Channel<AddMealCommand.Search>(Channel.UNLIMITED)
        val actionChannel = Channel<AddMealCommand>(Channel.UNLIMITED)

        launch {
            commands.collect { command ->
                when (command) {
                    is AddMealCommand.Search -> searchChannel.send(command)
                    is AddMealCommand.AddProduct,
                    is AddMealCommand.AddRecipe,
                    -> actionChannel.send(command)
                }
            }
            searchChannel.close()
            actionChannel.close()
        }

        launch {
            searchChannel.receiveAsFlow()
                .debounce { cmd -> if (cmd.forceRefresh) 0L else debounceMillis }
                .distinctUntilChanged { old, new ->
                    !new.forceRefresh && old.query == new.query
                }
                .flatMapLatest { cmd -> executeSearch(cmd) }
                .collect { send(it) }
        }

        launch {
            actionChannel.receiveAsFlow()
                .flatMapMerge { command ->
                    when (command) {
                        is AddMealCommand.AddProduct -> executeAddProduct(command)
                        is AddMealCommand.AddRecipe -> executeAddRecipe(command)
                        is AddMealCommand.Search -> flow { }
                    }
                }
                .collect { send(it) }
        }
    }

    private fun executeSearch(command: AddMealCommand.Search): Flow<AddMealEvent> = flow {
        val query = command.query.trim()
        if (query.isEmpty()) {
            emit(AddMealEvent.Internal.SearchLoaded(query = "", products = emptyList(), recipes = emptyList()))
            return@flow
        }

        val event = coroutineScope {
            val productsDeferred = async {
                catalogRepository.searchProducts(query = query, pageSize = SEARCH_PAGE_SIZE)
            }
            val recipesDeferred = async {
                recipeCatalogRepository.searchPublicRecipes(
                    RecipeSearchCriteria(query = query, pageSize = SEARCH_PAGE_SIZE),
                )
            }

            val productsResult = productsDeferred.await()
            val recipesResult = recipesDeferred.await()

            productsResult.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            recipesResult.exceptionOrNull()?.let { if (it is CancellationException) throw it }

            val products: List<Product> = productsResult.getOrNull()?.first.orEmpty()
            val recipes: List<Recipe> = recipesResult.getOrNull()?.items ?: run {
                // Офлайн-резерв: ищем среди локально закэшированных рецептов, если оба/один сервис недоступен
                runCatching {
                    recipeCatalogRepository.observeMyRecipes().first().filter {
                        it.title.contains(query, ignoreCase = true)
                    }
                }.getOrElse { error ->
                    if (error is CancellationException) throw error
                    emptyList()
                }
            }

            if (products.isNotEmpty() || recipes.isNotEmpty() || (productsResult.isSuccess && recipesResult.isSuccess)) {
                AddMealEvent.Internal.SearchLoaded(
                    query = query,
                    products = products,
                    recipes = recipes,
                )
            } else {
                val failure = productsResult.exceptionOrNull()
                    ?: recipesResult.exceptionOrNull()
                    ?: IllegalStateException("Сервисы каталога и рецептов недоступны")
                AddMealEvent.Internal.SearchFailed(failure)
            }
        }
        emit(event)
    }.flowOn(dispatchers.io)

    private fun executeAddProduct(command: AddMealCommand.AddProduct): Flow<AddMealEvent> = flow {
        val weightGrams = AddMealItemMapper.DEFAULT_PRODUCT_PORTION_GRAMS
        val portionNutrition = NutritionCalculator.calculatePortion(
            nutritionPer100g = command.product.nutritionPer100g,
            weightGrams = weightGrams,
        )
        val portionCost = command.product.prices.firstOrNull()?.price?.let {
            NutritionCalculator.calculatePortionCost(it, weightGrams)
        }
        val record = MealRecord(
            id = MealRecordId.generate(),
            mealType = command.mealType,
            recordedAt = resolveRecordedAt(command.dateEpochMs),
            sourceType = EntrySourceType.PRODUCT,
            sourceId = command.product.id.raw,
            nameSnapshot = command.product.name,
            weightGrams = weightGrams,
            nutrition = portionNutrition,
            cost = portionCost,
            idempotencyKey = IdempotencyKey.generate(),
            syncStatus = SyncStatus.PENDING,
        )

        val event = diaryRepository.addEntry(record).fold(
            onSuccess = { saved -> AddMealEvent.Internal.ItemSaved(saved) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                AddMealEvent.Internal.SaveFailed(error)
            },
        )
        emit(event)
    }.flowOn(dispatchers.io)

    private fun executeAddRecipe(command: AddMealCommand.AddRecipe): Flow<AddMealEvent> = flow {
        val weightGrams = AddMealItemMapper.singleServingWeightGrams(command.recipe)
        val event = logRecipeToDiaryUseCase(
            recipe = command.recipe,
            weightGrams = weightGrams,
            mealType = command.mealType,
            recordedAt = resolveRecordedAt(command.dateEpochMs),
        ).fold(
            onSuccess = { saved -> AddMealEvent.Internal.ItemSaved(saved) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                AddMealEvent.Internal.SaveFailed(error)
            },
        )
        emit(event)
    }.flowOn(dispatchers.io)

    private fun resolveRecordedAt(selectedDateEpochMs: Long): Instant {
        val nowMs = nowProvider()
        if (selectedDateEpochMs <= 0L) {
            return Instant.fromEpochMilliseconds(nowMs)
        }
        val selectedDate = Instant.fromEpochMilliseconds(selectedDateEpochMs).toLocalDateTime(timeZone).date
        val todayDate = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date
        return if (selectedDate == todayDate) {
            Instant.fromEpochMilliseconds(nowMs)
        } else {
            Instant.fromEpochMilliseconds(selectedDateEpochMs)
        }
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS = 300L
        const val SEARCH_PAGE_SIZE = 15
    }
}
