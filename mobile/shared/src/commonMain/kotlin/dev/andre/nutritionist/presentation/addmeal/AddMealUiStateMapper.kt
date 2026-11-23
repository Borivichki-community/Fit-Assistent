package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.dashboard.formatMacroGrams
import kotlin.math.round

/**
 * Чистый маппер состояния [AddMealState] в презентационную модель [AddMealUiState].
 */
class AddMealUiStateMapper : UiStateMapper<AddMealState, AddMealUiState> {

    private val availableCategories = listOf("Все", "Завтрак", "Обед", "Ужин", "Перекус")

    override fun map(state: AddMealState): AddMealUiState {
        return when (val content = state.content) {
            AddMealState.Content.Loading -> AddMealUiState(content = AddMealUiState.Content.Loading)
            is AddMealState.Content.Error -> AddMealUiState(content = AddMealUiState.Content.Error(content.message))
            is AddMealState.Content.Success -> {
                val data = mapSuccess(state, content.foods)
                AddMealUiState(content = AddMealUiState.Content.Success(data))
            }
        }
    }

    private fun mapSuccess(state: AddMealState, foods: List<dev.andre.nutritionist.domain.model.FoodItem>): AddMealSuccessUiModel {
        val mealTypeTitle = when (state.mealType) {
            MealType.BREAKFAST -> "Завтрак"
            MealType.LUNCH -> "Обед"
            MealType.DINNER -> "Ужин"
            MealType.SNACK -> "Перекус"
        }

        val categoriesUi = availableCategories.map { cat ->
            FoodCategoryUiModel(
                title = cat,
                isSelected = cat.equals(state.selectedCategory, ignoreCase = true),
            )
        }

        val foodsUi = foods.map { food ->
            val isSelected = state.selectedFoodItem?.id == food.id
            FoodItemUiModel(
                id = food.id.raw,
                name = food.name,
                brand = food.brand,
                caloriesPer100g = food.caloriesPer100g,
                caloriesDescription = "${food.caloriesPer100g} ккал / 100 г",
                macroDescription = "Б: ${formatMacroGrams(food.proteinsPer100g)} г • Ж: ${formatMacroGrams(food.fatsPer100g)} г • У: ${formatMacroGrams(food.carbsPer100g)} г",
                isSelected = isSelected,
            )
        }

        val selectedFood = state.selectedFoodItem
        val portionSummary = if (selectedFood != null) {
            val factor = state.portionGrams / 100.0
            val totalCalories = round(selectedFood.caloriesPer100g * factor).toInt()
            val totalProteins = round(selectedFood.proteinsPer100g * factor * 10.0) / 10.0
            val totalFats = round(selectedFood.fatsPer100g * factor * 10.0) / 10.0
            val totalCarbs = round(selectedFood.carbsPer100g * factor * 10.0) / 10.0

            PortionSummaryUiModel(
                portionGrams = state.portionGrams,
                portionGramsLabel = "${formatMacroGrams(state.portionGrams)} г",
                caloriesTotal = totalCalories,
                proteinsGrams = totalProteins,
                fatsGrams = totalFats,
                carbsGrams = totalCarbs,
                caloriesLabel = "$totalCalories ккал",
                macroSummaryText = "Б: ${formatMacroGrams(totalProteins)} г • Ж: ${formatMacroGrams(totalFats)} г • У: ${formatMacroGrams(totalCarbs)} г",
            )
        } else {
            null
        }

        val addButtonText = if (portionSummary != null) {
            "Добавить в $mealTypeTitle • ${portionSummary.caloriesLabel}"
        } else {
            "Выберите продукт"
        }

        return AddMealSuccessUiModel(
            mealTypeTitle = mealTypeTitle,
            searchQuery = state.searchQuery,
            categories = categoriesUi,
            foods = foodsUi,
            selectedFood = foodsUi.find { it.isSelected },
            portionSummary = portionSummary,
            addButtonText = addButtonText,
            isAddEnabled = selectedFood != null,
        )
    }
}
