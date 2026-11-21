package dev.andre.nutritionist.android.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.addmeal.AddMealEvent
import dev.andre.nutritionist.presentation.addmeal.AddMealNews
import dev.andre.nutritionist.presentation.addmeal.AddMealState
import dev.andre.nutritionist.presentation.addmeal.AddMealViewModel
import dev.andre.nutritionist.presentation.dashboard.DashboardEvent
import dev.andre.nutritionist.presentation.dashboard.DashboardNews
import dev.andre.nutritionist.presentation.dashboard.DashboardViewModel
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Stateful Route главного экрана (Dashboard).
 * Управляет жизненным циклом и событиями MVI через [DashboardViewModel] и [AddMealViewModel].
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
    addMealViewModel: AddMealViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
) {
    val currentUserId = authRepository.getUserId() ?: UserId("testuser")
    val today = remember {
        LocalDate.fromEpochDays(System.currentTimeMillis() / (24 * 60 * 60 * 1000L))
    }

    var activeAddMealType by remember { mutableStateOf<MealType?>(null) }

    LaunchedEffect(currentUserId, today) {
        dashboardViewModel.dispatch(
            DashboardEvent.Ui.LoadDashboard(
                userId = currentUserId,
                date = today,
                showCompleteProfileBanner = false,
            ),
        )
    }

    LaunchedEffect(activeAddMealType, currentUserId, today) {
        val mealType = activeAddMealType
        if (mealType != null) {
            addMealViewModel.dispatch(
                AddMealEvent.Ui.Init(
                    mealType = mealType,
                    userId = currentUserId,
                    date = today,
                ),
            )
        }
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

    LaunchedEffect(addMealViewModel) {
        addMealViewModel.news.collect { news ->
            when (news) {
                is AddMealNews.MealAddedSuccess -> {
                    dashboardViewModel.dispatch(DashboardEvent.Ui.AddMealOptimistic(news.entry))
                    activeAddMealType = null
                }
                AddMealNews.Dismiss -> {
                    activeAddMealType = null
                }
                is AddMealNews.ShowSnackbar -> {
                    // Обработка уведомления
                }
                AddMealNews.OpenBarcodeScanner -> {
                    // Обработка сканера штрихкодов
                }
                AddMealNews.OpenAiCamera -> {
                    // Обработка AI камеры
                }
            }
        }
    }

    val dashboardUiState by dashboardViewModel.state.collectAsStateWithLifecycle()

    MainScreen(
        state = dashboardUiState,
        onAddMealClick = { mealType ->
            activeAddMealType = mealType
            onAddMealClick(mealType)
        },
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

    if (activeAddMealType != null) {
        val addMealUiState by addMealViewModel.state.collectAsStateWithLifecycle()
        AddMealBottomSheet(
            uiState = addMealUiState,
            onQueryChange = { addMealViewModel.dispatch(AddMealEvent.Ui.SearchQueryChanged(it)) },
            onCategorySelect = { addMealViewModel.dispatch(AddMealEvent.Ui.CategorySelected(it)) },
            onFoodSelect = { foodModel ->
                val food = addMealViewModel.addMealState.value.let { s ->
                    (s.content as? AddMealState.Content.Success)?.foods?.find { it.id.raw == foodModel.id }
                }
                if (food != null) {
                    addMealViewModel.dispatch(AddMealEvent.Ui.FoodSelected(food))
                }
            },
            onPortionChange = { addMealViewModel.dispatch(AddMealEvent.Ui.PortionChanged(it)) },
            onConfirmAdd = { addMealViewModel.dispatch(AddMealEvent.Ui.ConfirmAdd) },
            onDismiss = { activeAddMealType = null },
            onScanBarcode = { addMealViewModel.dispatch(AddMealEvent.Ui.BarcodeScanClick) },
            onAiCamera = { addMealViewModel.dispatch(AddMealEvent.Ui.AiCameraClick) },
        )
    }
}
