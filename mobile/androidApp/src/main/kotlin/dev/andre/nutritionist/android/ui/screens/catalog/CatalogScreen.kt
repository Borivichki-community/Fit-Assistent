package dev.andre.nutritionist.android.ui.screens.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.android.ui.components.FitBottomNavigationBar
import dev.andre.nutritionist.android.ui.components.FitTopBar
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.presentation.catalog.CatalogUiState

/**
 * Главный экран каталога рационов питания в стиле Aether Zero-G Luxury.
 * Полностью stateless-компонент, принимающий [CatalogUiState] и лямбды пользовательских событий.
 */
@Composable
fun CatalogScreen(
    state: CatalogUiState,
    onSearchQueryChanged: (String) -> Unit = {},
    onCategorySelected: (String) -> Unit = {},
    onPlanSelected: (MealPlanId) -> Unit = {},
    onToggleFavorite: (MealPlanId) -> Unit = {},
    onGenerateAiPlanClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FitTopBar(
                title = "Каталог рационов",
                onProfileClick = onNavigateToProfile,
            )
        },
        bottomBar = {
            FitBottomNavigationBar(
                currentDestination = AppDestination.Recipes,
                onDestinationSelected = { destination ->
                    if (destination == AppDestination.Profile) {
                        onNavigateToProfile()
                    } else {
                        onDestinationSelected(destination)
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val content = state.content) {
                CatalogUiState.Content.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("catalog_loading"),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primaryContainer,
                        )
                    }
                }
                is CatalogUiState.Content.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.medium)
                            .testTag("catalog_error"),
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
                            onClick = onRetryClick,
                            shape = RoundedCornerShape(Spacing.buttonRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier.testTag("catalog_retry_button"),
                        ) {
                            Text(
                                text = "Повторить",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
                is CatalogUiState.Content.Success -> {
                    CatalogSuccessContent(
                        data = content.data,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onCategorySelected = onCategorySelected,
                        onPlanSelected = onPlanSelected,
                        onToggleFavorite = onToggleFavorite,
                        onGenerateAiPlanClick = onGenerateAiPlanClick,
                    )
                }
            }
        }
    }
}
