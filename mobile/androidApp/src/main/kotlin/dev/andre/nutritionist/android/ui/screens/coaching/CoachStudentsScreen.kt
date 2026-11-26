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
import dev.andre.nutritionist.presentation.coaching.CoachStudentsEvent
import dev.andre.nutritionist.presentation.coaching.CoachStudentsUiState

@Composable
fun CoachStudentsScreen(
    state: CoachStudentsUiState,
    onEvent: (CoachStudentsEvent.Ui) -> Unit,
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
                    Text(
                        text = stringResource(R.string.coach_students_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedButton(onClick = { onEvent(CoachStudentsEvent.Ui.OpenMealPlansClicked) }) {
                    Text(
                        text = stringResource(R.string.coaching_mode_plans_title),
                        maxLines = 2,
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    OutlinedTextField(
                        value = state.studentIdInput,
                        onValueChange = { onEvent(CoachStudentsEvent.Ui.StudentIdChanged(it)) },
                        label = { Text(stringResource(R.string.coach_students_invite_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.canViewDiary,
                            onCheckedChange = { onEvent(CoachStudentsEvent.Ui.CanViewDiaryChanged(it)) },
                        )
                        Text(
                            text = stringResource(R.string.coach_students_allow_diary),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Button(
                        onClick = { onEvent(CoachStudentsEvent.Ui.InviteClicked) },
                        enabled = !state.isInviting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.coach_students_invite_btn))
                    }
                }
            }

            when (val content = state.content) {
                CoachStudentsUiState.Content.Loading -> {
                    LoadingContent(modifier = Modifier.weight(1f))
                }

                is CoachStudentsUiState.Content.Error -> {
                    ErrorContent(
                        message = content.message,
                        onRetry = { onEvent(CoachStudentsEvent.Ui.Retry) },
                        modifier = Modifier.weight(1f),
                    )
                }

                is CoachStudentsUiState.Content.Success -> {
                    if (content.links.isEmpty()) {
                        Text(
                            text = stringResource(R.string.coach_students_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            items(content.links, key = { it.linkId }) { item ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = item.studentLabel,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "${item.statusLabel} • ${item.diaryAccessLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                        ) {
                                            if (item.canOpenDiary) {
                                                OutlinedButton(
                                                    onClick = {
                                                        onEvent(CoachStudentsEvent.Ui.OpenDiaryClicked(item.studentId))
                                                    },
                                                ) {
                                                    Text(stringResource(R.string.coach_students_open_diary))
                                                }
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Button(
                                                onClick = {
                                                    onEvent(CoachStudentsEvent.Ui.AssignMenuClicked(item.studentId))
                                                },
                                            ) {
                                                Text(stringResource(R.string.coach_students_assign_menu))
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
}
