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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.screens.EmeraldAccent
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.TextMidEmphasis
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.presentation.savedrecipes.MealTypeFilterUiModel
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipeItemUiModel

/**
 * Основной список сохранённых рецептов с фильтрацией и строкой поиска в стиле Aether Zero-G Luxury.
 * Полностью соответствует screenshots/темная тема/экран рецепты (избранное).png и fitapp_5/code.html.
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = "Поиск по сохранённым блюдам...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMidEmphasis,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = FitIcons.Search,
                        contentDescription = null,
                        tint = TextMidEmphasis,
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
                                tint = TextMidEmphasis,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldAccent,
                    unfocusedBorderColor = GlassBorderLevel1,
                    focusedContainerColor = Color(0xFF0B0E14),
                    unfocusedContainerColor = Color(0xFF0B0E14),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_recipes_search_input"),
            )

            // Meal Types Row (Luxury Pills)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_recipes_filters_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(mealTypeFilters) { filterChip ->
                    val isSelected = filterChip.isSelected
                    Surface(
                        shape = RoundedCornerShape(Spacing.pill),
                        color = if (isSelected) EmeraldAccent else Color(0xFF191F2B),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldAccent else GlassBorderLevel1,
                        ),
                        modifier = Modifier
                            .clickable { onMealTypeFilterSelected(filterChip.mealType) }
                            .testTag("saved_recipes_filter_${filterChip.title}"),
                    ) {
                        Text(
                            text = filterChip.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) Color(0xFF091A14) else Color(0xFFE2E8F0),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = EmeraldAccent,
                    modifier = Modifier.testTag("saved_recipes_count_text"),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Recipes List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("saved_recipes_list"),
            contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
