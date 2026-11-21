package dev.andre.nutritionist.android.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import dev.andre.nutritionist.android.ui.screens.ProfileRoute
import dev.andre.nutritionist.android.ui.screens.airecipe.AiRecipeRoute
import dev.andre.nutritionist.android.ui.screens.auth.LoginRoute
import dev.andre.nutritionist.android.ui.screens.auth.SignUpRoute
import dev.andre.nutritionist.android.ui.screens.catalog.CatalogRoute
import dev.andre.nutritionist.android.ui.screens.onboarding.RegistrationWizardRoute
import dev.andre.nutritionist.android.ui.screens.planmanager.MealPlanManagerRoute
import dev.andre.nutritionist.android.ui.screens.savedrecipes.SavedRecipesRoute
import dev.andre.nutritionist.android.ui.screens.splash.SplashRoute
import dev.andre.nutritionist.android.ui.screens.statistics.StatisticsRoute
import dev.andre.nutritionist.android.ui.theme.OnSurfaceDark
import dev.andre.nutritionist.android.ui.theme.PrimaryContainerDark
import dev.andre.nutritionist.android.ui.theme.SurfaceContainerHigh
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileNews
import dev.andre.nutritionist.presentation.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Any = AppDestination.Splash.route,
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
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceContainerHigh,
                    contentColor = OnSurfaceDark,
                    actionColor = PrimaryContainerDark,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = destinationRoute,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(AppDestination.Splash.route) {
                SplashRoute(
                    onNavigateToDashboard = {
                        navController.navigate(AppDestination.Dashboard.route) {
                            popUpTo(AppDestination.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToAuth = {
                        navController.navigate(AppDestination.Auth.route) {
                            popUpTo(AppDestination.Splash.route) { inclusive = true }
                        }
                    },
                    authRepository = authRepository,
                )
            }
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
                    onNavigateToAuth = {
                        navController.navigate(AppDestination.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onDestinationSelected = { dest ->
                        navController.navigate(dest.route) {
                            launchSingleTop = true
                        }
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
            composable(AppDestination.Registration.route) {
                val currentUserId = remember(authRepository) { authRepository.getUserId() }
                if (currentUserId == null) {
                    LaunchedEffect(Unit) {
                        navController.navigate(AppDestination.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    return@composable
                }
                val profileViewModel: ProfileViewModel = koinViewModel()
                var isSavingProfile by remember { mutableStateOf(false) }
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
                                snackbarHostState.showSnackbar(news.message)
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
                val currentUserId = remember(authRepository) { authRepository.getUserId() }
                if (currentUserId == null) {
                    LaunchedEffect(Unit) {
                        navController.navigate(AppDestination.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    return@composable
                }
                ProfileRoute(
                    userId = currentUserId,
                    isLoggingOut = isLoggingOut,
                    authViewModel = authViewModel,
                    snackbarHostState = snackbarHostState,
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
                CatalogRoute(
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
                    onNavigateToAiGenerator = {
                        navController.navigate(AppDestination.AiRecipeGenerator.route)
                    },
                    onNavigateToPlanDetail = { _ ->
                        navController.navigate(AppDestination.MealPlanManager.route)
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
            composable(AppDestination.MealPlanManager.route) {
                MealPlanManagerRoute(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToCatalog = {
                        navController.navigate(AppDestination.Recipes.route)
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
            composable(AppDestination.AiRecipeGenerator.route) {
                AiRecipeRoute(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToDiary = {
                        navController.navigate(AppDestination.Dashboard.route) {
                            popUpTo(AppDestination.Dashboard.route) { inclusive = true }
                        }
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
            composable(AppDestination.SavedRecipes.route) {
                SavedRecipesRoute(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToAiGenerator = {
                        navController.navigate(AppDestination.AiRecipeGenerator.route)
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
            composable(AppDestination.Statistics.route) {
                StatisticsRoute(
                    onDestinationSelected = { dest ->
                        navController.navigate(dest.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
