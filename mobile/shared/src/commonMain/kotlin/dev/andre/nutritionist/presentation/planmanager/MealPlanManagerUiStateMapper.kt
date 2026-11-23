package dev.andre.nutritionist.presentation.planmanager

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.GroceryItem
import dev.andre.nutritionist.domain.model.MealPlanDay

/**
 * Маппер внутреннего состояния KoTEA [MealPlanManagerState] в презентационное состояние [MealPlanManagerUiState].
 */
class MealPlanManagerUiStateMapper : UiStateMapper<MealPlanManagerState, MealPlanManagerUiState> {

    override fun map(state: MealPlanManagerState): MealPlanManagerUiState {
        val content = when (val c = state.content) {
            MealPlanManagerState.Content.Loading -> MealPlanManagerUiState.Content.Loading

            is MealPlanManagerState.Content.Empty -> MealPlanManagerUiState.Content.Empty(
                message = c.message,
            )

            is MealPlanManagerState.Content.Error -> MealPlanManagerUiState.Content.Error(
                message = c.message,
            )

            is MealPlanManagerState.Content.Success -> mapSuccess(c)
        }

        return MealPlanManagerUiState(content = content)
    }

    private fun mapSuccess(content: MealPlanManagerState.Content.Success): MealPlanManagerUiState.Content.Success {
        val plan = content.activePlan
        val dayIndex = content.selectedDayIndex.coerceIn(0, (content.days.size - 1).coerceAtLeast(0))

        val daysUi = content.days.mapIndexed { idx, day ->
            mapDay(day = day, isSelected = idx == dayIndex)
        }

        val currentDay = daysUi.getOrNull(dayIndex) ?: PlanDayUiModel(
            dayNumber = 1,
            dayName = "День 1",
            shortDayName = "Д1",
            targetCaloriesText = "${plan.targetCalories} ккал",
            isSelected = true,
            meals = emptyList(),
        )

        val groceryCategories = mapGroceries(content.groceryList)
        val totalGroceries = content.groceryList.size
        val boughtGroceries = content.groceryList.count { it.isBought }
        val progressPercent = if (totalGroceries > 0) {
            boughtGroceries.toFloat() / totalGroceries.toFloat()
        } else {
            0f
        }

        val totalBudget = content.groceryList.sumOf { it.estimatedPriceRub }

        return MealPlanManagerUiState.Content.Success(
            planId = plan.id,
            title = plan.title,
            category = plan.category,
            difficulty = plan.difficulty,
            durationText = "${plan.durationDays} дней",
            targetCaloriesText = "${plan.targetCalories} ккал/день",
            weeklyBudgetText = "${plan.estimatedWeeklyBudgetRub.toInt()} ₽/нед",
            proteinsText = "${plan.proteinsGrams.toInt()} г",
            fatsText = "${plan.fatsGrams.toInt()} г",
            carbsText = "${plan.carbsGrams.toInt()} г",
            selectedTab = content.selectedTab,
            days = daysUi,
            selectedDayIndex = dayIndex,
            currentDay = currentDay,
            groceryCategories = groceryCategories,
            totalGroceryItemsCount = totalGroceries,
            boughtGroceryItemsCount = boughtGroceries,
            groceryProgressPercent = progressPercent,
            groceryProgressText = "Куплено $boughtGroceries из $totalGroceries",
            totalGroceryPriceText = "${totalBudget.toInt()} ₽",
        )
    }

    private fun mapDay(day: MealPlanDay, isSelected: Boolean): PlanDayUiModel {
        val mealsUi = day.meals.map { meal ->
            PlanMealUiModel(
                title = meal.title,
                mealType = meal.mealType,
                caloriesText = "${meal.calories} ккал",
                proteinsText = "Б: ${meal.proteinsGrams.toInt()}г",
                fatsText = "Ж: ${meal.fatsGrams.toInt()}г",
                carbsText = "У: ${meal.carbsGrams.toInt()}г",
            )
        }

        return PlanDayUiModel(
            dayNumber = day.dayNumber,
            dayName = day.dayName,
            shortDayName = day.shortDayName,
            targetCaloriesText = "${day.targetCalories} ккал",
            isSelected = isSelected,
            meals = mealsUi,
        )
    }

    private fun mapGroceries(groceries: List<GroceryItem>): List<GroceryCategoryUiModel> {
        return groceries.groupBy { it.category }.map { (category, items) ->
            val itemsUi = items.map { item ->
                GroceryItemUiModel(
                    id = item.id,
                    name = item.name,
                    amount = item.amount,
                    estimatedPriceText = "${item.estimatedPriceRub.toInt()} ₽",
                    isBought = item.isBought,
                )
            }
            GroceryCategoryUiModel(
                categoryName = category,
                countText = "${items.size} поз.",
                items = itemsUi,
            )
        }
    }
}
