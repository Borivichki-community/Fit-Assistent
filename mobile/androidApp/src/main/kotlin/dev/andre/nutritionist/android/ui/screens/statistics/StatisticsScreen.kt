package dev.andre.nutritionist.android.ui.screens.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.domain.model.AnalyticsPeriod
import dev.andre.nutritionist.presentation.statistics.StatisticsUiState

/**
 * Главный экран аналитики питания и бюджета в стилистике Aether Zero-G Luxury.
 * Полностью stateless-компонент, управляемый исключительно [StatisticsUiState].
 */
@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onPeriodSelected: (AnalyticsPeriod) -> Unit = {},
    onRetryClicked: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
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
            ) {
                Text(
                    text = "Статистика и аналитика",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("statistics_top_bar_title"),
                )
            }
        },
        bottomBar = {
            FitBottomNavigationBar(
                currentDestination = AppDestination.Statistics,
                onDestinationSelected = onDestinationSelected,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val content = state.content) {
                is StatisticsUiState.Content.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("statistics_loading"),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primaryContainer,
                        )
                    }
                }

                is StatisticsUiState.Content.Success -> {
                    StatisticsSuccessContent(
                        selectedPeriod = state.selectedPeriod,
                        model = content.model,
                        onPeriodSelected = onPeriodSelected,
                    )
                }

                is StatisticsUiState.Content.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.medium)
                            .testTag("statistics_error"),
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
                            modifier = Modifier.testTag("statistics_retry_button"),
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
