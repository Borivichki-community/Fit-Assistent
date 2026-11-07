package dev.andre.nutritionist.android.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Варианты стиля кнопок на экранах авторизации и регистрации.
 */
enum class AuthButtonStyle {
    PRIMARY_WHITE,
    SECONDARY_DARK,
}

/**
 * Переиспользуемая кнопка действий авторизации (в точности по скриншотам).
 */
@Composable
fun AuthActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AuthButtonStyle = AuthButtonStyle.PRIMARY_WHITE,
    icon: ImageVector? = null,
    iconTint: Color = Color.Unspecified,
    enabled: Boolean = true,
) {
    val containerColor = when (style) {
        AuthButtonStyle.PRIMARY_WHITE -> Color.White
        AuthButtonStyle.SECONDARY_DARK -> Color(0xFF22242A)
    }

    val contentColor = when (style) {
        AuthButtonStyle.PRIMARY_WHITE -> Color(0xFF0E0F12)
        AuthButtonStyle.SECONDARY_DARK -> Color.White
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f),
        ),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (style == AuthButtonStyle.PRIMARY_WHITE) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}
