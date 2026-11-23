package dev.andre.nutritionist.presentation.addmeal

/**
 * UI-модель продукта в списке поиска.
 */
data class FoodItemUiModel(
    val id: String,
    val name: String,
    val brand: String,
    val caloriesPer100g: Int,
    val caloriesDescription: String,
    val macroDescription: String,
    val isSelected: Boolean = false,
)

/**
 * UI-модель категории фильтрации (Все, Завтрак, Обед, Ужин, Перекус).
 */
data class FoodCategoryUiModel(
    val title: String,
    val isSelected: Boolean = false,
)

/**
 * UI-модель выбранной порции с мгновенно пересчитанными КБЖУ.
 */
data class PortionSummaryUiModel(
    val portionGrams: Double,
    val portionGramsLabel: String,
    val caloriesTotal: Int,
    val proteinsGrams: Double,
    val fatsGrams: Double,
    val carbsGrams: Double,
    val caloriesLabel: String,
    val macroSummaryText: String,
)

/**
 * Успешные данные для отображения в шторке добавления блюда.
 */
data class AddMealSuccessUiModel(
    val mealTypeTitle: String,
    val searchQuery: String,
    val categories: List<FoodCategoryUiModel>,
    val foods: List<FoodItemUiModel>,
    val selectedFood: FoodItemUiModel?,
    val portionSummary: PortionSummaryUiModel?,
    val addButtonText: String,
    val isAddEnabled: Boolean,
)

/**
 * Состояние экрана/шторки добавления блюда по архитектуре KoTEA MVI.
 */
data class AddMealUiState(
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val data: AddMealSuccessUiModel) : Content
        data class Error(val message: String) : Content
    }
}
