package dev.andre.nutritionist.android.ui.screens.coaching

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.coaching.CoachStudentsEvent
import dev.andre.nutritionist.presentation.coaching.CoachStudentsNews
import dev.andre.nutritionist.presentation.coaching.CoachStudentsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CoachStudentsRoute(
    coachUserId: UserId,
    onBackClick: () -> Unit,
    onNavigateToStudentMenu: (UserId) -> Unit,
    onNavigateToMealPlans: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CoachStudentsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(coachUserId) {
        viewModel.dispatch(CoachStudentsEvent.Ui.Load(coachUserId))
    }

    LaunchedEffect(viewModel) {
        viewModel.news.collect { news ->
            when (news) {
                is CoachStudentsNews.NavigateToStudentMenu -> onNavigateToStudentMenu(news.studentId)
                CoachStudentsNews.NavigateToMealPlans -> onNavigateToMealPlans()
                is CoachStudentsNews.ShowSnackbar -> snackbarHostState.showSnackbar(news.message)
            }
        }
    }

    CoachStudentsScreen(
        state = state,
        onEvent = viewModel::dispatch,
        onBackClick = onBackClick,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
