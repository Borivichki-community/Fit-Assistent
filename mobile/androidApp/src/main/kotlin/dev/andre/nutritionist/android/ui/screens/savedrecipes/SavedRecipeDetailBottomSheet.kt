package dev.andre.nutritionist.android.ui.screens.savedrecipes

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import dev.andre.nutritionist.presentation.airecipe.RecipeResultUiModel

/**
 * Детальная шторка рецепта с макронутриентами, ингредиентами и шагами приготовления.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRecipeDetailBottomSheet(
    recipe: RecipeResultUiModel,
    onDismiss: () -> Unit,
    onAddToDiary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.testTag("saved_recipe_detail_sheet"),
    ) {
        SavedRecipeDetailContent(
            recipe = recipe,
            onDismiss = onDismiss,
            onAddToDiary = onAddToDiary,
        )
    }
}
