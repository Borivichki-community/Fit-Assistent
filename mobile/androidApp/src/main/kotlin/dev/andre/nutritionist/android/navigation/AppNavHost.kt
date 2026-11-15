package dev.andre.nutritionist.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.andre.nutritionist.android.auth.AuthEvent
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.auth.AuthUiState
import dev.andre.nutritionist.android.auth.AuthViewModel
import dev.andre.nutritionist.android.ui.screens.MainScreen
import dev.andre.nutritionist.android.ui.screens.ProfileRoute
import dev.andre.nutritionist.android.ui.screens.auth.LoginRoute
import dev.andre.nutritionist.android.ui.screens.auth.SignUpRoute
import dev.andre.nutritionist.android.ui.screens.onboarding.RegistrationWizardRoute
import dev.andre.nutritionist.domain.model.UserId
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
        composable(AppDestination.Registration.route) {
            RegistrationWizardRoute(
                onFinish = {
                    navController.navigate(AppDestination.Dashboard.route) {
                        popUpTo(AppDestination.Registration.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(AppDestination.Profile.route) {
            val currentUserId = authRepository.getUserId() ?: UserId("testuser")
            ProfileRoute(
                userId = currentUserId,
                isLoggingOut = isLoggingOut,
                authViewModel = authViewModel,
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
