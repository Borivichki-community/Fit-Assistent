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
import androidx.compose.material3.Checkbox
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
import dev.andre.nutritionist.presentation.coaching.StudentCoachingEvent
import dev.andre.nutritionist.presentation.coaching.StudentCoachingUiState

@Composable
fun StudentCoachingScreen(
    state: StudentCoachingUiState,
    onEvent: (StudentCoachingEvent.Ui) -> Unit,
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
                        text = stringResource(R.string.student_coaching_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedButton(onClick = { onEvent(StudentCoachingEvent.Ui.BrowsePlansClicked) }) {
                    Text(stringResource(R.string.coaching_mode_plans_title))
                }
            }

            when (val content = state.content) {
                StudentCoachingUiState.Content.Loading -> {
                    LoadingContent(modifier = Modifier.weight(1f))
                }

                is StudentCoachingUiState.Content.Error -> {
                    ErrorContent(
                        message = content.message,
                        onRetry = { onEvent(StudentCoachingEvent.Ui.Retry) },
                        modifier = Modifier.weight(1f),
                    )
                }

                is StudentCoachingUiState.Content.Success -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.student_coaching_pending_header),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (content.pendingInvites.isEmpty()) {
                            item {
                                Text(
                                    text = "—",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            items(content.pendingInvites, key = { it.linkId.raw }) { invite ->
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
                                            text = invite.coachLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = invite.canViewDiary,
                                                onCheckedChange = { checked ->
                                                    onEvent(
                                                        StudentCoachingEvent.Ui.DiaryConsentChanged(
                                                            invite.linkId,
                                                            checked,
                                                        ),
                                                    )
                                                },
                                            )
                                            Text(
                                                text = stringResource(R.string.student_coaching_allow_diary_checkbox),
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    onEvent(StudentCoachingEvent.Ui.AcceptInviteClicked(invite.linkId))
                                                },
                                                enabled = !invite.isBusy,
                                            ) {
                                                Text(stringResource(R.string.student_coaching_accept))
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    onEvent(StudentCoachingEvent.Ui.DeclineInviteClicked(invite.linkId))
                                                },
                                                enabled = !invite.isBusy,
                                            ) {
                                                Text(stringResource(R.string.student_coaching_decline))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = stringResource(R.string.student_coaching_active_header),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = Spacing.small),
                            )
                        }
                        if (content.activeCoaches.isEmpty()) {
                            item {
                                Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(content.activeCoaches, key = { it.linkId.raw }) { coach ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(coach.coachLabel, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                coach.diaryAccessLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onEvent(StudentCoachingEvent.Ui.RevokeCoachClicked(coach.linkId))
                                            },
                                            enabled = !coach.isBusy,
                                        ) {
                                            Text(stringResource(R.string.student_coaching_revoke))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = stringResource(R.string.student_coaching_plans_header),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = Spacing.small),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = state.planIdInput,
                                    onValueChange = { onEvent(StudentCoachingEvent.Ui.PlanIdInputChanged(it)) },
                                    label = { Text(stringResource(R.string.student_coaching_plan_id_placeholder)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { onEvent(StudentCoachingEvent.Ui.OpenEnteredPlanClicked) }) {
                                    Text(stringResource(R.string.student_coaching_open_menu))
                                }
                            }
                        }

                        items(content.availablePlans, key = { it.planId.raw }) { plan ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(plan.title, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            plan.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { onEvent(StudentCoachingEvent.Ui.OpenPlanClicked(plan.planId)) },
                                    ) {
                                        Text(stringResource(R.string.student_coaching_open_menu))
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
