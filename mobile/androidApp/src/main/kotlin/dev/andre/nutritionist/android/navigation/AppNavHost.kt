package dev.andre.nutritionist.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import dev.andre.nutritionist.presentation.dashboard.DiaryViewModel
import dev.andre.nutritionist.android.ui.screens.airecipe.AiRecipeRoute
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.andre.nutritionist.android.auth.AuthEvent
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.auth.AuthUiState
import dev.andre.nutritionist.android.auth.AuthViewModel
import dev.andre.nutritionist.android.ui.components.FullScreenLoading
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument
import dev.andre.nutritionist.android.ui.screens.DashboardRoute
import dev.andre.nutritionist.android.ui.screens.MainScreen
import dev.andre.nutritionist.android.ui.screens.ProfileRoute
import dev.andre.nutritionist.android.ui.screens.auth.LoginRoute
import dev.andre.nutritionist.android.ui.screens.auth.SignUpRoute
import dev.andre.nutritionist.android.ui.screens.coaching.CoachStudentMenuRoute
import dev.andre.nutritionist.android.ui.screens.coaching.CoachStudentsRoute
import dev.andre.nutritionist.android.ui.screens.coaching.MealPlansAndPublishRoute
import dev.andre.nutritionist.android.ui.screens.coaching.StudentCoachingRoute
import dev.andre.nutritionist.android.ui.screens.onboarding.RegistrationWizardRoute
import dev.andre.nutritionist.android.ui.screens.role.RoleSelectionRoute
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.model.UserRole
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileNews
import dev.andre.nutritionist.presentation.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Any = AppDestination.Auth.route,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val isAiRecipeVisible = currentEntry?.destination?.route == AppDestination.AiRecipe.pattern
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val isLoggingOut = authState.content is AuthUiState.Content.Loading
    val destinationRoute = when (startDestination) {
        is AppDestination -> startDestination.route
        is String -> startDestination
        else -> startDestination.toString()
    }
    val observedAuthState by authRepository.observeAuthState().collectAsStateWithLifecycle(
        initialValue = authRepository.isAuthenticated(),
    )
    var wasAuthenticated by remember { mutableStateOf(observedAuthState) }
    LaunchedEffect(observedAuthState) {
        if (wasAuthenticated && !observedAuthState) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != AppDestination.Auth.route && currentRoute != AppDestination.SignUp.route) {
                navController.navigate(AppDestination.Auth.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
        wasAuthenticated = observedAuthState
    }

    NavHost(
        navController = navController,
        startDestination = destinationRoute,
        modifier = modifier,
    ) {
        composable(AppDestination.Auth.route) {
            LoginRoute(
                onLoginSuccess = {
                    navController.navigate(AppDestination.Dashboard.route) {
                        popUpTo(AppDestination.Auth.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(AppDestination.SignUp.route)
                },
                viewModel = authViewModel,
            )
        }
        composable(AppDestination.SignUp.route) {
            SignUpRoute(
                onRegistrationSuccess = { _ ->
                    navController.navigate(AppDestination.RoleSelection.route) {
                        popUpTo(AppDestination.SignUp.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = authViewModel,
            )
        }
        composable(AppDestination.RoleSelection.route) {
            RoleSelectionRoute(
                onNavigateToOnboarding = {
                    navController.navigate(AppDestination.Registration.route) {
                        popUpTo(AppDestination.RoleSelection.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(AppDestination.Dashboard.route) {
                        popUpTo(AppDestination.RoleSelection.route) { inclusive = true }
                    }
                },
                authRepository = authRepository,
            )
        }
        composable(AppDestination.Dashboard.route) {
            DashboardRoute(
                isAiRecipeVisible = isAiRecipeVisible,
                onNavigateToAiGenerator = { slot, date, calories -> navController.navigate(AppDestination.AiRecipe(slot, date, calories).route) },
                onNavigateToProfile = {
                    navController.navigate(AppDestination.Profile.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToRegistration = {
                    navController.navigate(AppDestination.Registration.route)
                },
                onDestinationSelected = { dest ->
                    navController.navigate(dest.route) {
                        launchSingleTop = true
                    }
                },
            )
        }
        dialog(
            route = AppDestination.AiRecipe.pattern,
            dialogProperties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) { entry ->
            val args = requireNotNull(entry.arguments)
            val diaryEntry = remember(entry) { requireNotNull(navController.previousBackStackEntry) }
            val diary: DiaryViewModel = koinViewModel(viewModelStoreOwner = diaryEntry)
            val diaryState by diary.state.collectAsStateWithLifecycle()
            AiRecipeRoute(
                dev.andre.nutritionist.domain.model.MealType.valueOf(requireNotNull(args.getString("slot"))),
                requireNotNull(args.getString("date")).toLong(),
                requireNotNull(args.getString("calories")).toInt(),
                onBackClick = { navController.popBackStack() },
                onRecipeAdded = { navController.popBackStack() },
                remainingNutrition = diaryState.dailySummary.remainingNutrition,
            )
        }
        composable(AppDestination.Registration.route) {
            val profileViewModel: ProfileViewModel = koinViewModel()
            var isSavingProfile by remember { mutableStateOf(false) }
            val currentUserId = authRepository.getUserId() ?: UserId("testuser")
            val userDisplayName = remember(authRepository) { authRepository.getUserDisplayName() }

            LaunchedEffect(profileViewModel) {
                profileViewModel.news.collect { news ->
                    when (news) {
                        ProfileNews.ProfileSaved -> {
                            isSavingProfile = false
                            navController.navigate(AppDestination.Dashboard.route) {
                                popUpTo(AppDestination.Registration.route) { inclusive = true }
                            }
                        }
                        is ProfileNews.ShowSnackbar -> {
                            isSavingProfile = false
                        }
                        is ProfileNews.NavigateToEdit -> Unit
                    }
                }
            }

            if (isSavingProfile) {
                FullScreenLoading()
            } else {
                RegistrationWizardRoute(
                    initialData = dev.andre.nutritionist.domain.model.OnboardingData(
                        firstName = userDisplayName ?: "",
                    ),
                    onFinish = { onboardingData ->
                        isSavingProfile = true
                        profileViewModel.dispatch(
                            ProfileEvent.Ui.SaveOnboardingProfile(
                                data = onboardingData,
                                userId = currentUserId,
                            ),
                        )
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }
        }
        composable(AppDestination.Profile.route) {
            val currentUserId = authRepository.getUserId() ?: UserId("testuser")
            ProfileRoute(
                userId = currentUserId,
                isLoggingOut = isLoggingOut,
                authViewModel = authViewModel,
                onCreateProfileClick = {
                    navController.navigate(AppDestination.Registration.route)
                },
                onLogoutClick = {
                    authViewModel.dispatch(AuthEvent.Ui.LogoutClicked)
                },
                onLogoutSuccess = {
                    navController.navigate(AppDestination.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDestinationSelected = { dest ->
                    navController.navigate(dest.route) {
                        launchSingleTop = true
                    }
                },
                onOpenCoachModeClick = {
                    navController.navigate(AppDestination.CoachStudents.route) {
                        launchSingleTop = true
                    }
                },
                onOpenStudentModeClick = {
                    navController.navigate(AppDestination.StudentCoaching.route) {
                        launchSingleTop = true
                    }
                },
                onOpenMealPlansClick = {
                    navController.navigate(AppDestination.MealPlans().route) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(AppDestination.CoachStudents.route) {
            val currentUserId = authRepository.getUserId() ?: UserId("testuser")
            CoachStudentsRoute(
                coachUserId = currentUserId,
                onBackClick = { navController.popBackStack() },
                onNavigateToStudentMenu = { studentId ->
                    navController.navigate(AppDestination.CoachStudentMenu(studentId).route)
                },
                onNavigateToMealPlans = {
                    navController.navigate(AppDestination.MealPlans().route)
                },
            )
        }
        composable(AppDestination.StudentCoaching.route) {
            val currentUserId = authRepository.getUserId() ?: UserId("testuser")
            StudentCoachingRoute(
                studentUserId = currentUserId,
                onBackClick = { navController.popBackStack() },
                onNavigateToMealPlan = { planId ->
                    navController.navigate(AppDestination.MealPlans(planId).route)
                },
            )
        }
        composable(
            route = AppDestination.CoachStudentMenu.pattern,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType }),
        ) { entry ->
            val rawStudentId = requireNotNull(entry.arguments?.getString("studentId")) {
                "studentId argument is required"
            }
            CoachStudentMenuRoute(
                studentId = UserId(Uri.decode(rawStudentId)),
                onBackClick = { navController.popBackStack() },
                onNavigateToMealPlans = {
                    navController.navigate(AppDestination.MealPlans().route)
                },
            )
        }
        composable(
            route = AppDestination.MealPlans.pattern,
            arguments = listOf(
                navArgument("planId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val currentUserId = authRepository.getUserId() ?: UserId("testuser")
            val isCoach = authRepository.getUserRole() == UserRole.COACH
            val rawPlanId = entry.arguments?.getString("planId")
                ?.takeIf { it.isNotBlank() }
                ?.let(Uri::decode)
            MealPlansAndPublishRoute(
                userId = currentUserId,
                isCoach = isCoach,
                initialPlanId = rawPlanId?.let(::MealPlanTemplateId),
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(AppDestination.Recipes.route) {
            DashboardRoute(
                isAiRecipeVisible = isAiRecipeVisible,
                onNavigateToAiGenerator = { slot, date, calories -> navController.navigate(AppDestination.AiRecipe(slot, date, calories).route) },
                onNavigateToProfile = {
                    navController.navigate(AppDestination.Profile.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToRegistration = {
                    navController.navigate(AppDestination.Registration.route)
                },
                onDestinationSelected = { dest ->
                    navController.navigate(dest.route) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(AppDestination.Statistics.route) {
            DashboardRoute(
                isAiRecipeVisible = isAiRecipeVisible,
                onNavigateToAiGenerator = { slot, date, calories -> navController.navigate(AppDestination.AiRecipe(slot, date, calories).route) },
                onNavigateToProfile = {
                    navController.navigate(AppDestination.Profile.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToRegistration = {
                    navController.navigate(AppDestination.Registration.route)
                },
                onDestinationSelected = { dest ->
                    navController.navigate(dest.route) {
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
