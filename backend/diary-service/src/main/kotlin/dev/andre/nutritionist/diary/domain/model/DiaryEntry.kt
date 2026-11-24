package dev.andre.nutritionist.diary.domain.model

import kotlin.time.Instant

data class DiaryEntry(
    val id: DiaryEntryId,
    val userId: UserId,
    val sourceType: EntrySourceType = EntrySourceType.PRODUCT,
    val sourceId: SourceId? = null,
    val nameSnapshot: String,
    val weightGrams: Double,
    val nutrition: NutritionSummary,
    val mealType: MealType,
    val recordedAt: Instant,
    val createdAt: Instant,
    val cost: CostSnapshot? = null,
) {
    constructor(
        id: DiaryEntryId,
        userId: UserId,
        foodItemId: FoodItemId,
        foodName: String,
        quantityGrams: Double,
        nutrition: NutritionSummary,
        mealType: MealType,
        recordedAt: Instant,
        createdAt: Instant,
        cost: CostSnapshot? = null,
    ) : this(
        id = id,
        userId = userId,
        sourceType = EntrySourceType.PRODUCT,
        sourceId = SourceId(foodItemId.raw),
        nameSnapshot = foodName,
        weightGrams = quantityGrams,
        nutrition = nutrition,
        mealType = mealType,
        recordedAt = recordedAt,
        createdAt = createdAt,
        cost = cost,
    )

    val foodItemId: FoodItemId? get() = sourceId?.let { FoodItemId(it.raw) }
    val foodName: String get() = nameSnapshot
    val quantityGrams: Double get() = weightGrams
}
