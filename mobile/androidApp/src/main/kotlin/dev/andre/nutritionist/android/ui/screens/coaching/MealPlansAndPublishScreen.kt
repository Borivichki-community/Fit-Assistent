package dev.andre.nutritionist.android.ui.screens.coaching

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.ErrorContent
import dev.andre.nutritionist.android.ui.components.LoadingContent
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.domain.model.AccessMode
import dev.andre.nutritionist.presentation.mealplans.MealPlansEvent
import dev.andre.nutritionist.presentation.mealplans.MealPlansUiState

@Composable
fun MealPlansAndPublishScreen(
    state: MealPlansUiState,
    onEvent: (MealPlansEvent.Ui) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBackClick) {
                        Text(stringResource(R.string.common_back))
                    }
                    Text(
                        text = stringResource(R.string.meal_plans_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { onEvent(MealPlansEvent.Ui.SearchQueryChanged(it)) },
                    label = { Text(stringResource(R.string.meal_plans_search_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onEvent(MealPlansEvent.Ui.SearchClicked) }) {
                    Text(stringResource(R.string.common_search))
                }
            }

            when (val content = state.content) {
                MealPlansUiState.Content.Loading -> {
                    LoadingContent(modifier = Modifier.weight(1f))
                }

                is MealPlansUiState.Content.Error -> {
                    ErrorContent(
                        message = content.message,
                        onRetry = { onEvent(MealPlansEvent.Ui.Retry) },
                        modifier = Modifier.weight(1f),
                    )
                }

                is MealPlansUiState.Content.Success -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        content.selectedPlanTitle?.takeIf { it.isNotBlank() }?.let { planTitle ->
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = planTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        content.selectedPlanDetail?.takeIf { it.isNotBlank() }?.let { detail ->
                                            Text(
                                                text = detail,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.meal_plans_create_section),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    OutlinedTextField(
                                        value = state.titleInput,
                                        onValueChange = { onEvent(MealPlansEvent.Ui.TitleChanged(it)) },
                                        label = { Text(stringResource(R.string.meal_plans_name_placeholder)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    OutlinedTextField(
                                        value = state.descriptionInput,
                                        onValueChange = { onEvent(MealPlansEvent.Ui.DescriptionChanged(it)) },
                                        label = { Text(stringResource(R.string.meal_plans_desc_placeholder)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DurationChoiceButton(
                                            label = stringResource(R.string.meal_plans_day_1),
                                            selected = state.durationDays == 1,
                                            onClick = { onEvent(MealPlansEvent.Ui.DurationDaysSelected(1)) },
                                        )
                                        DurationChoiceButton(
                                            label = stringResource(R.string.meal_plans_day_7),
                                            selected = state.durationDays == 7,
                                            onClick = { onEvent(MealPlansEvent.Ui.DurationDaysSelected(7)) },
                                        )
                                    }
                                    val availableModes = if (state.isCoach) {
                                        listOf(AccessMode.FREE, AccessMode.COACH_ASSIGNED, AccessMode.PAID)
                                    } else {
                                        listOf(AccessMode.FREE)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        availableModes.forEach { mode ->
                                            DurationChoiceButton(
                                                label = mode.name,
                                                selected = state.selectedAccessMode == mode,
                                                onClick = { onEvent(MealPlansEvent.Ui.AccessModeSelected(mode)) },
                                            )
                                        }
                                    }
                                    if (state.selectedAccessMode == AccessMode.PAID) {
                                        OutlinedTextField(
                                            value = state.priceRubInput,
                                            onValueChange = { onEvent(MealPlansEvent.Ui.PriceRubChanged(it)) },
                                            label = { Text(stringResource(R.string.meal_plans_price_placeholder)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                    Button(
                                        onClick = { onEvent(MealPlansEvent.Ui.CreatePlanClicked) },
                                        enabled = !state.isSubmitting,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.meal_plans_create_btn))
                                    }
                                }
                            }
                        }

                        items(content.plans, key = { it.id.raw }) { plan ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(plan.title, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = plan.metaLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = plan.idLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                onEvent(MealPlansEvent.Ui.OpenPlanDetailsClicked(plan.id))
                                            },
                                        ) {
                                            Text(stringResource(R.string.student_coaching_open_menu))
                                        }
                                        if (!plan.isPublic) {
                                            Button(
                                                onClick = {
                                                    onEvent(MealPlansEvent.Ui.PublishPlanClicked(plan.id))
                                                },
                                            ) {
                                                Text(stringResource(R.string.meal_plans_publish_btn))
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                onEvent(MealPlansEvent.Ui.SubscribeToPlanClicked(plan.id))
                                            },
                                        ) {
                                            Text(stringResource(R.string.meal_plans_subscribe_btn))
                                        }
                                        if (plan.hasItemsForDayOne) {
                                            Button(
                                                onClick = {
                                                    onEvent(MealPlansEvent.Ui.ApplyPlanDayClicked(plan.id, 1))
                                                },
                                            ) {
                                                Text(stringResource(R.string.meal_plans_apply_day_btn))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = Spacing.small),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.meal_plans_recipes_section),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                OutlinedButton(
                                    onClick = { onEvent(MealPlansEvent.Ui.CreateQuickRecipeClicked) },
                                ) {
                                    Text(stringResource(R.string.meal_plans_quick_recipe_btn))
                                }
                            }
                        }

                        items(content.recipes, key = { it.id.raw }) { recipe ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(recipe.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = recipe.metaLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (!recipe.isPublic) {
                                            Button(
                                                onClick = {
                                                    onEvent(MealPlansEvent.Ui.PublishRecipeClicked(recipe.id))
                                                },
                                            ) {
                                                Text(stringResource(R.string.meal_plans_publish_btn))
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onEvent(MealPlansEvent.Ui.ForkRecipeClicked(recipe.id))
                                            },
                                        ) {
                                            Text(stringResource(R.string.meal_plans_fork_btn))
                                        }
                                        Button(
                                            onClick = {
                                                onEvent(MealPlansEvent.Ui.LogRecipeToDiaryClicked(recipe.id))
                                            },
                                        ) {
                                            Text(stringResource(R.string.meal_plans_log_diary_btn))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
