package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.domain.network.ConnectivityObserver
import dev.andre.nutritionist.presentation.airecipe.AiRecipeEvent
import dev.andre.nutritionist.presentation.airecipe.AiRecipeNews
import dev.andre.nutritionist.presentation.airecipe.AiRecipeViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
    connectivityObserver: ConnectivityObserver = koinInject(),
    snackbarHostState: SnackbarHostState? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val localSnackbarHostState = remember { SnackbarHostState() }
    val effectiveSnackbarHostState = snackbarHostState ?: localSnackbarHostState
    val isOnline by connectivityObserver.isConnected.collectAsStateWithLifecycle()

    LaunchedEffect(isOnline) {
        viewModel.dispatch(AiRecipeEvent.Ui.NetworkStatusChanged(isOnline))
    }

    LaunchedEffect(viewModel) {
        viewModel.news.collect { news ->
            when (news) {
                is AiRecipeNews.ShowSnackbar -> {
                    effectiveSnackbarHostState.showSnackbar(news.message)
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
        snackbarHostState = effectiveSnackbarHostState,
        modifier = modifier,
    )
}
