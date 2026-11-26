package dev.andre.nutritionist.android.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import dev.andre.nutritionist.domain.model.UserRole
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.auth.AuthNews
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.auth.AuthViewModel
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileNews
import dev.andre.nutritionist.presentation.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
    onCreateProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onLogoutSuccess: () -> Unit = {},
    isLoggingOut: Boolean = false,
    onDestinationSelected: (AppDestination) -> Unit = {},
    onOpenCoachModeClick: () -> Unit = {},
    onOpenStudentModeClick: () -> Unit = {},
    onOpenMealPlansClick: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val displayName = remember(authRepository) { authRepository.getUserDisplayName() }
    val email = remember(authRepository) { authRepository.getUserEmail() }
    var userRole by remember(authRepository) { mutableStateOf(authRepository.getUserRole()) }

    LaunchedEffect(userId, displayName, email, userRole) {
        viewModel.dispatch(
            ProfileEvent.Ui.LoadProfile(
                userId = userId,
                displayName = displayName,
                email = email,
                userRole = userRole,
            ),
        )
    }

    LaunchedEffect(Unit) {
        viewModel.news.collect { news ->
            when (news) {
                is ProfileNews.NavigateToEdit -> onNavigateToEdit(news.userId)
                is ProfileNews.ShowSnackbar -> snackbarHostState.showSnackbar(news.message)
                ProfileNews.ProfileSaved -> Unit
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
        onCreateProfileClick = onCreateProfileClick,
        onDestinationSelected = onDestinationSelected,
        snackbarHostState = snackbarHostState,
        onOpenCoachModeClick = onOpenCoachModeClick,
        onOpenStudentModeClick = onOpenStudentModeClick,
        onOpenMealPlansClick = onOpenMealPlansClick,
        onToggleDebugRoleClick = {
            val nextRole = when (userRole ?: UserRole.CLIENT) {
                UserRole.COACH -> UserRole.CLIENT
                UserRole.CLIENT -> UserRole.COACH
            }
            scope.launch {
                authRepository.setUserRole(nextRole)
                userRole = nextRole
            }
        },
    )
}
