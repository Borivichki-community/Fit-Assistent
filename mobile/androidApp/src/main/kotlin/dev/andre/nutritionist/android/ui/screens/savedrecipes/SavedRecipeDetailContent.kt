package dev.andre.nutritionist.android.ui.screens.savedrecipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.presentation.airecipe.RecipeResultUiModel

/**
 * Контент детального просмотра рецепта (КБЖУ, ингредиенты, шаги приготовления, действия).
 */
@Composable
fun SavedRecipeDetailContent(
    recipe: RecipeResultUiModel,
    onDismiss: () -> Unit,
    onAddToDiary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.medium)
            .padding(bottom = Spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        // Header: Title & Badges
        Column {
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
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = recipe.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("saved_recipe_detail_title"),
            )

            Spacer(modifier = Modifier.height(Spacing.extraSmall))

            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Macros Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.cardRadius),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, GlassBorderLevel1),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Text(
                    text = "Баланс нутриентов (КБЖУ)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Protein
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Белки",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = recipe.proteinText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { recipe.proteinFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }

                // Fats
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Жиры",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = recipe.fatsText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { recipe.fatsFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }

                // Carbs
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Углеводы",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = recipe.carbsText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { recipe.carbsFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }
            }
        }

        // Ingredients Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.cardRadius),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, GlassBorderLevel1),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Text(
                    text = "Ингредиенты",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                recipe.ingredients.forEach { ingredient ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = FitIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = ingredient.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = ingredient.amount,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Steps Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.cardRadius),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, GlassBorderLevel1),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Text(
                    text = "Приготовление",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                recipe.steps.forEach { step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${step.stepNumber}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            )
                        }
                        Text(
                            text = step.instruction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Button(
                onClick = onAddToDiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.buttonHeight)
                    .testTag("saved_recipe_detail_add_diary_button"),
                shape = RoundedCornerShape(Spacing.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    imageVector = FitIcons.Plus,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "Добавить в дневник",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.buttonHeight)
                    .testTag("saved_recipe_detail_close_button"),
                shape = RoundedCornerShape(Spacing.buttonRadius),
                border = BorderStroke(1.dp, GlassBorderLevel1),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(
                    text = "Закрыть",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
