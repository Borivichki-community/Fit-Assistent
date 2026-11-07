package dev.andre.nutritionist.android.ui.theme

import androidx.compose.runtime.Composable

/**
 * Алиас темы для обратной совместимости.
 */
@Composable
fun NutritionistTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) = FitAssistantTheme(darkTheme = darkTheme, content = content)
