package dev.andre.nutritionist.android.ui.screens.savedrecipes

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.screens.CardDarkSurface
import dev.andre.nutritionist.android.ui.screens.EmeraldAccent
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.android.ui.theme.TextMidEmphasis
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipeItemUiModel

/**
 * Карточка сохранённого рецепта в стилистике Aether Zero-G Luxury.
 * Полностью соответствует screenshots/темная тема/экран рецепты (избранное).png и fitapp_5/code.html.
 */
@Composable
fun SavedRecipeCard(
    recipe: SavedRecipeItemUiModel,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("saved_recipe_card_${recipe.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
        border = BorderStroke(1.dp, GlassBorderLevel1),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMidEmphasis,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (recipe.isFavorite) Color(0xFF0E281D) else Color(0xFF191F2B))
                        .border(
                            BorderStroke(
                                1.dp,
                                if (recipe.isFavorite) EmeraldAccent.copy(alpha = 0.4f) else GlassBorderLevel2,
                            ),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("saved_recipe_favorite_${recipe.id}"),
                    ) {
                        Icon(
                            imageVector = if (recipe.isFavorite) FitIcons.BookmarkFilled else FitIcons.Bookmark,
                            contentDescription = if (recipe.isFavorite) "Удалить из избранного" else "В избранное",
                            tint = if (recipe.isFavorite) EmeraldAccent else TextMidEmphasis,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Приём пищи
                Surface(
                    shape = RoundedCornerShape(Spacing.pill),
                    color = Color(0xFF0E281D),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f)),
                ) {
                    Text(
                        text = recipe.mealTypeBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }

                // Калории
                Surface(
                    shape = RoundedCornerShape(Spacing.pill),
                    color = Color(0xFF0E281D),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = FitIcons.Flame,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = recipe.caloriesBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldAccent,
                        )
                    }
                }

                // Бюджет
                Surface(
                    shape = RoundedCornerShape(Spacing.pill),
                    color = Color(0xFF261D0C),
                    border = BorderStroke(1.dp, Color(0xFFE4C277).copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = FitIcons.Wallet,
                            contentDescription = null,
                            tint = Color(0xFFE4C277),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = recipe.budgetBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFE4C277),
                        )
                    }
                }

                // Время готовки
                Surface(
                    shape = RoundedCornerShape(Spacing.pill),
                    color = Color(0xFF1B202A),
                    border = BorderStroke(1.dp, GlassBorderLevel1),
                ) {
                    Text(
                        text = recipe.cookingTimeBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Macro summary row
            Text(
                text = recipe.macroSummaryText,
                style = MaterialTheme.typography.bodySmall,
                color = TextMidEmphasis,
            )
        }
    }
}
