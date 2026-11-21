package dev.andre.nutritionist.android.ui.screens.catalog

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.presentation.catalog.CatalogEvent
import dev.andre.nutritionist.presentation.catalog.CatalogNews
import dev.andre.nutritionist.presentation.catalog.CatalogViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful Route для экрана каталога рационов питания.
 * Связывает [CatalogViewModel] с stateless UI [CatalogScreen].
 */
@Composable
fun CatalogRoute(
    onNavigateToProfile: () -> Unit,
    onDestinationSelected: (AppDestination) -> Unit,
    onNavigateToAiGenerator: () -> Unit = {},
    onNavigateToPlanDetail: (MealPlanId) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.dispatch(CatalogEvent.Ui.Load)
    }

    LaunchedEffect(viewModel) {
        viewModel.news.collect { news ->
            when (news) {
                is CatalogNews.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(news.message)
                }
                CatalogNews.NavigateToAiGenerator -> {
                    onNavigateToAiGenerator()
                }
                is CatalogNews.NavigateToPlanDetail -> {
                    onNavigateToPlanDetail(news.planId)
                }
            }
        }
    }

    CatalogScreen(
        state = state,
        onSearchQueryChanged = { query ->
            viewModel.dispatch(CatalogEvent.Ui.SearchQueryChanged(query))
        },
        onCategorySelected = { category ->
            viewModel.dispatch(CatalogEvent.Ui.CategorySelected(category))
        },
        onPlanSelected = { planId ->
            viewModel.dispatch(CatalogEvent.Ui.PlanSelected(planId))
        },
        onToggleFavorite = { planId ->
            viewModel.dispatch(CatalogEvent.Ui.ToggleFavorite(planId))
        },
        onGenerateAiPlanClick = {
            viewModel.dispatch(CatalogEvent.Ui.GenerateAiPlanClick)
        },
        onRetryClick = {
            viewModel.dispatch(CatalogEvent.Ui.Retry)
        },
        onNavigateToProfile = onNavigateToProfile,
        onDestinationSelected = onDestinationSelected,
        modifier = modifier,
    )
}
