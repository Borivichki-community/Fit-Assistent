package dev.andre.nutritionist.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.android.navigation.AppNavHost
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            FitAssistantTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    startDestination = AppDestination.Dashboard.route,
                )
            }
        }
    }
}
