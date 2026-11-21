package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.presentation.airecipe.AiRecipeEvent
import dev.andre.nutritionist.presentation.airecipe.AiRecipeNews
import dev.andre.nutritionist.presentation.airecipe.AiRecipeViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful Route экрана генерации AI-рецептов.
 * Связывает [AiRecipeViewModel] с [AiRecipeScreen].
 */
@Composable
fun AiRecipeRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDiary: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AiRecipeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.news.collect { news ->
            when (news) {
                is AiRecipeNews.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(news.message)
                }
                AiRecipeNews.RecipeAddedToDiarySuccess -> {
                    onNavigateToDiary()
                }
                AiRecipeNews.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    AiRecipeScreen(
        state = state,
        onQueryChanged = { viewModel.dispatch(AiRecipeEvent.Ui.QueryChanged(it)) },
        onCaloriesChanged = { viewModel.dispatch(AiRecipeEvent.Ui.CaloriesChanged(it)) },
        onBudgetChanged = { viewModel.dispatch(AiRecipeEvent.Ui.BudgetChanged(it)) },
        onMealTypeSelected = { viewModel.dispatch(AiRecipeEvent.Ui.MealTypeSelected(it)) },
        onDietaryPreferenceSelected = { viewModel.dispatch(AiRecipeEvent.Ui.DietaryPreferenceSelected(it)) },
        onGenerateClicked = { viewModel.dispatch(AiRecipeEvent.Ui.GenerateClicked) },
        onSaveRecipeClicked = { viewModel.dispatch(AiRecipeEvent.Ui.SaveRecipeClicked) },
        onAddRecipeToDiaryClicked = { viewModel.dispatch(AiRecipeEvent.Ui.AddRecipeToDiaryClicked) },
        onResetClicked = { viewModel.dispatch(AiRecipeEvent.Ui.ResetClicked) },
        onRetryClicked = { viewModel.dispatch(AiRecipeEvent.Ui.RetryClicked) },
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}
