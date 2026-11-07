package dev.andre.nutritionist.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Обратная совместимость с FullScreenLoading.
 */
@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) = LoadingContent(modifier = modifier)
