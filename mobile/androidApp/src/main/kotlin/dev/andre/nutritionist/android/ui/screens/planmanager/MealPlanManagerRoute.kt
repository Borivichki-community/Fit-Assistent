package dev.andre.nutritionist.android.ui.screens.planmanager

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.presentation.planmanager.MealPlanManagerEvent
import dev.andre.nutritionist.presentation.planmanager.MealPlanManagerNews
import dev.andre.nutritionist.presentation.planmanager.MealPlanManagerViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Stateful Route экрана управления рационом питания и списком покупок.
 * Связывает [MealPlanManagerViewModel] с stateless экраном [MealPlanManagerScreen].
 */
@Composable
fun MealPlanManagerRoute(
    onNavigateBack: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealPlanManagerViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
    snackbarHostState: SnackbarHostState? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val localSnackbarHostState = remember { SnackbarHostState() }
    val effectiveSnackbarHostState = snackbarHostState ?: localSnackbarHostState

    val currentUserId = remember(authRepository) { authRepository.getUserId() }
    if (currentUserId == null) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }

    LaunchedEffect(currentUserId) {
        viewModel.dispatch(MealPlanManagerEvent.Ui.Load(currentUserId))
    }

    LaunchedEffect(viewModel) {
        viewModel.news.collect { news ->
            when (news) {
                is MealPlanManagerNews.ShowSnackbar -> {
                    effectiveSnackbarHostState.showSnackbar(news.message)
                }
                MealPlanManagerNews.NavigateToCatalog -> {
                    onNavigateToCatalog()
                }
            }
        }
    }

    MealPlanManagerScreen(
        state = state,
        onSelectTab = { tab ->
            viewModel.dispatch(MealPlanManagerEvent.Ui.SelectTab(tab))
        },
        onSelectDay = { dayIndex ->
            viewModel.dispatch(MealPlanManagerEvent.Ui.SelectDay(dayIndex))
        },
        onToggleGrocery = { itemId ->
            viewModel.dispatch(MealPlanManagerEvent.Ui.ToggleGrocery(itemId))
        },
        onDeactivatePlan = {
            viewModel.dispatch(MealPlanManagerEvent.Ui.DeactivatePlan)
        },
        onNavigateToCatalog = {
            viewModel.dispatch(MealPlanManagerEvent.Ui.NavigateToCatalog)
        },
        onRetry = {
            viewModel.dispatch(MealPlanManagerEvent.Ui.Retry)
        },
        onNavigateBack = onNavigateBack,
        snackbarHostState = effectiveSnackbarHostState,
        modifier = modifier,
    )
}
