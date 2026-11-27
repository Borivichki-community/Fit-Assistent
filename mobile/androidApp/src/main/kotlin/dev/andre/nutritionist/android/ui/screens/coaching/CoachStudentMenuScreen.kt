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
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.presentation.coaching.CoachStudentMenuEvent
import dev.andre.nutritionist.presentation.coaching.CoachStudentMenuUiState

@Composable
fun CoachStudentMenuScreen(
    state: CoachStudentMenuUiState,
    onEvent: (CoachStudentMenuEvent.Ui) -> Unit,
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
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBackClick) {
                        Text(stringResource(R.string.common_back))
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.coach_menu_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = state.studentIdLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OutlinedButton(onClick = { onEvent(CoachStudentMenuEvent.Ui.OpenMealPlansClicked) }) {
                    Text(
                        text = stringResource(R.string.coaching_mode_plans_title),
                        maxLines = 2,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { onEvent(CoachStudentMenuEvent.Ui.PreviousDayClicked) }) {
                    Text("<")
                }
                Text(
                    text = state.dateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedButton(onClick = { onEvent(CoachStudentMenuEvent.Ui.NextDayClicked) }) {
                    Text(">")
                }
            }

            when (val content = state.content) {
                CoachStudentMenuUiState.Content.Loading -> {
                    LoadingContent(modifier = Modifier.weight(1f))
                }

                is CoachStudentMenuUiState.Content.Error -> {
                    ErrorContent(
                        message = content.message,
                        onRetry = { onEvent(CoachStudentMenuEvent.Ui.Retry) },
                        modifier = Modifier.weight(1f),
                    )
                }

                is CoachStudentMenuUiState.Content.Success -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.coach_menu_plan_section),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = content.selectedPlanTitle ?: "Меню не выбрано",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    content.selectedPlanSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                                        Text(
                                            text = summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
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
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.coach_menu_fact_section),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = content.factSummaryLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    content.diaryError?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                                        Text(
                                            text = errorMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }

                        items(content.diaryEntries, key = { it.id }) { entry ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(Spacing.small)) {
                                    Text(
                                        text = "${entry.mealTypeLabel}: ${entry.title}",
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = entry.nutritionLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
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
                                        text = stringResource(R.string.coach_menu_assign_section),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    OutlinedTextField(
                                        value = state.planIdInput,
                                        onValueChange = { onEvent(CoachStudentMenuEvent.Ui.PlanIdChanged(it)) },
                                        label = { Text(stringResource(R.string.student_coaching_plan_id_placeholder)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    OutlinedTextField(
                                        value = state.expiresDaysInput,
                                        onValueChange = { onEvent(CoachStudentMenuEvent.Ui.ExpiresDaysChanged(it)) },
                                        label = { Text(stringResource(R.string.coach_menu_expires_placeholder)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            GrantSource.COACH_GRANT,
                                            GrantSource.SUBSCRIPTION,
                                            GrantSource.PURCHASE,
                                        ).forEach { source ->
                                            val selected = state.selectedGrantSource == source
                                            if (selected) {
                                                Button(
                                                    onClick = {
                                                        onEvent(CoachStudentMenuEvent.Ui.GrantSourceSelected(source))
                                                    },
                                                ) {
                                                    Text(source.name, style = MaterialTheme.typography.labelSmall)
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = {
                                                        onEvent(CoachStudentMenuEvent.Ui.GrantSourceSelected(source))
                                                    },
                                                ) {
                                                    Text(source.name, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = { onEvent(CoachStudentMenuEvent.Ui.GrantAccessClicked) },
                                        enabled = !state.isSubmittingGrant,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.coach_menu_grant_btn))
                                    }
                                }
                            }
                        }

                        items(content.availablePlans, key = { it.id.raw }) { option ->
                            OutlinedButton(
                                onClick = { onEvent(CoachStudentMenuEvent.Ui.PlanSelected(option.id)) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("${option.title} (${option.subtitle})")
                            }
                        }

                        items(content.issuedGrants, key = { it.grantId.raw }) { grant ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(Spacing.small),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = grant.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            onEvent(CoachStudentMenuEvent.Ui.RevokeAccessClicked(grant.grantId))
                                        },
                                    ) {
                                        Text(stringResource(R.string.coach_menu_revoke_btn))
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
