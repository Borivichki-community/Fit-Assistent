package dev.andre.nutritionist.android.servicecheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing

@Composable
internal fun ServiceCheckMenuScreen(onSelect: (ServiceCheckTarget) -> Unit) {
    Scaffold { insets ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(stringResource(R.string.service_check_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.service_check_menu_description))
            ServiceCheckTarget.entries.forEach { target ->
                Button(onClick = { onSelect(target) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(target.label))
                }
            }
        }
    }
}
