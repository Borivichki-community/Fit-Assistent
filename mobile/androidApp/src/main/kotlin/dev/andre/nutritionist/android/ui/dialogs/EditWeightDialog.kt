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
 * Диалог точечного редактирования веса пользователя.
 * Валидирует ввод через доменный ProfileValidator (20.0 .. 500.0 кг).
 */
@Composable
fun EditWeightDialog(
    initialWeight: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var weightText by remember {
        mutableStateOf(if (initialWeight > 0.0) initialWeight.toString() else "")
    }
    var isError by remember { mutableStateOf(false) }

    fun validateAndConfirm() {
        val error = ProfileValidator.validateWeight(weightText)
        if (error == null) {
            val parsed = weightText.trim().replace(',', '.').toDoubleOrNull()
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
                text = stringResource(R.string.dialog_edit_weight_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        if (isError) isError = false
                    },
                    label = { Text(stringResource(R.string.profile_label_weight)) },
                    placeholder = { Text(stringResource(R.string.onboarding_hint_weight)) },
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        if (isError) {
                            Text(
                                text = stringResource(R.string.validation_invalid_weight),
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
