package dev.andre.nutritionist.android.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.domain.model.GoalType

/**
 * Диалог точечного выбора цели питания пользователя.
 */
@Composable
fun EditGoalDialog(
    initialGoal: GoalType,
    onDismiss: () -> Unit,
    onConfirm: (GoalType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedGoal by remember {
        mutableStateOf(
            if (initialGoal == GoalType.UNKNOWN) GoalType.LOSE_WEIGHT else initialGoal
        )
    }

    val options = listOf(
        GoalOption(
            type = GoalType.LOSE_WEIGHT,
            title = stringResource(R.string.onboarding_goal_lose),
            description = stringResource(R.string.goal_lose_desc),
        ),
        GoalOption(
            type = GoalType.MAINTAIN,
            title = stringResource(R.string.onboarding_goal_maintain),
            description = stringResource(R.string.goal_maintain_desc),
        ),
        GoalOption(
            type = GoalType.GAIN_WEIGHT,
            title = stringResource(R.string.onboarding_goal_gain),
            description = stringResource(R.string.goal_gain_desc),
        ),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_edit_goal_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEach { option ->
                    val isSelected = option.type == selectedGoal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { selectedGoal = option.type },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedGoal) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        modifier = modifier,
    )
}

private data class GoalOption(
    val type: GoalType,
    val title: String,
    val description: String,
)
