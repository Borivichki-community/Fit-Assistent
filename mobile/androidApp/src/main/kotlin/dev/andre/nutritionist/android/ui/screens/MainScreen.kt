package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.android.ui.components.CompleteProfileBanner
import dev.andre.nutritionist.android.ui.components.FitBottomNavigationBar
import dev.andre.nutritionist.android.ui.components.FitTopBar
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.FitThemeTokens
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.dashboard.DiaryUiState
import dev.andre.nutritionist.presentation.model.ActivePlanChipUiModel

/**
 * Главный экран Дневника питания по макету fitapp_1.
 * Полностью stateless-компонент, принимающий только [DiaryUiState] и коллбэки событий.
 */
@Composable
fun MainScreen(
    state: DiaryUiState,
    onDaySelected: (Long) -> Unit = {},
    onPreviousWeekClick: () -> Unit = {},
    onNextWeekClick: () -> Unit = {},
    onToggleMealSection: (MealType) -> Unit = {},
    onDeleteRecordClick: (MealRecordId) -> Unit = {},
    onAddMealClick: (MealType) -> Unit = {},
    onMealItemClick: (MealRecordId) -> Unit = {},
    onAiGenerateClick: (MealType) -> Unit = {},
    onQuickPresetClick: (MealType, String) -> Unit = { _, _ -> },
    onNotificationsClick: () -> Unit = {},
    onPlanDismissClick: (String) -> Unit = {},
    onAddNutritionClick: () -> Unit = {},
    onStubActionClick: () -> Unit = {},
    onCompleteProfileClick: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    onRetryClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val extendedColors = FitThemeTokens.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
        ) {
            FitTopBar(
                title = "FitApp",
                onNotificationsClick = onNotificationsClick,
            )

            when (val content = state.content) {
                DiaryUiState.Content.Loading -> {
                    LinearProgressIndicator(
                        color = extendedColors.emeraldAccent,
                        trackColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                    )
                }
                is DiaryUiState.Content.Error -> {
                    DiaryErrorBanner(
                        message = content.message,
                        onRetryClick = onRetryClick,
                        modifier = Modifier.padding(horizontal = Spacing.medium, vertical = 4.dp),
                    )
                }
                DiaryUiState.Content.Success,
                DiaryUiState.Content.Empty -> Unit
            }

            if (state.showCompleteProfileBanner) {
                Spacer(modifier = Modifier.height(Spacing.small))
                CompleteProfileBanner(
                    onCompleteClick = onCompleteProfileClick,
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // 1. Календарь недели с переключением дней и недель (< / >)
            DashboardWeekStreakBar(
                monthYearLabel = state.monthYearLabel,
                weekNumberLabel = state.weekNumberLabel,
                weekDays = state.weekDays,
                onDaySelected = onDaySelected,
                onPreviousWeekClick = onPreviousWeekClick,
                onNextWeekClick = onNextWeekClick,
                modifier = Modifier.padding(horizontal = Spacing.medium),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Секция «Мои планы» (UI-заглушка по макету fitapp_1)
            ActivePlansSection(
                plans = state.activePlans,
                onPlanDismissClick = onPlanDismissClick,
                modifier = Modifier.padding(horizontal = Spacing.medium),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Блок «СУТОЧНАЯ НОРМА» с реальными расчётами КБЖУ, стоимости и бюджета
            DashboardNutritionKpiBar(
                summary = state.dailySummary,
                onAddClick = onAddNutritionClick,
                modifier = Modifier.padding(horizontal = Spacing.medium),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Секции приёмов пищи (Завтрак, Обед, Ужин, Перекус & Другое)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                state.mealSections.forEach { meal ->
                    MealSectionCard(
                        meal = meal,
                        onAddMealClick = onAddMealClick,
                        onToggleExpand = onToggleMealSection,
                        onMealItemClick = onMealItemClick,
                        onDeleteRecordClick = onDeleteRecordClick,
                        onAiGenerateClick = onAiGenerateClick,
                        onQuickPresetClick = onQuickPresetClick,
                        onStubActionClick = onStubActionClick,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}

@Composable
private fun ActivePlansSection(
    plans: List<ActivePlanChipUiModel>,
    onPlanDismissClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (plans.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.section_my_plans),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.diary_active_plans_count),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            plans.forEach { plan ->
                PlanChip(
                    title = plan.title,
                    isPrimaryAccent = plan.isPrimaryAccent,
                    onDismissClick = { onPlanDismissClick(plan.id) },
                )
            }
        }
    }
}

@Composable
private fun DiaryErrorBanner(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetryClick) {
                Text(
                    text = stringResource(R.string.retry_button),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
