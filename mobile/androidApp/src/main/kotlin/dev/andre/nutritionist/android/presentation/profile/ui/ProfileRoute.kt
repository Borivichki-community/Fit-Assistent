package dev.andre.nutritionist.android.presentation.profile.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileNews
import dev.andre.nutritionist.presentation.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful Route экрана профиля.
 * Подключается к KoTEA [ProfileViewModel], подписывается на StateFlow и слушает одноразовые News.
 */
@Composable
fun ProfileRoute(
    userId: UserId,
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: (UserId) -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.profileState.collectAsStateWithLifecycle()
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

    ProfileScreen(
        state = state,
        onEvent = viewModel::dispatch,
        modifier = modifier,
    )
}
