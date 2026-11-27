package dev.andre.nutritionist.android.ui.screens.coaching

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.coaching.CoachStudentMenuEvent
import dev.andre.nutritionist.presentation.coaching.CoachStudentMenuNews
import dev.andre.nutritionist.presentation.coaching.CoachStudentMenuViewModel
import kotlin.time.Clock
import org.koin.androidx.compose.koinViewModel

@Composable
fun CoachStudentMenuRoute(
    studentId: UserId,
    onBackClick: () -> Unit,
    onNavigateToMealPlans: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CoachStudentMenuViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val initialDateEpochMs = rememberSaveable { Clock.System.now().toEpochMilliseconds() }

    LaunchedEffect(studentId) {
        viewModel.dispatch(
            CoachStudentMenuEvent.Ui.Load(
                studentId = studentId,
                dateEpochMs = initialDateEpochMs,
            ),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.news.collect { news ->
            when (news) {
                CoachStudentMenuNews.NavigateToMealPlans -> onNavigateToMealPlans()
                is CoachStudentMenuNews.ShowSnackbar -> snackbarHostState.showSnackbar(news.message)
            }
        }
    }

    CoachStudentMenuScreen(
        state = state,
        onEvent = viewModel::dispatch,
        onBackClick = onBackClick,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
