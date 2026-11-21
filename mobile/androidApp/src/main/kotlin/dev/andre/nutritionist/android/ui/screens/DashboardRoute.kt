package dev.andre.nutritionist.android.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.dashboard.DashboardEvent
import dev.andre.nutritionist.presentation.dashboard.DashboardNews
import dev.andre.nutritionist.presentation.dashboard.DashboardViewModel
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Stateful Route главного экрана (Dashboard).
 * Управляет жизненным циклом и событиями MVI через [DashboardViewModel].
 */
@Composable
fun DashboardRoute(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToRegistration: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    onAddMealClick: (MealType) -> Unit = {},
    onMealItemClick: (String) -> Unit = {},
    onAddExerciseSleepClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    dashboardViewModel: DashboardViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
) {
    val currentUserId = authRepository.getUserId() ?: UserId("testuser")
    val today = remember {
        LocalDate.fromEpochDays(System.currentTimeMillis() / (24 * 60 * 60 * 1000L))
    }

    LaunchedEffect(currentUserId, today) {
        dashboardViewModel.dispatch(
            DashboardEvent.Ui.LoadDashboard(
                userId = currentUserId,
                date = today,
                showCompleteProfileBanner = false,
            ),
        )
    }

    LaunchedEffect(dashboardViewModel) {
        dashboardViewModel.news.collect { news ->
            when (news) {
                is DashboardNews.ShowSnackbar -> {
                    // Обработка уведомления
                }
                is DashboardNews.NavigateToMealDetail -> {
                    onMealItemClick(news.mealId)
                }
            }
        }
    }

    val dashboardUiState by dashboardViewModel.state.collectAsStateWithLifecycle()

    MainScreen(
        state = dashboardUiState,
        onAddMealClick = onAddMealClick,
        onMealItemClick = onMealItemClick,
        onToggleMealSection = { mealType ->
            dashboardViewModel.dispatch(DashboardEvent.Ui.ToggleMealSection(mealType))
        },
        onCompleteProfileClick = onNavigateToRegistration,
        onNavigateToProfile = onNavigateToProfile,
        onDestinationSelected = onDestinationSelected,
        onAddExerciseSleepClick = onAddExerciseSleepClick,
        onRetry = {
            dashboardViewModel.dispatch(DashboardEvent.Ui.Retry)
        },
        modifier = modifier,
    )
}
