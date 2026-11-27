package dev.andre.nutritionist.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * Алиас темы для обратной совместимости.
 */
@Composable
fun NutritionistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) = FitAssistantTheme(darkTheme = darkTheme, content = content)
