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
import dev.andre.nutritionist.domain.model.ActivityLevel

/**
 * Диалог точечного выбора уровня активности пользователя.
 */
@Composable
fun EditActivityDialog(
    initialActivity: ActivityLevel,
    onDismiss: () -> Unit,
    onConfirm: (ActivityLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedActivity by remember {
        mutableStateOf(
            if (initialActivity == ActivityLevel.UNKNOWN) ActivityLevel.MODERATE else initialActivity
        )
    }

    val options = listOf(
        ActivityOption(
            level = ActivityLevel.SEDENTARY,
            title = stringResource(R.string.onboarding_activity_sedentary),
            description = stringResource(R.string.activity_sedentary_desc),
        ),
        ActivityOption(
            level = ActivityLevel.LIGHT,
            title = stringResource(R.string.onboarding_activity_light),
            description = stringResource(R.string.activity_light_desc),
        ),
        ActivityOption(
            level = ActivityLevel.MODERATE,
            title = stringResource(R.string.onboarding_activity_moderate),
            description = stringResource(R.string.activity_moderate_desc),
        ),
        ActivityOption(
            level = ActivityLevel.VERY_ACTIVE,
            title = stringResource(R.string.onboarding_activity_very_active),
            description = stringResource(R.string.activity_very_active_desc),
        ),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_edit_activity_title),
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
                    val isSelected = option.level == selectedActivity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { selectedActivity = option.level },
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
            TextButton(onClick = { onConfirm(selectedActivity) }) {
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

private data class ActivityOption(
    val level: ActivityLevel,
    val title: String,
    val description: String,
)
