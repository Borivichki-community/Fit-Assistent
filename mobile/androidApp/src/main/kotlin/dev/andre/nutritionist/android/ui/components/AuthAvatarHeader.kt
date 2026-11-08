package dev.andre.nutritionist.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Круглый аватар пользователя для экранов авторизации и регистрации
 * (в точности повторяет скриншоты: белый круг и чёрный силуэт).
 */
@Composable
fun AuthAvatarHeader(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // 1. Голова (залитый черный круг)
            drawCircle(
                color = Color(0xFF17181C),
                radius = w * 0.20f,
                center = Offset(w * 0.5f, h * 0.38f),
            )

            // 2. Плечи (залитый эллипс внизу)
            val path = Path().apply {
                moveTo(w * 0.18f, h * 0.88f)
                cubicTo(
                    w * 0.22f, h * 0.60f,
                    w * 0.78f, h * 0.60f,
                    w * 0.82f, h * 0.88f,
                )
                lineTo(w * 0.82f, h)
                lineTo(w * 0.18f, h)
                close()
            }
            drawPath(path = path, color = Color(0xFF17181C))
        }
    }
}
