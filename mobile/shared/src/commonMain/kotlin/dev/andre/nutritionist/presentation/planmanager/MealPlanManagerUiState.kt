package dev.andre.nutritionist.presentation.planmanager

import dev.andre.nutritionist.domain.model.GroceryItemId
import dev.andre.nutritionist.domain.model.MealPlanId

/**
 * UI-модель приема пищи внутри дня рациона.
 */
data class PlanMealUiModel(
    val title: String,
    val mealType: String,
    val caloriesText: String,
    val proteinsText: String,
    val fatsText: String,
    val carbsText: String,
)

/**
 * UI-модель дня плана питания.
 */
data class PlanDayUiModel(
    val dayNumber: Int,
    val dayName: String,
    val shortDayName: String,
    val targetCaloriesText: String,
    val isSelected: Boolean,
    val meals: List<PlanMealUiModel>,
)

/**
 * UI-модель отдельной позиции списка покупок.
 */
data class GroceryItemUiModel(
    val id: GroceryItemId,
    val name: String,
    val amount: String,
    val estimatedPriceText: String,
    val isBought: Boolean,
)

/**
 * UI-модель категории продуктов для удобной навигации в магазине.
 */
data class GroceryCategoryUiModel(
    val categoryName: String,
    val countText: String,
    val items: List<GroceryItemUiModel>,
)

/**
 * Состояние экрана менеджера рационов, подготовленное для декларативного отображения в Compose.
 */
data class MealPlanManagerUiState(
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content

        data class Empty(
            val message: String,
            val actionTitle: String = "Выбрать рацион в каталоге",
        ) : Content

        data class Error(
            val message: String,
        ) : Content

        data class Success(
            val planId: MealPlanId,
            val title: String,
            val category: String,
            val difficulty: String,
            val durationText: String,
            val targetCaloriesText: String,
            val weeklyBudgetText: String,
            val proteinsText: String,
            val fatsText: String,
            val carbsText: String,
            val selectedTab: PlanManagerTab,
            val days: List<PlanDayUiModel>,
            val selectedDayIndex: Int,
            val currentDay: PlanDayUiModel,
            val groceryCategories: List<GroceryCategoryUiModel>,
            val totalGroceryItemsCount: Int,
            val boughtGroceryItemsCount: Int,
            val groceryProgressPercent: Float,
            val groceryProgressText: String,
            val totalGroceryPriceText: String,
        ) : Content
    }
}
