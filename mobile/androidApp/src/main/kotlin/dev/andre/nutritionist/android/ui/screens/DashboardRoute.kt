package dev.andre.nutritionist.android.ui.screens

import androidx.compose.material3.SnackbarHostState
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
import dev.andre.nutritionist.domain.network.ConnectivityObserver
import dev.andre.nutritionist.domain.sync.SyncScheduler
import dev.andre.nutritionist.presentation.addmeal.AddMealEvent
import dev.andre.nutritionist.presentation.addmeal.AddMealNews
import dev.andre.nutritionist.presentation.addmeal.AddMealState
import dev.andre.nutritionist.presentation.addmeal.AddMealViewModel
import dev.andre.nutritionist.presentation.dashboard.DashboardEvent
import dev.andre.nutritionist.presentation.dashboard.DashboardNews
import dev.andre.nutritionist.presentation.dashboard.DashboardViewModel
import androidx.compose.runtime.rememberCoroutineScope
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.FoodItemId
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.RecipeRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.time.Instant

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
    onNavigateToAuth: () -> Unit = {},
    modifier: Modifier = Modifier,
    dashboardViewModel: DashboardViewModel = koinViewModel(),
    addMealViewModel: AddMealViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
    connectivityObserver: ConnectivityObserver = koinInject(),
    syncScheduler: SyncScheduler = koinInject(),
    recipeRepository: RecipeRepository = koinInject(),
    diaryRepository: DiaryRepository = koinInject(),
    snackbarHostState: SnackbarHostState? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = remember(authRepository) { authRepository.getUserId() }
    if (currentUserId == null) {
        LaunchedEffect(Unit) {
            onNavigateToAuth()
        }
        return
    }

    val isOnline by connectivityObserver.isConnected.collectAsStateWithLifecycle()

    LaunchedEffect(isOnline) {
        addMealViewModel.dispatch(AddMealEvent.Ui.NetworkStatusChanged(isOnline))
    }

    val today = remember {
        LocalDate.fromEpochDays(System.currentTimeMillis() / (24 * 60 * 60 * 1000L))
    }
    var selectedDate by remember { mutableStateOf(today) }
    var weekOffset by remember { mutableStateOf(0) }

    val months = remember {
        listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
    }
    val monthLabel = "${months.getOrElse(selectedDate.month.number - 1) { "" }} ${selectedDate.year}"
    val weekNumberLabel = "Неделя ${(selectedDate.dayOfYear / 7) + 1}"

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

    LaunchedEffect(activeAddMealType, currentUserId, selectedDate) {
        val mealType = activeAddMealType
        if (mealType != null) {
            addMealViewModel.dispatch(
                AddMealEvent.Ui.Init(
                    mealType = mealType,
                    userId = currentUserId,
                    date = selectedDate,
                ),
            )
        }
    }

    LaunchedEffect(dashboardViewModel) {
        dashboardViewModel.news.collect { news ->
            when (news) {
                is DashboardNews.ShowSnackbar -> {
                    snackbarHostState?.showSnackbar(news.message)
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
                    syncScheduler.scheduleDiarySync()
                }
                AddMealNews.Dismiss -> {
                    activeAddMealType = null
                }
                is AddMealNews.ShowSnackbar -> {
                    snackbarHostState?.showSnackbar(news.message)
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
        isOffline = !isOnline,
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
        onDayClick = { dayModel ->
            val d = dayModel.date
            if (d != null) {
                selectedDate = d
                dashboardViewModel.dispatch(DashboardEvent.Ui.SelectDate(d))
            }
        },
        onPrevWeekClick = {
            weekOffset -= 1
            val newDate = LocalDate.fromEpochDays(selectedDate.toEpochDays() - 7)
            selectedDate = newDate
            dashboardViewModel.dispatch(DashboardEvent.Ui.SelectDate(newDate))
        },
        onNextWeekClick = {
            weekOffset += 1
            val newDate = LocalDate.fromEpochDays(selectedDate.toEpochDays() + 7)
            selectedDate = newDate
            dashboardViewModel.dispatch(DashboardEvent.Ui.SelectDate(newDate))
        },
        monthLabel = monthLabel,
        weekNumberLabel = weekNumberLabel,
        onRetry = {
            dashboardViewModel.dispatch(DashboardEvent.Ui.Retry)
        },
        modifier = modifier,
    )

    val currentMealType = activeAddMealType
    if (currentMealType != null) {
        dev.andre.nutritionist.android.ui.screens.airecipe.AiRecipeGenerationBottomSheet(
            mealType = currentMealType,
            onDismiss = { activeAddMealType = null },
            recipeRepository = recipeRepository,
            onRecipeGeneratedAndAdded = { recipe ->
                activeAddMealType = null
                coroutineScope.launch {
                    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                    val entry = DiaryEntry(
                        id = DiaryEntryId("diary-gen-${recipe.id.raw}-${now.toEpochMilliseconds()}"),
                        userId = currentUserId,
                        foodItemId = FoodItemId("ai-recipe-${recipe.id.raw}"),
                        foodName = recipe.title,
                        portionGrams = 300.0,
                        calories = recipe.calories,
                        proteins = recipe.proteins,
                        fats = recipe.fats,
                        carbs = recipe.carbs,
                        mealType = currentMealType,
                        date = selectedDate,
                        recordedAt = now,
                        idempotencyKey = IdempotencyKey.generate(),
                        portionDescription = "1 порция (${recipe.calories} ккал)",
                    )
                    dashboardViewModel.dispatch(DashboardEvent.Ui.AddMealOptimistic(entry))
                    diaryRepository.addEntry(entry)
                    // Прямой синхронный gRPC вызов в diary-service:
                    dev.andre.nutritionist.data.remote.DefaultDiaryRemoteDataSource.companionSyncHandler?.invoke(entry)
                    syncScheduler.scheduleDiarySync()
                    snackbarHostState?.showSnackbar("Рецепт «${recipe.title}» добавлен в дневник!")
                }
            },
            onGenerate = { query, budget, calories ->
                activeAddMealType = null
                onDestinationSelected(AppDestination.AiRecipeGenerator)
            },
        )
    }
}
