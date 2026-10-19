package dev.andre.nutritionist.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.andre.nutritionist.android.ui.screens.MainScreen
import dev.andre.nutritionist.android.ui.theme.NutritionistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            NutritionistTheme {
                MainScreen()
            }
        }
    }
}
