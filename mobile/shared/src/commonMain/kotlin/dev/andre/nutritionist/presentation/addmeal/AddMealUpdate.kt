package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.presentation.dashboard.formatMacroGrams
import kotlin.math.round
import kotlin.time.Instant

/**
 * Чистый синхронный редьюсер экрана добавления блюда в дневник питания.
 * Выполнен строго в соответствии с принципами KoTEA MVI без ветки else.
 */
class AddMealUpdate : DslUpdate<AddMealState, AddMealEvent, AddMealCommand, AddMealNews>() {

    override fun NextBuilder<AddMealState, AddMealCommand, AddMealNews>.update(event: AddMealEvent) {
        when (event) {
            is AddMealEvent.Ui.Init -> {
                state {
                    copy(
                        mealType = event.mealType,
                        userId = event.userId,
                        date = event.date,
                        content = AddMealState.Content.Loading,
                    )
                }
                commands(AddMealCommand.SearchFoods(query = "", category = state.selectedCategory))
            }

            is AddMealEvent.Ui.SearchQueryChanged -> {
                state {
                    copy(
                        searchQuery = event.query,
                        content = AddMealState.Content.Loading,
                    )
                }
                commands(AddMealCommand.SearchFoods(query = event.query, category = state.selectedCategory))
            }

            is AddMealEvent.Ui.CategorySelected -> {
                state {
                    copy(
                        selectedCategory = event.category,
                        content = AddMealState.Content.Loading,
                    )
                }
                commands(AddMealCommand.SearchFoods(query = state.searchQuery, category = event.category))
            }

            is AddMealEvent.Ui.FoodSelected -> {
                state {
                    copy(
                        selectedFoodItem = event.food,
                        portionGrams = event.food.defaultPortionGrams,
                    )
                }
            }

            is AddMealEvent.Ui.PortionChanged -> {
                val clamped = if (event.grams < 1.0) 1.0 else event.grams
                state { copy(portionGrams = clamped) }
            }

            AddMealEvent.Ui.ConfirmAdd -> {
                val food = state.selectedFoodItem
                val userId = state.userId
                val date = state.date
                val portion = state.portionGrams

                if (food != null && userId != null && date != null) {
                    val factor = portion / 100.0
                    val calories = round(food.caloriesPer100g * factor).toInt()
                    val proteins = round(food.proteinsPer100g * factor * 10.0) / 10.0
                    val fats = round(food.fatsPer100g * factor * 10.0) / 10.0
                    val carbs = round(food.carbsPer100g * factor * 10.0) / 10.0

                    val randomSuffix = (1000..9999).random()
                    val entry = DiaryEntry(
                        id = DiaryEntryId("entry-${food.id.raw}-$randomSuffix"),
                        userId = userId,
                        foodItemId = food.id,
                        foodName = if (food.brand.isNotBlank()) "${food.name} (${food.brand})" else food.name,
                        portionGrams = portion,
                        calories = calories,
                        proteins = proteins,
                        fats = fats,
                        carbs = carbs,
                        mealType = state.mealType,
                        date = date,
                        recordedAt = Instant.fromEpochMilliseconds(0),
                        idempotencyKey = IdempotencyKey("idem-${food.id.raw}-$randomSuffix"),
                        portionDescription = "${formatMacroGrams(portion)} г, порция",
                    )
                    commands(AddMealCommand.SaveDiaryEntry(entry))
                } else {
                    news(AddMealNews.ShowSnackbar("Выберите продукт для добавления"))
                }
            }

            AddMealEvent.Ui.Dismiss -> {
                news(AddMealNews.Dismiss)
            }

            AddMealEvent.Ui.BarcodeScanClick -> {
                news(AddMealNews.OpenBarcodeScanner)
            }

            AddMealEvent.Ui.AiCameraClick -> {
                news(AddMealNews.OpenAiCamera)
            }

            is AddMealEvent.Ui.NetworkStatusChanged -> {
                state { copy(isOnline = event.isConnected) }
            }

            is AddMealEvent.Internal.FoodsLoaded -> {
                state { copy(content = AddMealState.Content.Success(event.foods)) }
            }

            is AddMealEvent.Internal.SearchFailed -> {
                state {
                    copy(
                        content = AddMealState.Content.Error(
                            event.error.message ?: "Ошибка поиска продуктов",
                        ),
                    )
                }
            }

            is AddMealEvent.Internal.EntrySaved -> {
                val message = if (state.isOnline) {
                    "Блюдо добавлено в дневник"
                } else {
                    "Сохранено локально. Отправится при появлении сети"
                }
                news(
                    AddMealNews.MealAddedSuccess(event.entry),
                    AddMealNews.ShowSnackbar(message),
                    AddMealNews.Dismiss,
                )
            }

            is AddMealEvent.Internal.SaveFailed -> {
                news(AddMealNews.ShowSnackbar(event.error.message ?: "Не удалось сохранить блюдо"))
            }
        }
    }
}
