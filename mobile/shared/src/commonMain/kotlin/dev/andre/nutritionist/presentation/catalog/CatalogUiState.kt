package dev.andre.nutritionist.presentation.catalog

/**
 * UI-модель карточки плана питания.
 */
data class MealPlanCardUiModel(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val caloriesBadge: String,
    val budgetBadge: String,
    val macroSummaryText: String,
    val ratingAndDifficultyText: String,
    val isFavorite: Boolean,
    val tags: List<String>,
)

/**
 * UI-модель чипса категории плана питания.
 */
data class PlanCategoryUiModel(
    val title: String,
    val isSelected: Boolean = false,
)

/**
 * Успешные данные каталога рационов питания.
 */
data class CatalogSuccessUiModel(
    val searchQuery: String,
    val categories: List<PlanCategoryUiModel>,
    val plans: List<MealPlanCardUiModel>,
    val featuredAiTitle: String = "Персональный AI-рацион",
    val featuredAiSubtitle: String = "Сгенерируйте сбалансированное меню под ваш бюджет и цели",
    val featuredAiButtonText: String = "Сгенерировать рацион",
)

/**
 * Состояние экрана каталога рационов питания.
 */
data class CatalogUiState(
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val data: CatalogSuccessUiModel) : Content
        data class Error(val message: String) : Content
    }
}
