package dev.andre.nutritionist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_records")
data class MealRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "PENDING",
    @ColumnInfo(name = "meal_type") val mealType: String,
    @ColumnInfo(name = "recorded_at_epoch") val recordedAtEpoch: Long,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "source_id") val sourceId: String? = null,
    @ColumnInfo(name = "name_snapshot") val nameSnapshot: String,
    @ColumnInfo(name = "weight_grams") val weightGrams: Double,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    @ColumnInfo(name = "cost_amount_minor") val costAmountMinor: Long? = null,
    @ColumnInfo(name = "cost_currency") val costCurrency: String? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
)
