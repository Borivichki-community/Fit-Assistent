package dev.andre.nutritionist.android.ui.screens.airecipe

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
import dev.andre.nutritionist.presentation.airecipe.AiRecipeUiState

/**
 * Главный экран генерации AI-рецептов в стиле Aether Zero-G Luxury.
 * Полностью stateless-компонент, принимающий только [AiRecipeUiState] и лямбды событий.
 */
@Composable
fun AiRecipeScreen(
    state: AiRecipeUiState,
    onQueryChanged: (String) -> Unit = {},
    onCaloriesChanged: (Int) -> Unit = {},
    onBudgetChanged: (Int) -> Unit = {},
    onMealTypeSelected: (MealType) -> Unit = {},
    onDietaryPreferenceSelected: (String) -> Unit = {},
    onGenerateClicked: () -> Unit = {},
    onSaveRecipeClicked: () -> Unit = {},
    onAddRecipeToDiaryClicked: () -> Unit = {},
    onResetClicked: () -> Unit = {},
    onRetryClicked: () -> Unit = {},
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
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("ai_recipe_back_button"),
                ) {
                    Icon(
                        imageVector = FitIcons.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = "AI Шеф-нутрициолог",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("ai_recipe_top_bar_title"),
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val content = state.content) {
                is AiRecipeUiState.Content.Configuring -> {
                    AiRecipeConfiguringContent(
                        prompt = content.prompt,
                        onQueryChanged = onQueryChanged,
                        onCaloriesChanged = onCaloriesChanged,
                        onBudgetChanged = onBudgetChanged,
                        onMealTypeSelected = onMealTypeSelected,
                        onDietaryPreferenceSelected = onDietaryPreferenceSelected,
                        onGenerateClicked = onGenerateClicked,
                    )
                }

                is AiRecipeUiState.Content.Generating -> {
                    AiRecipeGeneratingContent(
                        generating = content.generating,
                    )
                }

                is AiRecipeUiState.Content.Success -> {
                    AiRecipeSuccessContent(
                        recipe = content.recipe,
                        onAddRecipeToDiaryClicked = onAddRecipeToDiaryClicked,
                        onSaveRecipeClicked = onSaveRecipeClicked,
                        onResetClicked = onResetClicked,
                    )
                }

                is AiRecipeUiState.Content.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.medium)
                            .testTag("ai_recipe_error"),
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
                            modifier = Modifier.testTag("ai_recipe_retry_button"),
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
