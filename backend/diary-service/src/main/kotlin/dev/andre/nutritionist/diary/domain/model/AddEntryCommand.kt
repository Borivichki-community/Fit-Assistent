package dev.andre.nutritionist.diary.domain.model

import kotlin.time.Instant

data class AddEntryCommand(
    val entryId: DiaryEntryId,
    val userId: UserId,
    val idempotencyKey: IdempotencyKey,
    val sourceType: EntrySourceType = EntrySourceType.PRODUCT,
    val sourceId: SourceId? = null,
    val nameSnapshot: String,
    val weightGrams: Double,
    val nutrition: NutritionSummary,
    val mealType: MealType,
    val recordedAt: Instant,
    val cost: CostSnapshot? = null,
) {
    constructor(
        entryId: DiaryEntryId,
        userId: UserId,
        idempotencyKey: IdempotencyKey,
        foodItemId: FoodItemId,
        foodName: String,
        quantityGrams: Double,
        nutrition: NutritionSummary,
        mealType: MealType,
        recordedAt: Instant,
        cost: CostSnapshot? = null,
    ) : this(
        entryId = entryId,
        userId = userId,
        idempotencyKey = idempotencyKey,
        sourceType = EntrySourceType.PRODUCT,
        sourceId = SourceId(foodItemId.raw),
        nameSnapshot = foodName,
        weightGrams = quantityGrams,
        nutrition = nutrition,
        mealType = mealType,
        recordedAt = recordedAt,
        cost = cost,
    )

    val id: DiaryEntryId get() = entryId
    val foodItemId: FoodItemId? get() = sourceId?.let { FoodItemId(it.raw) }
    val foodName: String get() = nameSnapshot
    val quantityGrams: Double get() = weightGrams
}
