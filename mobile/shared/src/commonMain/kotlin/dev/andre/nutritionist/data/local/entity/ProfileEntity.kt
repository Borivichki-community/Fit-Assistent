package dev.andre.nutritionist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val userId: String,
    val profileId: String,
    val heightCm: Int,
    val weightKg: Double,
    val gender: String,
    val activityLevel: String,
    val goalType: String,
    val targetCalories: Int,
    val targetProteinsGrams: Double,
    val targetFatsGrams: Double,
    val targetCarbsGrams: Double,
    val targetWeightKg: Double,
    val weeklyBudget: Double,
    val updatedAtEpochMs: Long,
)