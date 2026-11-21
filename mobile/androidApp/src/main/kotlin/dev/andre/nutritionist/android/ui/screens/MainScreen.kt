package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.android.ui.components.CompleteProfileBanner
import dev.andre.nutritionist.android.ui.components.ErrorContent
import dev.andre.nutritionist.android.ui.components.FitBottomNavigationBar
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.FitTopBar
import dev.andre.nutritionist.android.ui.components.LoadingContent
import dev.andre.nutritionist.android.ui.components.OfflineBanner
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.dashboard.DashboardSuccessUiModel
import dev.andre.nutritionist.presentation.dashboard.DashboardUiState
import dev.andre.nutritionist.presentation.dashboard.DashboardUiStateMapper

/**
 * Главный экран приложения (дневник питания и сводка КБЖУ в Antigravity дизайне).
 * Полностью stateless-компонент, принимающий только [DashboardUiState] и лямбды событий.
 */
@Composable
fun MainScreen(
    state: DashboardUiState,
    onAddMealClick: (MealType) -> Unit = {},
    onMealItemClick: (String) -> Unit = {},
    onToggleMealSection: (MealType) -> Unit = {},
    onCompleteProfileClick: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    onAddExerciseSleepClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    isOffline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FitBottomNavigationBar(
                currentDestination = AppDestination.Dashboard,
                onDestinationSelected = { destination ->
                    if (destination == AppDestination.Profile) {
                        onNavigateToProfile()
                    } else {
                        onDestinationSelected(destination)
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val content = state.content) {
                DashboardUiState.Content.Loading -> {
                    LoadingContent()
                }
                is DashboardUiState.Content.Error -> {
                    ErrorContent(
                        message = content.message,
                        onRetry = onRetry,
                    )
                }
                is DashboardUiState.Content.Success -> {
                    MainScreenSuccessContent(
                        data = content.data,
                        onAddMealClick = onAddMealClick,
                        onMealItemClick = onMealItemClick,
                        onToggleMealSection = onToggleMealSection,
                        onCompleteProfileClick = onCompleteProfileClick,
                        onAddExerciseSleepClick = onAddExerciseSleepClick,
                        isOffline = isOffline,
                    )
                }
            }
        }
    }
}

/**
 * Отображение успешных данных главного экрана дневника питания.
 */
@Composable
fun MainScreenSuccessContent(
    data: DashboardSuccessUiModel,
    onAddMealClick: (MealType) -> Unit,
    onMealItemClick: (String) -> Unit,
    onToggleMealSection: (MealType) -> Unit,
    onCompleteProfileClick: () -> Unit,
    onAddExerciseSleepClick: () -> Unit,
    isOffline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        // Верхняя плашка приложения
        FitTopBar(
            title = "FitApp",
            onMenuClick = { /* открытие меню */ },
        )

        if (isOffline) {
            OfflineBanner()
        }

        // Баннер предупреждения о незаполненном профиле
        if (data.showCompleteProfileBanner) {
            Spacer(modifier = Modifier.height(Spacing.small))
            CompleteProfileBanner(
                onCompleteClick = onCompleteProfileClick,
                modifier = Modifier.padding(horizontal = Spacing.medium),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        // 1. Недельный календарь и стрик
        DashboardWeekStreakBar(
            selectedDateLabel = data.selectedDateLabel,
            streakDays = data.streakDays,
            weekDays = data.weekDays,
            modifier = Modifier.padding(horizontal = Spacing.medium),
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        // 2. Сводная плашка КБЖУ (KPI Bar) с бэкенда
        DashboardNutritionKpiBar(
            summary = data.nutritionSummary,
            modifier = Modifier.padding(horizontal = Spacing.medium),
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        // 3. Секции приёмов пищи в порядке дня (Завтрак, Обед, Ужин, Перекус/Другое)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            data.meals.forEach { meal ->
                MealSectionCard(
                    meal = meal,
                    onAddMealClick = onAddMealClick,
                    onToggleExpand = onToggleMealSection,
                    onMealItemClick = onMealItemClick,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        // 4. Кнопка добавления упражнения / сна
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium)
                .clickable(onClick = onAddExerciseSleepClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(Spacing.cardRadius),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium, vertical = Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = FitIcons.Exercise,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )

                Spacer(modifier = Modifier.width(Spacing.medium))

                Text(
                    text = stringResource(R.string.btn_add_exercise_sleep),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.large))
    }
}

/**
 * Перегрузка для обратной совместимости с вызовами без явного состояния.
 */
@Composable
fun MainScreen(
    showCompleteProfileBanner: Boolean = false,
    onCompleteProfileClick: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val mapper = remember { DashboardUiStateMapper() }
    val defaultState = remember(showCompleteProfileBanner) {
        mapper.map(
            profile = null,
            showCompleteProfileBanner = showCompleteProfileBanner,
        )
    }

    MainScreen(
        state = defaultState,
        onAddMealClick = {},
        onMealItemClick = {},
        onToggleMealSection = {},
        onCompleteProfileClick = onCompleteProfileClick,
        onNavigateToProfile = onNavigateToProfile,
        onDestinationSelected = onDestinationSelected,
        modifier = modifier,
    )
}

/**
 * Перегрузка для прямого отображения [DashboardSuccessUiModel].
 */
@Composable
fun MainScreen(
    data: DashboardSuccessUiModel,
    onAddMealClick: (MealType) -> Unit = {},
    onMealItemClick: (String) -> Unit = {},
    onToggleMealSection: (MealType) -> Unit = {},
    onCompleteProfileClick: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    onAddExerciseSleepClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    MainScreen(
        state = DashboardUiState(content = DashboardUiState.Content.Success(data)),
        onAddMealClick = onAddMealClick,
        onMealItemClick = onMealItemClick,
        onToggleMealSection = onToggleMealSection,
        onCompleteProfileClick = onCompleteProfileClick,
        onNavigateToProfile = onNavigateToProfile,
        onDestinationSelected = onDestinationSelected,
        onAddExerciseSleepClick = onAddExerciseSleepClick,
        modifier = modifier,
    )
}
