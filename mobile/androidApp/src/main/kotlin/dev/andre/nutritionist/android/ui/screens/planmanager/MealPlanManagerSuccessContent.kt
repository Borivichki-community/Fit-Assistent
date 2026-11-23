package dev.andre.nutritionist.android.ui.screens.planmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.domain.model.GroceryItemId
import dev.andre.nutritionist.presentation.planmanager.MealPlanManagerUiState
import dev.andre.nutritionist.presentation.planmanager.PlanManagerTab

/**
 * Контент экрана управления активным рационом питания в стиле Aether Zero-G Luxury.
 */
@Composable
fun MealPlanManagerSuccessContent(
    content: MealPlanManagerUiState.Content.Success,
    onSelectTab: (PlanManagerTab) -> Unit,
    onSelectDay: (Int) -> Unit,
    onToggleGrocery: (GroceryItemId) -> Unit,
    onDeactivatePlan: () -> Unit,
    onChangePlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("plan_manager_success_content"),
    ) {
        // Hero-карточка активного плана питания
        ActivePlanHeroCard(
            content = content,
            onChangePlan = onChangePlan,
            onDeactivatePlan = onDeactivatePlan,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Табы переключения: "Расписание" / "Список покупок"
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.pill),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, GlassBorderLevel1),
        ) {
            PrimaryTabRow(
                selectedTabIndex = if (content.selectedTab == PlanManagerTab.SCHEDULE) 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Spacing.pill)),
                containerColor = Color.Transparent,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(
                            selectedTabIndex = if (content.selectedTab == PlanManagerTab.SCHEDULE) 0 else 1,
                            matchContentSize = true,
                        ),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    )
                },
                divider = {},
            ) {
                Tab(
                    selected = content.selectedTab == PlanManagerTab.SCHEDULE,
                    onClick = { onSelectTab(PlanManagerTab.SCHEDULE) },
                    text = {
                        Text(
                            text = stringResource(R.string.plan_manager_tab_schedule),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (content.selectedTab == PlanManagerTab.SCHEDULE) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("tab_schedule"),
                )

                Tab(
                    selected = content.selectedTab == PlanManagerTab.GROCERY_LIST,
                    onClick = { onSelectTab(PlanManagerTab.GROCERY_LIST) },
                    text = {
                        Text(
                            text = stringResource(R.string.plan_manager_tab_groceries),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (content.selectedTab == PlanManagerTab.GROCERY_LIST) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("tab_groceries"),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Содержимое выбранного таба
        when (content.selectedTab) {
            PlanManagerTab.SCHEDULE -> {
                PlanDayScheduleSection(
                    days = content.days,
                    currentDay = content.currentDay,
                    onSelectDay = onSelectDay,
                )
            }
            PlanManagerTab.GROCERY_LIST -> {
                GroceryListSection(
                    categories = content.groceryCategories,
                    progressText = content.groceryProgressText,
                    progressPercent = content.groceryProgressPercent,
                    totalPriceText = content.totalGroceryPriceText,
                    onToggleGrocery = onToggleGrocery,
                )
            }
        }
    }
}
