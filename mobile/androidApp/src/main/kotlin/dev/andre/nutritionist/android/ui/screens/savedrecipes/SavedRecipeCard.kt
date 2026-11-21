package dev.andre.nutritionist.android.ui.screens.savedrecipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipeItemUiModel

/**
 * Карточка сохранённого рецепта в стилистике Aether Zero-G Luxury.
 */
@Composable
fun SavedRecipeCard(
    recipe: SavedRecipeItemUiModel,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("saved_recipe_card_${recipe.id}"),
        shape = RoundedCornerShape(Spacing.cardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, GlassBorderLevel1),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
        ) {
            // Header Row: Title + Favorite Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(Spacing.extraSmall))
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("saved_recipe_favorite_${recipe.id}"),
                ) {
                    Icon(
                        imageVector = if (recipe.isFavorite) FitIcons.BookmarkFilled else FitIcons.Bookmark,
                        contentDescription = if (recipe.isFavorite) "Удалить из избранного" else "В избранное",
                        tint = if (recipe.isFavorite) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(Spacing.small),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = recipe.mealTypeBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.small, vertical = 2.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(Spacing.small),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = recipe.caloriesBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.small, vertical = 2.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(Spacing.small),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = recipe.budgetBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = Spacing.small, vertical = 2.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(Spacing.small),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = recipe.cookingTimeBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.small, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // Macro summary pill
            Surface(
                shape = RoundedCornerShape(Spacing.extraSmall),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
            ) {
                Text(
                    text = recipe.macroSummaryText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
                )
            }
        }
    }
}
