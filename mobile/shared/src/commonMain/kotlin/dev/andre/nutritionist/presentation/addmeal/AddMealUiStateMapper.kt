package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.MealType

/**
 * Чистый маппер [AddMealState] -> [AddMealUiState].
 */
class AddMealUiStateMapper : UiStateMapper<AddMealState, AddMealUiState> {

    override fun map(state: AddMealState): AddMealUiState {
        val uiContent = when (val c = state.content) {
            AddMealState.Content.Idle -> AddMealUiState.Content.Idle
            AddMealState.Content.Loading -> AddMealUiState.Content.Loading
            is AddMealState.Content.Success -> AddMealUiState.Content.Success(isSaving = c.isSaving)
            AddMealState.Content.Empty -> AddMealUiState.Content.Empty
            is AddMealState.Content.Error -> AddMealUiState.Content.Error(c.message)
        }
        return AddMealUiState(
            mealType = state.mealType,
            mealTypeTitle = state.mealType.toSlotTitle(),
            query = state.query,
            items = state.items,
            content = uiContent,
        )
    }

    private fun MealType.toSlotTitle(): String = when (this) {
        MealType.BREAKFAST -> "Утренний слот"
        MealType.LUNCH -> "Дневной приём"
        MealType.DINNER -> "Вечерний приём"
        MealType.SNACK -> "Перекус & Другое"
    }
}
