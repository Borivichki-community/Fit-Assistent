package dev.andre.nutritionist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val foodItemId: String,
    val foodName: String,
    val portionGrams: Double,
    val calories: Int,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val mealType: String,
    val dateEpochDay: Long,
    val recordedAtEpochMs: Long,
    val idempotencyKey: String,
    val portionDescription: String = "1 порция",
    val syncStatus: String = "SYNC_PENDING",
)
