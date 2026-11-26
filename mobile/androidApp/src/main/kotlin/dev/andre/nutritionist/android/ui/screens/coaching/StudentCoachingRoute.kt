package dev.andre.nutritionist.android.ui.screens.coaching

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.coaching.StudentCoachingEvent
import dev.andre.nutritionist.presentation.coaching.StudentCoachingNews
import dev.andre.nutritionist.presentation.coaching.StudentCoachingViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun StudentCoachingRoute(
    studentUserId: UserId,
    onBackClick: () -> Unit,
    onNavigateToMealPlan: (MealPlanTemplateId?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentCoachingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(studentUserId) {
        viewModel.dispatch(StudentCoachingEvent.Ui.Load(studentUserId))
    }

    LaunchedEffect(viewModel) {
        viewModel.news.collect { news ->
            when (news) {
                is StudentCoachingNews.NavigateToMealPlan -> onNavigateToMealPlan(news.planId)
                is StudentCoachingNews.ShowSnackbar -> snackbarHostState.showSnackbar(news.message)
            }
        }
    }

    StudentCoachingScreen(
        state = state,
        onEvent = viewModel::dispatch,
        onBackClick = onBackClick,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
