package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.FitThemeTokens
import dev.andre.nutritionist.domain.model.UserRole
import dev.andre.nutritionist.presentation.profile.ProfileUiModel

/**
 * Карточка профиля пользователя с аватаром, реальным именем, email, ролью и биометрикой.
 */
@Composable
fun ProfileIdentityCard(
    profile: ProfileUiModel,
    onEditDataClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extendedColors = FitThemeTokens.colors
    val cardShape = RoundedCornerShape(26.dp)
    val shadowElevation = if (extendedColors.isDark) 0.dp else 4.dp
    val emeraldAccent = extendedColors.emeraldAccent
    val glowColor = emeraldAccent.copy(
        alpha = if (extendedColors.isDark) 0.14f else 0.10f,
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel2),
        shadowElevation = shadowElevation,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    Box(modifier = Modifier.size(72.dp)) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            emeraldAccent,
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.surfaceContainerHighest,
                                        ),
                                    ),
                                )
                                .padding(2.5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = FitIcons.Profile,
                                    contentDescription = stringResource(R.string.content_avatar),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(emeraldAccent)
                                .border(2.dp, MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                                .clickable(onClick = onEditDataClick),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Edit,
                                contentDescription = stringResource(R.string.profile_edit_data),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            Text(
                                text = profile.displayName.ifBlank { profile.userId.raw },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            RoleBadge(role = profile.userRole)
                        }

                        if (profile.email.isNotBlank()) {
                            Text(
                                text = profile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onEditDataClick)
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.profile_edit_data),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = emeraldAccent,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = FitIcons.ChevronRight,
                                contentDescription = null,
                                tint = emeraldAccent,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                val biometricsText = when {
                    profile.heightLabel.isNotBlank() && profile.genderLabel.isNotBlank() ->
                        stringResource(
                            R.string.profile_biometrics_format,
                            profile.heightLabel,
                            profile.genderLabel,
                        )
                    profile.heightLabel.isNotBlank() -> profile.heightLabel
                    profile.genderLabel.isNotBlank() -> profile.genderLabel
                    else -> ""
                }

                if (biometricsText.isNotBlank()) {
                    HorizontalDivider(color = extendedColors.glassBorderLevel1)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Info,
                                contentDescription = null,
                                tint = emeraldAccent,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Text(
                            text = biometricsText,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(
    role: UserRole?,
    modifier: Modifier = Modifier,
) {
    val badgeConfig = when (role) {
        UserRole.CLIENT -> Triple(
            stringResource(R.string.role_badge_client),
            FitThemeTokens.colors.emeraldAccent,
            FitThemeTokens.colors.emeraldAccent.copy(alpha = 0.14f),
        )
        UserRole.COACH -> Triple(
            stringResource(R.string.role_badge_coach),
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
        )
        null -> null
    } ?: return

    val (label, contentColor, backgroundColor) = badgeConfig
    val badgeShape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .clip(badgeShape)
            .background(backgroundColor)
            .border(1.dp, contentColor.copy(alpha = 0.35f), badgeShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp,
            ),
            color = contentColor,
            maxLines = 1,
        )
    }
}
