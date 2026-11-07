package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.android.ui.components.ErrorContent
import dev.andre.nutritionist.android.ui.components.FitBottomNavigationBar
import dev.andre.nutritionist.android.ui.components.FitTopBar
import dev.andre.nutritionist.android.ui.components.LoadingContent
import dev.andre.nutritionist.android.ui.components.OfflineBanner
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileUiState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent.Ui) -> Unit,
    onDestinationSelected: (AppDestination) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    isOffline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            FitBottomNavigationBar(
                currentDestination = AppDestination.Profile,
                onDestinationSelected = onDestinationSelected,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Переиспользуемая верхняя плашка FitApp
            FitTopBar(
                title = "FitApp",
                onMenuClick = { /* Открытие бокового меню */ },
            )

            if (isOffline) {
                OfflineBanner()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                when (val content = state.content) {
                    ProfileUiState.Content.Loading -> {
                        LoadingContent()
                    }
                    is ProfileUiState.Content.Error -> {
                        ErrorContent(
                            message = content.message,
                            onRetry = { onEvent(ProfileEvent.Ui.Retry) },
                        )
                    }
                    is ProfileUiState.Content.Success -> {
                        ProfileSuccessContent(
                            profile = content.profile,
                            onEditClick = { onEvent(ProfileEvent.Ui.EditClicked) },
                        )
                    }
                }
            }
        }
    }
}
