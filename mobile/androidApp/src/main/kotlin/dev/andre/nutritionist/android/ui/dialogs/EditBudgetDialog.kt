package dev.andre.nutritionist.android.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.domain.validation.ProfileValidator

/**
 * Диалог точечного редактирования недельного бюджета пользователя.
 * Валидирует ввод через доменный ProfileValidator (1.0 .. 1_000_000.0).
 */
@Composable
fun EditBudgetDialog(
    initialBudget: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var budgetText by remember {
        mutableStateOf(if (initialBudget > 0.0) initialBudget.toInt().toString() else "")
    }
    var isError by remember { mutableStateOf(false) }

    fun validateAndConfirm() {
        val error = ProfileValidator.validateWeeklyBudget(budgetText)
        if (error == null) {
            val parsed = budgetText.trim().replace(',', '.').toDoubleOrNull()
            if (parsed != null) {
                onConfirm(parsed)
                return
            }
        }
        isError = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_edit_budget_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        budgetText = it
                        if (isError) isError = false
                    },
                    label = { Text(stringResource(R.string.profile_label_budget)) },
                    placeholder = { Text(stringResource(R.string.onboarding_budget_hint)) },
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        if (isError) {
                            Text(
                                text = stringResource(R.string.validation_invalid_budget),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = ::validateAndConfirm) {
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
