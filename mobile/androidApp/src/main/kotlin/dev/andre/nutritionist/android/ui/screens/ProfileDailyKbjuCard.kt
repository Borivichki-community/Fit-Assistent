package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.FitThemeTokens
import dev.andre.nutritionist.presentation.profile.ProfileUiModel

/**
 * Карточка суточной нормы КБЖУ в стиле fitapp_8.
 */
@Composable
fun ProfileDailyKbjuCard(
    profile: ProfileUiModel,
    modifier: Modifier = Modifier,
) {
    val extendedColors = FitThemeTokens.colors
    val cardShape = RoundedCornerShape(22.dp)
    val shadowElevation = if (extendedColors.isDark) 0.dp else 2.dp

    val caloriesText = profile.caloriesGoalFormatted.ifBlank { profile.caloriesGoalLabel }
    val proteinsText = profile.proteinsGoalFormatted.ifBlank { profile.proteinsGoalLabel }
    val fatsText = profile.fatsGoalFormatted.ifBlank { profile.fatsGoalLabel }
    val carbsText = profile.carbsGoalFormatted.ifBlank { profile.carbsGoalLabel }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, extendedColors.glassBorderLevel1),
        shadowElevation = shadowElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(extendedColors.emeraldAccent),
                    )
                    Text(
                        text = stringResource(R.string.profile_daily_kbju_title),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Text(
                    text = caloriesText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = extendedColors.emeraldAccent,
                )
            }

            HorizontalDivider(color = extendedColors.glassBorderLevel1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.extraSmall),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                MacroTargetTile(
                    label = stringResource(R.string.profile_macro_proteins),
                    value = proteinsText,
                    modifier = Modifier.weight(1f),
                )
                MacroTargetTile(
                    label = stringResource(R.string.profile_macro_fats),
                    value = fatsText,
                    modifier = Modifier.weight(1f),
                )
                MacroTargetTile(
                    label = stringResource(R.string.profile_macro_carbs),
                    value = carbsText,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MacroTargetTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = Spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
