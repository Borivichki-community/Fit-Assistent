package dev.andre.nutritionist.presentation.planmanager

import dev.andre.nutritionist.domain.model.GroceryItemId
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.domain.model.UserId

/**
 * Команды для выполнения асинхронных операций экрана управления рационом.
 */
sealed interface MealPlanManagerCommand {
    data class Load(val userId: UserId) : MealPlanManagerCommand
    data class ToggleGrocery(val planId: MealPlanId, val itemId: GroceryItemId) : MealPlanManagerCommand
    data class DeactivatePlan(val userId: UserId) : MealPlanManagerCommand
}
