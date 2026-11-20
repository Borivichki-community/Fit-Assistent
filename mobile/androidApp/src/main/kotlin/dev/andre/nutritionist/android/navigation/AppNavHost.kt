package dev.andre.nutritionist.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.andre.nutritionist.android.auth.AuthEvent
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.auth.AuthUiState
import dev.andre.nutritionist.android.auth.AuthViewModel
import dev.andre.nutritionist.android.ui.components.FullScreenLoading
import dev.andre.nutritionist.android.ui.screens.DashboardRoute
import dev.andre.nutritionist.android.ui.screens.MainScreen
import dev.andre.nutritionist.android.ui.screens.ProfileRoute
import dev.andre.nutritionist.android.ui.screens.auth.LoginRoute
import dev.andre.nutritionist.android.ui.screens.auth.SignUpRoute
import dev.andre.nutritionist.android.ui.screens.onboarding.RegistrationWizardRoute
import dev.andre.nutritionist.domain.model.UserId
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
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val isLoggingOut = authState.content is AuthUiState.Content.Loading
    val destinationRoute = when (startDestination) {
        is AppDestination -> startDestination.route
        is String -> startDestination
        else -> startDestination.toString()
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
                    navController.navigate(AppDestination.Registration.route) {
                        popUpTo(AppDestination.SignUp.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = authViewModel,
            )
        }
        composable(AppDestination.Dashboard.route) {
            DashboardRoute(
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
            )
        }
        composable(AppDestination.Recipes.route) {
            MainScreen(
                onNavigateToProfile = {
                    navController.navigate(AppDestination.Profile.route) {
                        launchSingleTop = true
                    }
                },
                onDestinationSelected = { dest ->
                    navController.navigate(dest.route) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(AppDestination.Statistics.route) {
            MainScreen(
                onNavigateToProfile = {
                    navController.navigate(AppDestination.Profile.route) {
                        launchSingleTop = true
                    }
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
