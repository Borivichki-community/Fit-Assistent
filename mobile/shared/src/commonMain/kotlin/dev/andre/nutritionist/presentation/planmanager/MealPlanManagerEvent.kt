package dev.andre.nutritionist.presentation.planmanager

import dev.andre.nutritionist.domain.model.GroceryItem
import dev.andre.nutritionist.domain.model.GroceryItemId
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanDay
import dev.andre.nutritionist.domain.model.UserId

/**
 * События экрана управления планами питания.
 */
sealed interface MealPlanManagerEvent {

    sealed interface Ui : MealPlanManagerEvent {
        data class Load(val userId: UserId) : Ui
        data object Retry : Ui
        data class SelectTab(val tab: PlanManagerTab) : Ui
        data class SelectDay(val dayIndex: Int) : Ui
        data class ToggleGrocery(val itemId: GroceryItemId) : Ui
        data object DeactivatePlan : Ui
        data object NavigateToCatalog : Ui
    }

    sealed interface Internal : MealPlanManagerEvent {
        data class Loaded(
            val activePlan: MealPlan?,
            val days: List<MealPlanDay>,
            val groceryList: List<GroceryItem>,
        ) : Internal

        data class GroceryToggled(
            val itemId: GroceryItemId,
            val isBought: Boolean,
        ) : Internal

        data object PlanDeactivated : Internal

        data class Failed(val error: Throwable) : Internal
    }
}
