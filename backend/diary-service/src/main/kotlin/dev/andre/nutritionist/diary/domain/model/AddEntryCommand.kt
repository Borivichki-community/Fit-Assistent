package dev.andre.nutritionist.diary.domain.model

import kotlin.time.Instant

data class AddEntryCommand(
    val entryId: DiaryEntryId,
    val userId: UserId,
    val idempotencyKey: IdempotencyKey,
    val foodItemId: FoodItemId,
    val foodName: String,
    val quantityGrams: Double,
    val nutrition: NutritionSummary,
    val mealType: MealType,
    val recordedAt: Instant,
    val cost: CostSnapshot? = null,
)
