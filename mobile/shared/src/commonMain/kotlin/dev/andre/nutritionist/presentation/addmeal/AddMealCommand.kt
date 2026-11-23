package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.model.DiaryEntry

/**
 * Асинхронные команды экрана добавления блюда в дневник питания.
 */
sealed interface AddMealCommand {
    data class SearchFoods(val query: String, val category: String) : AddMealCommand
    data class SaveDiaryEntry(val entry: DiaryEntry) : AddMealCommand
}
