package dev.andre.nutritionist.android.ui.screens.catalog

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
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.android.ui.components.FitBottomNavigationBar
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.OfflineBanner
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.screens.EmeraldAccent
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.presentation.catalog.CatalogUiState

/**
 * Главный экран каталога рационов питания в стиле Aether Zero-G Luxury.
 * Полностью соответствует screenshots/темная тема/Экран рационы (все блюда).png и fitapp_3/code.html.
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
    isOffline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // 1. Верхний заголовок в стиле Luxury Telemetry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                            text = "FITAPP ПИТАНИЕ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = EmeraldAccent,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Блюда",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = Color.White,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF191F2B))
                        .border(BorderStroke(1.dp, GlassBorderLevel2), CircleShape)
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FitIcons.Notifications,
                        contentDescription = "Уведомления",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 9.dp, end = 9.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent),
                    )
                }
            }

            if (isOffline) {
                OfflineBanner()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
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
                                color = EmeraldAccent,
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
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldAccent,
                                    contentColor = Color(0xFF091A14),
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
}
