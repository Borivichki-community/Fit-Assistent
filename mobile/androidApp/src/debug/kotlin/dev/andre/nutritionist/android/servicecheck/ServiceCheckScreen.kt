package dev.andre.nutritionist.android.servicecheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing

@Composable
internal fun ServiceCheckScreen(
    state: ServiceCheckState,
    onEvent: (ServiceCheckEvent.Ui) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold { insets ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(insets)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.service_check_back)) }
            Text(stringResource(state.target.label), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(state.target.description))
            if (state.target.defaultGrpcPort != null) {
                OutlinedTextField(
                    value = state.port,
                    onValueChange = { onEvent(ServiceCheckEvent.Ui.ChangePort(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canEditPort,
                    label = { Text(stringResource(R.string.service_check_port)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            if (state.target == ServiceCheckTarget.RECIPE) {
                OutlinedTextField(
                    value = state.gatewayUrl,
                    onValueChange = { onEvent(ServiceCheckEvent.Ui.ChangeGateway(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canEditPort,
                    label = { Text(stringResource(R.string.service_check_gateway)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
            state.target.actions.forEach { action ->
                Button(
                    onClick = { onEvent(ServiceCheckEvent.Ui.Run(action)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canRun(action),
                ) {
                    Text(stringResource(action.label))
                }
            }
            when (val content = state.content) {
                ServiceCheckState.Content.Idle -> Unit
                is ServiceCheckState.Content.Loading -> Text(
                    stringResource(R.string.service_check_running, stringResource(content.action.label)),
                )
                is ServiceCheckState.Content.Success -> {
                    Text(
                        stringResource(R.string.service_check_success, stringResource(content.action.label)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    SelectionContainer { Text(content.response) }
                }
                is ServiceCheckState.Content.Error -> {
                    Text(
                        stringResource(R.string.service_check_failure, stringResource(content.action.label)),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    SelectionContainer { Text(content.response) }
                }
            }
        }
    }
}
