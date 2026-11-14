package dev.andre.nutritionist.diary.domain.model

data class DailyEntries(
    val entries: List<DiaryEntry>,
    val dailyTotal: NutritionSummary,
)
