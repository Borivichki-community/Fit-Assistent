package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.android.ui.components.CompleteProfileBanner
import dev.andre.nutritionist.android.ui.components.ErrorContent
import dev.andre.nutritionist.android.ui.components.FitBottomNavigationBar
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.LoadingContent
import dev.andre.nutritionist.android.ui.components.OfflineBanner
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.android.ui.theme.TextMidEmphasis
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.dashboard.DashboardSuccessUiModel
import dev.andre.nutritionist.presentation.dashboard.DashboardUiState
import dev.andre.nutritionist.presentation.dashboard.DashboardUiStateMapper

/**
 * Главный экран приложения (дневник питания и сводка КБЖУ в стиле Aether Zero-G Luxury).
 * Полностью соответствует screenshots/темная тема/дневник.png и fitapp_1/code.html.
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
    onDayClick: (dev.andre.nutritionist.presentation.dashboard.DayOfWeekUiModel) -> Unit = {},
    onPrevWeekClick: () -> Unit = {},
    onNextWeekClick: () -> Unit = {},
    monthLabel: String = "Ноябрь 2026",
    weekNumberLabel: String = "Неделя 45",
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
                        onNavigateToMealPlanManager = { onDestinationSelected(AppDestination.MealPlanManager) },
                        onDayClick = onDayClick,
                        onPrevWeekClick = onPrevWeekClick,
                        onNextWeekClick = onNextWeekClick,
                        monthLabel = monthLabel,
                        weekNumberLabel = weekNumberLabel,
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
    onNavigateToMealPlanManager: () -> Unit = {},
    onDayClick: (dev.andre.nutritionist.presentation.dashboard.DayOfWeekUiModel) -> Unit = {},
    onPrevWeekClick: () -> Unit = {},
    onNextWeekClick: () -> Unit = {},
    monthLabel: String = "Ноябрь 2026",
    weekNumberLabel: String = "Неделя 45",
    isOffline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        // 1. Верхняя плашка приложения (FitApp логотип + кнопка уведомлений со светодиодом)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "FitApp",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                ),
                color = EmeraldAccent,
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF191F2B))
                    .border(BorderStroke(1.dp, GlassBorderLevel2), CircleShape)
                    .clickable { /* Уведомления */ },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FitIcons.Notifications,
                    contentDescription = "Уведомления",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 9.dp, end = 9.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent),
                )
            }
        }

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

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Недельный календарь и стрик
        DashboardWeekStreakBar(
            selectedDateLabel = data.selectedDateLabel,
            streakDays = data.streakDays,
            weekDays = data.weekDays,
            monthLabel = monthLabel,
            weekNumberLabel = weekNumberLabel,
            onDayClick = onDayClick,
            onPrevWeekClick = onPrevWeekClick,
            onNextWeekClick = onNextWeekClick,
            modifier = Modifier.padding(horizontal = Spacing.medium),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Секция «Мои планы» (активные диет-планы)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Мои планы",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFE2E8F0),
                )
                Text(
                    text = "2 активных",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMidEmphasis,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Горизонтальный ряд чипов планов
            val plansScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(plansScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // План 1: Сушка 2400 ккал
                Surface(
                    onClick = onNavigateToMealPlanManager,
                    shape = RoundedCornerShape(20.dp),
                    color = CardDarkSurface,
                    border = BorderStroke(1.dp, GlassBorderLevel1),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "СУШКА 2400 ККАЛ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFE2E8F0),
                        )
                    }
                }

                // План 2: Кето баланс
                Surface(
                    onClick = onNavigateToMealPlanManager,
                    shape = RoundedCornerShape(20.dp),
                    color = CardDarkSurface,
                    border = BorderStroke(1.dp, GlassBorderLevel1),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF9F1C)),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "КЕТО БАЛАНС",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFE2E8F0),
                        )
                    }
                }

                // Добавить план
                Surface(
                    onClick = onNavigateToMealPlanManager,
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1B1F2B),
                    border = BorderStroke(1.dp, Color(0xFF2B3242)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "+ Добавить",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = EmeraldAccent,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Сводная плашка КБЖУ (KPI Bar) с суточной нормой и стоимостью
        DashboardNutritionKpiBar(
            summary = data.nutritionSummary,
            modifier = Modifier.padding(horizontal = Spacing.medium),
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Секции приёмов пищи в порядке дня (Завтрак, Обед, Ужин, Перекус/Другое)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Кнопка добавления упражнения / сна
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium)
                .clickable(onClick = onAddExerciseSleepClick),
            shape = RoundedCornerShape(16.dp),
            color = CardDarkSurface,
            border = BorderStroke(1.dp, GlassBorderLevel1),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF192233)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FitIcons.Exercise,
                        contentDescription = null,
                        tint = Color(0xFF5AB2FF),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = stringResource(R.string.btn_add_exercise_sleep),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
        onCompleteProfileClick = onCompleteProfileClick,
        onNavigateToProfile = onNavigateToProfile,
        onDestinationSelected = onDestinationSelected,
        modifier = modifier,
    )
}
