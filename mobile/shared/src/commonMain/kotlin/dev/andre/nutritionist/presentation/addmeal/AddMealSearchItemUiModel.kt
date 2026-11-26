package dev.andre.nutritionist.presentation.addmeal

/**
 * Тип элемента в результатах поиска при добавлении в дневник.
 */
enum class AddMealItemKind {
    PRODUCT,
    RECIPE,
}

/**
 * UI-модель найденного продукта или рецепта для выбора в меню добавления (@fitapp_4).
 */
data class AddMealSearchItemUiModel(
    val id: String,
    val kind: AddMealItemKind,
    val title: String,
    val typeLabel: String,
    val portionLabel: String,
    val caloriesLabel: String,
    val macrosLabel: String,
    val priceLabel: String?,
)
