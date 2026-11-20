package dev.andre.nutritionist.diary.domain.model

import kotlin.time.Instant

data class DiaryEntry(
    val id: DiaryEntryId,
    val userId: UserId,
    val foodItemId: FoodItemId,
    val foodName: String,
    val quantityGrams: Double,
    val nutrition: NutritionSummary,
    val mealType: MealType,
    val recordedAt: Instant,
    val createdAt: Instant,
    val cost: CostSnapshot? = null,
)
