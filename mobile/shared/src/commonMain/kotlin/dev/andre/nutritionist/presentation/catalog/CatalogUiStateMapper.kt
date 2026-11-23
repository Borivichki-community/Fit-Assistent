package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.presentation.dashboard.formatMacroGrams

/**
 * Чистый маппер состояния каталога [CatalogState] в модель отображения [CatalogUiState].
 */
class CatalogUiStateMapper : UiStateMapper<CatalogState, CatalogUiState> {

    private val availableCategories = listOf("Все", "Баланс", "Эконом", "Похудение", "Набор массы", "Веган", "Кето")

    override fun map(state: CatalogState): CatalogUiState {
        return when (val content = state.content) {
            CatalogState.Content.Loading -> CatalogUiState(content = CatalogUiState.Content.Loading)
            is CatalogState.Content.Error -> CatalogUiState(content = CatalogUiState.Content.Error(content.message))
            is CatalogState.Content.Success -> {
                val data = mapSuccess(state, content.plans)
                CatalogUiState(content = CatalogUiState.Content.Success(data))
            }
        }
    }

    private fun mapSuccess(state: CatalogState, plans: List<MealPlan>): CatalogSuccessUiModel {
        val categoriesUi = availableCategories.map { cat ->
            PlanCategoryUiModel(
                title = cat,
                isSelected = cat.equals(state.selectedCategory, ignoreCase = true),
            )
        }

        val plansUi = plans.map { plan ->
            val budgetInt = plan.estimatedWeeklyBudgetRub.toInt()
            val budgetBadge = "$budgetInt ₽/нед"
            val caloriesBadge = "${plan.targetCalories} ккал/день"
            val macroSummary = "Б: ${formatMacroGrams(plan.proteinsGrams)}г • Ж: ${formatMacroGrams(plan.fatsGrams)}г • У: ${formatMacroGrams(plan.carbsGrams)}г"
            val ratingAndDifficulty = "★ ${plan.rating} • ${plan.difficulty} • ${plan.durationDays} дн."

            MealPlanCardUiModel(
                id = plan.id.raw,
                title = plan.title,
                description = plan.description,
                category = plan.category,
                caloriesBadge = caloriesBadge,
                budgetBadge = budgetBadge,
                macroSummaryText = macroSummary,
                ratingAndDifficultyText = ratingAndDifficulty,
                isFavorite = plan.isFavorite,
                tags = plan.tags,
            )
        }

        return CatalogSuccessUiModel(
            searchQuery = state.query,
            categories = categoriesUi,
            plans = plansUi,
        )
    }
}
