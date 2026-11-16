package dev.andre.nutritionist.android.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.auth.AuthNews
import dev.andre.nutritionist.android.auth.AuthViewModel
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileNews
import dev.andre.nutritionist.presentation.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful Route экрана профиля.
 * Подключается к ViewModel, слушает News и управляет переходами.
 */
@Composable
fun ProfileRoute(
    userId: UserId,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: (UserId) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onLogoutSuccess: () -> Unit = {},
    isLoggingOut: Boolean = false,
    onDestinationSelected: (AppDestination) -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) {
        viewModel.dispatch(ProfileEvent.Ui.LoadProfile(userId))
    }

    LaunchedEffect(Unit) {
        viewModel.news.collect { news ->
            when (news) {
                is ProfileNews.NavigateToEdit -> onNavigateToEdit(news.userId)
                is ProfileNews.ShowSnackbar -> snackbarHostState.showSnackbar(news.message)
            }
        }
    }

    LaunchedEffect(authViewModel) {
        authViewModel.news.collect { news ->
            when (news) {
                is AuthNews.NavigateToAuth -> onLogoutSuccess()
                is AuthNews.LaunchAuthTab -> Unit
                is AuthNews.NavigateToMain -> Unit
                is AuthNews.ShowSnackbar -> snackbarHostState.showSnackbar(news.message)
            }
        }
    }

    ProfileScreen(
        state = state,
        onEvent = viewModel::dispatch,
        modifier = modifier,
        onLogoutClick = onLogoutClick,
        isLoggingOut = isLoggingOut,
        onDestinationSelected = onDestinationSelected,
        snackbarHostState = snackbarHostState,
    )
}
