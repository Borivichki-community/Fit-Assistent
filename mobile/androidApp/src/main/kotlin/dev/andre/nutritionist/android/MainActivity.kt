package dev.andre.nutritionist.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dev.andre.nutritionist.android.auth.AuthEvent
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.auth.AuthViewModel
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.android.navigation.AppNavHost
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModel()
    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleAuthRedirect(intent)

        setContent {
            FitAssistantTheme {
                val navController = rememberNavController()
                val startDestination = if (authRepository.isAuthenticated()) {
                    AppDestination.Dashboard.route
                } else {
                    AppDestination.Auth.route
                }

                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    authViewModel = authViewModel,
                    authRepository = authRepository,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthRedirect(intent)
    }

    private fun handleAuthRedirect(intent: Intent?) {
        val data = intent?.data
        if (data != null && (data.scheme == "com.budgetntr.fitassistant") && (data.host == "oauth2redirect")) {
            authViewModel.dispatch(AuthEvent.Ui.AuthResponseReceived(intent))
        }
    }
}
