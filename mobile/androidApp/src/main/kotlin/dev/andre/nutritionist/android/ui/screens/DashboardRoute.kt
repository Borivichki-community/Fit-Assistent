package dev.andre.nutritionist.android.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileState
import dev.andre.nutritionist.presentation.profile.ProfileUiState
import dev.andre.nutritionist.presentation.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Stateful Route главного экрана (Dashboard).
 * Наблюдает за состоянием профиля пользователя через [ProfileViewModel],
 * преобразует цели КБЖУ в [DashboardUiState] и обрабатывает навигацию.
 */
@Composable
fun DashboardRoute(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToRegistration: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    onAddMealClick: (MealType) -> Unit = {},
    onMealItemClick: (String) -> Unit = {},
    onAddExerciseSleepClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
) {
    val currentUserId = authRepository.getUserId() ?: UserId("testuser")
    val displayName = remember(authRepository) { authRepository.getUserDisplayName() }

    LaunchedEffect(currentUserId, displayName) {
        profileViewModel.dispatch(ProfileEvent.Ui.LoadProfile(userId = currentUserId, displayName = displayName))
    }

    val profileState by profileViewModel.state.collectAsStateWithLifecycle()
    val rawProfileState by profileViewModel.profileState.collectAsStateWithLifecycle()

    val domainProfile = remember(rawProfileState.content) {
        when (val content = rawProfileState.content) {
            ProfileState.Content.Loading -> null
            is ProfileState.Content.Form -> null
            is ProfileState.Content.Success -> content.profile
            is ProfileState.Content.Error -> null
            is ProfileState.Content.ValidationError -> null
        }
    }

    val showCompleteProfileBanner = remember(profileState.content) {
        when (val content = profileState.content) {
            ProfileUiState.Content.Loading -> false
            is ProfileUiState.Content.Error -> true
            is ProfileUiState.Content.Success -> !content.profile.isComplete
            is ProfileUiState.Content.Form,
            is ProfileUiState.Content.ValidationError -> false
        }
    }

    var expandedMeals by remember { mutableStateOf(setOf(MealType.DINNER)) }
    val mapper = remember { DashboardUiStateMapper() }

    val dashboardState = remember(domainProfile, expandedMeals, showCompleteProfileBanner) {
        mapper.map(
            profile = domainProfile,
            expandedMeals = expandedMeals,
            showCompleteProfileBanner = showCompleteProfileBanner,
        )
    }

    MainScreen(
        state = dashboardState,
        onAddMealClick = onAddMealClick,
        onMealItemClick = onMealItemClick,
        onToggleMealSection = { mealType ->
            expandedMeals = if (expandedMeals.contains(mealType)) {
                expandedMeals - mealType
            } else {
                expandedMeals + mealType
            }
        },
        onCompleteProfileClick = onNavigateToRegistration,
        onNavigateToProfile = onNavigateToProfile,
        onDestinationSelected = onDestinationSelected,
        onAddExerciseSleepClick = onAddExerciseSleepClick,
        modifier = modifier,
    )
}
