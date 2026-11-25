package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.android.ui.components.ErrorContent
import dev.andre.nutritionist.android.ui.components.FitBottomNavigationBar
import dev.andre.nutritionist.android.ui.components.LoadingContent
import dev.andre.nutritionist.android.ui.components.OfflineBanner
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileUiState

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent.Ui) -> Unit,
    modifier: Modifier = Modifier,
    onLogoutClick: () -> Unit = {},
    isLoggingOut: Boolean = false,
    onCreateProfileClick: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    isOffline: Boolean = false,
    onNotificationsClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
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
            ProfileHeader(
                onNotificationsClick = onNotificationsClick,
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
                        val isNotFound = content.message.contains("NOT_FOUND", ignoreCase = true) ||
                            content.message.contains("not found", ignoreCase = true)
                        val displayMessage = if (isNotFound) {
                            stringResource(R.string.profile_not_found_message)
                        } else {
                            content.message
                        }
                        ErrorContent(
                            message = displayMessage,
                            onRetry = { onEvent(ProfileEvent.Ui.Retry) },
                            actionButtonText = stringResource(R.string.profile_fill_survey_again),
                            onActionClick = onCreateProfileClick,
                        )
                    }
                    is ProfileUiState.Content.Success -> {
                        ProfileSuccessContent(
                            profile = content.profile,
                            onEditClick = onCreateProfileClick,
                            onLogoutClick = onLogoutClick,
                            isLoggingOut = isLoggingOut,
                            onFillSurveyClick = onCreateProfileClick,
                            onEvent = onEvent,
                            onNotificationsClick = onNotificationsClick,
                            onHelpClick = onHelpClick,
                        )
                    }
                    is ProfileUiState.Content.Form -> {
                        LoadingContent()
                    }
                    is ProfileUiState.Content.ValidationError -> {
                        LoadingContent()
                    }
                }
            }
        }
    }
}
