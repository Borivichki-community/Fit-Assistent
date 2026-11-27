package dev.andre.nutritionist.presentation.model

import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.SyncStatus

data class MealRecordUiModel(
    val id: MealRecordId,
    val mealType: MealType,
    val mealTypeLabel: String,
    val title: String,
    val weightLabel: String,
    val caloriesLabel: String,
    val caloriesInt: Int = 0,
    val proteinsLabel: String,
    val fatsLabel: String,
    val carbsLabel: String,
    val macrosLabel: String,
    val compactMacrosLabel: String = macrosLabel,
    val costLabel: String?,
    val dailyBalanceShareLabel: String = "0% от суточной нормы",
    val durationLabel: String = "1 мин",
    val syncStatus: SyncStatus,
    val syncStatusLabel: String,
    val timeLabel: String,
)
