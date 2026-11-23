package dev.andre.nutritionist.presentation.planmanager

import dev.andre.nutritionist.domain.model.GroceryItem
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanDay
import dev.andre.nutritionist.domain.model.UserId

/**
 * Вкладки управления текущим активным планом питания.
 */
enum class PlanManagerTab {
    SCHEDULE,
    GROCERY_LIST,
}

/**
 * Состояние экрана менеджера рационов питания.
 */
data class MealPlanManagerState(
    val content: Content = Content.Loading,
    val selectedTab: PlanManagerTab = PlanManagerTab.SCHEDULE,
    val userId: UserId? = null,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(
            val activePlan: MealPlan,
            val days: List<MealPlanDay>,
            val groceryList: List<GroceryItem>,
            val selectedDayIndex: Int = 0,
            val selectedTab: PlanManagerTab = PlanManagerTab.SCHEDULE,
        ) : Content
        data class Empty(
            val message: String = "Нет активного плана питания",
        ) : Content
        data class Error(
            val message: String,
        ) : Content
    }
}
