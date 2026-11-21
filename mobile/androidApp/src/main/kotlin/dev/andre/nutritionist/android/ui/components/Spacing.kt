package dev.andre.nutritionist.android.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Токены отступов и скруглений приложения в дизайн-системе Aether Zero-G Luxury.
 */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp

    // Совместимость с предыдущими наименованиями
    val extraSmall: Dp = xs
    val small: Dp = sm
    val medium: Dp = md
    val large: Dp = lg
    val extraLarge: Dp = xl

    // Геометрия squircle и pill
    val squircleLg: Dp = 24.dp
    val squircleXl: Dp = 32.dp
    val pill: Dp = 9999.dp

    val cardRadius: Dp = squircleLg
    val innerCardRadius: Dp = 20.dp
    val buttonRadius: Dp = pill
    val buttonHeight: Dp = 56.dp
}
