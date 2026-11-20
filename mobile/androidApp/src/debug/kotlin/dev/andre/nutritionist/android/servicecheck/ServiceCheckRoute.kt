package dev.andre.nutritionist.android.servicecheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun ServiceCheckRoute(
    viewModel: ServiceCheckViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ServiceCheckScreen(state = state, onEvent = viewModel::dispatch, onBack = onBack)
}
