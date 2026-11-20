package dev.andre.nutritionist.android.presentation.profile.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileNews
import dev.andre.nutritionist.presentation.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Stateful Route экрана профиля.
 * Подключается к KoTEA [ProfileViewModel], подписывается на StateFlow и слушает одноразовые News.
 */
@Composable
fun ProfileRoute(
    userId: UserId,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: (UserId) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    isLoggingOut: Boolean = false,
    viewModel: ProfileViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
) {
    val state by viewModel.profileState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val displayName = remember(authRepository) { authRepository.getUserDisplayName() }

    LaunchedEffect(userId, displayName) {
        viewModel.dispatch(ProfileEvent.Ui.LoadProfile(userId = userId, displayName = displayName))
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

    ProfileScreen(
        state = state,
        onEvent = viewModel::dispatch,
        modifier = modifier,
        onLogoutClick = onLogoutClick,
        isLoggingOut = isLoggingOut,
    )
}
