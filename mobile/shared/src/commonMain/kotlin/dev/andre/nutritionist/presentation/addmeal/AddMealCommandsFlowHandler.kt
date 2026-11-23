package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.FoodCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Асинхронный обработчик команд экрана добавления блюда в дневник питания.
 */
class AddMealCommandsFlowHandler(
    private val foodCatalogRepository: FoodCatalogRepository,
    private val diaryRepository: DiaryRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<AddMealCommand, AddMealEvent> {

    override fun handle(commands: Flow<AddMealCommand>): Flow<AddMealEvent> = flow {
        commands.collect { command ->
            when (command) {
                is AddMealCommand.SearchFoods -> {
                    val event = withContext(dispatchers.io) {
                        foodCatalogRepository.searchFood(command.query, command.category).fold(
                            onSuccess = { AddMealEvent.Internal.FoodsLoaded(it) },
                            onFailure = { AddMealEvent.Internal.SearchFailed(it) },
                        )
                    }
                    emit(event)
                }

                is AddMealCommand.SaveDiaryEntry -> {
                    val event = withContext(dispatchers.io) {
                        diaryRepository.addEntry(command.entry).fold(
                            onSuccess = { AddMealEvent.Internal.EntrySaved(it) },
                            onFailure = { AddMealEvent.Internal.SaveFailed(it) },
                        )
                    }
                    emit(event)
                }
            }
        }
    }
}
