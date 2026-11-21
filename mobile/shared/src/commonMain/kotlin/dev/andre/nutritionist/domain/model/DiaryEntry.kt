package dev.andre.nutritionist.domain.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * Доменная модель записи о съеденном продукте или блюде в дневнике питания.
 */
data class DiaryEntry(
    val id: DiaryEntryId,
    val userId: UserId,
    val foodItemId: FoodItemId,
    val foodName: String,
    val portionGrams: Double,
    val calories: Int,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val mealType: MealType,
    val date: LocalDate,
    val recordedAt: Instant,
    val idempotencyKey: IdempotencyKey,
    val portionDescription: String = "1 порция",
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)
