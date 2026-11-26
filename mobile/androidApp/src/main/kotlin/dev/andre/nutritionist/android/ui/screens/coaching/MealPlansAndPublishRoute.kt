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
import dev.andre.nutritionist.presentation.mealplans.MealPlansEvent
import dev.andre.nutritionist.presentation.mealplans.MealPlansNews
import dev.andre.nutritionist.presentation.mealplans.MealPlansViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MealPlansAndPublishRoute(
    userId: UserId,
    isCoach: Boolean,
    initialPlanId: MealPlanTemplateId?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealPlansViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId, isCoach, initialPlanId) {
        viewModel.dispatch(
            MealPlansEvent.Ui.Load(
                userId = userId,
                isCoach = isCoach,
                initialPlanId = initialPlanId,
            ),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.news.collect { news ->
            when (news) {
                is MealPlansNews.ShowSnackbar -> snackbarHostState.showSnackbar(news.message)
            }
        }
    }

    MealPlansAndPublishScreen(
        state = state,
        onEvent = viewModel::dispatch,
        onBackClick = onBackClick,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
