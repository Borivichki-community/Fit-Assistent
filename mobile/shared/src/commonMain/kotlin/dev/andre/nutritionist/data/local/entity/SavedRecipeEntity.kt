package dev.andre.nutritionist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_recipes")
data class SavedRecipeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plan_id") val planId: String? = null,
    val title: String,
    @ColumnInfo(name = "meal_type") val mealType: String,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    @ColumnInfo(name = "cost_minor") val costMinor: Long,
    @ColumnInfo(name = "ingredients_json") val ingredientsJson: String,
    val instructions: String,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
)
