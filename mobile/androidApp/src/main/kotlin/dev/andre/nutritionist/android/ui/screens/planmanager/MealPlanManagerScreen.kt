package dev.andre.nutritionist.android.ui.screens.planmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.ErrorContent
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.LoadingContent
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.domain.model.GroceryItemId
import dev.andre.nutritionist.presentation.planmanager.MealPlanManagerUiState
import dev.andre.nutritionist.presentation.planmanager.PlanManagerTab

/**
 * Stateless экран менеджера планов питания в стиле Aether Zero-G Luxury.
 */
@Composable
fun MealPlanManagerScreen(
    state: MealPlanManagerUiState,
    onSelectTab: (PlanManagerTab) -> Unit,
    onSelectDay: (Int) -> Unit,
    onToggleGrocery: (GroceryItemId) -> Unit,
    onDeactivatePlan: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("plan_manager_screen"),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, GlassBorderLevel1),
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("plan_manager_back_button"),
                        ) {
                            Icon(
                                imageVector = FitIcons.ArrowBack,
                                contentDescription = "Назад",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "FITAPP ПЛАНЫ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.08.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.plan_manager_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("plan_manager_top_bar_title"),
                        )
                    }
                }

                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, GlassBorderLevel1),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = FitIcons.Sparkles,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val content = state.content) {
                MealPlanManagerUiState.Content.Loading -> {
                    LoadingContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("plan_manager_loading"),
                    )
                }

                is MealPlanManagerUiState.Content.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.lg)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        MealPlanManagerEmptyContent(
                            onNavigateToCatalog = onNavigateToCatalog,
                            message = content.message,
                        )
                    }
                }

                is MealPlanManagerUiState.Content.Error -> {
                    ErrorContent(
                        message = content.message,
                        onRetry = onRetry,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.lg),
                    )
                }

                is MealPlanManagerUiState.Content.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        MealPlanManagerSuccessContent(
                            content = content,
                            onSelectTab = onSelectTab,
                            onSelectDay = onSelectDay,
                            onToggleGrocery = onToggleGrocery,
                            onDeactivatePlan = onDeactivatePlan,
                            onChangePlan = onNavigateToCatalog,
                        )
                    }
                }
            }
        }
    }
}
