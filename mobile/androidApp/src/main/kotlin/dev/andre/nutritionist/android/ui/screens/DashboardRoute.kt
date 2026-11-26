package dev.andre.nutritionist.android.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.addmeal.AddMealEvent
import dev.andre.nutritionist.presentation.addmeal.AddMealNews
import dev.andre.nutritionist.presentation.addmeal.AddMealViewModel
import dev.andre.nutritionist.presentation.dashboard.DiaryEvent
import dev.andre.nutritionist.presentation.dashboard.DiaryNews
import dev.andre.nutritionist.presentation.dashboard.DiaryViewModel
import kotlin.time.Clock
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Stateful Route главного экрана Дневника питания (Dashboard).
 * Подключён к [DiaryViewModel] и [AddMealViewModel] (KoTEA) для реактивной загрузки записей из Room,
 * расчёта КБЖУ и бюджета по реальному профилю, переключения дней/недель календаря и выбора блюд (@fitapp_4).
 */
@Composable
fun DashboardRoute(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToRegistration: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    onNavigateToAiGenerator: (MealType, Long, Int) -> Unit = { _, _, _ -> },
    isAiRecipeVisible: Boolean = false,
    onAddMealClick: (MealType) -> Unit = {},
    onMealItemClick: (MealRecordId) -> Unit = {},
    modifier: Modifier = Modifier,
    diaryViewModel: DiaryViewModel = koinViewModel(),
    addMealViewModel: AddMealViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
) {
    val currentUserId = authRepository.getUserId() ?: UserId("testuser")
    val initialEpochMs = androidx.compose.runtime.saveable.rememberSaveable { Clock.System.now().toEpochMilliseconds() }
    val snackbarHostState = remember { SnackbarHostState() }
    var activeMealType by remember { mutableStateOf<MealType?>(null) }

    var initialized by androidx.compose.runtime.saveable.rememberSaveable(currentUserId) { mutableStateOf(false) }
    LaunchedEffect(currentUserId) {
        if (initialized) return@LaunchedEffect
        initialized = true
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

    LaunchedEffect(addMealViewModel) {
        addMealViewModel.news.collect { news ->
            when (news) {
                is AddMealNews.NavigateToAiGenerator -> {
                    activeMealType = null
                    onNavigateToAiGenerator(news.mealType, news.dateEpochMs, news.targetCalories)
                }
                AddMealNews.Dismiss -> activeMealType = null
                is AddMealNews.MealAdded -> activeMealType = null
                is AddMealNews.ShowSnackbar -> snackbarHostState.showSnackbar(news.message)
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
        onAddMealClick = { mealType ->
            activeMealType = mealType
            addMealViewModel.dispatch(
                AddMealEvent.Ui.Open(
                    mealType = mealType,
                    dateEpochMs = uiState.selectedDateEpochMs,
                    dailyTargetCalories = uiState.dailySummary.dailyTargetCalories,
                ),
            )
            onAddMealClick(mealType)
        },
        onAiGenerateClick = { mealType ->
            addMealViewModel.dispatch(
                AddMealEvent.Ui.Open(mealType, uiState.selectedDateEpochMs, uiState.dailySummary.dailyTargetCalories),
            )
            addMealViewModel.dispatch(AddMealEvent.Ui.AiGenerateClicked)
        },
        onMealItemClick = onMealItemClick,
        onCompleteProfileClick = onNavigateToRegistration,
        onNavigateToProfile = onNavigateToProfile,
        onDestinationSelected = onDestinationSelected,
        onRetryClick = {
            diaryViewModel.dispatch(DiaryEvent.Ui.Retry)
        },
        snackbarHostState = snackbarHostState,
        modifier = if (isAiRecipeVisible) modifier.blur(14.dp) else modifier,
    )

    if (activeMealType != null) {
        val addMealUiState by addMealViewModel.state.collectAsStateWithLifecycle()
        AddMealBottomSheet(
            uiState = addMealUiState,
            onEvent = addMealViewModel::dispatch,
        )
    }
}

