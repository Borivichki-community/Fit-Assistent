package dev.andre.nutritionist.android.ui.screens.statistics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.statistics.StatisticsEvent
import dev.andre.nutritionist.presentation.statistics.StatisticsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Stateful Route экрана статистики и аналитики.
 * Связывает [StatisticsViewModel] с [StatisticsScreen].
 */
@Composable
fun StatisticsRoute(
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val userId = authRepository.getUserId() ?: UserId("current_user")

    LaunchedEffect(userId) {
        viewModel.dispatch(StatisticsEvent.Ui.Init(userId))
    }

    StatisticsScreen(
        state = state,
        onPeriodSelected = { viewModel.dispatch(StatisticsEvent.Ui.PeriodSelected(it)) },
        onRetryClicked = { viewModel.dispatch(StatisticsEvent.Ui.RetryClicked) },
        onDestinationSelected = onDestinationSelected,
        modifier = modifier,
    )
}
