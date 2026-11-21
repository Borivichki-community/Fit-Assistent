package dev.andre.nutritionist.android.ui.screens.savedrecipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipesUiState

/**
 * Главный экран сохранённых и избранных рецептов в стиле Aether Zero-G Luxury.
 * Полностью stateless-компонент, управляемый исключительно [SavedRecipesUiState].
 */
@Composable
fun SavedRecipesScreen(
    state: SavedRecipesUiState,
    onSearchQueryChanged: (String) -> Unit = {},
    onMealTypeFilterSelected: (MealType?) -> Unit = {},
    onRecipeClicked: (RecipeId) -> Unit = {},
    onToggleFavorite: (RecipeId) -> Unit = {},
    onAddRecipeToDiary: (RecipeId) -> Unit = {},
    onDismissDetail: () -> Unit = {},
    onEmptyActionClicked: () -> Unit = {},
    onRetryClicked: () -> Unit = {},
    onGenerateRecipeClicked: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("saved_recipes_back_button"),
                    ) {
                        Icon(
                            imageVector = FitIcons.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        text = "Сохранённые рецепты",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("saved_recipes_top_bar_title"),
                    )
                }

                IconButton(
                    onClick = onGenerateRecipeClicked,
                    modifier = Modifier.testTag("saved_recipes_ai_gen_button"),
                ) {
                    Icon(
                        imageVector = FitIcons.Sparkles,
                        contentDescription = "AI-генератор",
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val content = state.content) {
                is SavedRecipesUiState.Content.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("saved_recipes_loading"),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primaryContainer,
                        )
                    }
                }

                is SavedRecipesUiState.Content.Empty -> {
                    SavedRecipesEmptyContent(
                        title = content.title,
                        description = content.description,
                        actionTitle = content.actionTitle,
                        onActionClicked = onEmptyActionClicked,
                    )
                }

                is SavedRecipesUiState.Content.Success -> {
                    SavedRecipesSuccessContent(
                        searchQuery = state.searchQuery,
                        mealTypeFilters = state.mealTypeFilters,
                        recipes = content.recipes,
                        countText = content.countText,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onMealTypeFilterSelected = onMealTypeFilterSelected,
                        onRecipeClicked = onRecipeClicked,
                        onToggleFavorite = onToggleFavorite,
                    )

                    val selectedDetail = content.selectedRecipeDetail
                    if (selectedDetail != null) {
                        SavedRecipeDetailBottomSheet(
                            recipe = selectedDetail,
                            onDismiss = onDismissDetail,
                            onAddToDiary = {
                                onAddRecipeToDiary(RecipeId(selectedDetail.id))
                            },
                        )
                    }
                }

                is SavedRecipesUiState.Content.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.medium)
                            .testTag("saved_recipes_error"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = content.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        Button(
                            onClick = onRetryClicked,
                            shape = RoundedCornerShape(Spacing.buttonRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier.testTag("saved_recipes_retry_button"),
                        ) {
                            Text(
                                text = "Повторить",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }
}
