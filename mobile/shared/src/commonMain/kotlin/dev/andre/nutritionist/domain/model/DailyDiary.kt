package dev.andre.nutritionist.domain.model

import kotlinx.datetime.LocalDate

/**
 * Доменная модель суточного дневника питания за выбранную дату.
 */
data class DailyDiary(
    val userId: UserId,
    val date: LocalDate,
    val entries: List<DiaryEntry> = emptyList(),
    val streakDays: Int = 1,
) {
    val totalCalories: Int = entries.sumOf { it.calories }
    val totalProteins: Double = entries.sumOf { it.proteins }
    val totalFats: Double = entries.sumOf { it.fats }
    val totalCarbs: Double = entries.sumOf { it.carbs }
}
