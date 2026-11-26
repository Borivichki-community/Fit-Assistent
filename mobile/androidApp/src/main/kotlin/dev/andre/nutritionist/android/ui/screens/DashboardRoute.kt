package dev.andre.nutritionist.android.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.dashboard.DiaryEvent
import dev.andre.nutritionist.presentation.dashboard.DiaryNews
import dev.andre.nutritionist.presentation.dashboard.DiaryViewModel
import kotlin.time.Clock
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Stateful Route главного экрана Дневника питания (Dashboard).
 * Подключён к [DiaryViewModel] (KoTEA) для реактивной загрузки записей из Room,
 * расчёта КБЖУ и бюджета по реальному профилю и переключения дней/недель календаря.
 */
@Composable
fun DashboardRoute(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToRegistration: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    onAddMealClick: (MealType) -> Unit = {},
    onMealItemClick: (MealRecordId) -> Unit = {},
    modifier: Modifier = Modifier,
    diaryViewModel: DiaryViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
) {
    val currentUserId = authRepository.getUserId() ?: UserId("testuser")
    val initialEpochMs = remember { Clock.System.now().toEpochMilliseconds() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentUserId) {
        diaryViewModel.dispatch(
            DiaryEvent.Ui.DateSelected(
                dateEpochMs = initialEpochMs,
                userId = currentUserId,
            ),
        )
    }

    LaunchedEffect(diaryViewModel) {
        diaryViewModel.news.collect { news ->
            when (news) {
                is DiaryNews.ShowSnackbar -> snackbarHostState.showSnackbar(news.message)
            }
        }
    }

    val uiState by diaryViewModel.state.collectAsStateWithLifecycle()

    MainScreen(
        state = uiState,
        onDaySelected = { dateEpochMs ->
            diaryViewModel.dispatch(
                DiaryEvent.Ui.DateSelected(
                    dateEpochMs = dateEpochMs,
                    userId = currentUserId,
                ),
            )
        },
        onPreviousWeekClick = {
            diaryViewModel.dispatch(DiaryEvent.Ui.PreviousWeekClicked)
        },
        onNextWeekClick = {
            diaryViewModel.dispatch(DiaryEvent.Ui.NextWeekClicked)
        },
        onToggleMealSection = { mealType ->
            diaryViewModel.dispatch(DiaryEvent.Ui.ToggleMealSection(mealType))
        },
        onDeleteRecordClick = { recordId ->
            diaryViewModel.dispatch(DiaryEvent.Ui.DeleteRecord(recordId))
        },
        onAddMealClick = onAddMealClick,
        onMealItemClick = onMealItemClick,
        onCompleteProfileClick = onNavigateToRegistration,
        onNavigateToProfile = onNavigateToProfile,
        onDestinationSelected = onDestinationSelected,
        onRetryClick = {
            diaryViewModel.dispatch(DiaryEvent.Ui.Retry)
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}
