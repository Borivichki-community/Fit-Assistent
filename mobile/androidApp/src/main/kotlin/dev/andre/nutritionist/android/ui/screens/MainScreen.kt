package dev.andre.nutritionist.android.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.navigation.AppDestination
import dev.andre.nutritionist.android.ui.components.FitBottomNavigationBar
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.FitTopBar
import dev.andre.nutritionist.android.ui.components.Spacing

/**
 * Главный экран приложения (в точности повторяет screenshots/главный экран.jpg).
 */
@Composable
fun MainScreen(
    onNavigateToProfile: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var clickCount by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FitBottomNavigationBar(
                currentDestination = AppDestination.Dashboard,
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
                .padding(paddingValues)
                .verticalScroll(scrollState),
        ) {
            // Переиспользуемая верхняя плашка FitApp
            FitTopBar(
                title = "FitApp",
                onMenuClick = { /* меню */ },
            )

            // Быстрое действие / интерактивная кнопка
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium),
            ) {
                OutlinedButton(
                    onClick = { clickCount++ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Spacing.buttonRadius),
                ) {
                    Text(text = stringResource(R.string.btn_add_meal_test))
                }
            }
            if (clickCount > 0) {
                Text(
                    text = stringResource(R.string.meals_added_count, clickCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.extraSmall),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // 1. Календарь на неделю
            CalendarCard(
                month = stringResource(R.string.dashboard_month_november_2026),
                selectedDay = 7,
                modifier = Modifier.padding(horizontal = Spacing.medium),
            )

            Spacer(modifier = Modifier.height(Spacing.large))

            // 2. Блок "Мои планы"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.section_my_plans),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlanChip(title = stringResource(R.string.plan_chip_format, 1))
                    PlanChip(title = stringResource(R.string.plan_chip_format, 1))
                    AddPlanButton(onClick = { /* добавить план */ })
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            // 3. Блок "Завтрак"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.section_breakfast),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    shape = RoundedCornerShape(Spacing.cardRadius),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = FitIcons.MealForkKnife,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(36.dp),
                        )

                        Spacer(modifier = Modifier.height(Spacing.medium))

                        Text(
                            text = stringResource(R.string.empty_plan_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(Spacing.small))

                        Text(
                            text = stringResource(R.string.empty_plan_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(Spacing.large))

                        // Кнопка ✦ Сгенерировать
                        Button(
                            onClick = { /* генерация ИИ */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            shape = RoundedCornerShape(Spacing.buttonRadius),
                        ) {
                            Text(
                                text = stringResource(R.string.btn_ai_generate),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.small))

                        // Кнопка Создать план
                        Button(
                            onClick = { /* создать вручную */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            shape = RoundedCornerShape(Spacing.buttonRadius),
                        ) {
                            Text(
                                text = stringResource(R.string.btn_create_plan),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))
        }
    }
}
