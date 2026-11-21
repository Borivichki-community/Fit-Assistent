package dev.andre.nutritionist.android.ui.screens.savedrecipes

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.domain.network.ConnectivityObserver
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipesEvent
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipesNews
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipesViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Stateful Route экрана сохранённых рецептов.
 * Инжектирует [SavedRecipesViewModel] через Koin и связывает MVI Store с UI.
 */
@Composable
fun SavedRecipesRoute(
    onNavigateBack: () -> Unit,
    onNavigateToAiGenerator: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    viewModel: SavedRecipesViewModel = koinViewModel(),
    connectivityObserver: ConnectivityObserver = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isConnected by connectivityObserver.isConnected.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.dispatch(SavedRecipesEvent.Ui.Init)
    }

    LaunchedEffect(viewModel) {
        viewModel.news.collect { news ->
            when (news) {
                is SavedRecipesNews.ShowSnackbar -> {
                    snackbarHostState?.showSnackbar(news.message)
                }
                is SavedRecipesNews.ShowMessage -> {
                    snackbarHostState?.showSnackbar(news.message)
                }
                is SavedRecipesNews.NavigateToAiGenerator -> {
                    onNavigateToAiGenerator()
                }
            }
        }
    }

    SavedRecipesScreen(
        state = state,
        isOffline = !isConnected,
        onSearchQueryChanged = { viewModel.dispatch(SavedRecipesEvent.Ui.SearchQueryChanged(it)) },
        onMealTypeFilterSelected = { viewModel.dispatch(SavedRecipesEvent.Ui.MealTypeFilterSelected(it)) },
        onRecipeClicked = { viewModel.dispatch(SavedRecipesEvent.Ui.RecipeClicked(it)) },
        onToggleFavorite = { viewModel.dispatch(SavedRecipesEvent.Ui.ToggleFavoriteClicked(it)) },
        onAddRecipeToDiary = { viewModel.dispatch(SavedRecipesEvent.Ui.AddRecipeToDiaryClicked(it)) },
        onDismissDetail = { viewModel.dispatch(SavedRecipesEvent.Ui.RecipeDetailDismissed) },
        onEmptyActionClicked = {
            if (state.searchQuery.isNotBlank() || state.mealTypeFilters.any { it.mealType != null && it.isSelected }) {
                viewModel.dispatch(SavedRecipesEvent.Ui.SearchQueryChanged(""))
                viewModel.dispatch(SavedRecipesEvent.Ui.MealTypeFilterSelected(null))
            } else {
                viewModel.dispatch(SavedRecipesEvent.Ui.GenerateRecipeClicked)
            }
        },
        onRetryClicked = { viewModel.dispatch(SavedRecipesEvent.Ui.RetryClicked) },
        onGenerateRecipeClicked = { viewModel.dispatch(SavedRecipesEvent.Ui.GenerateRecipeClicked) },
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}
