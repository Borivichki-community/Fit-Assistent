package dev.andre.nutritionist.android.ui.screens.savedrecipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.presentation.savedrecipes.MealTypeFilterUiModel
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipeItemUiModel

/**
 * Основной список сохранённых рецептов с фильтрацией и строкой поиска.
 */
@Composable
fun SavedRecipesSuccessContent(
    searchQuery: String,
    mealTypeFilters: List<MealTypeFilterUiModel>,
    recipes: List<SavedRecipeItemUiModel>,
    countText: String,
    onSearchQueryChanged: (String) -> Unit,
    onMealTypeFilterSelected: (MealType?) -> Unit,
    onRecipeClicked: (RecipeId) -> Unit,
    onToggleFavorite: (RecipeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Search & Filter header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = "Поиск по сохранённым блюдам...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = FitIcons.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { onSearchQueryChanged("") },
                            modifier = Modifier.testTag("saved_recipes_search_clear"),
                        ) {
                            Icon(
                                imageVector = FitIcons.Close,
                                contentDescription = "Очистить поиск",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(Spacing.buttonRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                    unfocusedBorderColor = GlassBorderLevel1,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_recipes_search_input"),
            )

            // Meal Types Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_recipes_filters_row"),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                items(mealTypeFilters) { filterChip ->
                    Surface(
                        shape = RoundedCornerShape(Spacing.buttonRadius),
                        color = if (filterChip.isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            1.dp,
                            if (filterChip.isSelected) MaterialTheme.colorScheme.primaryContainer else GlassBorderLevel1,
                        ),
                        modifier = Modifier
                            .clickable { onMealTypeFilterSelected(filterChip.mealType) }
                            .testTag("saved_recipes_filter_${filterChip.title}"),
                    ) {
                        Text(
                            text = filterChip.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (filterChip.isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (filterChip.isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
                        )
                    }
                }
            }

            // Count summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Сохранённые блюда",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("saved_recipes_count_text"),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.extraSmall))

        // Recipes List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("saved_recipes_list"),
            contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            items(recipes, key = { it.id }) { recipe ->
                SavedRecipeCard(
                    recipe = recipe,
                    onClick = { onRecipeClicked(RecipeId(recipe.id)) },
                    onToggleFavorite = { onToggleFavorite(RecipeId(recipe.id)) },
                )
            }
        }
    }
}
