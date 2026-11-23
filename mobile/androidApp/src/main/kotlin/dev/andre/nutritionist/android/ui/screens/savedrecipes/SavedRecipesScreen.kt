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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.OfflineBanner
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.screens.EmeraldAccent
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.presentation.savedrecipes.SavedRecipesUiState

/**
 * Главный экран сохранённых и избранных рецептов в стиле Aether Zero-G Luxury.
 * Полностью соответствует screenshots/темная тема/экран рецепты (избранное).png и fitapp_5/code.html.
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
    isOffline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF191F2B))
                            .border(BorderStroke(1.dp, GlassBorderLevel2), CircleShape)
                            .clickable(onClick = onNavigateBack)
                            .testTag("saved_recipes_back_button"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = FitIcons.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldAccent),
                            )
                            Text(
                                text = "FITAPP РЕЦЕПТЫ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                ),
                                color = EmeraldAccent,
                            )
                        }
                        Text(
                            text = "Избранное",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                            ),
                            color = Color.White,
                            modifier = Modifier.testTag("saved_recipes_top_bar_title"),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0E281D))
                        .border(BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)), CircleShape)
                        .clickable(onClick = onGenerateRecipeClicked)
                        .testTag("saved_recipes_ai_gen_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FitIcons.Sparkles,
                        contentDescription = "AI-генератор",
                        tint = EmeraldAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (isOffline) {
                OfflineBanner()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
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
                                color = EmeraldAccent,
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
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldAccent,
                                    contentColor = Color(0xFF091A14),
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

                        val detail = content.selectedRecipeDetail
                        if (detail != null) {
                            SavedRecipeDetailBottomSheet(
                                recipe = detail,
                                onDismiss = onDismissDetail,
                                onAddToDiary = { onAddRecipeToDiary(RecipeId(detail.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
